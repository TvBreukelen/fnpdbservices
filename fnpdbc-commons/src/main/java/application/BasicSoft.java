package application;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.interfaces.FilterOperator;
import application.model.ViewerModel;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.export.CsvFile;
import dbengine.export.HanDBase;
import dbengine.utils.DatabaseHelper;

public abstract class BasicSoft {
	/**
	 * Title: BasicSoft
	 *
	 * @description: Basic abstract class for all software classes (FNProgramvare +
	 *               DBConvert) Copyright: (c) 2006-2019
	 *
	 * @author Tom van Breukelen
	 * @version 6.0
	 */
	protected int totalRecords = 0;
	protected int firstRecord = 0;
	protected int currentRecord = 0;
	protected int writtenRecords = 0;

	protected List<FieldDefinition> dbTableModelFields = new ArrayList<>(); // All fields loaded in the TableModel
	protected List<FieldDefinition> dbInfoToWrite = new ArrayList<>(); // Export fields for the export file

	// Definition of all fields in the database
	protected Map<String, FieldDefinition> dbFieldDefinition = new HashMap<>();

	// User defined fields (note: userFields + dbSpecialFields = dbInfoToWrite)
	protected List<BasisField> dbUserFields = new ArrayList<>();

	protected boolean isFilterDefined;
	protected ExportFile myExportFile = ExportFile.EXCEL;
	protected ExportFile myImportFile = ExportFile.EXCEL;

	protected Profiles pdaSettings;
	protected GeneralDB dbOut;

	protected int numFilter;
	protected Timer timer;

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	protected BasicSoft(Profiles profile) {
		pdaSettings = profile;
		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
	}

