package dbengine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Database.FileFormat;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Index;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import application.interfaces.FieldTypes;
import application.interfaces.FilterOperator;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.utils.MSTable;

public class MSAccess extends GeneralDB implements IConvert {
	private Database database;
	private Table table;
	private String myTable;
	private boolean isIndexSupported = true;

	private Map<String, MSTable> hTables;
	private Map<String, CursorBuilder> hTableCursors;
	private Map<String, String[]> hTableIndexes;
	private List<String> aTables;

	public MSAccess(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean createBackup, boolean isInputFile) throws Exception {
		// For the moment we only open the database file for input
		database = DatabaseBuilder.open(new File(myFilename));

		try {
			isIndexSupported = database.getFileFormat() != FileFormat.V1997;
		} catch (Exception e) {
			e.printStackTrace();
		}

		getDBFieldNamesAndTypes();
		myTable = myPref.getTableName();
		if (myTable.isEmpty() || !aTables.contains(myTable)) {
			myTable = aTables.get(0);
		}
	}

	public boolean isIndexedSupported() {
		return isIndexSupported;
	}

	private void getDBFieldNamesAndTypes() {
		// read all tables in the database
		hTables = new HashMap<>();
		hTableCursors = new HashMap<>();
		hTableIndexes = new HashMap<>();
		aTables = new ArrayList<>();

		try {
			aTables.addAll(database.getTableNames());
		} catch (IOException e1) {
			return;
		}

		Iterator<String> iter = aTables.iterator();
		while (iter.hasNext()) {
			String s = iter.next();
			try {
				table = database.getTable(s);
				MSTable msTable = new MSTable(s, s);

				List<? extends Index> lIndex = table.getIndexes();
				if (lIndex != null && !lIndex.isEmpty()) {
					msTable.setIndexes(lIndex);
				} else if (isIndexSupported) {
					continue;
				}

				List<FieldDefinition> aFields = new ArrayList<>();
				for (Column col : table.getColumns()) {
					String field = col.getName();
					FieldDefinition fieldDef = new FieldDefinition(field, field, FieldTypes.TEXT);
					fieldDef.setTable(s);

					switch (col.getType()) {
					case BINARY:
					case GUID:
					case OLE:
					case UNKNOWN_0D:
					case UNKNOWN_11:
						fieldDef.setFieldType(FieldTypes.UNKNOWN);
						break;
					case BOOLEAN:
						fieldDef.setFieldType(FieldTypes.BOOLEAN);
						break;
					case BYTE:
					case INT:
					case LONG:
						fieldDef.setFieldType(FieldTypes.NUMBER);
						break;
					case DOUBLE:
					case FLOAT:
					case MONEY:
					case NUMERIC:
						fieldDef.setFieldType(FieldTypes.FLOAT);
						break;
					case MEMO:
						fieldDef.setFieldType(FieldTypes.MEMO);
						break;
					case SHORT_DATE_TIME:
						fieldDef.setFieldType(FieldTypes.TIMESTAMP);
						break;
					default:
						break;
					}
					aFields.add(fieldDef);
				}

				msTable.setDbFields(aFields);
				hTables.put(s, msTable);
			} catch (Exception e) {
				iter.remove();
				e.printStackTrace();
			}
		}
	}

	public Map<String, Object> getSingleRecord(String tableName, String indexName, Map<String, Object> colValue)
			throws Exception {
		Map<String, Object> result = new HashMap<>();
		Cursor cursor = getCursor(tableName, indexName, colValue, FilterOperator.IS_EQUAL_TO);

		if (cursor != null) {
			Iterator<Row> iter = cursor.iterator();
			if (colValue == null) {
				return iter.next();
			}

			while (iter.hasNext()) {
				Map<String, Object> map = iter.next();
				if (cursor.currentRowMatches(colValue)) {
					return map;
				}
			}
		}
		return result;
	}

	public List<Map<String, Object>> getMultipleRecords(String tableName) throws Exception {
		return getMultipleRecords(tableName, null, null, false, FilterOperator.IS_EQUAL_TO);
	}

	public List<Map<String, Object>> getMultipleRecords(String tableName, String indexName) throws Exception {
		return getMultipleRecords(tableName, indexName, null, false, FilterOperator.IS_EQUAL_TO);
	}

	public List<Map<String, Object>> getMultipleRecords(String tableName, String indexName,
			Map<String, Object> colValue) throws Exception {
		return getMultipleRecords(tableName, indexName, colValue, false, FilterOperator.IS_EQUAL_TO);
	}

	public List<Map<String, Object>> getMultipleRecords(String tableName, String indexName,
			Map<String, Object> colValue, boolean isOverride) throws Exception {
		return getMultipleRecords(tableName, indexName, colValue, isOverride, FilterOperator.IS_EQUAL_TO);
	}

