package dbengine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.healthmarketscience.jackcess.Index;

import application.interfaces.FieldTypes;
import application.utils.FieldDefinition;
import application.utils.ini.IniItem;
import application.utils.ini.IniSection;

public class MSTable {
	private String alias = "";
	private String name = "";
	private String index = "";
	private String fromIndex = "";
	private String fromTable = "";

	private Map<String, FieldDefinition> dbFieldsHash;
	private List<FieldDefinition> dbFields;
	private Map<String, Object> columnValues = new HashMap<>();
	private HashSet<String> hIndex = new HashSet<>();

	private boolean isMainLine = false;
	private boolean isVisible = true;
	private boolean isShowAll = false;
	private boolean isNodupIndex = false;
	private boolean isMultiColumnIndex = false;

	public MSTable(String name, String alias) {
		this.alias = alias;
		this.name = name;
	}

	public void setIndexes(List<? extends Index> lIndex) {
		Index idx = lIndex.get(0);
		setIndex(idx.getName());
		isMultiColumnIndex = idx.getColumns().size() > 1;

		for (Index index : lIndex) {
			if (index.isPrimaryKey()) {
				setIndex(index.getName());
				isMultiColumnIndex = index.getColumns().size() > 1;
			}
			hIndex.add(index.getName());
		}
	}

	public boolean isIndexedColumn(String col) {
		return hIndex.contains(col);
	}

	public boolean isMultiColumnIndex() {
		return isMultiColumnIndex;
	}

	public void setMultiColumnIndex(boolean isMultiColumnIndex) {
		this.isMultiColumnIndex = isMultiColumnIndex;
	}

	public void setDbFields(List<FieldDefinition> fields) {
		dbFieldsHash = new HashMap<>();
		dbFields = new ArrayList<>();

		for (FieldDefinition field : fields) {
			FieldDefinition clone = field.copy();
			clone.setTable(alias);
			dbFields.add(clone);
			dbFieldsHash.put(field.getFieldAlias(), field);
		}
	}

	public List<FieldDefinition> getDbFields() {
		return dbFields;
	}

	public void init(String... args) {
		int idx = args.length - 1;
		isShowAll = false;
		isVisible = true;
		isNodupIndex = false;

		boolean isIndexOverride = false;

		// Check for ;Hide or ;ShowAll
		String[] options = args[idx].split(";");
		if (options.length > 1) {
			args[idx] = options[0];

			for (int i = 1; i < options.length; i++) {
				String s = options[i].toUpperCase().trim();

				if (s.equals("HIDE")) {
					isVisible = false; // Hide all fields
				} else if (s.equals("SHOWALL")) {
					isShowAll = true; // Show all fields
				} else if (s.equals("NODUP")) {
					isNodupIndex = true;
					isVisible = false;
				}
			}
		}

		switch (idx) {
		case 2:
			if (!args[2].isEmpty() && hIndex.contains(args[2])) {
				index = args[2];
				fromIndex = index;
				isIndexOverride = true;
			}
		case 1:
			if (!args[1].isEmpty()) {
				int div = args[1].indexOf('.');
				if (div != -1) {
					fromTable = args[1].substring(0, div);
					fromIndex = args[1].substring(div + 1);
				} else {
					fromIndex = args[1];
				}

				if (!isIndexOverride && !index.equals(fromIndex) && hIndex.contains(fromIndex)) {
					index = fromIndex;
				}
			}
		case 0:
			name = args[0];
		default:
			break;
		}
	}

	public boolean isNoDupIndex() {
		return isNodupIndex;
	}

	public String getName() {
		return name;
	}

