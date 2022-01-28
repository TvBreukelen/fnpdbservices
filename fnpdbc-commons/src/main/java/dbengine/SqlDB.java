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
	private Statement dbStatement;

	protected SqlDB(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
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
				ResultSet columns;
				try {
					columns = metaData.getColumns(null, null, table, null);
				} catch (Exception e) {
					continue;
				}

				List<FieldDefinition> aFields = new ArrayList<>();
				while (columns.next()) {
					String field = columns.getString(4);
					FieldDefinition fieldDef = new FieldDefinition(field, field, FieldTypes.TEXT);
					fieldDef.setSQLType(columns.getInt(5));

					String type = columns.getString(6);
					if (!(type.isEmpty() || type.equals("TEXT"))) {
						if (!(setFieldType(fieldDef, type) || setFieldType(fieldDef))) {
							continue;
						}
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

	private boolean setFieldType(FieldDefinition field) {
		switch (field.getSQLType()) {
		case Types.BIGINT:
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
			field.setFieldType(FieldTypes.NUMBER);
			break;
		case Types.BIT:
		case Types.BOOLEAN:
			field.setFieldType(FieldTypes.BOOLEAN);
			break;
		case Types.DATE:
			field.setFieldType(FieldTypes.DATE);
			break;
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.REAL:
			field.setFieldType(FieldTypes.FLOAT);
			break;
		case Types.LONGVARCHAR:
			field.setFieldType(FieldTypes.MEMO);
			break;
		case Types.TIME:
			field.setFieldType(FieldTypes.TIME);
			break;
		case Types.TIMESTAMP:
			field.setFieldType(FieldTypes.TIMESTAMP);
			break;
		default:
			return false;
		}
		return true;
	}

	private boolean setFieldType(FieldDefinition field, String type) {
		// SqLite sets the SQL Type incorrectly, but the type name correct
		switch (type) {
		case "BOOL":
			field.setFieldType(FieldTypes.BOOLEAN);
			field.setSQLType(Types.BOOLEAN);
			break;
		case "DATE":
			field.setFieldType(FieldTypes.DATE);
			field.setSQLType(Types.DATE);
			break;
		case "INTEGER":
			field.setFieldType(FieldTypes.NUMBER);
			field.setSQLType(Types.INTEGER);
			break;
		case "REAL":
			field.setFieldType(FieldTypes.FLOAT);
			field.setSQLType(Types.FLOAT);
			break;
		case "TIME":
			field.setFieldType(FieldTypes.TIME);
			field.setSQLType(Types.TIME);
			break;
		case "TIMESTAMP":
			field.setFieldType(FieldTypes.TIMESTAMP);
			field.setSQLType(Types.TIMESTAMP);
			break;
		default:
			return false;
		}

		return true;
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
	public List<FieldDefinition> getTableModelFields() {
		int index = aTables.indexOf(myTable);
		return hTables.get(index == -1 ? 1 : index);
	}

	@Override
	public String getPdaDatabase() {
		return myTable;
	}

	@Override
	public List<String> getTableOrSheetNames() {
		if (aTables == null) {
			return new ArrayList<>();
		}
		return aTables;
	}

	@Override
	public void verifyDatabase(List<FieldDefinition> newFields) throws Exception {
		Object obj = getFieldObject("SELECT COUNT(*) FROM " + myTable);
		myTotalRecords = Integer.parseInt(obj.toString());
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		if (isFirstRead) {
			dbStatement = connection.createStatement();
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

		Map<String, Object> result = new HashMap<>();
		int index = 1;
		if (dbResultSet.next()) {
			for (FieldDefinition field : getTableModelFields()) {
				Object obj = getFieldValue(field.getSQLType(), index++, dbResultSet);
				result.put(field.getFieldAlias(), obj);
			}
		} else {
			dbStatement.close();
		}
		return result;
	}

	private Object getFieldObject(String sqlStatement) throws Exception {
		Object result;

		try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sqlStatement)) {
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
				return readMemoField(rs, colNo);
			case Types.BIT:
			case Types.BOOLEAN:
				return rs.getBoolean(colNo);
			case Types.DOUBLE:
				return rs.getDouble(colNo);
			case Types.DATE:
				return rs.getDate(colNo).toLocalDate();
			case Types.FLOAT:
				return rs.getFloat(colNo);
			case Types.INTEGER:
				return rs.getInt(colNo);
			case Types.SMALLINT:
				return (int) rs.getShort(colNo);
			case Types.TIMESTAMP:
				return rs.getTimestamp(colNo).toLocalDateTime();
			case Types.TIME:
				return rs.getTime(colNo).toLocalTime();
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

	private String readMemoField(ResultSet rs, int colNo) throws Exception {
		Reader reader;
		int c = 0;
		try {
			reader = rs.getCharacterStream(colNo);
			if (reader == null) {
				return "";
			}
		} catch (NullPointerException ex) {
			return "";
		}

		StringBuilder buf = new StringBuilder();
		while (c != -1) {
			c = reader.read();
			if (c != -1) {
				buf.append((char) c);
			}
		}
		reader.close();
		return buf.toString();
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
	public void processData(Map<String, Object> dbRecord) throws Exception {
	}

	protected abstract String[] getConnectionStrings();

	protected abstract Object getObject(int colType, int colNo, ResultSet rs) throws Exception;
}
