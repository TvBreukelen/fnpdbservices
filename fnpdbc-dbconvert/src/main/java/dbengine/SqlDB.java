package dbengine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.utils.ForeignKey;
import dbengine.utils.SqlTable;
import microsoft.sql.DateTimeOffset;

public abstract class SqlDB extends GeneralDB implements IConvert {
	private List<FieldDefinition> dbInfoToRead;
	private Map<String, FieldDefinition> hFieldMap;
	private Map<String, SqlTable> aTables;

	private static final String TABLE_CAT = "TABLE_CAT";
	private static final String TABLE_NAME = "TABLE_NAME";
	private static final String COLUMN_NAME = "COLUMN_NAME";
	private static final String TABLE_SCHEM = "TABLE_SCHEM";

	protected Connection connection;
	protected boolean isConnected;
	private Statement dbStatement;
	private ResultSet dbResultSet;
	private Session session;
	protected int assignedPort;

	private Set<String> ambiguousFields = new HashSet<>();
	private Set<String> linkedTables = new HashSet<>();

	protected SqlDB(Profiles pref) {
		super(pref);
	}

	protected void getSshSession() throws Exception {
		// Remote host
		final String remoteHost = myHelper.getSshHost();

		boolean usePrivateKey = !myHelper.getPrivateKeyFile().isEmpty();
		boolean usePassword = !myHelper.getSshPassword().isEmpty();

		JSch jsch = new JSch();

		// Check if a private key is provided
		if (usePrivateKey) {
			verifyKeyFile(myHelper.getPrivateKeyFile());

			if (usePassword) {
				jsch.addIdentity(myHelper.getPrivateKeyFile(), General.decryptPassword(myHelper.getSshPassword()));
			} else {
				jsch.addIdentity(myHelper.getPrivateKeyFile());
			}
		}

		// Create SSH session. Port 22 is the default SSH port which is open in your
		// firewall setup.
		session = jsch.getSession(myHelper.getSshUser(), remoteHost, myHelper.getSshPort());

		if (usePassword && !usePrivateKey) {
			session.setPassword(General.decryptPassword(myHelper.getSshPassword()));
		}

		// Additional SSH options. See your ssh_config manual for more options. Set
		// options according to your requirements.
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no"); // Not really wanted..
		config.put("Compression", "yes");
		config.put("ConnectionAttempts", "2");

		session.setConfig(config);

		// Connect
		session.connect();

		// Create the tunnel through port forwarding. This is basically instructing jsch
		// session to send data received from local_port in the local machine to
		// remote_port of the remote_host assigned_port is the port assigned by jsch for
		// use, it may not always be the same as local_port.

		assignedPort = session.setPortForwardingL(0, remoteHost, myHelper.getPort());

		if (assignedPort == 0) {
			throw new JSchException("Port forwarding failed !");
		}
	}

	protected void verifyKeyFile(String keyFile) throws FileNotFoundException, FNProgException {
		if (!General.existFile(keyFile)) {
			// Should not occur unless file has been deleted
			throw FNProgException.getException("noDatabaseExists", keyFile);
		}

		boolean hasBeginTag = false;
		boolean hasEndTag = false;

		try (Scanner sc = new Scanner(new File(keyFile))) {
			while (sc.hasNext()) {
				String line = sc.nextLine();
				if (line.startsWith("-----BEGIN ")) {
					hasBeginTag = true;
				} else if (line.startsWith("-----END ")) {
					hasEndTag = true;
					break;
				}
			}
		}

		if (!(hasBeginTag && hasEndTag)) {
			throw FNProgException.getException("invalidKeyfile", keyFile);
		}
	}

