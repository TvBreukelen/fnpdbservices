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

public class MariaDB extends SqlRemote {
	private Properties info;

	public MariaDB(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.MARIADB);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Close any existing database connection
		if (isConnected) {
			closeFile();
		}

		if (myHelper.isUseSsh()) {
			getSshSession();
		}

		// Try to obtain a new database connection
		info = new Properties();
		info.put("user", myHelper.getUser());
		info.put("password", General.decryptPassword(myHelper.getPassword()));

		if (myHelper.isUseSsl()) {
			info.put("ssl", "true");
			addToProperies("sslMode", myHelper.getSslMode());
			addToProperies("serverSslCert", myHelper.getServerSslCert());
			addToProperies("keyStore", myHelper.getKeyStore());
			addToProperies("keyStorePassword", General.decryptPassword(myHelper.getKeyStorePassword()));
			addToProperies("keyStoreType", getKeyStoreType());
		}

		StringBuilder url = new StringBuilder("jdbc:mariadb://").append(myHelper.getHost()).append(":")
				.append(myHelper.isUseSsh() ? assignedPort : myHelper.getPort()).append("/")
				.append(myHelper.getDatabase());

		connection = DriverManager.getConnection(url.toString(), info);
		isConnected = true;
	}

	private void addToProperies(String key, String value) {
		if (StringUtils.isNotBlank(value)) {
			info.put(key, value);
		}
	}

	private String getKeyStoreType() {
		if (StringUtils.isNotBlank(myHelper.getKeyStore())) {
			return myHelper.getKeyStore().toLowerCase().endsWith(".jks") ? "JKS" : "PKCS12";
		}
		return General.EMPTY_STRING;
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
					buf.append(" BOOL");
					break;
				case DATE:
					buf.append(" DATE");
					break;
				case FLOAT:
					buf.append(" DECIMAL(").append(field.getSize()).append(",").append(field.getDecimalPoint())
							.append(")");
					break;
				case NUMBER:
					buf.append(field.isAutoIncrement() ? " INT AUTO_INCREMENT" : " INT");
					break;
				case TEXT, MEMO, IMAGE, THUMBNAIL:
					buf.append(" TEXT");
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
		prepReplace.clear();
		int maxFields = dbInfo2Write.size();
		boolean isIgnore = myPref.getOnConflict() == Profiles.ON_CONFLICT_IGNORE;
		boolean isReplace = myPref.getOnConflict() == Profiles.ON_CONFLICT_REPLACE;

		StringBuilder buf = new StringBuilder("INSERT ");
		if (isIgnore) {
			buf.append("IGNORE ");
			isReplace = false;
		}

		buf.append("INTO ").append(myPref.getDatabaseName()).append(" (");
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
		if (isReplace && pkOpt.isPresent()) {
			String pk = pkOpt.get().getFieldHeader();
			buf.append(" ON DUPLICATE KEY UPDATE\n");
			dbInfo2Write.forEach(field -> {
				if (!pk.equals(field.getFieldHeader())) {
					prepReplace.add(field);
					String sqlColumn = getSqlFieldName(field.getFieldHeader());
					buf.append(sqlColumn).append(" = ?").append(",\n");
				}
			});
			buf.delete(buf.length() - 2, buf.length());
			buf.append(";");
		}
		prepStmt = connection.prepareStatement(buf.toString());
		connection.setAutoCommit(false);
	}

}