	public void addObserver(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
		pcs.addPropertyChangeListener(listener);

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				setCurrentRecord(currentRecord);
			}
		}, 1000, 500);
	}

	public void setCurrentRecord(int value) {
		int oldValue = value == totalRecords ? 0 : totalRecords;
		currentRecord = value;
		if (value > 0) {
			pcs.firePropertyChange("currentRecord", oldValue, currentRecord);
		}
	}

	public ExportFile getImportFile() {
		return myImportFile;
	}

	public ExportFile getExportFile() {
		return myExportFile;
	}

	/* Read the input file and load the input records in the TableModel */
	public void processFiles(ViewerModel tabModel) throws Exception {
		setCurrentRecord(0);

		try {
			// Load TableModel
			tabModel.setDataListMap(getDataListMap());
		} finally {
			timer.cancel();
			setCurrentRecord(totalRecords);
		}
	}

	/* Write the TableModel data into the ExportFile */
	public void convertFromTableModel(ViewerModel tabModel) throws Exception {
		List<Map<String, Object>> table = tabModel.getDataListMap();

		setCurrentRecord(0);
		totalRecords = table.size();
		writtenRecords = 0;

		boolean isHeaderCreated = false;
		boolean isToFileOpened = false;

		try {
			openToFile(); // Sets dbOut
			isToFileOpened = true;
			dbOut.createDbHeader();
			isHeaderCreated = true;
			for (Map<String, Object> rowData : table) {
				Map<String, Object> dbRecord = new HashMap<>();
				dbTableModelFields.forEach(field -> dbRecord.putIfAbsent(field.getFieldAlias(),
						rowData.getOrDefault(field.getFieldAlias(), General.EMPTY_STRING)));
				writtenRecords += dbOut.processData(dbRecord);
				currentRecord++;
			}
		} catch (Exception e) {
			if (e instanceof FNProgException) {
				throw e;
			}

			String mesg = "cannotOpen";
			if (isToFileOpened) {
				mesg = "cannotCreateTable";
			}
			if (isHeaderCreated) {
				mesg = "cannotWrite";
			}

			DatabaseHelper helper = dbOut.getDatabaseHelper();
			throw FNProgException.getException(mesg, helper.getDatabaseName(), e.getMessage(),
					pdaSettings.getTableName());
		} finally {
			timer.cancel();
			setCurrentRecord(totalRecords);
			if (isHeaderCreated) {
				dbOut.closeData();

			}
		}
	}

	public abstract void openToFile() throws Exception;

	public List<FieldDefinition> getDbInfoToWrite() {
		return dbInfoToWrite;
	}

	public List<FieldDefinition> getTableModelFields() {
		return dbTableModelFields;
	}

	public List<BasisField> getDbUserFields() {
		return new ArrayList<>(dbUserFields);
	}

	protected void validateUserFields(List<String> validFields, boolean isDBConvert) {
		if (validFields.isEmpty()) {
			dbUserFields.clear();
			if (isDBConvert) {
				dbTableModelFields.forEach(b -> dbUserFields.add(new BasisField(b)));
			}
			return;
		}

		dbUserFields = dbUserFields.stream().filter(b -> validFields.contains(b.getFieldAlias()))
				.collect(Collectors.toList());
	}

	protected boolean verifyUserfields(List<String> usrList, Map<String, FieldDefinition> dbInfoToExport) {
		boolean result = false;
		dbTableModelFields.clear();
		for (BasisField field : dbUserFields) {
			FieldDefinition dbField = dbFieldDefinition.get(field.getFieldAlias());
			if (dbField == null) {
				result = true;
				continue;
			}

			dbField = dbField.copy();
			field.setFieldType(dbField.getFieldType()); // Just in case the type has changed
			dbField.set(field);

			dbTableModelFields.add(dbField);
			usrList.add(dbField.getFieldAlias());

			// FNProg2PDA only
			if (dbInfoToExport != null && !dbInfoToExport.containsKey(dbField.getFieldAlias())) {
				dbInfoToExport.put(dbField.getFieldAlias(), dbField);
			}

		}
		return result;
	}

	protected void verifySortFields() {
		// Verify sort fields
		for (String dbField : pdaSettings.getSortFields()) {
			FieldDefinition fieldDef = dbFieldDefinition.get(dbField);
			if (fieldDef == null) {
				pdaSettings.removeSortField(dbField);
				pdaSettings.removeGroupField(dbField);
			}
		}
	}

	public void setupDbInfoToWrite() {
		Set<String> dbSpecialFields = pdaSettings.getSpecialFields();
		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
		dbTableModelFields.stream().filter(b -> dbSpecialFields.contains(b.getFieldAlias()))
				.forEach(b -> b.setExport(true));
		dbInfoToWrite = dbTableModelFields.stream().filter(FieldDefinition::isExport).toList();
	}

	protected void verifyFilter() {
		// Verify if the active filter is valid
		boolean isFirst = false;
		if (pdaSettings.isFilterDefined()) {
			for (int i = 0; i < 2; i++) {
				String filterField = pdaSettings.getFilterField(i);
				if (!filterField.isEmpty() && dbFieldDefinition.get(filterField) == null) {
					// Filter field doesn't exist
					pdaSettings.setFilterCondition("AND");
					pdaSettings.setFilterField(i, General.EMPTY_STRING);
					pdaSettings.setFilterValue(i, General.EMPTY_STRING);
					pdaSettings.setFilterOperator(i, FilterOperator.IS_EQUAL_TO);

					isFirst = i == 0;
				}
			}
		}

		if (isFirst && !pdaSettings.getFilterValue(1).isEmpty()) {
			pdaSettings.setFilterField(0, pdaSettings.getFilterField(1));
			pdaSettings.setFilterValue(0, pdaSettings.getFilterValue(1));
			pdaSettings.setFilterOperator(0, pdaSettings.getFilterOperator(1));
			pdaSettings.setFilterField(1, General.EMPTY_STRING);
			pdaSettings.setFilterValue(1, General.EMPTY_STRING);
			pdaSettings.setFilterOperator(1, FilterOperator.IS_EQUAL_TO);
		}

		isFilterDefined = pdaSettings.isFilterDefined() && !GeneralSettings.getInstance().isNoFilterExport();
		numFilter = 0;

		if (isFilterDefined) {
			numFilter = pdaSettings.noOfFilters();
		}
	}

	protected boolean isIncludeRecord(Map<String, Object> dbRecord, FieldDefinition field, String filterValue,
			FilterOperator operator) {
		if (!isFilterDefined) {
			return true;
		}

		if (filterValue.isEmpty()) {
			// We are dealing here with a "NULL" value
			switch (field.getFieldType()) {
			case FLOAT:
				filterValue = Double.toString(Double.MIN_VALUE);
				break;
			case NUMBER:
				filterValue = Integer.toString(Integer.MIN_VALUE);
				break;
			default:
				break;
			}
		}

		Object obj = dbRecord.get(field.getFieldName());
		if (obj == null || obj.equals(General.EMPTY_STRING)) {
			switch (field.getFieldType()) {
			case FLOAT:
				obj = Double.MIN_VALUE;
				break;
			case NUMBER:
				obj = Integer.MIN_VALUE;
				break;
			default:
				obj = General.EMPTY_STRING;
			}
		}

		int idx = 0;
		try {
			idx = obj.toString().compareTo(filterValue);
			switch (field.getFieldType()) {
			case DATE:
				if (obj instanceof LocalDate localdate) {
					idx = General.convertDate(localdate, General.sdInternalDate).compareTo(filterValue);
				}
				break;
			case FLOAT:
				idx = ((Double) obj).compareTo(Double.valueOf(filterValue));
				break;
			case FUSSY_DATE:
				if (!obj.toString().isEmpty()) {
					String date = General.convertFussyDate2DB(filterValue);
					idx = obj.toString().compareTo(date);
				}
				break;
			case NUMBER:
				idx = ((Integer) obj).compareTo(Integer.valueOf(filterValue));
				break;
			case TIMESTAMP:
				if (obj instanceof LocalDateTime localdatetime) {
					idx = General.convertTimestamp(localdatetime, General.sdInternalTimestamp).compareTo(filterValue);
				}
				break;
			default:
				break;
			}
		} catch (Exception ex) {
			// Database field isn't a valid NumberField
			dbRecord.put(field.getFieldAlias(), obj.toString());
			field.setFieldType(FieldTypes.TEXT);
			idx = obj.toString().compareTo(filterValue);
		}

		switch (operator) {
		case IS_EQUAL_TO:
			return idx == 0;
		case IS_NOT_EQUAL_TO:
			return idx != 0;
		case IS_GREATER_THAN:
			return idx > 0;
		case IS_GREATER_THAN_OR_EQUAL_TO:
			return idx > -1;
		case IS_LESS_THAN:
			return idx < 0;
		case IS_LESS_THAN_OR_EQUAL_TO:
			return idx < 1;
		}

		return false;
	}

	protected boolean isIncludeRecord(List<Map<String, Object>> dbRecord, FieldDefinition field, String filterValue,
			FilterOperator operator) {
		if (!isFilterDefined || dbRecord.isEmpty() && filterValue.isEmpty() && operator == FilterOperator.IS_EQUAL_TO) {
			return true;
		}

		for (Map<String, Object> map : dbRecord) {
			if (isIncludeRecord(map, field, filterValue, operator)) {
				return true;
			}
		}

		return false;
	}

	public int getTotalRecords() {
		return totalRecords;
	}

	public void runConversionProgram(Component parent) throws Exception {
		if (myExportFile == ExportFile.HANDBASE) {
			((HanDBase) dbOut).runConversionProgram();
		}

		if (myExportFile == ExportFile.TEXTFILE) {
			General.showMessage(parent,
					GUIFactory.getMessage("createdFiles",
							((CsvFile) dbOut).getExportFiles(GUIFactory.getText("file"), GUIFactory.getText("files"))),
					GUIFactory.getTitle("information"), false);
		} else {
			General.showMessage(parent,
					GUIFactory.getMessage("createdFile", pdaSettings.getToDatabase().getDatabaseName(),
							Integer.toString(totalRecords), Integer.toString(writtenRecords),
							Integer.toString(totalRecords - writtenRecords)),
					GUIFactory.getTitle("information"), false);
		}

		// Save last export date
		pdaSettings.setLastExported(General.convertTimestamp(LocalDateTime.now(), General.sdInternalTimestamp));
	}

	protected abstract List<Map<String, Object>> getDataListMap() throws Exception;
}
