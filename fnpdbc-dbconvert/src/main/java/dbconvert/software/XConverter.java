package dbconvert.software;

import java.awt.Component;
import java.beans.PropertyChangeSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import application.BasicSoft;
import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.interfaces.IDatabaseFactory;
import application.interfaces.TvBSoftware;
import application.model.ViewerModel;
import application.preferences.Databases;
import application.preferences.Profiles;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.XComparator;
import dbconvert.dialog.ExportProcess;
import dbconvert.preferences.PrefDBConvert;
import dbengine.GeneralDB;
import dbengine.IConvert;
import dbengine.SqlDB;
import dbengine.export.CsvFile;
import dbengine.export.HanDBase;
import dbengine.utils.DatabaseHelper;

public class XConverter extends BasicSoft implements IDatabaseFactory {
	/**
	 * Title: XConverter Description: Database Cross Converter Class Copyright: (c)
	 * 2004-2011
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private String[] myFile;
	private IConvert dbIn;
	private GeneralDB dbOut;
	private DatabaseHelper dbInHelper;

	private boolean isInputFileOpen = false;
	private boolean isOutputFileOpen = false;

	private String[] dbFilterFields = new String[0]; // All fields that can be filtered or sorted
	private List<FieldDefinition> dbSelectFields; // All fields that can be exported
	private Map<String, Object> dbDataRecord = new HashMap<>(); // database record

	private static final String FILTER_FIELD = "{filterfield}";

	private PropertyChangeSupport support;
	private String recordsRead;

	private ViewerModel myModel; // Tablemodel containing all records of the inputfile

	/*
	 * Default Constructor (used by ConfigXConverter)
	 */
	public XConverter() {
		super(PrefDBConvert.getInstance());
		Databases dbSettings = Databases.getInstance(TvBSoftware.DBCONVERT);
		dbInHelper = new DatabaseHelper(dbSettings);

		myFile = new String[2];
		myFile[0] = dbSettings.getDatabase();
		myFile[1] = pdaSettings.getExportFile();

		dbDataRecord.clear();
		myImportFile = dbInHelper.getDatabaseType();
		support = new PropertyChangeSupport(this);
	}

	// Called via ConfigSoft.verifyDatabase
	public void connect2DB(DatabaseHelper helper) throws Exception {
		close();
		dbInHelper = helper;
		myFile[0] = helper.getDatabase();
		myImportFile = helper.getDatabaseType();
		connect2DB();
	}

	public DatabaseHelper getDbInHelper() {
		if (dbInHelper == null) {
			dbInHelper = getNewDbInHelper(myImportFile);
		}
		return dbInHelper;
	}

	public DatabaseHelper getNewDbInHelper(ExportFile importFile) {
		myImportFile = importFile;
		dbInHelper = new DatabaseHelper("", importFile);
		return dbInHelper;
	}

