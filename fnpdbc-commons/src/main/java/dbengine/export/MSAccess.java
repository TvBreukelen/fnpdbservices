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
