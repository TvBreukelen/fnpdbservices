package dbengine.export;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.SqlRemote;

public class PostgreSQL extends SqlRemote {
	public PostgreSQL(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// close an existing database connection
		if (isConnected) {
			closeFile();
		}

		if (myHelper.isUseSsh()) {
			getSshSession();
		}

		// Try to obtain the database connection
		Properties info = new Properties();
		info.setProperty("user", myHelper.getUser());
		info.setProperty("password", General.decryptPassword(myHelper.getPassword()));

		if (myHelper.isUseSsl()) {
			StringBuilder options = new StringBuilder();
			info.put("ssl", "true");
			addOption("sslcert", myHelper.getServerSslCert(), options);
			addOption("sslkey", myHelper.getKeyStore(), options);
			addOption("sslpassword", General.decryptPassword(myHelper.getKeyStorePassword()), options);
			addOption("sslrootcert", myHelper.getServerSslCaCert(), options);
			addOption("sslmode", myHelper.getSslMode(), options);

			if (options.length() > 0) {
				options.delete(0, 1); // remove first comma
				info.setProperty("options", options.toString());
			}
		}

		StringBuilder url = new StringBuilder("jdbc:postgresql://").append(myHelper.getHost()).append(":")
				.append(myHelper.isUseSsh() ? assignedPort : myHelper.getPort()).append("/")
				.append(myHelper.getDatabase().equalsIgnoreCase("public") ? General.EMPTY_STRING
						: myHelper.getDatabase());

		connection = DriverManager.getConnection(url.toString(), info);
		isConnected = true;
	}

	private void addOption(String key, String value, StringBuilder options) {
		if (StringUtils.isNotBlank(value)) {
			options.append(",").append(key).append("=").append(value);
		}
	}

	@Override
	public String buildTableString(String table, List<FieldDefinition> fields) {
		StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS \"").append(table).append("\" (\n");
		StringBuilder pkBuf = new StringBuilder();

		fields.forEach(field -> {
			String fieldName = getSqlFieldName(field.getFieldHeader(), true);
			buf.append(fieldName);

			if (field.isOutputAsText()) {
				buf.append(" VARCHAR(").append(field.getSize()).append(")");
			} else {
				switch (field.getFieldType()) {
				case BOOLEAN:
					buf.append(" BOOLEAN");
					break;
				case DATE:
					buf.append(" DATE");
					break;
				case IMAGE, THUMBNAIL:
					buf.append(" BLOB");
					break;
				case MEMO:
					buf.append(" MEMO");
					break;
				case TIME:
					buf.append(" TIME");
					break;
				case TIMESTAMP:
					buf.append(" TIMESTAMP");
					break;
				case NUMBER:
					buf.append(" INTEGER");
					break;
				case FLOAT:
					buf.append(" NUMERIC(").append(field.getSize()).append(",").append(field.getDecimalPoint())
							.append(")");
					break;
				default:
					buf.append(" VARCHAR(").append(field.getSize()).append(")");
					break;
				}
			}

			if (field.isPrimaryKey()) {
				pkBuf.append(fieldName).append(",");
				if (field.isAutoIncrement()) {
					buf.append(" AUTO INCREMENT");
				}
			}

			if (field.isNotNullable()) {
				buf.append(" NOT NULL");
			}

			if (field.isUnique()) {
				buf.append(" UNIQUE");
			}

			buf.append(",\n");
		});

		buf.delete(buf.lastIndexOf(","), buf.length());

		if (!pkBuf.isEmpty()) {
			pkBuf.delete(pkBuf.length() - 1, pkBuf.length());
			buf.append(",\nPRIMARY KEY (").append(pkBuf).append(")");
		}

		buf.append(");");
		return buf.toString();
	}

	@Override
	protected void createPreparedStatement() throws SQLException {
		int maxFields = dbInfo2Write.size();
		StringBuilder buf = new StringBuilder("INSERT INTO ").append(myPref.getDatabaseName()).append(" (");
		dbInfo2Write.forEach(field -> buf.append(getSqlFieldName(field.getFieldHeader(), true)).append(","));

		buf.deleteCharAt(buf.length() - 1);
		buf.append(")\n");
		buf.append("VALUES (");

		for (int i = 0; i < maxFields; i++) {
			buf.append("?, ");
		}
		buf.delete(buf.lastIndexOf(","), buf.length());
		buf.append(")");

		// Check for a primary key
		Optional<FieldDefinition> pkOpt = dbInfo2Write.stream().filter(FieldDefinition::isPrimaryKey).findFirst();
		if (pkOpt.isPresent()) {
			String pk = pkOpt.get().getFieldHeader();
			switch (myPref.getOnConflict()) {
			case Profiles.ON_CONFLICT_IGNORE:
				buf.append(" ON CONFLICT (").append(getSqlFieldName(pk, true)).append(") DO NOTHING");
				break;
			case Profiles.ON_CONFLICT_REPLACE:
				buf.append(" ON CONFLICT (").append(getSqlFieldName(pk, true)).append(") DO UPDATE\nSET ");
				dbInfo2Write.forEach(field -> {
					if (!pk.equals(field.getFieldHeader())) {
						String sqlColumn = getSqlFieldName(field.getFieldHeader(), true);
						buf.append(sqlColumn).append(" =  EXCLUDED.").append(sqlColumn).append(",\n");
					}
				});
				buf.delete(buf.length() - 2, buf.length());
				buf.append(";");
				break;
			default:
				break;
			}

		}
		prepStmt = connection.prepareStatement(buf.toString());
		connection.setAutoCommit(false);
	}

	@Override
	public void closeData() throws Exception {
		try {
			connection.commit();
		} catch (SQLException ex) {
			connection.rollback();
			throwInsertException(ex);
		} finally {
			super.closeData();
		}
	}

}