	@Override
	public void readTableContents() throws Exception {
		// read all tables in the database
		aTables = new LinkedHashMap<>();

		String db = myImportFile.isConnectHost() ? myDatabase.substring(myDatabase.indexOf("/") + 1) : myDatabase;

		String[] types = null;
		switch (myImportFile) {
		case POSTGRESQL:
			types = new String[] { "TABLE", "VIEW" };
			break;
		case SQLSERVER:
		case FIREBIRD:
			types = new String[] { "TABLE" };
			break;
		default:
			break;
		}

		DatabaseMetaData metaData = connection.getMetaData();
		try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
			while (rs.next()) {
				String tableCat = rs.getString(TABLE_CAT);
				String table = rs.getString(TABLE_NAME);

				if (tableCat != null && !tableCat.equals(db) || "trace_xe_action_map".equals(table)
						|| "trace_xe_event_map".equals(table)) {
					// SQL Server internal tables
					continue;
				}

				SqlTable sqlTable = new SqlTable();
				sqlTable.setName(table);

				String schema = rs.getString(TABLE_SCHEM);
				try (ResultSet primaryKeys = metaData.getPrimaryKeys(tableCat, schema, table)) {
					while (primaryKeys.next()) {
						sqlTable.getPkList().add(primaryKeys.getString(COLUMN_NAME));
					}
				}

				try (ResultSet foreignKeys = metaData.getImportedKeys(tableCat, schema, table)) {
					while (foreignKeys.next()) {
						ForeignKey fk = new ForeignKey();
						fk.setColumnFrom(foreignKeys.getString("PKCOLUMN_NAME"));
						fk.setColumnTo(foreignKeys.getString("FKCOLUMN_NAME"));
						sqlTable.addFkList(foreignKeys.getString("PKTABLE_NAME"), fk);
					}
				}

				try (ResultSet foreignKeys = metaData.getExportedKeys(tableCat, schema, table)) {
					while (foreignKeys.next()) {
						ForeignKey fk = new ForeignKey();
						fk.setColumnFrom(foreignKeys.getString("FKCOLUMN_NAME"));
						fk.setColumnTo(foreignKeys.getString("PKCOLUMN_NAME"));
						sqlTable.addFkList(foreignKeys.getString("FKTABLE_NAME"), fk);
					}
				}

				ResultSet columns = metaData.getColumns(tableCat, schema, table, null);
				getColumns(sqlTable, columns);
			}
		}

		if (aTables.isEmpty()) {
			throw FNProgException.getException("noTablesFound", "");
		}

		String myTable = myPref.getTableName();
		if (myTable.isEmpty() || !aTables.containsKey(myTable)) {
			// Get first table from the database
			Map.Entry<String, SqlTable> entry = aTables.entrySet().iterator().next();
			myTable = entry.getValue().getName();
			myPref.setTableName(myTable, false);
		}

