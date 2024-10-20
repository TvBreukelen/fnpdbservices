package dbconvert.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import application.BasicSoft;
import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.interfaces.IDatabaseFactory;
import application.model.ViewerModel;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import application.utils.XComparator;
import dbconvert.dialog.ExportProcess;
import dbconvert.preferences.PrefDBConvert;
import dbengine.GeneralDB;
import dbengine.IConvert;
import dbengine.SqlDB;
import dbengine.utils.DatabaseHelper;

public class XConverter extends BasicSoft implements IDatabaseFactory {
	/**
	 * Title: XConverter Description: Database Cross Converter Class Copyright: (c)
	 * 2004-2011
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private IConvert dbIn;
	private DatabaseHelper dbInHelper;
	private DatabaseHelper dbOutHelper;

	private boolean isInputFileOpen = false;
	private boolean isOutputFileOpen = false;

	private String[] dbFilterFields; // All fields that can be filtered
	private String[] dbSortFields; // All fields that can be sorted
	private List<FieldDefinition> dbSelectFields = new ArrayList<>(); // All fields that can be exported
	private Map<String, Object> dbDataRecord = new HashMap<>(); // database record

	private static final String FILTER_FIELD = "{filterfield}";
	private ViewerModel myModel; // Tablemodel containing all records of the inputfile

	/*
	 * Default Constructor (used by ConfigXConverter)
	 */
	public XConverter() {
		super(PrefDBConvert.getInstance());
		dbInHelper = pdaSettings.getFromDatabase();
		dbOutHelper = pdaSettings.getToDatabase();
		dbDataRecord.clear();
		myImportFile = dbInHelper.getDatabaseType();
	}

	// Called via ConfigSoft.verifyDatabase
	public void connect2DB(DatabaseHelper helper) throws Exception {
		close();
		dbInHelper = helper;
		myImportFile = helper.getDatabaseType();
		connect2DB();
	}

	public DatabaseHelper getDbInHelper() {
		return dbInHelper;
	}

	public void connect2DB() throws Exception {
		if (!(myImportFile.isConnectHost() || General.existFile(dbInHelper.getDatabase()))) {
			throw FNProgException.getException("noDatabaseExists", dbInHelper.getDatabaseName());
		}

		firstRecord = myImportFile == ExportFile.EXCEL ? 1 : 0;

		dbIn = (IConvert) new ExportProcess().getDatabase(myImportFile, pdaSettings);
		dbIn.setSoftware(this);
		dbIn.openFile(dbInHelper, true);

		isInputFileOpen = true;
		dbIn.readTableContents();
	}

	@Override
	public IConvert getInputFile() {
		return dbIn;
	}

	@Override
	public Profiles getProfiles() {
		return pdaSettings;
	}

	public void setupDBTranslation(boolean loadFromRegistry) throws Exception {
		// Load filter and mapping fields
		List<FieldDefinition> dbFields = dbIn.getTableModelFields(loadFromRegistry);

		// Load dbFieldDefinition, dbSelectFields and dbFilterFields with all available
		// fields of dbIn
		int maxFields = dbFields.size();
		dbFieldDefinition.clear();
		dbSelectFields.clear();

		List<String> filterFields = new ArrayList<>();
		List<String> sortFields = new ArrayList<>();
		filterFields.add(General.EMPTY_STRING);
		sortFields.add(General.EMPTY_STRING);

		for (int i = 0; i < maxFields; i++) {
			FieldDefinition fieldDef = dbFields.get(i);
			String fieldName = fieldDef.getFieldAlias();

			if (dbFieldDefinition.containsKey(fieldName)) { // Duplicate field name
				int index = 2;
				while (true) {
					StringBuilder buf = new StringBuilder(fieldName.length() + 10);
					buf.append(fieldName);
					buf.append(" (");
					buf.append(index++);
					buf.append(")");
					fieldName = buf.toString();

					if (!dbFieldDefinition.containsKey(fieldName)) {
						fieldDef = fieldDef.copy();
						fieldDef.setFieldAlias(fieldName);
						dbFields.set(i, fieldDef);
						break;
					}
				}
			}

			dbFieldDefinition.put(fieldName, fieldDef);
			dbSelectFields.add(fieldDef);

			if (fieldDef.getFieldType() != FieldTypes.IMAGE) {
				filterFields.add(fieldName);
				if (fieldDef.getFieldType().isSort()) {
					sortFields.add(fieldName);
				}
			}
		}

		dbFilterFields = filterFields.stream().sorted().toArray(String[]::new);
		dbSortFields = sortFields.stream().sorted().toArray(String[]::new);
		refreshUserFields();
	}

