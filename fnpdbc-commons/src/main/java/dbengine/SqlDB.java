package dbengine;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.utils.ForeignKey;
import dbengine.utils.RelationData;
import dbengine.utils.SqlTable;

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
	protected String sqlQuery;

	protected int offset;

	private Set<String> linkedTables = new HashSet<>();

	protected SqlDB(Profiles pref) {
		super(pref);
	}

	@Override
	public void readTableContents() throws Exception {
		// read all tables in the database
		aTables = new LinkedHashMap<>();
		ExportFile dbFile = isInputFile ? myImportFile : myExportFile;

		String db = dbFile.isConnectHost() ? myDatabase.substring(myDatabase.indexOf("/") + 1) : myDatabase;
		if (dbFile == ExportFile.PARADOX) {
			db = myDatabase.substring(myDatabase.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1,
					myDatabase.lastIndexOf("."));
		}

		String[] types = null;
		switch (dbFile) {
		case POSTGRESQL:
			types = new String[] { "TABLE", "VIEW" };
			break;
		case SQLSERVER:
		case PARADOX:
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
				String schema = rs.getString(TABLE_SCHEM);
				SqlTable sqlTable = null;

				boolean isOK = true;
				switch (dbFile) {
				case PARADOX:
					if (!db.equals(table)) {
						isOK = false;
					}
					break;
				case SQLITE:
					if (table.startsWith("sqlite_")) {
						isOK = false;
					}
					break;
				default:
					if (tableCat != null && !tableCat.equals(db) || "trace_xe_action_map".equals(table)
							|| "trace_xe_event_map".equals(table)) {
						// SQL Server internal tables
						isOK = false;
					}
					break;
				}

				if (isOK) {
					sqlTable = new SqlTable();
					sqlTable.setName(table);

					try (ResultSet primaryKeys = metaData.getPrimaryKeys(tableCat, schema, table)) {
						while (primaryKeys.next()) {
							sqlTable.getPkList().add(primaryKeys.getString(COLUMN_NAME));
						}
					} catch (Exception ex) {
						// Cannot read primary keys from table, due data corruption or missing query
						// (SQLite)
						isOK = false;
					}
				}

				if (!isOK) {
					continue;
				}

				try (ResultSet foreignKeys = metaData.getImportedKeys(tableCat, schema, table)) {
					while (foreignKeys.next()) {
						String pmTable = foreignKeys.getString("PKTABLE_NAME");
						ForeignKey fk = sqlTable.getFkList().computeIfAbsent(pmTable, e -> new ForeignKey());
						fk.setColumnFrom(foreignKeys.getString("FKCOLUMN_NAME"));
						fk.setColumnTo(foreignKeys.getString("PKCOLUMN_NAME"));
						fk.setTableFrom(table);
						fk.setTableTo(pmTable);
					}
				}

				try (ResultSet columns = metaData.getColumns(tableCat, schema, table, null)) {
					getColumns(sqlTable, columns);
				}
			}
		}

		if (aTables.isEmpty()) {
			if (isInputFile) {
				throw FNProgException.getException("noTablesFound", General.EMPTY_STRING);
			} else {
				return;
			}
		}

		setReversedForeignKeys();

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

	private void setReversedForeignKeys() {
		// Add reversed ForeignKeys
		aTables.entrySet().forEach(e -> {
			SqlTable table = e.getValue();
			table.getFkList().entrySet().forEach(f -> {
				SqlTable fromTable = aTables.get(f.getKey());
				if (!table.getName().equals(fromTable.getName())) {
					ForeignKey fk1 = f.getValue();
					ForeignKey fk2 = new ForeignKey();
					fk2.setColumnFrom(fk1.getColumnTo());
					fk2.setColumnTo(fk1.getColumnFrom());
					fk2.setTableFrom(fk1.getTableTo());
					fk2.setTableTo(table.getName());
					fromTable.getFkList().putIfAbsent(table.getName(), fk2);
				}
			});
		});
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
			fieldDef.setNotNullable("NO".equals(columns.getString("IS_NULLABLE")));
			fieldDef.setAutoIncrement("YES".equals(columns.getString("IS_AUTOINCREMENT")));

			aFields.add(fieldDef);
		}

		if (!aFields.isEmpty()) {
			table.setDbFields(aFields);
			aTables.put(table.getName(), table);
		}
	}

	protected boolean setFieldType(FieldDefinition field) {
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
		case "NUMBER":
			field.setFieldType(FieldTypes.NUMBER);
			field.setSQLType(Types.INTEGER);
			break;
		case "MEMO":
			field.setFieldType(FieldTypes.MEMO);
			field.setSQLType(Types.VARCHAR);
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
		} catch (Exception e) {
			// Should not occur
		}
		isConnected = false;
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		return getTableModelFields(true);
	}

	@Override
	public List<FieldDefinition> getTableModelFields(boolean loadFromRegistry) {
		SqlTable table = aTables.get(myPref.getTableName());
		Map<String, ForeignKey> map = table.getFkList();
		Set<String> primaryKeys = table.getPkList();

		if (loadFromRegistry) {
			RelationData joinData = new RelationData();
			joinData.loadProfile(myPref); // Read foreign key definitions from registry

			// Update system keys and add user keys to map
			map.putAll(joinData.getRelationMap());
		}

		List<FieldDefinition> result = new ArrayList<>(table.getDbFields());
		result.forEach(field -> {
			if (primaryKeys.contains(field.getFieldName())) {
				field.setPrimaryKey(true);
			}
		});

		for (Entry<String, ForeignKey> entry : map.entrySet()) {
			Optional<SqlTable> optPkTable = Optional.ofNullable(aTables.get(entry.getKey()));
			if (optPkTable.isPresent()) {
				optPkTable.get().getDbFields().forEach(f -> {
					FieldDefinition fd = f.copy();
					fd.setFieldName(entry.getKey() + "." + f.getFieldName());
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

	public SqlTable getSqlTable() {
		return getSqlTable(isInputFile ? myPref.getTableName() : myPref.getPdaDatabaseName());
	}

	public SqlTable getSqlTable(String table) {
		return aTables.get(table);
	}

	@Override
	public void readInputFile() throws SQLException {
		// Extract fields to read from the table model, based on what we want to write.
		// We do that because dbInfo2Write doesn't have the SQL types, needed for the
		// data conversions
		dbInfoToRead = new ArrayList<>();
		mySoft.getDbInfoToWrite().forEach(field -> dbInfoToRead.add(hFieldMap.get(field.getFieldName())));

		linkedTables.clear();

		// Check if foreign keys are used in fields to be read
		dbInfoToRead.forEach(this::getLinkedTables);

		// Verify the filters for foreign keys
		if (!GeneralSettings.getInstance().isNoFilterExport() && myPref.isFilterDefined()) {
			for (int i = 0; i < myPref.noOfFilters(); i++) {
				getLinkedTables(hFieldMap.get(myPref.getFilterField(i)));
			}
		}

		// Add linked field(s) for SortBy statement
		if (myPref.isSortFieldDefined()) {
			myPref.getSortFields().forEach(s -> getLinkedTables(hFieldMap.get(s)));
		}

		getSqlQuery();
		dbStatement = connection.createStatement();
		dbResultSet = dbStatement.executeQuery(getPaginationSqlString());
	}

	protected String getPaginationSqlString() {
		StringBuilder b = new StringBuilder(sqlQuery);
		if (myPref.getSqlSelectLimit() > 0) {
			b.append("\nOFFSET ").append(offset).append(" ROWS ").append("\nFETCH NEXT ")
					.append(myPref.getSqlSelectLimit()).append(" ROWS ONLY");
		}
		return b.toString();
	}

	@Override
	public Map<String, Object> readRecord() throws FNProgException, IOException, SQLException {
		Map<String, Object> result = new HashMap<>();
		int index = 1;

		if (dbResultSet.next()) {
			offset++;
			for (FieldDefinition field : dbInfoToRead) {
				try {
					Object obj = getFieldValue(field.getSQLType(), index++, dbResultSet);
					result.put(field.getFieldAlias(), obj);
				} catch (Exception e) {
					throw new FNProgException(
							"Unable to read database field '" + field.getFieldName() + "', due to\n" + e.toString());
				}
			}
		} else if (myPref.isPagination() && offset < totalRecords) {
			dbResultSet.close();
			dbStatement.close();
			readInputFile();
			return readRecord();
		}
		return result;
	}

	private void getSqlQuery() {
		StringBuilder sql = new StringBuilder("SELECT ");
		SqlTable table = aTables.get(myPref.getTableName());

		dbInfoToRead.forEach(field -> {
			String fieldName = field.getFieldName();
			sql.append(fieldName.contains(".") ? fieldName : "A." + getSqlFieldName(fieldName));
			if (field.getSQLType() == Types.NUMERIC && myImportFile == ExportFile.POSTGRESQL) {
				// cast money field to numeric, otherwise a string preceded by a currency sign
				// will be returned (like $1,000.99)
				sql.append("::numeric");
			}
			sql.append(", ");
		});

		sql.delete(sql.length() - 2, sql.length());
		sql.append("\nFROM ").append(getSqlFieldName(table.getName()));
		sql.append(" AS A");

		getJoinStatement(sql);
		sql.append(getWhereStatement());
		sql.append(getOrderBy());

		sqlQuery = sql.toString();
	}

	private void getJoinStatement(StringBuilder buf) {
		List<ForeignKey> keys = new ArrayList<>();
		Set<String> linked = new HashSet<>();
		SqlTable table = getSqlTable();

		for (String link : linkedTables) {
			while (true) {
				if (linked.contains(link)) {
					break;
				}

				linked.add(link);
				ForeignKey key = table.getFkList().get(link).copy();
				keys.add(0, key);

				if (key.getTableFrom().equals(table.getName())) {
					key.setTableFrom("A");
					break;
				}

				link = key.getTableFrom();
			}
		}
		keys.forEach(key -> getJoinedTable(buf, key));
	}

	private void getJoinedTable(StringBuilder buf, ForeignKey key) {
		if (myImportFile == ExportFile.SQLITE && key.getJoin().equals("Right Join")) {
			// SQLite doesn't support a Right Join, so we'll swap the From and To Tables
			// and use a Left Join instead
			List<String> columnTo = new ArrayList<>(key.getColumnFrom());
			key.setJoin("Left Join");
			key.setColumnFrom(key.getColumnTo());
			key.setColumnTo(columnTo);

			String tableFrom = key.getTableFrom() + key.getTableTo();
			key.setTableTo(tableFrom.substring(0, tableFrom.length() - key.getTableTo().length()));
			key.setTableFrom(tableFrom.substring(key.getTableTo().length()));
		}

		buf.append("\n").append(key.getJoin().toUpperCase()).append(General.SPACE).append(key.getTableTo());
		for (int row = 0; row < key.getColumnFrom().size(); row++) {
			buf.append(row == 0 ? "\nON " : "\nAND ")
					.append(getSqlFieldName(key.getTableTo() + "." + key.getColumnTo().get(row))).append(" = ")
					.append(getSqlFieldName(key.getTableFrom() + "." + key.getColumnFrom().get(row)));

		}
	}

	private String getWhereStatement() {
		if (GeneralSettings.getInstance().isNoFilterExport() || myPref.isNoFilters()) {
			return General.EMPTY_STRING;
		}

		StringBuilder sql = new StringBuilder("\nWHERE ");
		for (int i = 0; i < myPref.noOfFilters(); i++) {
			FieldDefinition field = hFieldMap.get(myPref.getFilterField(i));
			if (field == null) {
				// Should not happen
				return General.EMPTY_STRING;
			}

			if (i > 0) {
				sql.append(General.SPACE).append(myPref.getFilterCondition()).append(General.SPACE);
			}

			String fieldName = field.getFieldName();
			sql.append(fieldName.contains(".") ? fieldName : "A." + getSqlFieldName(fieldName));
			getFilterOperatorAndValue(sql, i, field);
		}

		return sql.toString();
	}

	private String getOrderBy() {
		List<String> list = myPref.getSortFields();
		if (list.isEmpty()) {
			return General.EMPTY_STRING;
		}

		StringBuilder sql = new StringBuilder("\nORDER BY ");
		list.forEach(s -> sql.append(s.contains(".") ? s : "A." + getSqlFieldName(s)).append(", "));
		return sql.toString().substring(0, sql.lastIndexOf(","));
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
		case CURRENCY:
		case FLOAT:
		case NUMBER:
			buf.append(myPref.getFilterValue(i));
			break;
		default:
			buf.append("'").append(myPref.getFilterValue(i)).append("'");
			if (myPref.getFilterValue(i).isBlank()) {
				// Also check for NULL values
				switch (myPref.getFilterOperator(i)) {
				case IS_EQUAL_TO:
				case IS_GREATER_THAN_OR_EQUAL_TO:
				case IS_LESS_THAN_OR_EQUAL_TO:
					buf.append(" OR ").append(field.getFieldName()).append(" IS NULL");
					break;
				default:
					// No change
				}
			}
			break;
		}
	}

	protected String getSqlFieldName(String value, boolean noDot) {
		return noDot ? getSqlFieldName(value).replace(".", "") : getSqlFieldName(value);
	}

	protected String getSqlFieldName(String value) {
		if (isNotReservedWord(value) && value.matches("^[a-zA-Z0-9_.]*$")) {
			return value;
		}

		return "[" + value + "]";
	}

	protected boolean isNotReservedWord(String value) {
		return !"user".equalsIgnoreCase(value);
	}

	@Override
	public List<Object> getDbFieldValues(String field) throws Exception {
		List<Object> result = new ArrayList<>();

		String table = myPref.getTableName();
		StringBuilder sql = new StringBuilder("SELECT DISTINCT ");
		sql.append(getSqlFieldName(field)).append(" FROM ").append(getSqlFieldName(table)).append(" AS A");

		if (field.contains(".")) {
			linkedTables.clear();
			getLinkedTables(hFieldMap.get(field));
			getJoinStatement(sql);
		}

		try (Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(sql.toString())) {
			while (rs.next()) {
				result.add(getFieldValue(rs.getMetaData().getColumnType(1), 1, rs));
			}
		}

		return result;
	}

	@Override
	public int getTotalRecords() throws Exception {
		offset = 0;
		totalRecords = 0;

		StringBuilder sql = new StringBuilder("SELECT COUNT(*)");
		if (myPref.isSortFieldDefined()) {
			// Remove Order statement
			sql.append(sqlQuery.substring(sqlQuery.indexOf("\n"), sqlQuery.indexOf("ORDER BY")));
		} else {
			sql.append(sqlQuery.substring(sqlQuery.indexOf("\n")));
		}

		try (Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(sql.toString())) {
			while (rs.next()) {
				Object obj = getFieldValue(rs.getMetaData().getColumnType(1), 1, rs);
				totalRecords = ((Number) obj).intValue();
				totalRecords = myPref.getSqlSelectLimit() == 0 || myPref.isPagination() ? totalRecords
						: Math.min(totalRecords, myPref.getSqlSelectLimit());
			}
		}

		return totalRecords;
	}

	private void getLinkedTables(FieldDefinition field) {
		SqlTable table = getSqlTable();

		if (field != null && field.getFieldName().contains(".")) {
			String linkedTable = field.getFieldName().substring(0, field.getFieldName().indexOf("."));
			Optional<ForeignKey> key = Optional.ofNullable(table.getFkList().get(linkedTable));
			if (key.isPresent()) {
				linkedTables.add(linkedTable);
			}
		}
	}

	protected Object getFieldValue(int colType, int colNo, ResultSet rs) throws SQLException, IOException {
		switch (colType) {
		case Types.LONGVARCHAR:
			return readMemoField(rs, colNo);
		case Types.BIT:
		case Types.BOOLEAN:
			return rs.getBoolean(colNo);
		case Types.BLOB:
			return rs.getBlob(colNo);
		case Types.DOUBLE:
			return rs.getDouble(colNo);
		case Types.DATE:
			try {
				LocalDate date = rs.getObject(colNo, LocalDate.class);
				return date == null ? General.EMPTY_STRING : date;
			} catch (IllegalArgumentException e) {
				// We are dealing with a Year "0000" (MariaDB)
				return General.EMPTY_STRING;
			}
		case Types.FLOAT:
			return rs.getFloat(colNo);
		case Types.NUMERIC:
			BigDecimal bd = rs.getBigDecimal(colNo);
			return bd == null ? General.EMPTY_STRING : bd;
		case Types.DECIMAL:
		case Types.INTEGER:
			return rs.getInt(colNo);
		case Types.SMALLINT:
		case Types.TINYINT:
			return (int) rs.getShort(colNo);
		case Types.BIGINT:
			return rs.getLong(colNo);
		case Types.TIMESTAMP:
			LocalDateTime ts = rs.getObject(colNo, LocalDateTime.class);
			return ts == null ? General.EMPTY_STRING : ts;
		case Types.TIME:
			LocalTime time = rs.getObject(colNo, LocalTime.class);
			return time == null ? General.EMPTY_STRING : time;
		default:
			String s = rs.getString(colNo);
			return s == null ? General.EMPTY_STRING : s;
		}
	}

	private String readMemoField(ResultSet rs, int colNo) throws SQLException, IOException {
		Reader reader;
		int c = 0;
		try {
			reader = rs.getCharacterStream(colNo);
			if (reader == null) {
				return General.EMPTY_STRING;
			}
		} catch (NullPointerException ex) {
			return General.EMPTY_STRING;
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
	public void createDbHeader() throws Exception {
		readTableContents();

		SqlTable table = getSqlTable();
		String tableName = myPref.getPdaDatabaseName();

		if (table == null || myPref.getExportOption() == 0) {
			useAppend = false;
			if (myPref.getExportOption() == 0) {
				executeStatement("DROP TABLE " + tableName);
			}
			executeStatement(buildTableString(tableName, dbInfo2Write));
			createPreparedStatement();
			return;
		}

		validateTable(table);

		if (myPref.getExportOption() == 1) {
			executeStatement("DELETE FROM " + tableName);
			useAppend = true;
		}

		createPreparedStatement();
	}

	private void validateTable(SqlTable table) throws FNProgException {
		int numFields = dbInfo2Write.size();
		if (numFields != table.getDbFields().size()) {
			throw FNProgException.getException("noMatchFieldsDatabase", Integer.toString(numFields),
					Integer.toString(table.getDbFields().size()));
		}

		// Verify if fields match in type and size
		for (int i = 0; i < numFields; i++) {
			FieldDefinition field1 = table.getDbFields().get(i);
			FieldDefinition field2 = dbInfo2Write.get(i);

			if (!field1.getFieldName().equals(getSqlFieldName(field2.getFieldHeader(), true))) {
				throw FNProgException.getException("noMatchFieldName", Integer.toString(i + 1), field1.getFieldName(),
						field2.getFieldHeader());
			}

			boolean isField1Text = field1.getFieldType() == FieldTypes.MEMO || field1.getFieldType() == FieldTypes.TEXT;
			boolean isField2Text = field2.isOutputAsText() || field2.getFieldType() == FieldTypes.MEMO
					|| field2.getFieldType() == FieldTypes.TEXT;

			if (field1.getFieldType() != field2.getFieldType() && isField1Text != isField2Text) {
				throw FNProgException.getException("noMatchFieldType", field1.getFieldName(), field2.getFieldHeader());
			}

			if (field1.getSize() < field2.getSize()) {
				throw FNProgException.getException("noMatchFieldLength", field1.getFieldName(),
						Integer.toString(field1.getSize()), Integer.toString(field2.getSize()));
			}
		}
	}

	protected void executeStatement(String statement) throws SQLException {
		dbStatement = connection.createStatement();
		try {
			dbStatement.execute(statement);
		} finally {
			dbStatement.close();
		}
	}

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		// To be implemented by the child classes
		return 0;
	}

	protected void createPreparedStatement() throws SQLException {
		// To be implemented by the child classes
	}

	public String buildTableString(String table, List<FieldDefinition> fields) {
		// To be implemented by the child classes
		return null;
	}
}
