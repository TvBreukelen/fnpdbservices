package dbengine.export;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.IConvert;
import dbengine.SqlDB;
import dbengine.utils.DatabaseHelper;

public class SQLite extends SqlDB implements IConvert {
	public SQLite(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.SQLITE);
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
		url.append(myDatabase);

		connection = DriverManager.getConnection(url.toString());
		isConnected = true;
	}

	private void verifyDbHeader() throws IOException, FNProgException {
		if (!isInputFile && !General.existFile(myDatabase)) {
			return;
		}

		String header = null;
		ExportFile dbFile = isInputFile ? myImportFile : myExportFile;

		try (RandomAccessFile raf = new RandomAccessFile(myDatabase, "r")) {
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
			throw FNProgException.getException("invalidDatabaseID", myDatabase, dbFile.getName(), dbFile.getDbType(),
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
		StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getSqlFieldName(table))
				.append(" (\n");
		StringBuilder pkBuf = new StringBuilder();

		fields.forEach(field -> {
			String fieldName = getSqlFieldName(field.getFieldHeader());
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
		case Profiles.ON_CONFLICT_ABORT:
			buf.append("ABORT INTO ");
			break;
		case Profiles.ON_CONFLICT_FAIL:
			buf.append("FAIL INTO ");
			break;
		case Profiles.ON_CONFLICT_IGNORE:
			buf.append("IGNORE INTO ");
			break;
		case Profiles.ON_CONFLICT_REPLACE:
			buf.append("REPLACE INTO ");
			break;
		case Profiles.ON_CONFLICT_ROLLBACK:
			buf.append("ROLLBACK INTO ");
			break;
		default:
			break;
		}

		buf.append(myPref.getDatabaseName()).append(" (");
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
	protected void throwInsertException(SQLException ex) throws FNProgException {
		String error = ex.getMessage();
		error = error.substring(error.lastIndexOf("(") + 1, error.lastIndexOf(")"));
		throw FNProgException.getException("tableInsertError", Integer.toString(currentRecord),
				myPref.getDatabaseName(), error);
	}

}