	public List<Map<String, Object>> getMultipleRecords(String tableName, String indexName,
			Map<String, Object> colValue, boolean isOverride, FilterOperator operator) throws Exception {
		List<Map<String, Object>> result = new ArrayList<>();
		Cursor cursor = getCursor(tableName, indexName, colValue, operator);

		if (!isOverride) {
			isOverride = operator != FilterOperator.IS_EQUAL_TO;
		}

		if (cursor != null) {
			Iterator<Row> iter = cursor.iterator();
			if (colValue == null || isOverride) {
				while (iter.hasNext()) {
					result.add(iter.next());
				}
			} else {
				while (iter.hasNext()) {
					Map<String, Object> map = iter.next();
					if (cursor.currentRowMatches(colValue)) {
						result.add(map);
					}
				}
			}
		}
		return result;
	}

	public Cursor getCursor(String tableName, String indexName, Map<String, Object> colValue, FilterOperator operator)
			throws Exception {
		CursorBuilder cb = getCursorBuilder(tableName, indexName);
		if (cb == null) {
			return null;
		}

		String key = tableName + "." + indexName;

		String[] cols = hTableIndexes.get(key);
		if (operator != FilterOperator.IS_NOT_EQUAL_TO && colValue != null && cols != null) {
			// Read via the Index
			Object[] objs = new Object[cols.length];
			for (int i = 0; i < cols.length; i++) {
				objs[i] = colValue.get(cols[i]);
			}

			boolean inclusiv = true;

			switch (operator) {
			case IS_EQUAL_TO:
				cb.setSpecificEntry(objs);
				break;
			case IS_GREATER_THAN:
				inclusiv = false;
			case IS_GREATER_THAN_OR_EQUAL_TO:
				cb.setStartEntry(objs);
				cb.setStartRowInclusive(inclusiv);
				break;
			case IS_LESS_THAN:
				inclusiv = false;
			case IS_LESS_THAN_OR_EQUAL_TO:
				cb.setEndEntry(objs);
				cb.setEndRowInclusive(inclusiv);
				break;
			default:
				return cb.toIndexCursor();
			}
		}
		// Sequential read
		return cb.toCursor();
	}

	private CursorBuilder getCursorBuilder(String tableName, String indexName) {
		String key = tableName + "." + indexName;
		CursorBuilder result = hTableCursors.get(key);

		if (result == null) {
			try {
				table = database.getTable(tableName);
				result = new CursorBuilder(table);

				if (indexName != null) {
					Index index = table.getIndex(indexName);
					result.setIndex(index);

					List<? extends com.healthmarketscience.jackcess.Index.Column> lCol = index.getColumns();
					String[] cols = new String[lCol.size()];
					for (int i = 0; i < lCol.size(); i++) {
						cols[i] = lCol.get(i).getName();
					}
					hTableIndexes.put(key, cols);
				}
			} catch (Exception ex) {
				return null;
			}
			hTableCursors.put(key, result);
		}
		return result;
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		return hTables.get(myTable).getDbFields();
	}

	public List<FieldDefinition> getTableModelFields(String table) {
		return hTables.get(table).getDbFields();
	}

	public MSTable getMSTable(String table) {
		return hTables.get(table);
	}

	@Override
	public String getPdaDatabase() {
		return myTable;
	}

	@Override
	public List<String> getTableNames() {
		if (aTables == null) {
			return new ArrayList<>();
		}
		return aTables;
	}

	public void setTable(String tableName) throws Exception {
		table = database.getTable(tableName);
		myTotalRecords = table.getRowCount();
		myTable = tableName;
	}

	public Table getTable() {
		return table;
	}

	@Override
	public void verifyDatabase(List<FieldDefinition> newFields) throws Exception {
		table = database.getTable(myTable);
		myTotalRecords = table.getRowCount();
	}

	@Override
	public void closeFile() {
		try {
			database.close();
		} catch (Exception e) {
			// Should not occur
		}
		hTableCursors = null;
		hTableIndexes = null;
		hTables = null;
	}

	public boolean tableColumnExists(String... strings) {
		try {
			switch (strings.length) {
			case 0:
				return false;
			case 1:
				getTableModelFields(strings[0]);
				return true;
			case 2:
				database.getTable(strings[0]).getColumn(strings[1]);
				return true;
			default:
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public void deleteFile() {
		// We don't delete the entire database
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		// Not used
	}

	@Override
	public void createDbHeader() throws Exception {
		// No header is used
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();

		try {
			result = table.getNextRow();
			for (FieldDefinition field : getTableModelFields()) {
				result.put(field.getFieldAlias(), convertObject(result, field));
			}
		} catch (IOException e) {
			// End of file
			table.reset();
		}
		return result;
	}

	public Object convertObject(Map<String, Object> map, FieldDefinition field) {
		Object result = map.get(field.getFieldName());
		if (result == null || result.equals("")) {
			return "";
		}

		switch (field.getFieldType()) {
		case FLOAT:
			return ((Number) result).doubleValue();
		case NUMBER:
			return ((Number) result).intValue();
		case TIMESTAMP:
			return General.convertTimestamp2DB((Date) result);
		default:
			return result;
		}
	}
}
