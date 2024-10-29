package fnprog2pda.software;

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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
	private String databaseVersion;
	private List<String> versions;
	private int maxVersions;
	private int versions2Test;

	private String pdaDatabase;
	private String currentTable;
	private String contentsColumn;
	private Map<String, String> renameSection;
	private Map<String, Object> fnpMap;

	private Map<String, MSTable> dbTables = new LinkedHashMap<>();
	private Set<String> hShowFields = new HashSet<>();

	private Map<String, FieldDefinition> dbFieldDefinition = new HashMap<>();
	private String[] dbFilterFields;
	private String[] dbSortFields;
	private List<FieldDefinition> dbSelectFields = new ArrayList<>();

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
		return databaseVersion;
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

	@SuppressWarnings("unchecked")
	public void verifyDatabase(String pDatabase) throws FNProgException {
		boolean isVersionNotFound = true;

		// Load FNProg2PDA properties
		Map<String, Object> map = loadYamlFile("config/FNProg2PDA.yaml");

		// Check with which software we are dealing with
		Map<String, Object> software = (Map<String, Object>) map.get("Software");
		for (FNPSoftware soft : FNPSoftware.values()) {
			Optional<Object> table = Optional.ofNullable(software.get(soft.getName()));
			if (table.isPresent() && msAccess.tableColumnExists(table.get().toString())) {
				databaseType = soft;
				break;
			}
		}

		if (databaseType == null) {
			close();
			throw FNProgException.getException("noFNprogramwareDB", pDatabase, "FNProgramvare");
		}

		Map<String, Object> fnp = (Map<String, Object>) map.get(databaseType.getName());
		versions = (List<String>) fnp.get("versions");
		maxVersions = versions.size();
		versions2Test = maxVersions;

		for (String version : versions) {
			Optional<Object> verify = Optional.ofNullable(fnp.get("version" + version + ".exists"));
			if (verify.isPresent() && msAccess.tableColumnExists(verify.get().toString().split(","))) {
				databaseVersion = version;
				isVersionNotFound = false;
				break;
			}
			versions2Test--;
		}

		if (isVersionNotFound) {
			close();
			throw FNProgException.getException("noFNprogramwareDB", pDatabase, databaseType.getName());
		}

		pdaDatabase = fnp.get("pda.database.name").toString();
		renameSection = (Map<String, String>) map.get("FieldRename");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadYamlFile(String path) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Map<String, Object> result;
		try {
			result = mapper.readValue(General.getInputStreamReader(path), Map.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public void loadConfiguration(String view) throws FNProgException {
		// Load software properties
		fnpMap = loadYamlFile("config/" + databaseType.getName() + "_" + view + ".yaml");
		currentTable = view;

		dbFieldDefinition.clear();
		dbSelectFields.clear();
		dbTables.clear();

		List<String> filterFields = new ArrayList<>();
		List<String> sortFields = new ArrayList<>();

		filterFields.add(General.EMPTY_STRING);
		sortFields.add(General.EMPTY_STRING);
		hShowFields.clear();

		Map<String, String> tableHash = getSectionHash("table");
		MSTable table = msAccess.getMSTable(tableHash.get("Name"));
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
		if (personHash.isEmpty()) {
			return;
		}

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

	@SuppressWarnings("unchecked")
	private Set<String> getSectionHash(String sectionName, String key) {
		Set<String> result = new HashSet<>();
		Map<String, String> section = (Map<String, String>) fnpMap.get(sectionName);
		if (section == null || section.isEmpty()) {
			return result;
		}

		String item = section.get(key);
		if (item == null) {
			return result;
		}

		String[] values = item.split(",");
		result.addAll(Arrays.asList(values));
		return result;
	}

	private Map<String, String> getSectionHash(String sectionName) {
		Map<String, String> result = new LinkedHashMap<>();
		int index = maxVersions - versions2Test;

		for (int i = index; i < maxVersions; i++) {
			getSection(sectionName + versions.get(i), result);
		}

		getSection(sectionName, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	private void getSection(String sectionName, Map<String, String> result) {
		Map<String, String> section = (Map<String, String>) fnpMap.get(sectionName);
		if (section != null) {
			section.entrySet().forEach(item -> result.putIfAbsent(item.getKey(), item.getValue()));
		}
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
}
