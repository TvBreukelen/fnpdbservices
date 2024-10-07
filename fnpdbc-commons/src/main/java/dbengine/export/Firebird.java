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

	static {
		reservedWords.clear();
		reservedWords.add("ADD");
		reservedWords.add("ADMIN");
		reservedWords.add("ALL");
		reservedWords.add("ALTER");
		reservedWords.add("AND");
		reservedWords.add("ANY");
		reservedWords.add("AS");
		reservedWords.add("AT");
		reservedWords.add("AVG");
		reservedWords.add("BEGIN");
		reservedWords.add("BETWEEN");
		reservedWords.add("BIGINT");
		reservedWords.add("BIT_LENGTH");
		reservedWords.add("BLOB");
		reservedWords.add("BOTH");
		reservedWords.add("BY");
		reservedWords.add("CASE");
		reservedWords.add("CAST");
		reservedWords.add("CHAR");
		reservedWords.add("CHARACTER");
		reservedWords.add("CHARACTER_LENGTH");
		reservedWords.add("CHAR_LENGTH");
		reservedWords.add("CHECK");
		reservedWords.add("CLOSE");
		reservedWords.add("COLLATE");
		reservedWords.add("COLUMN");
		reservedWords.add("COMMIT");
		reservedWords.add("CONNECT");
		reservedWords.add("CONSTRAINT");
		reservedWords.add("COUNT");
		reservedWords.add("CREATE");
		reservedWords.add("CROSS");
		reservedWords.add("CURRENT");
		reservedWords.add("CURRENT_CONNECTION");
		reservedWords.add("CURRENT_DATE");
		reservedWords.add("CURRENT_ROLE");
		reservedWords.add("CURRENT_TIME");
		reservedWords.add("CURRENT_TIMESTAMP");
		reservedWords.add("CURRENT_TRANSACTION");
		reservedWords.add("CURRENT_USER");
		reservedWords.add("CURSOR");
		reservedWords.add("DATE");
		reservedWords.add("DAY");
		reservedWords.add("DEC");
		reservedWords.add("DECIMAL");
		reservedWords.add("DECLARE");
		reservedWords.add("DEFAULT");
		reservedWords.add("DELETE");
		reservedWords.add("DELETING");
		reservedWords.add("DISCONNECT");
		reservedWords.add("DISTINCT");
		reservedWords.add("DOUBLE");
		reservedWords.add("DROP");
		reservedWords.add("ELSE");
		reservedWords.add("END");
		reservedWords.add("ESCAPE");
		reservedWords.add("EXECUTE");
		reservedWords.add("EXISTS");
		reservedWords.add("EXTERNAL");
		reservedWords.add("EXTRACT");
		reservedWords.add("FETCH");
		reservedWords.add("FILTER");
		reservedWords.add("FLOAT");
		reservedWords.add("FOR");
		reservedWords.add("FOREIGN");
		reservedWords.add("FROM");
		reservedWords.add("FULL");
		reservedWords.add("FUNCTION");
		reservedWords.add("GDSCODE");
		reservedWords.add("GLOBAL");
		reservedWords.add("GRANT");
		reservedWords.add("GROUP");
		reservedWords.add("HAVING");
		reservedWords.add("HOUR");
		reservedWords.add("IN");
		reservedWords.add("INDEX");
		reservedWords.add("INNER");
		reservedWords.add("INSENSITIVE");
		reservedWords.add("INSERT");
		reservedWords.add("INSERTING");
		reservedWords.add("INT");
		reservedWords.add("INTEGER");
		reservedWords.add("INTO");
		reservedWords.add("IS");
		reservedWords.add("JOIN");
		reservedWords.add("LEADING");
		reservedWords.add("LEFT");
		reservedWords.add("LIKE");
		reservedWords.add("LONG");
		reservedWords.add("LOWER");
		reservedWords.add("MAX");
		reservedWords.add("MAXIMUM_SEGMENT");
		reservedWords.add("MERGE");
		reservedWords.add("MIN");
		reservedWords.add("MINUTE");
		reservedWords.add("MONTH");
		reservedWords.add("NATIONAL");
		reservedWords.add("NATURAL");
		reservedWords.add("NCHAR");
		reservedWords.add("NO");
		reservedWords.add("NOT");
		reservedWords.add("NULL");
		reservedWords.add("NUMERIC");
		reservedWords.add("OCTET_LENGTH");
		reservedWords.add("OF");
		reservedWords.add("ON");
		reservedWords.add("ONLY");
		reservedWords.add("OPEN");
		reservedWords.add("OR");
		reservedWords.add("ORDER");
		reservedWords.add("OUTER");
		reservedWords.add("PARAMETER");
		reservedWords.add("PLAN");
		reservedWords.add("POSITION");
		reservedWords.add("POST_EVENT");
		reservedWords.add("PRECISION");
		reservedWords.add("PRIMARY");
		reservedWords.add("PROCEDURE");
		reservedWords.add("RDB$DB_KEY");
		reservedWords.add("REAL");
		reservedWords.add("RECORD_VERSION");
		reservedWords.add("RECREATE");
		reservedWords.add("RECURSIVE");
		reservedWords.add("REFERENCES");
		reservedWords.add("RELEASE");
		reservedWords.add("RETURNING_VALUES");
		reservedWords.add("RETURNS");
		reservedWords.add("REVOKE");
		reservedWords.add("RIGHT");
		reservedWords.add("ROLLBACK");
		reservedWords.add("ROWS");
		reservedWords.add("ROW_COUNT");
		reservedWords.add("SAVEPOINT");
		reservedWords.add("SECOND");
		reservedWords.add("SELECT");
		reservedWords.add("SENSITIVE");
		reservedWords.add("SET");
		reservedWords.add("SIMILAR");
		reservedWords.add("SMALLINT");
		reservedWords.add("SOME");
		reservedWords.add("SQLCODE");
		reservedWords.add("SQLSTATE");
		reservedWords.add("START");
		reservedWords.add("SUM");
		reservedWords.add("TABLE");
		reservedWords.add("THEN");
		reservedWords.add("TIME");
		reservedWords.add("TIMESTAMP");
		reservedWords.add("TO");
		reservedWords.add("TRAILING");
		reservedWords.add("TRIGGER");
		reservedWords.add("TRIM");
		reservedWords.add("UNION");
		reservedWords.add("UNIQUE");
		reservedWords.add("UPDATE");
		reservedWords.add("UPDATING");
		reservedWords.add("UPPER");
		reservedWords.add("USER");
		reservedWords.add("USING");
		reservedWords.add("VALUE");
		reservedWords.add("VALUES");
		reservedWords.add("VARCHAR");
		reservedWords.add("VARIABLE");
		reservedWords.add("VARYING");
		reservedWords.add("VIEW");
		reservedWords.add("WHEN");
		reservedWords.add("WHERE");
		reservedWords.add("WHILE");
		reservedWords.add("WITH");
		reservedWords.add("YEAR");
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
