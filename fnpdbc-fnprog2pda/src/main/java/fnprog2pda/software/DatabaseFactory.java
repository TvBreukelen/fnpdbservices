package fnprog2pda.software;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.healthmarketscience.jackcess.Cursor;

import application.interfaces.ExportFile;
import application.interfaces.FNPSoftware;
import application.interfaces.FieldTypes;
import application.interfaces.FilterOperator;
import application.interfaces.IDatabaseFactory;
import application.preferences.Profiles;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import application.utils.XComparator;
import application.utils.ini.IniFile;
import application.utils.ini.IniItem;
import application.utils.ini.IniSection;
import dbengine.MSAccess;
import dbengine.utils.DatabaseHelper;
import dbengine.utils.MSTable;
import fnprog2pda.preferences.PrefFNProg;

public final class DatabaseFactory implements IDatabaseFactory {
	/**
	 * Title: DatabaseFactory Description: Class that handles all (MS-Access)
	 * database access for FNProg2PDA Copyright: (c) 2003-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private MSAccess msAccess;

	private FNPSoftware databaseType;
	private int databaseVersion;
	private String[] versions;
	private int maxVersions;
	private int versions2Test;

	private String pdaDatabase;
	private String currentTable;
	private String contentsColumn;
	private IniSection renameSection;

	private Map<String, MSTable> dbTables = new LinkedHashMap<>();
	private Set<String> hShowFields = new HashSet<>();

	private Map<String, FieldDefinition> dbFieldDefinition = new HashMap<>();
	private List<String> dbFilterFields = new ArrayList<>();
	private List<BasisField> dbSelectFields = new ArrayList<>();

	private IniFile ini;

	private DatabaseHelper dbHelper;
	private PrefFNProg pdaSettings = PrefFNProg.getInstance();
	private boolean isConnected = false;

	private static final DatabaseFactory gInstance = new DatabaseFactory();

	private DatabaseFactory() {
		msAccess = new MSAccess(pdaSettings);
	}

	public static DatabaseFactory getInstance() {
		return gInstance;
	}

	@Override
	public Profiles getProfiles() {
		return pdaSettings;
	}

	public void connect2DB(DatabaseHelper helper) throws Exception {
		close();
		dbHelper = helper;

		// Try to obtain the database connection
		try {
			msAccess.openFile(helper, true);
			isConnected = true;
		} catch (Exception e) {
			throw FNProgException.getException("cannotOpen", helper.getDatabase(), e.getMessage());
		}
	}

	@Override
	public void close() {
		msAccess.closeFile();
		isConnected = false;
	}

	public FNPSoftware getDatabaseType() {
		return databaseType;
	}

	public String getDatabaseVersion() {
		return versions[databaseVersion];
	}

	public String getPdaDatabase() {
		return pdaDatabase;
	}

	public String getContentsColumn() {
		return contentsColumn;
	}

	public MSTable getMSTable(String table) {
		return dbTables.get(table);
	}

	public MSTable getMSTable() {
		return dbTables.get(currentTable);
	}

	public Collection<MSTable> getMSTables() {
		return dbTables.values();
	}

	@Override
	public Map<String, FieldDefinition> getDbFieldDefinition() {
		return dbFieldDefinition;
	}

	@Override
	public List<String> getDbFilterFields() {
		return dbFilterFields;
	}

	@Override
	public List<BasisField> getDbSelectFields() {
		return dbSelectFields;
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}

	public void verifyDatabase(String pDatabase) throws Exception {
		boolean isVersionNotFound = true;

		// Load FNProg2PDA properties
		IniFile properties = General.getIniFile("config/FNProg2PDA.ini");
		IniSection section = properties.getSection("Software");

		// Check with which software we are dealing with
		for (FNPSoftware soft : FNPSoftware.values()) {
			if (msAccess.tableColumnExists(section.getItem(soft.getName()).getValue())) {
				databaseType = soft;
				break;
			}
		}

		if (databaseType == null) {
			close();
			throw FNProgException.getException("noFNprogramwareDB", pDatabase, "FNProgramvare");
		}

		section = properties.getSection(databaseType.getName());
		versions = section.getItem("versions").getValue().split(",");
		maxVersions = versions.length;

		for (int i = 0; i < maxVersions; i++) {
			String verify = section.getItem("version" + versions[i] + ".exists").getValue();
			if (verify != null && msAccess.tableColumnExists(verify.split(","))) {
				databaseVersion = i;
				versions2Test = maxVersions - i;
				isVersionNotFound = false;
				break;
			}
		}

		if (isVersionNotFound) {
			close();
			throw FNProgException.getException("noFNprogramwareDB", pDatabase, databaseType.getName());
		}

		pdaDatabase = section.getItem("pda.database.name").getValue();
		renameSection = properties.getSection("FieldRename");
	}

	public void loadConfiguration(String view) throws Exception {
		ini = General.getIniFile("config/" + databaseType.getName() + "_" + view + ".ini");
		currentTable = view;

		dbFieldDefinition.clear();
		dbSelectFields.clear();
		dbFilterFields.clear();
		dbTables.clear();
		dbFilterFields.add("");
		hShowFields.clear();

		MSTable table = msAccess.getMSTable(currentTable);
		if (table == null) {
			Map<String, String> tableHash = getSectionHash("table");
			if (tableHash.containsKey("Name")) {
				table = msAccess.getMSTable(tableHash.get("Name"));
			}
		}

		if (table == null) {
			throw FNProgException.getException("noTable", currentTable);
		}

		table.setMainLine(true);
		table.setShowAll(true);
		table.renameFields(renameSection);
		dbTables.put(view, table);

		if (msAccess.isIndexedSupported()) {
			setContentsDefinition();
			setRoleFieldDefinitions();
			setTableDefinitions();
		}

		setUserFieldDefinitions();
		setDBFieldDefinitions();

		dbSelectFields = dbSelectFields.stream().sorted(Comparator.comparing(BasisField::getFieldAlias)).collect(Collectors.toList());
		dbFilterFields = dbFilterFields.stream().sorted().collect(Collectors.toList());
	}

	private List<IniSection> getSections(String key) {
		List<IniSection> result = new ArrayList<>();
		int index = maxVersions - versions2Test;

		for (int i = index; i < maxVersions; i++) {
			IniSection section = ini.getSection(key + versions[i]);
			if (section != null) {
				result.add(section);
			}
		}

		IniSection main = ini.getSection(key);
		if (main != null) {
			result.add(main);
		}

		return result;
	}

	private void setContentsDefinition() {
		Map<String, String> tableHash = getSectionHash("table");
		contentsColumn = tableHash.get("Contents");
		if (contentsColumn == null) {
			return;
		}

		FieldDefinition field = new FieldDefinition(contentsColumn, FieldTypes.MEMO, true);
		field.setTable(contentsColumn);
		field.setContentsField(true);
		dbSelectFields.add(field);
		dbFieldDefinition.put(contentsColumn, field);
	}

	private void setRoleFieldDefinitions() {
		Map<String, String> personHash = getSectionHash("personColumns");
		if (!personHash.isEmpty()) {
			for (String key : personHash.keySet()) {
				List<FieldDefinition> list = null;
				try {
					list = msAccess.getTableModelFields(key);
				} catch (Exception e) {
					continue;
				}

				String[] personFields = personHash.get(key).split(",");
				for (String s : personFields) {
					for (FieldDefinition field : list) {
						if (field.getFieldName().equals(s)) {
							field.setRoleField(true);
							break;
						}
					}
				}
			}
		}
	}

	private void setTableDefinitions() {
		Set<String> hMainLine = getSectionHash("mainLineTables", "tables");
		Map<String, String> tableHash = getSectionHash("tables");

		for (String key : tableHash.keySet()) {
			if (dbTables.containsKey(key)) {
				continue;
			}

			String[] init = tableHash.get(key).split(",");
			String[] info = init[0].split(";");
			String origTable = info[0];

			MSTable table = msAccess.getMSTable(origTable);
			if (table == null) {
				continue;
			}

			if (!key.equals(origTable)) {
				table = table.clone(key);
			}

			// Update from table -name and -index
			table.init(init);
			table.setMainLine(hMainLine.contains(key));

			if (table.getFromTable().isEmpty()) {
				table.setFromTable(currentTable);
			}

			dbTables.put(key, table);
		}

		for (MSTable table : dbTables.values()) {
			table.renameFields(renameSection);
		}
	}

	private void setUserFieldDefinitions() {
		Map<String, String> fieldHash = getSectionHash("userfields");
		String imageField = null;

		for (String newAlias : fieldHash.keySet()) {
			String field = newAlias;
			hShowFields.add(newAlias);

			String oldAlias = fieldHash.get(newAlias);
			String type = null;
			String table = currentTable;
			String indexField = null;
			int indexValue = 0;

			int index = oldAlias.indexOf(",");
			if (index != -1) {
				type = oldAlias.substring(index + 1);
				oldAlias = oldAlias.substring(0, index);

				index = type.indexOf(";");
				if (index != -1) {
					// Image Field
					String[] split = type.substring(index + 1).split("=");
					indexField = split[0];
					indexValue = Integer.parseInt(split[1]);
					type = type.substring(0, index);
				}
			}

			index = newAlias.indexOf(".");
			if (index != -1) {
				table = newAlias.substring(0, index);
				newAlias = newAlias.substring(index + 1);
			}

			index = oldAlias.indexOf(".");
			if (index != -1) {
				table = oldAlias.substring(0, index);
				oldAlias = oldAlias.substring(index + 1);
			}

			if (!table.isEmpty()) {
				MSTable msTable = dbTables.get(table);
				if (msTable != null) {
					if (!msTable.isVisible()) {
						hShowFields.remove(field);
					}
					if (indexField != null) {
						if (indexValue > 1) {
							msTable.updateIndexFields(imageField, newAlias, indexField, indexValue);
						} else {
							msTable.changeFieldAlias(oldAlias, newAlias, type);
							msTable.updateIndexFields(newAlias, newAlias, indexField, indexValue);
							imageField = newAlias;
						}
					} else {
						msTable.changeFieldAlias(oldAlias, newAlias, type);
					}
				}
			}
		}
	}

	private void setDBFieldDefinitions() {
		HashSet<String> hHidden = getSectionHash("hideColumns", "columns");
		for (MSTable table : dbTables.values()) {
			HashSet<String> hHiddenColumns = getSectionHash("hideColumns", table.getName());
			for (FieldDefinition field : table.getFields()) {
				String alias = field.getFieldAlias();

				if (dbFieldDefinition.containsKey(alias) || field.getFieldType() == FieldTypes.UNKNOWN) {
					continue;
				}

				dbFieldDefinition.put(alias, field);
				if (!hShowFields.contains(alias)) {
					if (!table.isVisible()
							|| !(table.isShowAll() || field.getFieldName().indexOf("Sort") != -1
									|| alias.equals(field.getTable()))
							|| hHidden.contains(field.getFieldName())
							|| hHiddenColumns.contains(field.getFieldName())) {
						continue;
					}
				}

				dbSelectFields.add(field);
				if (field.getFieldType() != FieldTypes.IMAGE) {
					dbFilterFields.add(alias);
				}
			}
		}
	}

	private HashSet<String> getSectionHash(String sectionName, String key) {
		HashSet<String> result = new HashSet<>();
		IniSection section = ini.getSection(sectionName);
		if (section == null || section.isEmpty()) {
			return result;
		}

		IniItem item = section.getItem(key);
		if (item == null) {
			return result;
		}

		String[] values = item.getValue().split(",");
		for (String s : values) {
			result.add(s);
		}
		return result;
	}

	private Map<String, String> getSectionHash(String sectionName) {
		Map<String, String> result = new LinkedHashMap<>();
		List<IniSection> list = getSections(sectionName);
		for (IniSection section : list) {
			for (String name : section.getItemNames()) {
				if (!result.containsKey(name)) {
					result.put(name, section.getItem(name).getValue());
				}
			}
		}
		return result;
	}

	public boolean isValidField(String pField) {
		return dbFieldDefinition.get(pField) != null;
	}

	@Override
	public List<Object> getDbFieldValues(String pField) throws Exception {
		// Check if the field is valid
		FieldDefinition dbField = dbFieldDefinition.get(pField);
		List<Object> result = new ArrayList<>();

		if (dbField == null) {
			// Should not occur !!
			return result;
		}

		MSTable table = dbTables.get(dbField.getTable());

		if (table != null) {
			boolean isUseLink = !table.getFromTable().isEmpty();
			HashMap<String, Object> linkMap = new HashMap<>();

			List<Map<String, Object>> list = msAccess.getMultipleRecords(table.getName());
			if (!list.isEmpty()) {
				for (Map<String, Object> map : list) {
					Object obj = msAccess.convertObject(map, dbField);
					if (!obj.equals("") && !result.contains(obj)) {
						if (isUseLink) {
							try {
								linkMap.put(table.getFromIndex(), map.get(table.getIndex()));
								Cursor cursor = msAccess.getCursor(table.getFromTable(), table.getFromIndex(), linkMap,
										FilterOperator.ISEQUALTO);
								if (cursor.getNextRow() != null) {
									result.add(obj);
								}
							} catch (Exception ex) {
							}
						} else {
							result.add(obj);
						}
					}
				}
			}

			// Sort objects
			XComparator compare = new XComparator(dbField.getFieldType());
			Collections.sort(result, compare);
		}
		return result;
	}

	public MSAccess getMSAccess() {
		return msAccess;
	}

	@Override
	public String getDatabaseFilename() {
		return dbHelper.getDatabase();
	}

	@Override
	public ExportFile getExportFile() {
		return ExportFile.getExportFile(pdaSettings.getProjectID());
	}

	@Override
	public boolean isDbConvert() {
		return false;
	}
}
