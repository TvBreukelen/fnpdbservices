package dbengine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;

public class JsonFile extends GeneralDB implements IConvert {
	private File outFile;
	private File backupFile;
	protected ObjectMapper mapper;
	private List<Map<String, Object>> writeList = new ArrayList<>();
	private List<FieldDefinition> dbFields = new ArrayList<>();
	private List<String> hElements;

	private String dbName;
	private int myCurrentRecord;

	public JsonFile(Profiles pref) {
		super(pref);
		mapper = new ObjectMapper();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void openFile(boolean createBackup, boolean isInputFile) throws Exception {
		hasBackup = false;

		if (createBackup) {
			hasBackup = General.copyFile(myFilename, myFilename + ".bak");
		}

		outFile = new File(myFilename);
		backupFile = new File(myFilename + ".bak");
		myCurrentRecord = 0;

		this.isInputFile = isInputFile;
		if (isInputFile) {
			Map<String, Object> map = mapper.readValue(outFile, Map.class);
			if (!map.isEmpty()) {
				Entry<String, Object> entry = map.entrySet().iterator().next();
				dbName = entry.getKey();
				if (entry.getValue() instanceof List) {
					writeList = (List<Map<String, Object>>) entry.getValue();
					myTotalRecords = writeList.size();
					getDBFieldNamesAndTypes();
				} else if (entry.getValue() instanceof Map) {
					writeList.add((Map<String, Object>) entry.getValue());
					myTotalRecords = 1;
					getDBFieldNamesAndTypes();
				} else {
					writeList.add(map);
					myTotalRecords = 1;
					getDBFieldNamesAndTypes();
				}
			}
		} else {
			hElements = new ArrayList<>();
			Map<String, String> map = new HashMap<>();
			dbInfo2Write.forEach(field -> map.putIfAbsent(field.getFieldAlias(), field.getFieldHeader()));
			myPref.getGroupFields().forEach(field -> hElements.add(map.getOrDefault(field, field)));
		}
	}

	@Override
	public void closeFile() {
		try {
			if (!isInputFile && !writeList.isEmpty()) {
				// Indent arrays with a line feed
				DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
				prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
				ObjectWriter writer = mapper.writer(prettyPrinter);

				Map<String, List<Map<String, Object>>> map = new HashMap<>();
				map.put(myPref.getPdaDatabaseName(), writeList);
				writer.writeValue(outFile, map);
			}
		} catch (Exception e) {
			// Nothing that can be done about this
		}
	}

	@Override
	public void deleteFile() {
		closeFile();
		if (outFile.exists()) {
			outFile.delete();
		}
		if (hasBackup) {
			backupFile.renameTo(outFile);
		}
	}

	@SuppressWarnings("unchecked")
	private void getDBFieldNamesAndTypes() {
		dbFields.clear();

		if (writeList.isEmpty()) {
			return;
		}

		Set<String> memoFields = new HashSet<>();
		Map<String, Object> map = writeList.get(0);
		Map<String, Object> newMap = new LinkedHashMap<>();

		map.entrySet().forEach(entry -> addFieldDefinition(entry.getKey(), entry.getValue(), newMap, memoFields));
		map.putAll(newMap);

		// Convert Array fields to Memo fields
		for (String field : memoFields) {
			for (Map<String, Object> mapList : writeList) {
				Object obj = mapList.get(field);
				if (obj == null || obj.equals("")) {
					mapList.put(field, "");
				} else {
					StringBuilder buf = new StringBuilder();
					((List<Object>) obj).forEach(o -> buf.append(o.toString()).append("\n"));
					mapList.put(field, buf.toString().trim());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addFieldDefinition(String name, Object value, Map<String, Object> map, Set<String> memoFields) {
		if (value instanceof Boolean) {
			dbFields.add(new FieldDefinition(name, name, FieldTypes.BOOLEAN));
			return;
		}

		if (value instanceof String) {
			dbFields.add(new FieldDefinition(name, name, FieldTypes.TEXT));
			return;
		}

		if (value instanceof Double) {
			dbFields.add(new FieldDefinition(name, name, FieldTypes.FLOAT));
			return;
		}

		if (value instanceof Integer) {
			dbFields.add(new FieldDefinition(name, name, FieldTypes.NUMBER));
			return;
		}

		if (value instanceof List) {
			dbFields.add(new FieldDefinition(name, name, FieldTypes.MEMO));
			memoFields.add(name);
		}

		if (value instanceof Map) {
			for (Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
				String newName = name + "." + entry.getKey();
				Object obj = entry.getValue();
				map.put(newName, obj);
				addFieldDefinition(newName, obj, map, memoFields);
			}
		}
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		return dbFields;
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		Map<String, Object> map = new LinkedHashMap<>();

		dbInfo2Write.forEach(field -> {
			Object obj = convertDataFields(dbRecord.get(field.getFieldAlias()), field);
			map.putIfAbsent(field.getFieldHeader(), obj);
		});

		if (!map.isEmpty()) {
			if (hElements.isEmpty()) {
				writeList.add(map);
				return;
			}

			createSortedMap(map);
		}
	}

	@SuppressWarnings("unchecked")
	private void createSortedMap(Map<String, Object> dbRecord) {
		Map<String, Object> map;
		if (writeList.isEmpty()) {
			// Create our first entry in the tree
			map = new LinkedHashMap<>();
			writeList.add(map);
		} else {
			map = writeList.get(0);
		}

		for (String element : hElements) {
			String value = dbRecord.get(element).toString();
			dbRecord.remove(element);

			// Get the element list
			List<Map<String, Object>> elementList = (List<Map<String, Object>>) map.computeIfAbsent(element,
					k -> new ArrayList<LinkedHashMap<String, Object>>());

			if (!elementList.isEmpty()) {
				// Get last map in the list
				map = elementList.get(elementList.size() - 1);
				if (map.get(element).equals(value)) {
					// Element value hasn't changed
					continue;
				}
			}

			// Create new map because element value has changed
			map = new LinkedHashMap<>();
			map.put(element, value);
			elementList.add(map);
		}
		map.putAll(dbRecord);
	}

	@Override
	public void createDbHeader() throws Exception {
		// Not used
	}

	@Override
	public void verifyDatabase(List<FieldDefinition> newFields) throws Exception {
		// Not used
	}

	@Override
	public String getPdaDatabase() {
		return dbName;
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = writeList.get(myCurrentRecord);
		myCurrentRecord++;
		return result;
	}
}
