package dbengine.export;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.SqlDB;
import dbengine.utils.DatabaseHelper;
import net.ucanaccess.jdbc.UcanaccessDriver;

public class MSAccess extends SqlDB {

	public MSAccess(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.ACCESS);
	}

	static {
		reservedWords.clear();
		reservedWords.add("ADD");
		reservedWords.add("ALL");
		reservedWords.add("ALTER");
		reservedWords.add("AND");
		reservedWords.add("ANY");
		reservedWords.add("AS");
		reservedWords.add("ASC");
		reservedWords.add("AUTOINCREMENT");
		reservedWords.add("BETWEEN");
		reservedWords.add("BINARY");
		reservedWords.add("BIT");
		reservedWords.add("BOOLEAN");
		reservedWords.add("BY");
		reservedWords.add("BYTE");
		reservedWords.add("CHAR");
		reservedWords.add("CHARACTER");
		reservedWords.add("COLUMN");
		reservedWords.add("CONSTRAINT");
		reservedWords.add("COUNTER");
		reservedWords.add("CREATE");
		reservedWords.add("CURRENCY");
		reservedWords.add("DATABASE");
		reservedWords.add("DATE");
		reservedWords.add("DATETIME");
		reservedWords.add("DELETE");
		reservedWords.add("DESC");
		reservedWords.add("DISALLOW");
		reservedWords.add("DISTINCT");
		reservedWords.add("DISTINCTROW");
		reservedWords.add("DOUBLE");
		reservedWords.add("DROP");
		reservedWords.add("EXISTS");
		reservedWords.add("FALSE");
		reservedWords.add("FLOAT");
		reservedWords.add("FOREIGN");
		reservedWords.add("FROM");
		reservedWords.add("FUNCTION");
		reservedWords.add("GENERAL");
		reservedWords.add("GROUP");
		reservedWords.add("GUID");
		reservedWords.add("HAVING");
		reservedWords.add("IGNORE");
		reservedWords.add("IN");
		reservedWords.add("INDEX");
		reservedWords.add("INNER");
		reservedWords.add("INSERT");
		reservedWords.add("INT");
		reservedWords.add("INTEGER");
		reservedWords.add("INTO");
		reservedWords.add("IS");
		reservedWords.add("JOIN");
		reservedWords.add("KEY");
		reservedWords.add("LEFT");
		reservedWords.add("LIKE");
		reservedWords.add("LOGICAL");
		reservedWords.add("LONG");
		reservedWords.add("LONGBINARY");
		reservedWords.add("LONGTEXT");
		reservedWords.add("MEMO");
		reservedWords.add("MONEY");
		reservedWords.add("NAME");
		reservedWords.add("NO");
		reservedWords.add("NULL");
		reservedWords.add("NUMBER");
		reservedWords.add("NUMERIC");
		reservedWords.add("OLEOBJECT");
		reservedWords.add("OFF");
		reservedWords.add("ON");
		reservedWords.add("OPTION");
		reservedWords.add("OR");
		reservedWords.add("ORDER");
		reservedWords.add("OWNERACCESS");
		reservedWords.add("PARAMETERS");
		reservedWords.add("PERCENT");
		reservedWords.add("PIVOT");
		reservedWords.add("PRIMARY");
		reservedWords.add("PROCEDURE");
		reservedWords.add("REAL");
		reservedWords.add("REFERENCES");
		reservedWords.add("RIGHT");
		reservedWords.add("SCREEN");
		reservedWords.add("SECTION");
		reservedWords.add("SELECT");
		reservedWords.add("SET");
		reservedWords.add("SHORT");
		reservedWords.add("SINGLE");
		reservedWords.add("SMALLINT");
		reservedWords.add("SOME");
		reservedWords.add("SQL");
		reservedWords.add("STRING");
		reservedWords.add("TABLE");
		reservedWords.add("TEXT");
		reservedWords.add("TIME");
		reservedWords.add("TIMESTAMP");
		reservedWords.add("TOP");
		reservedWords.add("TRANSFORM");
		reservedWords.add("TRUE");
		reservedWords.add("UNION");
		reservedWords.add("UNIQUE");
		reservedWords.add("UPDATE");
		reservedWords.add("USER");
		reservedWords.add("VALUE");
		reservedWords.add("VALUES");
		reservedWords.add("VARBINARY");
		reservedWords.add("VARCHAR");
		reservedWords.add("VERSION");
		reservedWords.add("WHERE");
		reservedWords.add("WITH");
		reservedWords.add("YES");
		reservedWords.add("YESNO");
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Close any existing database connection
		if (isConnected) {
			closeFile();
		}

		String db = myHelper.getDatabase();
		StringBuilder url = new StringBuilder(UcanaccessDriver.URL_PREFIX);
		url.append(db);

		if (!(isInputFile && General.existFile(db))) {
			url.append(";newdatabaseversion=");
			url.append(db.toLowerCase().endsWith(".accdb") ? "V2010" : "V2003");
		}

		connection = DriverManager.getConnection(url.toString());
		isConnected = true;
	}

	@Override
	public String buildTableString(String table, List<FieldDefinition> fields) {
		StringBuilder buf = new StringBuilder("CREATE TABLE ").append(getSqlFieldName(table)).append(" (\n");

		fields.forEach(field -> {
			String fieldName = getSqlFieldName(field.getFieldHeader());
			buf.append(fieldName);

			if (field.isOutputAsText()) {
				getText(field.getSize(), buf);
			} else {
				switch (field.getFieldType()) {
				case BOOLEAN:
					buf.append(" YESNO");
					break;
				case FLOAT:
					buf.append(" FLOAT");
					break;
				case NUMBER:
					if (field.isAutoIncrement()) {
						buf.append(" COUNTER");
					} else {
						buf.append(" INTEGER");
					}
					break;
				case IMAGE, THUMBNAIL:
					buf.append(" BLOB");
					break;
				case TIMESTAMP:
					buf.append(" DATETIME");
					break;
				default:
					getText(field.getSize(), buf);
					break;
				}
			}

			if (field.isPrimaryKey()) {
				buf.append(" PRIMARY KEY");
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
		buf.append(");");
		return buf.toString();
	}

	private void getText(int maxSize, StringBuilder buf) {
		if (maxSize < 256 || maxSize == 0) {
			buf.append(" TEXT(").append(maxSize).append(")");
		} else {
			buf.append(" MEMO");
		}
	}

	@Override
	protected void createPreparedStatement() throws SQLException {
		String table = getSqlFieldName(myPref.getDatabaseName());

		// Verify is we insert an auto increment column
		Optional<FieldDefinition> optIncr = dbInfo2Write.stream().filter(FieldDefinition::isPrimaryKey).findAny();

		if (optIncr.isPresent()) {
			executeStatement("DISABLE AUTOINCREMENT ON " + table);
		} else {
			executeStatement("ENABLE AUTOINCREMENT ON " + table);
		}

		int maxFields = dbInfo2Write.size();
		StringBuilder buf = new StringBuilder("INSERT INTO ");

		buf.append(table).append(" (");
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
