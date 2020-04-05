package dbengine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.utils.DatabaseHelper;

public abstract class SqlDB extends GeneralDB implements IConvert {
	private String myTable;
	private boolean isConnected;
	private boolean isFirstRead = true;

	private Map<Integer, List<FieldDefinition>> hTables;
	private List<String> aTables;

	private Connection connection;
	private DatabaseMetaData metaData;
	private ResultSet dbResultSet;

	public SqlDB(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean createBackup, boolean isInputFile) throws Exception {
		// Try to obtain the database connection
		connection = openDatabase();
		getDBFieldNamesAndTypes();

		myTable = myPref.getTableName();
		if (myTable.isEmpty() || !aTables.contains(myTable)) {
			myTable = aTables.get(0);
			myPref.setTableName(myTable, false);
		}
	}

	// Called from DatabaseFactory (FNProg2PDA)
	public Connection openDatabase(DatabaseHelper helper) throws Exception {
		myDatabaseHelper = helper;
		return openDatabase();
	}

	// Called directly from XConvert (DBConvert)
	public Connection openDatabase() throws Exception {
		if (isConnected) {
			closeFile();
		}

		// Try to obtain the database connection
		String[] connectionStrings = getConnectionStrings();
		try {
			Class.forName(connectionStrings[0]);
		} catch (ClassNotFoundException e) {
			FNProgException.getFatalException("driverNotFound", connectionStrings[0]);
		}

		connection = DriverManager.getConnection(connectionStrings[1]);
		metaData = connection.getMetaData();
		isConnected = true;
		return connection;
	}

	private void getDBFieldNamesAndTypes() {
		// read all tables in the database
		hTables = new HashMap<>();
		aTables = new ArrayList<>();
		int index = 0;

		try {
			ResultSet rs = metaData.getTables(null, null, "%", null);
			while (rs.next()) {
				String table = rs.getString(3);
				ResultSet columns = metaData.getColumns(null, null, table, null);
				List<FieldDefinition> aFields = new ArrayList<>();
				while (columns.next()) {
					String field = columns.getString(4);
					FieldDefinition fieldDef = new FieldDefinition(field, field, FieldTypes.TEXT);
					fieldDef.setSQLType(columns.getInt(5));
					
					switch (fieldDef.getSQLType()) {
					case Types.ARRAY:
					case Types.BINARY:
					case Types.BLOB:
					case Types.CLOB:
					case Types.DATALINK:
					case Types.DISTINCT:
					case Types.JAVA_OBJECT:
					case Types.LONGVARBINARY:
					case Types.NULL:
					case Types.OTHER:
					case Types.REF:
					case Types.STRUCT:
					case Types.VARBINARY:
						continue;
					case Types.BIGINT:
					case Types.INTEGER:
					case Types.SMALLINT:
					case Types.TINYINT:
						fieldDef.setFieldType(FieldTypes.NUMBER);
						break;
					case Types.BIT:
					case Types.BOOLEAN:
						fieldDef.setFieldType(FieldTypes.BOOLEAN);
						break;
					case Types.DATE:
						fieldDef.setFieldType(FieldTypes.DATE);
						break;
					case Types.DOUBLE:
					case Types.FLOAT:
					case Types.REAL:
						fieldDef.setFieldType(FieldTypes.FLOAT);
						fieldDef.setDecimalPoint(columns.getInt(8));
						break;
					case Types.LONGVARCHAR:
						fieldDef.setFieldType(FieldTypes.MEMO);
						break;
					case Types.TIME:
						fieldDef.setFieldType(FieldTypes.TIME);
						break;
					case Types.TIMESTAMP:
						fieldDef.setFieldType(FieldTypes.TIMESTAMP);
						break;
					default:
						fieldDef.setFieldType(getFieldType(fieldDef.getSQLType()));
					}
					aFields.add(fieldDef);
				}

				if (!aFields.isEmpty()) {
					hTables.put(index++, aFields);
					aTables.add(table);
				}
			}
		} catch (Exception e) {
			// should not occur
		}
	}

	@Override
	public void closeFile() {
		try {
			connection.close();
		} catch (Exception e) {
			// Should not occur
		}
		isConnected = false;
	}

	@Override
	public List<FieldDefinition> getTableModelFields() throws Exception {
		int index = aTables.indexOf(myTable);
		return hTables.get(index == -1 ? 1 : index);
	}