	public void sortTableModel() {
		// If we have less than one record or we importing from a SQL database then we
		// don't need to sort
		if (myModel.getRowCount() <= 1 || myImportFile.isSqlDatabase()) {
			return;
		}

		Map<String, FieldTypes> sortList = new LinkedHashMap<>();

		// Load all fields to be sorted in sortList
		if (pdaSettings.isSortFieldDefined()) {
			Set<String> aDuplicate = new HashSet<>();
			pdaSettings.getSortFields().forEach(dbField -> {
				if (!dbField.isEmpty() && !aDuplicate.contains(dbField)) {
					FieldDefinition fieldDef = dbFieldDefinition.get(dbField);
					if (fieldDef != null) {
						sortList.put(fieldDef.getFieldAlias(), fieldDef.getFieldType());
						aDuplicate.add(dbField);
					}
				}
			});
		}

		if (!sortList.isEmpty()) {
			XComparator compare = new XComparator(sortList);
			Collections.sort(myModel.getDataListMap(), compare);
		}
	}

	public void refreshUserFields() {
		// Load user fields from the registry and verify whether they match with the
		// dbFieldDefinition
		boolean isUserListError = false;
		dbUserFields = pdaSettings.getUserList();

		if (!myExportFile.isImageExport()) {
			// Images are not supported
			Iterator<Entry<String, FieldDefinition>> iter = dbFieldDefinition.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, FieldDefinition> entry = iter.next();
				if (entry.getValue().getFieldType() == FieldTypes.IMAGE) {
					iter.remove();
				}
			}
		}

		List<String> usrList = new ArrayList<>();
		isUserListError = verifyUserfields(usrList, null);

		if (isUserListError || dbUserFields.isEmpty()) {
			// Correct the list of selected user fields because they don't match the
			// database definition
			validateUserFields(usrList, true);
		}

		// Verify sort fields
		verifySortFields();

		// Verify filter fields
		verifyFilter();

