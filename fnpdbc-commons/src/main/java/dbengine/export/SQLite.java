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

	static {
		reservedWords.clear();
		reservedWords.add("ABORT");
		reservedWords.add("ACTION");
		reservedWords.add("ADD");
		reservedWords.add("AFTER");
		reservedWords.add("ALL");
		reservedWords.add("ALTER");
		reservedWords.add("ALWAYS");
		reservedWords.add("ANALYZE");
		reservedWords.add("AND");
		reservedWords.add("AS");
		reservedWords.add("ASC");
		reservedWords.add("ATTACH");
		reservedWords.add("AUTOINCREMENT");
		reservedWords.add("BEFORE");
		reservedWords.add("BEGIN");
		reservedWords.add("BETWEEN");
		reservedWords.add("BY");
		reservedWords.add("CASCADE");
		reservedWords.add("CASE");
		reservedWords.add("CAST");
		reservedWords.add("CHECK");
		reservedWords.add("COLLATE");
		reservedWords.add("COLUMN");
		reservedWords.add("COMMIT");
		reservedWords.add("CONFLICT");
		reservedWords.add("CONSTRAINT");
		reservedWords.add("CREATE");
		reservedWords.add("CROSS");
		reservedWords.add("CURRENT");
		reservedWords.add("CURRENT_DATE");
		reservedWords.add("CURRENT_TIME");
		reservedWords.add("CURRENT_TIMESTAMP");
		reservedWords.add("DATABASE");
		reservedWords.add("DEFAULT");
		reservedWords.add("DEFERRABLE");
		reservedWords.add("DEFERRED");
		reservedWords.add("DELETE");
		reservedWords.add("DESC");
		reservedWords.add("DETACH");
		reservedWords.add("DISTINCT");
		reservedWords.add("DO");
		reservedWords.add("DROP");
		reservedWords.add("EACH");
		reservedWords.add("ELSE");
		reservedWords.add("END");
		reservedWords.add("ESCAPE");
		reservedWords.add("EXCEPT");
		reservedWords.add("EXCLUDE");
		reservedWords.add("EXCLUSIVE");
		reservedWords.add("EXISTS");
		reservedWords.add("EXPLAIN");
		reservedWords.add("FAIL");
		reservedWords.add("FILTER");
		reservedWords.add("FIRST");
		reservedWords.add("FOLLOWING");
		reservedWords.add("FOR");
		reservedWords.add("FOREIGN");
		reservedWords.add("FROM");
		reservedWords.add("FULL");
		reservedWords.add("GENERATED");
		reservedWords.add("GLOB");
		reservedWords.add("GROUP");
		reservedWords.add("GROUPS");
		reservedWords.add("HAVING");
		reservedWords.add("IF");
		reservedWords.add("IGNORE");
		reservedWords.add("IMMEDIATE");
		reservedWords.add("IN");
		reservedWords.add("INDEX");
		reservedWords.add("INDEXED");
		reservedWords.add("INITIALLY");
		reservedWords.add("INNER");
		reservedWords.add("INSERT");
		reservedWords.add("INSTEAD");
		reservedWords.add("INTERSECT");
		reservedWords.add("INTO");
		reservedWords.add("IS");
		reservedWords.add("ISNULL");
		reservedWords.add("JOIN");
		reservedWords.add("KEY");
		reservedWords.add("LAST");
		reservedWords.add("LEFT");
		reservedWords.add("LIKE");
		reservedWords.add("LIMIT");
		reservedWords.add("MATCH");
		reservedWords.add("MATERIALIZED");
		reservedWords.add("NATURAL");
		reservedWords.add("NO");
		reservedWords.add("NOT");
		reservedWords.add("NOTHING");
		reservedWords.add("NOTNULL");
		reservedWords.add("NULL");
		reservedWords.add("NULLS");
		reservedWords.add("OF");
		reservedWords.add("OFFSET");
		reservedWords.add("ON");
		reservedWords.add("OR");
		reservedWords.add("ORDER");
		reservedWords.add("OTHERS");
		reservedWords.add("OUTER");
		reservedWords.add("OVER");
		reservedWords.add("PARTITION");
		reservedWords.add("PLAN");
		reservedWords.add("PRAGMA");
		reservedWords.add("PRECEDING");
		reservedWords.add("PRIMARY");
		reservedWords.add("QUERY");
		reservedWords.add("RAISE");
		reservedWords.add("RANGE");
		reservedWords.add("RECURSIVE");
		reservedWords.add("REFERENCES");
		reservedWords.add("REGEXP");
		reservedWords.add("REINDEX");
		reservedWords.add("RELEASE");
		reservedWords.add("RENAME");
		reservedWords.add("REPLACE");
		reservedWords.add("RESTRICT");
		reservedWords.add("RETURNING");
		reservedWords.add("RIGHT");
		reservedWords.add("ROLLBACK");
		reservedWords.add("ROW");
		reservedWords.add("ROWS");
		reservedWords.add("SAVEPOINT");
		reservedWords.add("SELECT");
		reservedWords.add("SET");
		reservedWords.add("TABLE");
		reservedWords.add("TEMP");
		reservedWords.add("TEMPORARY");
		reservedWords.add("THEN");
		reservedWords.add("TIES");
		reservedWords.add("TO");
		reservedWords.add("TRANSACTION");
		reservedWords.add("TRIGGER");
		reservedWords.add("UNBOUNDED");
		reservedWords.add("UNION");
		reservedWords.add("UNIQUE");
		reservedWords.add("UPDATE");
		reservedWords.add("USING");
		reservedWords.add("VACUUM");
		reservedWords.add("VALUES");
		reservedWords.add("VIEW");
		reservedWords.add("VIRTUAL");
		reservedWords.add("WHEN");
		reservedWords.add("WHERE");
		reservedWords.add("WINDOW");
		reservedWords.add("WITH");
		reservedWords.add("WITHOUT");
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
