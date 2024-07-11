package dbengine.export;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.SqlRemote;
import dbengine.utils.DatabaseHelper;

public class Firebird extends SqlRemote {

	public Firebird(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.FIREBIRD);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Close any existing database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain a new database connection
		Properties info = new Properties();
		info.put("user", myHelper.getUser());
		info.put("password", General.decryptPassword(myHelper.getPassword()));
		info.put("charSet", "UTF8");

		// Try to obtain the database connection
		StringBuilder url = new StringBuilder();
		url.append("jdbc:firebirdsql://").append(myHelper.getHost()).append(":").append(myHelper.getPort()).append("/")
				.append(myHelper.getDatabase());
		connection = DriverManager.getConnection(url.toString(), info);
		isConnected = true;
	}

	@Override
	public String buildTableString(String table, List<FieldDefinition> fields) {
		StringBuilder buf = new StringBuilder("CREATE TABLE ").append(getSqlFieldName(table)).append(" (\n");
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
				case NUMBER:
					buf.append(" INT");
					break;
				case FLOAT:
					buf.append(" REAL");
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
		StringBuilder buf = new StringBuilder("UPDATE OR INSERT INTO ");

		buf.append(getSqlFieldName(myPref.getDatabaseName())).append(" (");
		dbInfo2Write.forEach(field -> buf.append(getSqlFieldName(field.getFieldHeader())).append(","));

		buf.deleteCharAt(buf.length() - 1);
		buf.append(")\n");
		buf.append("VALUES (");

		for (int i = 0; i < maxFields; i++) {
			buf.append("?, ");
		}
		buf.delete(buf.lastIndexOf(","), buf.length());
		buf.append(")");

		prepStmt = connection.prepareStatement(buf.toString());
		connection.setAutoCommit(false);
	}

}