		// Add special fields for list or Xml to the fields to
		// export and to write, but deactivate their visibility
		for (String dbField : pdaSettings.getSpecialFields()) {
			FieldDefinition fieldDef = dbFieldDefinition.get(dbField);
			if (!dbTableModelFields.contains(fieldDef)) {
				dbTableModelFields.add(new FieldDefinition(dbField, fieldDef.getFieldType(), false));
			}
		}
		setupDbInfoToWrite();
	}

	@Override
	public List<FieldDefinition> getDbSelectFields() {
		return dbSelectFields;
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
	public Map<String, FieldDefinition> getDbFieldDefinition() {
		return new HashMap<>(dbFieldDefinition);
	}

	public void loadInputFile() throws Exception {
		List<FieldDefinition> dbFields = new ArrayList<>(dbIn.getTableModelFields());
		dbFields.add(new FieldDefinition(FILTER_FIELD, FieldTypes.BOOLEAN, false));
		myModel = new ViewerModel(dbFields);

		int emptyRecord = 0;

		List<Map<String, Object>> result = new ArrayList<>();
		Predicate<FieldDefinition> filter = field -> field.getFieldType().isSetFieldSize();

		dbIn.obtainQuery(); // SQL Databases only
		totalRecords = dbIn.getTotalRecords() - firstRecord;

		// Check if there are any records to process
		if (totalRecords == 0) {
			throw FNProgException.getException("noRecordsFound", myImportFile.getName());
		}

		dbIn.executeQuery(); // SQL Databases only

		// Write all records into the table model
		for (int i = 0; i < totalRecords; i++) {
			Map<String, Object> pRead = dbIn.readRecord();
			currentRecord++;

			// Verify if the record to write contains any values
			if (pRead.isEmpty() || pdaSettings.isSkipEmptyRecords() && dbInfoToWrite.stream().noneMatch(field -> !pRead
					.getOrDefault(field.getFieldAlias(), General.EMPTY_STRING).equals(General.EMPTY_STRING))) {
				emptyRecord++;
				continue;
			}

			pRead.put(FILTER_FIELD, false);
			if (!isIncludeRecord(pRead)) {
				emptyRecord++;
				pRead.put(FILTER_FIELD, true);
			}

			result.add(pRead);
			dbInfoToWrite.stream().filter(filter)
					.forEach(field -> field.setSize(pRead.getOrDefault(field.getFieldAlias(), General.EMPTY_STRING)));
		}

		totalRecords -= emptyRecord;
		myModel.setDataListMap(result);
	}

	private boolean isIncludeRecord(Map<String, Object> dbRecord) {
		if (!isFilterDefined || myImportFile.isSqlDatabase()) {
			return true;
		}

		boolean[] isTrue = { true, true };
		for (int i = 0; i < numFilter; i++) {
			isTrue[i] = isIncludeRecord(dbRecord, dbFieldDefinition.get(pdaSettings.getFilterField(i)),
					pdaSettings.getFilterValue(i), pdaSettings.getFilterOperator(i));
		}

		return pdaSettings.getFilterCondition().equals("AND") ? isTrue[0] && isTrue[1] : isTrue[0] || isTrue[1];
	}

	@Override
	public List<Object> getFilterFieldValues(String pField) throws Exception {
		FieldDefinition field = dbFieldDefinition.get(pField);
		List<Object> result = dbIn.getDbFieldValues(field.getFieldName());

		if (result == null) {
			if (myModel == null) {
				// Load entire input file in table model
				loadInputFile();
			}

			// Read values from the table model
			Set<Object> set = new HashSet<>();
			List<Map<String, Object>> table = myModel.getDataListMap();
			table.forEach(m -> set.add(m.getOrDefault(pField, General.EMPTY_STRING)));
			result = new ArrayList<>(set);
		}

		XComparator compare = new XComparator(field.getFieldType());
		Collections.sort(result, compare);
		return result;
	}

	@Override
	public void openToFile() throws Exception {
		dbOut = new ExportProcess().getDatabase(myExportFile, pdaSettings);
		dbOut.setSoftware(this);
		dbOut.openFile(dbOutHelper, false);
		isOutputFileOpen = true;
	}

	public List<String> getTableOrSheetNames() {
		if (dbIn != null) {
			return dbIn.getTableOrSheetNames();
		}
		return new ArrayList<>();
	}

	public GeneralDB getDbOut() {
		return dbOut;
	}

	public SqlDB getSqlDB() {
		return myImportFile.isSqlDatabase() ? (SqlDB) dbIn : null;
	}

	public void checkNumberOfFields() throws FNProgException {
		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
		int userFields = dbInfoToWrite.size();

		if (userFields == 0) {
			throw FNProgException.getException("noFieldsDefined", myExportFile.getName());
		}

		if (userFields > myExportFile.getMaxFields()) {
			throw FNProgException.getException("maxFieldsOverride", Integer.toString(userFields),
					myExportFile.getName(), Integer.toString(myExportFile.getMaxFields()));
		}
	}

	@Override
	protected List<Map<String, Object>> getDataListMap() {
		List<Map<String, Object>> result = new ArrayList<>();
		List<Map<String, Object>> listMap = myModel.getDataListMap();

		setCurrentRecord(0);
		for (Map<String, Object> tableRecord : listMap) {
			if ((boolean) tableRecord.get(FILTER_FIELD)) {
				continue;
			}
			result.add(tableRecord);
			currentRecord++;
		}
		return result;
	}

	@Override
	public void close() {
		if (isInputFileOpen) {
			dbIn.closeFile();
			isInputFileOpen = false;
		}

		if (isOutputFileOpen) {
			dbOut.closeFile();
			isOutputFileOpen = false;
		}
	}

	public void closeFiles(boolean delete) {
		if (!delete) {
			close();
			return;
		}

		if (isInputFileOpen) {
			dbIn.closeFile();
			isInputFileOpen = false;
		}

		if (isOutputFileOpen) {
			dbOut.closeFile();
			if (!myExportFile.isAppend() || dbOut.hasBackup()) {
				dbOut.deleteFile();
			}
			isOutputFileOpen = false;
		}
	}

	@Override
	public boolean isConnected() {
		return isInputFileOpen;
	}

	@Override
	public String getDatabaseFilename() {
		return dbInHelper.getDatabase();
	}

	@Override
	public ExportFile getExportFile() {
		return ExportFile.getExportFile(pdaSettings.getProjectID());
	}
}