		hFieldMap = getTableModelFields().stream()
				.collect(Collectors.toMap(FieldDefinition::getFieldName, Function.identity()));
	}

	private void getColumns(SqlTable table, ResultSet columns) throws SQLException {
		List<FieldDefinition> aFields = new ArrayList<>();
		while (columns.next()) {
			String columnName = columns.getString(COLUMN_NAME);

			FieldDefinition fieldDef = new FieldDefinition(columnName, columnName, FieldTypes.TEXT);
			fieldDef.setSQLType(columns.getInt("DATA_TYPE"));

			String type = columns.getString(6);
			if (!(type.isEmpty() || type.equals("TEXT")) && !(setFieldType(fieldDef, type) || setFieldType(fieldDef))) {
				// Non SQL compatible field
				continue;
			}

			fieldDef.setSize(columns.getInt("COLUMN_SIZE"));
			fieldDef.setAutoIncrement(columns.getBoolean("IS_AUTOINCREMENT"));
			fieldDef.setNullable(columns.getBoolean("IS_NULLABLE"));

			aFields.add(fieldDef);
		}

		if (!aFields.isEmpty()) {
			table.setDbFields(aFields);
			aTables.put(table.getName(), table);
		}
	}

	private boolean setFieldType(FieldDefinition field) {
		switch (field.getSQLType()) {
		case Types.BIGINT:
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
		case Types.DECIMAL:
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
		case microsoft.sql.Types.DATETIMEOFFSET:
		case Types.TIMESTAMP:
			field.setFieldType(FieldTypes.TIMESTAMP);
			break;
		case Types.CHAR:
		case Types.LONGNVARCHAR:
		case Types.VARCHAR:
			return true;
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
		case "DATETIME":
		case "TIMESTAMP":
			field.setFieldType(FieldTypes.TIMESTAMP);
			field.setSQLType(Types.TIMESTAMP);
			break;
		case "INTEGER":
		case "DECIMAL":
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
		case "money":
			field.setFieldType(FieldTypes.BIG_DECIMAL);
			field.setSQLType(Types.NUMERIC);
			break;
		case "nchar":
		case "nvarchar":
		case "ntext":
			// time and timestamps with time zones will be treated as string
		case "timestamptz":
		case "timetz":
			field.setFieldType(FieldTypes.TEXT);
			field.setSQLType(Types.VARCHAR);
			break;
		case "YEAR":
			field.setFieldType(FieldTypes.YEAR);
			field.setSQLType(Types.DATE);
			break;
		default:
			return false;
		}

		return true;
	}

	@Override
	public void closeFile() {
		try {
			if (dbResultSet != null && !dbResultSet.isClosed()) {
				dbResultSet.close();
				dbResultSet = null;
			}

			if (dbStatement != null && !dbStatement.isClosed()) {
				dbStatement.close();
				dbStatement = null;
			}

			if (connection != null) {
				connection.close();
				connection = null;
			}
			// Verify if we have a SSH session open
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		} catch (Exception e) {
			// Should not occur
		}
		isConnected = false;
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		SqlTable table = aTables.get(myPref.getTableName());
		List<FieldDefinition> result = new ArrayList<>(table.getDbFields());

		for (String pkTable : table.getFkList().keySet()) {
			Optional<SqlTable> optPkTable = Optional.ofNullable(aTables.get(pkTable));
			if (optPkTable.isPresent()) {
				optPkTable.get().getDbFields().forEach(f -> {
					FieldDefinition fd = f.copy();
					fd.setFieldName(pkTable + "." + f.getFieldName());
					fd.setFieldAlias(fd.getFieldName());
					fd.setFieldHeader(fd.getFieldName());
					result.add(fd);
				});
			}
		}

		return result;
	}

	@Override
	public String getPdaDatabase() {
		return myPref.getTableName();
	}

	@Override
	public List<String> getTableOrSheetNames() {
		if (aTables == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(aTables.keySet());
	}

	public void createQueryStatement() throws SQLException {
		dbStatement = connection.createStatement();
		dbResultSet = dbStatement.executeQuery(getSqlQuery());
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		int index = 1;

		if (dbResultSet.next()) {
			for (FieldDefinition field : dbInfoToRead) {
				try {
					Object obj = getFieldValue(field.getSQLType(), index++, dbResultSet);
					result.put(field.getFieldAlias(), obj);
				} catch (Exception e) {
					throw new Exception(
							"Unable to read database field '" + field.getFieldName() + "', due to\n" + e.toString());
				}
			}
		}
		return result;
	}

	private String getSqlQuery() {
		StringBuilder sql = new StringBuilder("SELECT ");
		SqlTable table = aTables.get(myPref.getTableName());

		dbInfoToRead.forEach(field -> {
			String fieldName = field.getFieldName();
			if (!linkedTables.isEmpty() && ambiguousFields.contains(field.getFieldName())) {
				fieldName = table.getName() + "." + field.getFieldName();
			}

			sql.append(getSqlFieldName(fieldName));
			if (field.getSQLType() == Types.NUMERIC && myImportFile == ExportFile.POSTGRESQL) {
				// cast money field to numeric, otherwise a string preceded by a currency sign
				// will be returned (like $1,000.99)
				sql.append("::numeric");
			}
			sql.append(", ");
		});

		sql.delete(sql.length() - 2, sql.length());
		sql.append("\nFROM ").append(getSqlFieldName(myPref.getTableName()));

		getJoinStatement(sql, table);
		sql.append(getWhereStatement());
		return sql.toString();
	}

	private void getJoinStatement(StringBuilder buf, SqlTable table) {
		for (String link : linkedTables) {
			Optional<ForeignKey> fk = Optional.ofNullable(table.getFkList().get(link));
			if (fk.isPresent()) {
				buf.append("\nLEFT JOIN ").append(link).append(" ON ")
						.append(getSqlFieldName(link + "." + fk.get().getColumnTo())).append(" = ")
						.append(getSqlFieldName(table.getName() + "." + fk.get().getColumnFrom()));
			}
		}
	}

	private String getWhereStatement() {
		if (GeneralSettings.getInstance().isNoFilterExport() || myPref.isNoFilters()) {
			return "";
		}

		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < myPref.noOfFilters(); i++) {
			FieldDefinition field = hFieldMap.get(myPref.getFilterField(i));
			if (field == null) {
				// Should not happen
				return "";
			}

			if (i > 0) {
				buf.append(" ").append(myPref.getFilterCondition()).append(" ");
			} else {
				buf.append(" WHERE ");
			}

			String fieldName = field.getFieldName();
			if (!linkedTables.isEmpty() && ambiguousFields.contains(field.getFieldName())) {
				fieldName = myPref.getTableName() + "." + field.getFieldName();
			}

			buf.append(getSqlFieldName(fieldName));
			getFilterOperatorAndValue(buf, i, field);
		}

		return buf.toString();
	}

	private void getFilterOperatorAndValue(StringBuilder buf, int i, FieldDefinition field) {
		switch (myPref.getFilterOperator(i)) {
		case IS_EQUAL_TO:
			buf.append(" = ");
			break;
		case IS_GREATER_THAN:
			buf.append(" > ");
			break;
		case IS_GREATER_THAN_OR_EQUAL_TO:
			buf.append(" >= ");
			break;
		case IS_LESS_THAN:
			buf.append(" < ");
			break;
		case IS_LESS_THAN_OR_EQUAL_TO:
			buf.append(" <= ");
			break;
		case IS_NOT_EQUAL_TO:
			buf.append(" <> ");
			break;
		}

		switch (field.getFieldType()) {
		case BOOLEAN:
			buf.append("true".equals(myPref.getFilterValue(i)) ? 1 : 0);
			break;
		case CURRENCY:
		case FLOAT:
		case NUMBER:
			buf.append(myPref.getFilterValue(i));
			break;
		default:
			buf.append("'");
			buf.append(myPref.getFilterValue(i));
			buf.append("'");
			break;
		}
	}

	private String getSqlFieldName(String value) {
		if (isNotReservedWord(value) && value.matches("^[a-zA-Z0-9_.]*$")) {
			return value;
		}

		return "[" + value + "]";
	}

	private boolean isNotReservedWord(String value) {
		return !"user".equalsIgnoreCase(value);
	}

	@Override
	public List<Object> getDbFieldValues(String field) throws Exception {
		List<Object> result = new ArrayList<>();

		String table = myPref.getTableName();

		StringBuilder sql = new StringBuilder("SELECT DISTINCT ");
		if (field.contains(".")) {
			int index = field.indexOf(".");
			table = field.substring(0, index);
			field = field.substring(index + 1);
		}

		sql.append(getSqlFieldName(field)).append(" FROM ").append(getSqlFieldName(table));

		try (Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(sql.toString())) {
			while (rs.next()) {
				result.add(getFieldValue(rs.getMetaData().getColumnType(1), 1, rs));
			}
		}

		return result;
	}

	@Override
	public int getTotalRecords() {
		// Extract fields to read from the table model, based on what we want to write.
		// We do that because dbInfo2Write doesn't have the SQL types, needed for the
		// data conversions

		SqlTable table = aTables.get(myPref.getTableName());
		dbInfoToRead = new ArrayList<>();
		mySoft.getDbInfoToWrite().forEach(field -> dbInfoToRead.add(hFieldMap.get(field.getFieldName())));

		ambiguousFields.clear();
		linkedTables.clear();

		// Check if foreign keys are used in fields to be read
		dbInfoToRead.forEach(field -> {
			getLinkedTableAndKeys(table, field);
		});

		// Verify the filters for foreign keys
		if (!GeneralSettings.getInstance().isNoFilterExport() && myPref.isFilterDefined()) {
			for (int i = 0; i < myPref.noOfFilters(); i++) {
				getLinkedTableAndKeys(table, hFieldMap.get(myPref.getFilterField(i)));
			}
		}

		try {
			StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
			sql.append(getSqlFieldName(myPref.getTableName()));
			String where = getWhereStatement();
			if (!where.isEmpty()) {
				getJoinStatement(sql, table);
				sql.append(where);
			}

			try (Statement statement = connection.createStatement();
					ResultSet rs = statement.executeQuery(sql.toString())) {
				while (rs.next()) {
					Object obj = getFieldValue(rs.getMetaData().getColumnType(1), 1, rs);
					return ((Number) obj).intValue();
				}
			}

			return 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private void getLinkedTableAndKeys(SqlTable table, FieldDefinition field) {
		if (field != null && field.getFieldName().contains(".")) {
			String linkedTable = field.getFieldName().substring(0, field.getFieldName().indexOf("."));
			Optional<ForeignKey> key = Optional.ofNullable(table.getFkList().get(linkedTable));
			if (key.isPresent()) {
				linkedTables.add(linkedTable);
				SqlTable fkTable = aTables.get(linkedTable);

				// Get identical field names
				fkTable.getDbFields().forEach(f -> {
					if (hFieldMap.containsKey(f.getFieldName())) {
						ambiguousFields.add(f.getFieldName());
					}
				});
			}
		}
	}

	private Object getFieldValue(int colType, int colNo, ResultSet rs) throws Exception {
		switch (colType) {
		case Types.LONGVARCHAR:
			return readMemoField(rs, colNo);
		case Types.BIT:
		case Types.BOOLEAN:
			return rs.getBoolean(colNo);
		case Types.DOUBLE:
			return rs.getDouble(colNo);
		case Types.DATE:
			try {
				LocalDate date = rs.getObject(colNo, LocalDate.class);
				return date == null ? "" : date;
			} catch (IllegalArgumentException e) {
				// We are dealing with a Year "0000" (MariaDB)
				return "";
			}
		case microsoft.sql.Types.DATETIMEOFFSET:
			try {
				DateTimeOffset date = rs.getObject(colNo, DateTimeOffset.class);
				return date == null ? "" : date.getOffsetDateTime().toLocalDateTime();
			} catch (IllegalArgumentException e) {
				return "";
			}
		case Types.FLOAT:
			return rs.getFloat(colNo);
		case Types.NUMERIC:
			BigDecimal bd = rs.getBigDecimal(colNo);
			return bd == null ? "" : bd;
		case Types.DECIMAL:
		case Types.INTEGER:
			return rs.getInt(colNo);
		case Types.SMALLINT:
		case Types.TINYINT:
			return (int) rs.getShort(colNo);
		case Types.BIGINT:
			return rs.getLong(colNo);
		case Types.TIMESTAMP:
			Timestamp ts = rs.getTimestamp(colNo);
			return ts == null ? "" : ts.toLocalDateTime();
		case Types.TIME:
			LocalTime time = rs.getObject(colNo, LocalTime.class);
			return time == null ? "" : time;
		default:
			String s = rs.getString(colNo);
			return s == null ? "" : s;
		}
	}

	private String readMemoField(ResultSet rs, int colNo) throws SQLException, IOException {
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

	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		// Not supported yet
	}
}
