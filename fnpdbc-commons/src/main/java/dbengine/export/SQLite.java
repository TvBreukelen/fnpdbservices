package dbengine.export;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.sqlite.SQLiteException;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.IConvert;
import dbengine.SqlDB;

public class SQLite extends SqlDB implements IConvert {
	private PreparedStatement prepStmt;
	private int currentRecord;

	public SQLite(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Try to obtain the database connection
		if (isConnected) {
			closeFile();
		}

		verifyDbHeader();

		// Try to obtain the database connection
		StringBuilder url = new StringBuilder();
		url.append("jdbc:sqlite:");
		url.append(getDbFile());

		connection = DriverManager.getConnection(url.toString());
		isConnected = true;
	}

	private void verifyDbHeader() throws IOException, FNProgException {
		if (!isInputFile && !General.existFile(getDbFile())) {
			return;
		}

		String header = null;
		ExportFile dbFile = isInputFile ? myImportFile : myExportFile;

		try (RandomAccessFile raf = new RandomAccessFile(getDbFile(), "r")) {
			FileChannel channel = raf.getChannel();
			int len = dbFile.getDbType().length();

			if (channel.size() > len) {
				byte[] byteArray = new byte[len];
				raf.read(byteArray);
				header = new String(byteArray);
			}

			channel.close();
		}

		if (header != null && !header.equals(dbFile.getDbType())) {
			throw FNProgException.getException("invalidDatabaseID", getDbFile(), dbFile.getName(), dbFile.getDbType(),
					header);
		}
	}

	@Override
	protected String getPaginationSqlString() {
		StringBuilder b = new StringBuilder(sqlQuery);
		if (myPref.getSqlSelectLimit() > 0) {
			b.append("\nLIMIT ").append(myPref.getSqlSelectLimit()).append("\nOFFSET ").append(offset);
		}
		return b.toString();
	}

	@Override
	public String buildTableString(String table, List<FieldDefinition> fields) {
		StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(table).append(" (\n");
		StringBuilder pkBuf = new StringBuilder();

		fields.forEach(field -> {
			String fieldName = getSqlFieldName(field.getFieldHeader(), true);
			buf.append(fieldName);

			if (field.isOutputAsText()) {
				buf.append(" TEXT");
			} else {
				switch (field.getFieldType()) {
				case BOOLEAN:
					buf.append(" BOOLEAN");
					break;
				case DATE:
					buf.append(" DATE");
					break;
				case TIME:
					buf.append(" TIME");
					break;
				case TIMESTAMP:
					buf.append(" TIMESTAMP");
					break;
				case TEXT, MEMO, IMAGE, THUMBNAIL:
					buf.append(" TEXT");
					break;
				case NUMBER:
					buf.append(" INTEGER");
					break;
				case FLOAT:
					buf.append(" REAL");
					break;
				default:
					buf.append(" NUMERIC");
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
		StringBuilder buf = new StringBuilder("INSERT OR ");

		switch (myPref.getOnConflict()) {
		case 0:
			buf.append("ABORT INTO ");
			break;
		case 1:
			buf.append("FAIL INTO ");
			break;
		case 2:
			buf.append("IGNORE INTO ");
			break;
		case 3:
			buf.append("REPLACE INTO ");
			break;
		case 4:
			buf.append("ROLLBACK INTO ");
			break;
		default:
			break;
		}

		buf.append(myPref.getPdaDatabaseName()).append(" (");
		dbInfo2Write.forEach(field -> buf.append(getSqlFieldName(field.getFieldHeader(), true)).append(","));

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
		currentRecord = 0;
	}

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		currentRecord++;
		int result = 0;

		int index = 1;
		for (FieldDefinition field : dbInfo2Write) {
			Object obj = dbRecord.get(field.getFieldAlias());
			if (obj == null || obj.equals("")) {
				prepStmt.setNull(index, field.getSQLType());
			} else {
				prepStmt.setObject(index, convertDataFields(obj, field));
			}
			index++;
		}

		try {
			result = prepStmt.executeUpdate();
		} catch (SQLiteException ex) {
			String error = ex.getMessage();
			error = error.substring(error.lastIndexOf("(") + 1, error.lastIndexOf(")"));
			throw FNProgException.getException("tableInsertError", Integer.toString(currentRecord),
					myPref.getPdaDatabaseName(), error);
		}

		return result;
	}

	@Override
	public void closeData() {
		try {
			// commits the transaction as well
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			// Transaction is no longer active
		}
	}

	@Override
	public void closeFile() {
		try {
			if (prepStmt != null && !prepStmt.isClosed()) {
				prepStmt.close();
				prepStmt = null;
			}
		} catch (SQLException ex) {
			// Should not occur
		}

		super.closeFile();
	}

}