	public void connect2DB() throws Exception {
		if (!myImportFile.isConnectHost() && !General.existFile(myFile[0])) {
			throw FNProgException.getException("noDatabaseExists", myFile[0]);
		}

		if (myImportFile == ExportFile.EXCEL) {
			firstRecord = 1;
		} else {
			firstRecord = myImportFile == ExportFile.MOBILEDB ? 4 : 0;
		}

		dbIn = (IConvert) ExportProcess.getDatabase(myImportFile, pdaSettings);
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

	public void setupDBTranslation(boolean isNew) throws Exception {
		// Load filter and mapping fields
		List<FieldDefinition> dbFields = dbIn.getTableModelFields();

		// Load dbFieldDefinition, dbSelectFields and dbFilterFields with all available
		// fields of dbIn
		int maxFields = dbFields.size();
		dbFieldDefinition = new HashMap<>(maxFields);
		dbSelectFields = new ArrayList<>();

		List<String> filterFields = new ArrayList<>();
		filterFields.add("");

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
			filterFields.add(fieldName);
		}

		dbFilterFields = filterFields.stream().sorted().toArray(String[]::new);

		if (isNew) {
			dbUserFields = new ArrayList<>();
		} else {
			refreshUserFields();
		}
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
		dbTableModelFields.clear();
		dbUserFields = pdaSettings.getUserList();
		List<String> usrList = new ArrayList<>();

		for (BasisField field : dbUserFields) {
			FieldDefinition dbField = dbFieldDefinition.get(field.getFieldAlias());
			if (dbField == null) {
				isUserListError = true;
				continue;
			}

			dbField = dbField.copy();
			field.setFieldType(dbField.getFieldType()); // Just incase the type has changed
			dbField.set(field);

			dbTableModelFields.add(dbField);
			usrList.add(dbField.getFieldAlias());
		}

		if (isUserListError || dbUserFields.isEmpty()) {
			// Correct the list of selected user fields because they don't match the
			// database definition
			validateUserFields(usrList, true);
		}

		// Add special fields for list or Xml to the fields to
		// export and to write, but deactivate their visibility
		for (String dbField : pdaSettings.getSpecialFields()) {
			FieldDefinition fieldDef = dbFieldDefinition.get(dbField);
			if (fieldDef == null) {
				pdaSettings.removeSortField(dbField);
				pdaSettings.removeGroupField(dbField);
				continue;
			}

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
	public Map<String, FieldDefinition> getDbFieldDefinition() {
		return new HashMap<>(dbFieldDefinition);
	}

	public void loadInputFile() throws Exception {
		verifyFilter();

		List<FieldDefinition> dbFields = new ArrayList<>(dbIn.getTableModelFields());
		dbFields.add(new FieldDefinition(FILTER_FIELD, FieldTypes.BOOLEAN, false));
		myModel = new ViewerModel(dbFields);

		// Get Category Field
		String categoryField = pdaSettings.getCategoryField();
		myCategories.clear();
		int catCount = 0;
		int emptyRecord = 0;

		List<Map<String, Object>> result = new ArrayList<>();
		Predicate<FieldDefinition> filter = field -> field.getFieldType().isSetFieldSize();

		totalRecords = dbIn.getTotalRecords() - firstRecord;

		// Check if there are any records to process
		if (totalRecords == 0) {
			throw FNProgException.getException("noRecordsFound", myImportFile.getName());
		}

		if (myImportFile.isSqlDatabase()) {
			((SqlDB) dbIn).createQueryStatement();
		}

		// Write all records into the table model
		for (int i = 0; i < totalRecords; i++) {
			Map<String, Object> pRead = dbIn.readRecord();

			// Verify if the record to write contains any values
			if (pRead.isEmpty() || pdaSettings.isSkipEmptyRecords() && dbInfoToWrite.stream()
					.noneMatch(field -> !pRead.getOrDefault(field.getFieldAlias(), "").equals(""))) {
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
					.forEach(field -> field.setSize(pRead.getOrDefault(field.getFieldAlias(), "")));

			// Check if we have to load the List categories
			if (!categoryField.isEmpty() && catCount < LISTDB_MAX_CATEGORIES) {
				String s = pRead.get(categoryField).toString();
				if (s.length() > 15) {
					s = s.substring(0, 15);
				}
				if (!myCategories.contains(s)) {
					myCategories.add(s);
					catCount++;
				}
			}
		}

		Collections.sort(myCategories);
		myCategories.add(0, "Unfiled");
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
			table.forEach(m -> set.add(m.getOrDefault(pField, "")));
			result = new ArrayList<>(set);
		}

		XComparator compare = new XComparator(field.getFieldType());
		Collections.sort(result, compare);
		return result;
	}

	public void openToFile() throws Exception {
		dbOut = ExportProcess.getDatabase(myExportFile, pdaSettings);
		dbOut.setSoftware(this);
		dbOut.openFile(new DatabaseHelper(myFile[1], myExportFile), false);
		isOutputFileOpen = true;
	}

	public String getPdaDatabase() {
		if (dbIn == null) {
			return null;
		}

		String result = dbIn.getPdaDatabase();
		if (result == null) {
			result = myFile[0].substring(myFile[0].lastIndexOf(System.getProperty("file.separator", "\\")) + 1,
					myFile[0].lastIndexOf('.'));
		}
		return result;
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

	public void checkNumberOfFields() throws Exception {
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
			dbOut.deleteFile();
			isOutputFileOpen = false;
		}
	}

	@Override
	public boolean isConnected() {
		return isInputFileOpen;
	}

	public void runConversionProgram(Component parent) throws Exception {
		if (myExportFile == ExportFile.HANDBASE) {
			((HanDBase) dbOut).runConversionProgram(pdaSettings);
		}

		if (myExportFile == ExportFile.TEXTFILE) {
			General.showMessage(parent,
					GUIFactory.getMessage("createdFiles",
							((CsvFile) dbOut).getExportFiles(GUIFactory.getText("file"), GUIFactory.getText("files"))),
					GUIFactory.getTitle("information"), false);
		} else {
			General.showMessage(parent, GUIFactory.getMessage("createdFile", pdaSettings.getExportFile()),
					GUIFactory.getTitle("information"), false);
		}

		// Save last export date
		pdaSettings.setLastModified(General.convertTimestamp(LocalDateTime.now(), General.sdInternalTimestamp));
	}

	public void setRecordsRead(String value) {
		support.firePropertyChange("recordsRead", recordsRead, value);
		recordsRead = value;
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