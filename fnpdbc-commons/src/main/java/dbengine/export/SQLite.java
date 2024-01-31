package dbengine.export;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.IConvert;
import dbengine.SqlDB;

public class SQLite extends SqlDB implements IConvert {
	private PreparedStatement prepStmt;

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
	protected void createTable() throws SQLException {
		StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(myPref.getPdaDatabaseName())
				.append(" (\n");

		dbInfo2Write.forEach(field -> {
			buf.append(getSqlFieldName(field.getFieldHeader(), true));
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
			case TEXT:
			case MEMO:
			case IMAGE:
			case THUMBNAIL:
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

			if (buf.indexOf(" PRIMARY KEY") == -1) {
				buf.append(" PRIMARY KEY");
			}

			buf.append(",\n");
		});

		buf.delete(buf.lastIndexOf(","), buf.length());
		buf.append("\n);");

		Statement dbStatement = connection.createStatement();
		try {
			dbStatement.execute(buf.toString());
		} finally {
			dbStatement.close();
		}
	}

	@Override
	protected void createPreparedStatement() throws SQLException {
		int maxFields = dbInfo2Write.size();
		StringBuilder buf = new StringBuilder("REPLACE INTO ").append(myPref.getPdaDatabaseName()).append(" (");
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
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		int index = 1;
		for (FieldDefinition field : dbInfo2Write) {
			Object obj = dbRecord.get(field.getFieldAlias());
			if (obj == null || obj.equals("")) {
				prepStmt.setNull(index, field.getSQLType());
			} else {
				switch (field.getFieldType()) {
				case BIG_DECIMAL:
					prepStmt.setBigDecimal(index, (BigDecimal) obj);
					break;
				case BOOLEAN:
					prepStmt.setBoolean(index, (Boolean) obj);
					break;
				case DATE:
					prepStmt.setDate(index, Date.valueOf((LocalDate) obj));
					break;
				case FLOAT:
					prepStmt.setDouble(index, ((Number) obj).doubleValue());
					break;
				case NUMBER:
					prepStmt.setInt(index, ((Number) obj).intValue());
					break;
				case TIME:
					prepStmt.setTime(index, Time.valueOf((LocalTime) obj));
					break;
				case TIMESTAMP:
					prepStmt.setTimestamp(index, Timestamp.valueOf((LocalDateTime) obj));
					break;
				default:
					prepStmt.setString(index, obj.toString());
					break;
				}
			}
			index++;
		}
		prepStmt.executeUpdate();
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