	public String getAlias() {
		return alias;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getFromIndex() {
		return fromIndex.isEmpty() ? index : fromIndex;
	}

	public String getFromTable() {
		return fromTable;
	}

	public void setFromTable(String table) {
		fromTable = table;
	}

	public void renameFields(IniSection renameSection) {
		if (renameSection == null || renameSection.isEmpty()) {
			return;
		}

		dbFieldsHash = new HashMap<>();

		for (FieldDefinition field : dbFields) {
			String fieldAlias = field.getFieldAlias();
			IniItem iniAlias = renameSection.getItem(fieldAlias);
			String newAlias = iniAlias == null ? null : iniAlias.getValue();

			if (newAlias != null) {
				field.setFieldAlias(newAlias);
				field.setFieldHeader(newAlias);
			} else {
				for (String key : renameSection.getItemNames()) {
					if (key.startsWith(".")) {
						newAlias = field.getTable() + renameSection.getItem(key).getValue();
						key = key.substring(1);
						if (fieldAlias.equals(key)) {
							field.setFieldAlias(newAlias);
							field.setFieldHeader(newAlias);
							field.setHideTable(true);
							break;
						}
					} else if (key.startsWith("*")) {
						key = key.substring(1);
						if (fieldAlias.endsWith(key)) {
							field.setFieldAlias(fieldAlias.substring(0, fieldAlias.length() - key.length()));
							if (field.getFieldAlias().isEmpty()) {
								field.setFieldAlias(alias);
							}
							field.setFieldHeader(field.getFieldAlias());
						}
					}
				}
			}

			dbFieldsHash.put(field.getFieldAlias(), field);
		}
	}

	public void setMainLine(boolean value) {
		isMainLine = value;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isExport) {
		isVisible = isExport;
	}

	public boolean isShowAll() {
		return isShowAll;
	}

	public void setShowAll(boolean isShowAll) {
		this.isShowAll = isShowAll;
	}

	public void changeFieldAlias(String oldAlias, String newAlias, String type) {
		FieldDefinition field = dbFieldsHash.get(oldAlias);
		if (field != null) {
			dbFieldsHash.remove(oldAlias);
			dbFieldsHash.remove(newAlias);

			field.setFieldAlias(newAlias);
			field.setFieldHeader(newAlias);
			field.setHideTable(false);

			if (type != null) {
				field.setFieldType(FieldTypes.getField(type.charAt(0)));
			}
			dbFieldsHash.put(newAlias, field);
		}
	}

	public void updateIndexFields(String oldAlias, String newAlias, String indexField, int indexValue) {
		FieldDefinition field = dbFieldsHash.get(newAlias);

		if (field == null) {
			field = dbFieldsHash.get(oldAlias);
			if (field != null) {
				field = field.copy();
				field.setFieldAlias(newAlias);
				field.setFieldHeader(newAlias);
				dbFieldsHash.put(newAlias, field);
			}
		}

		if (field != null) {
			field.setIndexField(indexField);
			field.setIndexValue(indexValue);
		}
	}

	public List<FieldDefinition> getFields() {
		List<FieldDefinition> result = new ArrayList<>();
		if (isMainLine) {
			result.addAll(dbFieldsHash.values());
		} else {
			for (FieldDefinition field : dbFieldsHash.values()) {
				FieldDefinition clone = field.copy();
				if (!clone.isHideTable()) {
					clone.setFieldAlias(alias
							+ (clone.getTable().equals(clone.getFieldAlias()) ? "" : "." + clone.getFieldAlias()));
					clone.setFieldHeader(clone.getFieldAlias());
				}
				result.add(clone);
			}
		}
		return result;
	}

	public Map<String, FieldDefinition> getDbFieldsHash() {
		return dbFieldsHash;
	}

	public boolean setColumnValues(Map<String, Object> tableMap) {
		Object obj = tableMap.get(getFromIndex());
		if (obj != null) {
			if (obj instanceof Number && ((Number) obj).intValue() == -1) {
				return false;
			}

			columnValues.put(isMultiColumnIndex ? getFromIndex() : index, obj);
			return true;
		}
		return false;
	}

	public Map<String, Object> getColumnValues() {
		return columnValues;
	}

	public MSTable clone(String alias) {
		MSTable result = new MSTable(name, alias);
		result.hIndex.addAll(hIndex);
		result.setIndex(index);
		result.setDbFields(dbFields);
		result.setMultiColumnIndex(isMultiColumnIndex);
		return result;
	}

	@Override
	public String toString() {
		return alias;
	}
}