	@Override
	public String getPdaDatabase() {
		return myTable;
	}

	@Override
	public String[] getTableNames() {
		if (aTables == null) {
			return null;
		}

		String[] result = new String[aTables.size()];
		aTables.toArray(result);
		return result;
	}

	@Override
	public void verifyDatabase(List<FieldDefinition> newFields) throws Exception {
		Object obj = getFieldObject("SELECT COUNT(*) FROM " + myTable);
		myTotalRecords = Integer.parseInt(obj.toString());
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		if (isFirstRead) {
			try (Statement dbStatement = connection.createStatement()) {
				List<FieldDefinition> fields = getTableModelFields();
				StringBuilder buf = new StringBuilder("SELECT ");

				for (FieldDefinition field : fields) {
					buf.append(field.getFieldName());
					buf.append(", ");
				}

				buf.delete(buf.length() - 2, buf.length());
				buf.append(" FROM ");
				buf.append(myTable);

				dbResultSet = dbStatement.executeQuery(buf.toString());
				isFirstRead = false;
			}
		}

		Map<String, Object> result = new HashMap<>();
		int index = 1;
		if (dbResultSet.next()) {
			for (FieldDefinition field : getTableModelFields()) {
				Object obj = getFieldValue(field.getSQLType(), index++, dbResultSet);
				result.put(field.getFieldAlias(), obj);
			}
		}
		return result;
	}

	private Object getFieldObject(String sqlStatement) throws Exception {
		Object result;

		try (Statement statement = connection.createStatement(); 
				ResultSet rs = statement.executeQuery(sqlStatement)) {
			rs.next();
			result = getFieldValue(rs.getMetaData().getColumnType(1), 1, rs);
		} catch (SQLException e) {
			throw new Exception(
					"Internal Program Error:\n\nSQL Statement: '" + sqlStatement + "' could not be excecuted."
							+ "\nThe JDBC driver returned the following error: '" + e.getMessage() + "'");
		}

		return result;
	}

	private Object getFieldValue(int colType, int colNo, ResultSet rs) throws Exception {
		StringBuilder buf = new StringBuilder();

		try {
			int c = 0;
			switch (colType) {
			case Types.LONGVARBINARY:
				InputStream isr;
				try {
					isr = rs.getBinaryStream(colNo);
					if (isr == null) {
						return "";
					}
				} catch (NullPointerException ex) {
					return "";
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] rb = new byte[1024];
				// process blob
				while ((c = isr.read(rb)) != -1) {
					out.write(rb, 0, c);
				}
				byte[] b = out.toByteArray();
				isr.close();
				out.close();
				return b;
			case Types.LONGVARCHAR:
				Reader reader;
				try {
					reader = rs.getCharacterStream(colNo);
					if (reader == null) {
						return "";
					}
				} catch (NullPointerException ex) {
					return "";
				}

				buf = new StringBuilder();
				while (c != -1) {
					c = reader.read();
					if (c != -1) {
						buf.append((char) c);
					}
				}
				reader.close();
				return buf.toString();
			case Types.BIT:
			case Types.BOOLEAN:
				return rs.getBoolean(colNo);
			case Types.DOUBLE:
				return rs.getDouble(colNo);
			case Types.DATE:
				return General.convertDate2DB(rs.getDate(colNo));
			case Types.FLOAT:
				return rs.getFloat(colNo);
			case Types.INTEGER:
				return rs.getInt(colNo);
			case Types.SMALLINT:
				return (int) rs.getShort(colNo);
			case Types.TIMESTAMP:
				return General.convertTimestamp2DB(rs.getTimestamp(colNo).toLocalDateTime());
			case Types.TIME:
				return General.convertTime2DB(rs.getTime(colNo).toLocalTime());
			case Types.VARCHAR:
				String s = rs.getString(colNo);
				return s == null ? "" : s;
			default:
				return getObject(colType, colNo, rs);
			}
		} catch (Exception e) {
			throw new Exception("Unable to read database field, due to " + e.toString());
		}
	}

	public void verifyStatement(String sqlStatement) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			ResultSet rs = statement.executeQuery(sqlStatement);
			rs.close();
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public void createDbHeader() throws Exception {
	}

	@Override
	public void deleteFile() {
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
	}

	protected abstract String[] getConnectionStrings();

	protected abstract Object getObject(int colType, int colNo, ResultSet rs) throws Exception;

	protected abstract FieldTypes getFieldType(int type);
}
