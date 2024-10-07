package dbengine.export;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.SqlRemote;
import dbengine.utils.DatabaseHelper;

public class PostgreSQL extends SqlRemote {
	public PostgreSQL(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.POSTGRESQL);
	}

	static {
		reservedWords.clear();
		reservedWords.add("ABS");
		reservedWords.add("ACOS");
		reservedWords.add("ALL");
		reservedWords.add("ANALYSE");
		reservedWords.add("ANALYZE");
		reservedWords.add("AND");
		reservedWords.add("ANY");
		reservedWords.add("ARRAY");
		reservedWords.add("AS");
		reservedWords.add("ASC");
		reservedWords.add("ASIN");
		reservedWords.add("ASYMMETRIC");
		reservedWords.add("AT");
		reservedWords.add("AUTHORIZATION");
		reservedWords.add("AVG");
		reservedWords.add("BINARY");
		reservedWords.add("BOTH");
		reservedWords.add("CASE");
		reservedWords.add("CAST");
		reservedWords.add("CHECK");
		reservedWords.add("COLLATE");
		reservedWords.add("COLLATION");
		reservedWords.add("COLUMN");
		reservedWords.add("CONCURRENTLY");
		reservedWords.add("CONSTRAINT");
		reservedWords.add("CREATE");
		reservedWords.add("CROSS");
		reservedWords.add("CURRENT_CATALOG");
		reservedWords.add("CURRENT_DATE");
		reservedWords.add("CURRENT_SCHEMA");
		reservedWords.add("CURRENT_TIME");
		reservedWords.add("CURRENT_TIMESTAMP");
		reservedWords.add("CURRENT_USER");
		reservedWords.add("DEFAULT");
		reservedWords.add("DEFERRABLE");
		reservedWords.add("DESC");
		reservedWords.add("DISTINCT");
		reservedWords.add("DO");
		reservedWords.add("ELSE");
		reservedWords.add("END");
		reservedWords.add("EXCEPT");
		reservedWords.add("FETCH");
		reservedWords.add("FOR");
		reservedWords.add("FOREIGN");
		reservedWords.add("FREEZE");
		reservedWords.add("FROM");
		reservedWords.add("FULL");
		reservedWords.add("GET");
		reservedWords.add("GRANT");
		reservedWords.add("GROUP");
		reservedWords.add("HAVING");
		reservedWords.add("ILIKE");
		reservedWords.add("IN");
		reservedWords.add("INITIALLY");
		reservedWords.add("INNER");
		reservedWords.add("INTERSECT");
		reservedWords.add("INTO");
		reservedWords.add("IS");
		reservedWords.add("ISNULL");
		reservedWords.add("JOIN");
		reservedWords.add("LATERAL");
		reservedWords.add("LEADING");
		reservedWords.add("LEFT");
		reservedWords.add("LIKE");
		reservedWords.add("LIMIT");
		reservedWords.add("MAX");
		reservedWords.add("MIN");
		reservedWords.add("MODULE");
		reservedWords.add("NATURAL");
		reservedWords.add("NULL");
		reservedWords.add("OFFSET");
		reservedWords.add("ON");
		reservedWords.add("ONLY");
		reservedWords.add("OPEN");
		reservedWords.add("OPERATOR");
		reservedWords.add("OR");
		reservedWords.add("ORDER");
		reservedWords.add("OUTER");
		reservedWords.add("OVERLAPS");
		reservedWords.add("PLACING");
		reservedWords.add("PRIMARY");
		reservedWords.add("REFERENCES");
		reservedWords.add("RIGHT");
		reservedWords.add("SELECT");
		reservedWords.add("SESSION_USER");
		reservedWords.add("SIMILAR");
		reservedWords.add("SOME");
		reservedWords.add("SQLSTATE");
		reservedWords.add("SUM");
		reservedWords.add("SYMMETRIC");
		reservedWords.add("SYSTEM_USER");
		reservedWords.add("TABLE");
		reservedWords.add("TABLESAMPLE");
		reservedWords.add("THEN");
		reservedWords.add("TO");
		reservedWords.add("TRAILING");
		reservedWords.add("TRANSLATE");
		reservedWords.add("TRANSLATION");
		reservedWords.add("TRUE");
		reservedWords.add("UNION");
		reservedWords.add("UNIQUE");
		reservedWords.add("USER");
		reservedWords.add("USING");
		reservedWords.add("WHEN");
		reservedWords.add("WHENEVER");
		reservedWords.add("WHERE");
		reservedWords.add("WINDOW");
		reservedWords.add("WITH");
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
		StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getSqlFieldName(table))
				.append(" (\n");
		StringBuilder pkBuf = new StringBuilder();

		fields.forEach(field -> {
			String fieldName = getSqlFieldName(field.getFieldHeader());
			buf.append(fieldName);

			if (field.isOutputAsText()) {
				getTextOrVarchar(field.getSize(), buf);
			} else {
				switch (field.getFieldType()) {
				case BOOLEAN:
					buf.append(" BOOLEAN");
					break;
				case DATE:
					buf.append(" DATE");
					break;
				case FLOAT:
					buf.append(" NUMERIC(").append(field.getSize()).append(",").append(field.getDecimalPoint())
							.append(")");
					break;
				case IMAGE, THUMBNAIL:
					buf.append(" BYTEA");
					break;
				case MEMO:
					buf.append(" TEXT");
					break;
				case NUMBER:
					buf.append(field.isAutoIncrement() ? " SERIAL" : " INTEGER");
					break;
				case TIME:
					buf.append(" TIME");
					break;
				case TIMESTAMP:
					buf.append(" TIMESTAMP");
					break;
				default:
					getTextOrVarchar(field.getSize(), buf);
					break;
				}
			}

			if (field.isPrimaryKey()) {
				pkBuf.append(fieldName).append(",");
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
		StringBuilder buf = new StringBuilder("INSERT INTO ").append(getSqlFieldName(myPref.getDatabaseName()))
				.append(" (");
		dbInfo2Write.forEach(field -> buf.append(getSqlFieldName(field.getFieldHeader())).append(","));

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
				buf.append(" ON CONFLICT (").append(getSqlFieldName(pk)).append(") DO NOTHING");
				break;
			case Profiles.ON_CONFLICT_REPLACE:
				buf.append(" ON CONFLICT (").append(getSqlFieldName(pk)).append(") DO UPDATE\nSET ");
				dbInfo2Write.forEach(field -> {
					if (!pk.equals(field.getFieldHeader())) {
						String sqlColumn = getSqlFieldName(field.getFieldHeader());
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
		if (!isConnected) {
			return;
		}

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
