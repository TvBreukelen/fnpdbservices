package fnprog2pda.software;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.healthmarketscience.jackcess.Cursor;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.interfaces.FilterOperator;
import application.interfaces.IDatabaseFactory;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import application.utils.XComparator;
import dbengine.IConvert;
import dbengine.utils.DatabaseHelper;
import fnprog2pda.dbengine.MSAccess;
import fnprog2pda.dbengine.utils.MSTable;
import fnprog2pda.preferences.PrefFNProg;
import fnprog2pda.utils.IniFile;
import fnprog2pda.utils.IniFileReader;
import fnprog2pda.utils.IniItem;
import fnprog2pda.utils.IniSection;

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
	private String[] dbFilterFields;
	private String[] dbSortFields;
	private List<FieldDefinition> dbSelectFields = new ArrayList<>();

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
		msAccess.openFile(helper, true);
		isConnected = true;
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
	public String[] getDbFilterFields() {
		return dbFilterFields;
	}

	@Override
	public String[] getDbSortFields() {
		return dbSortFields;
	}

	@Override
	public List<FieldDefinition> getDbSelectFields() {
		return dbSelectFields;
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}

	public void verifyDatabase(String pDatabase) throws FNProgException {
		boolean isVersionNotFound = true;

		// Load FNProg2PDA properties
		IniFile properties = getIniFile("config/FNProg2PDA.ini");
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

	public void loadConfiguration(String view) throws FNProgException {
		ini = getIniFile("config/" + databaseType.getName() + "_" + view + ".ini");
		currentTable = view;

		dbFieldDefinition.clear();
		dbSelectFields.clear();
		dbTables.clear();

		List<String> filterFields = new ArrayList<>();
		List<String> sortFields = new ArrayList<>();

		filterFields.add(General.EMPTY_STRING);
		sortFields.add(General.EMPTY_STRING);
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
		table.setFromTable(currentTable);
		table.renameFields(renameSection);
		dbTables.put(view, table);

		if (msAccess.isIndexedSupported()) {
			setContentsDefinition();
			setRoleFieldDefinitions();
			setTableDefinitions();
		}

		setUserFieldDefinitions();
		setDBFieldDefinitions(filterFields, sortFields);

		dbSelectFields = dbSelectFields.stream().sorted(Comparator.comparing(FieldDefinition::getFieldAlias))
				.collect(Collectors.toList());
		dbFilterFields = filterFields.stream().sorted().toArray(String[]::new);
		dbSortFields = sortFields.stream().sorted().toArray(String[]::new);
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
			for (Entry<String, String> entry : personHash.entrySet()) {
				List<FieldDefinition> list = null;
				try {
					list = msAccess.getTableModelFields(entry.getKey());
				} catch (Exception e) {
					continue;
				}

				String[] personFields = entry.getValue().split(",");
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

		for (Entry<String, String> entry : tableHash.entrySet()) {
			String[] init = entry.getValue().split(",");
			String[] info = init[0].split(";");
			String origTable = info[0];

			MSTable table = msAccess.getMSTable(origTable);

			if (table == null || dbTables.containsKey(entry.getKey())) {
				continue;
			}

			if (!entry.getKey().equals(origTable)) {
				table = table.clone(entry.getKey());
			}

			// Update from table -name and -index
			table.init(init);
			table.setMainLine(hMainLine.contains(entry.getKey()));

			if (table.getFromTable().isEmpty()) {
				table.setFromTable(currentTable);
			}

			dbTables.put(entry.getKey(), table);
		}

		for (MSTable table : dbTables.values()) {
			table.renameFields(renameSection);
		}
	}

	private void setUserFieldDefinitions() {
		Map<String, String> fieldHash = getSectionHash("userfields");
		String imageField = null;

		for (Entry<String, String> entry : fieldHash.entrySet()) {
			String field = entry.getKey();
			hShowFields.add(field);

			String oldAlias = entry.getValue();
			String newAlias = field;

			String type = null;
			String table = currentTable;
			String newTable = table;

			String indexField = null;
			int indexValue = 0;

			int index = oldAlias.indexOf(',');
			if (index != -1) {
				type = oldAlias.substring(index + 1);
				oldAlias = oldAlias.substring(0, index);

				index = type.indexOf(';');
				if (index != -1) {
					// Image Field
					String[] split = type.substring(index + 1).split("=");
					indexField = split[0];
					indexValue = Integer.parseInt(split[1]);
					type = type.substring(0, index);
				}
			}

			index = field.indexOf('.');
			if (index != -1) {
				newTable = field.substring(0, index);
				newAlias = newAlias.substring(index + 1);
				table = newTable;
			}

			index = oldAlias.indexOf('.');
			if (index != -1) {
				table = oldAlias.substring(0, index);
				oldAlias = oldAlias.substring(index + 1);
			}

			if (newTable.equals(currentTable)) {
				newTable = null;
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
							msTable.changeFieldAlias(oldAlias, newAlias, newTable, type);
							msTable.updateIndexFields(newAlias, newAlias, indexField, indexValue);
							imageField = newAlias;
						}
					} else {
						msTable.changeFieldAlias(oldAlias, newAlias, newTable, type);
					}
				}
			}
		}
	}

	private void setDBFieldDefinitions(List<String> filterFields, List<String> sortFields) {
		Set<String> hHidden = getSectionHash("hideColumns", "columns");
		for (MSTable table : dbTables.values()) {
			boolean ishideIDs = !table.getName().equals(currentTable);

			Set<String> hHiddenColumns = getSectionHash("hideColumns", table.getName());
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
							|| hHidden.contains(field.getFieldName()) || hHiddenColumns.contains(field.getFieldName())
							|| ishideIDs && alias.endsWith("ID")) {
						continue;
					}
				}

				dbSelectFields.add(field);
				if (field.getFieldType() != FieldTypes.IMAGE) {
					filterFields.add(alias);
					if (field.getFieldType().isSort()) {
						sortFields.add(alias);
					}
				}
			}
		}
	}

	private Set<String> getSectionHash(String sectionName, String key) {
		Set<String> result = new HashSet<>();
		IniSection section = ini.getSection(sectionName);
		if (section == null || section.isEmpty()) {
			return result;
		}

		IniItem item = section.getItem(key);
		if (item == null) {
			return result;
		}

		String[] values = item.getValue().split(",");
		result.addAll(Arrays.asList(values));
		return result;
	}

	private Map<String, String> getSectionHash(String sectionName) {
		Map<String, String> result = new LinkedHashMap<>();
		List<IniSection> list = getSections(sectionName);
		for (IniSection section : list) {
			for (String name : section.getItemNames()) {
				result.putIfAbsent(name, section.getItem(name).getValue());
			}
		}
		return result;
	}

	public boolean isValidField(String pField) {
		return dbFieldDefinition.get(pField) != null;
	}

	@Override
	public List<Object> getFilterFieldValues(String pField) throws Exception {
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
			Map<String, Object> linkMap = new HashMap<>();

			List<Map<String, Object>> list = msAccess.getMultipleRecords(table.getName());
			if (CollectionUtils.isNotEmpty(list)) {
				for (Map<String, Object> map : list) {
					Object obj = msAccess.convertObject(map, dbField);
					if (!obj.equals(General.EMPTY_STRING) && !result.contains(obj)) {
						if (isUseLink) {
							try {
								linkMap.put(table.getFromIndex(), map.get(table.getIndex()));
								Cursor cursor = msAccess.getCursor(table.getFromTable(), table.getFromIndex(), linkMap,
										FilterOperator.IS_EQUAL_TO);
								if (cursor.getNextRow() != null) {
									result.add(obj);
								}
							} catch (Exception ex) {
								// Should be logged
							}
						} else {
							result.add(obj);
						}
					}
				}
			}

			if (dbField.getFieldType() == FieldTypes.TEXT) {
				result.add(General.EMPTY_STRING);
			}
			// Sort objects
			XComparator compare = new XComparator(dbField.getFieldType());
			Collections.sort(result, compare);
		}
		return result;
	}

	@Override
	public IConvert getInputFile() {
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

	public IniFile getIniFile(String file) {
		IniFile result = new IniFile();

		try (BufferedReader reader = new BufferedReader(General.getInputStreamReader(file))) {
			IniFileReader iniReader = new IniFileReader(result, reader);
			iniReader.read();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
