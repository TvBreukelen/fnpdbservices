package application;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
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
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.utils.SpecialFields;

public abstract class BasicSoft extends Observable {
	/**
	 * Title: BasicSoft
	 *
	 * @description: Basic abstract class for all software classes (FNProgramvare +
	 *               DBConvert) Copyright: (c) 2006-2019
	 *
	 * @author Tom van Breukelen
	 * @version 6.0
	 */
	protected int myTotalRecord = 0;
	protected int firstRecord = 0;
	protected int myCurrentRecord = 0;

	protected static final int LISTDB_MAX_CATEGORIES = 15;
	protected static final int LISTDB_MAX_CATEGORY_LENGTH = 15;

	protected List<String> myCategories = new ArrayList<>(); // Stores the List and SmartList categories
	protected List<FieldDefinition> dbTableModelFields = new ArrayList<>(); // All fields loaded in the TableModel
	protected List<FieldDefinition> dbInfoToWrite = new ArrayList<>(); // Export fields for the export file
	protected Map<String, FieldDefinition> dbFieldDefinition; // Definition of all fields in the database
	protected List<BasisField> dbUserFields = new ArrayList<>(); // User defined fields (note: userFields +
																	// dbSpecialFields = dbInfoToWrite)

	protected boolean isFilterDefined;
	protected ExportFile myExportFile = ExportFile.EXCEL;
	protected ExportFile myImportFile = ExportFile.EXCEL;
	protected Profiles pdaSettings;

	protected int numFilter;
	private Timer timer;

	public BasicSoft(Profiles profile) {
		pdaSettings = profile;
		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
	}

	public ExportFile getImportFile() {
		return myImportFile;
	}

	public ExportFile getExportFile() {
		return myExportFile;
	}

	/* Read the input file and load the input records in the TableModel */
	public void processFiles(ViewerModel tabModel) throws Exception {
		myCurrentRecord = 0;

		try {
			// Load TableModel
			tabModel.setDataListMap(getDataListMap());
		} finally {
			timer.cancel();
			notifyObservers(new int[] { myTotalRecord, myTotalRecord });
		}
	}

	/* Write the TableModel data into the ExportFile */
	public void convertFromTableModel(ViewerModel tabModel, GeneralDB dbOut) throws Exception {
		List<Map<String, Object>> table = tabModel.getDataListMap();

		myCurrentRecord = 0;
		myTotalRecord = table.size();

		dbOut.createDbHeader();
		for (Map<String, Object> rowData : table) {
			dbOut.processData(dbTableModelFields.stream().collect(Collectors.toMap(FieldDefinition::getFieldAlias,
					field -> rowData.getOrDefault(field.getFieldAlias(), ""))));
			setChanged();
			myCurrentRecord++;
		}

		timer.cancel();
		notifyObservers(new int[] { myTotalRecord, myTotalRecord });
		dbOut.closeData();
	}

	public void startMonitoring(Observer obj) {
		deleteObservers();
		addObserver(obj);

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (BasicSoft.this.hasChanged()) {
					notifyObservers(new int[] { myTotalRecord, myCurrentRecord + 1 });
				}
			}
		}, 1000, 500);
	}

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
				dbTableModelFields.forEach(b -> dbUserFields.add(
						new BasisField(b.getFieldAlias(), b.getFieldName(), b.getFieldHeader(), b.getFieldType())));
			}
			return;
		}

		dbUserFields = dbUserFields.stream().filter(b -> validFields.contains(b.getFieldAlias()))
				.collect(Collectors.toList());
	}

	public void refreshSpecialFields() {
		SpecialFields dbSpecialFields = pdaSettings.getSpecialFields();
		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
		dbTableModelFields.stream().filter(b -> dbSpecialFields.getSpecialFields().contains(b.getFieldAlias()))
				.forEach(b -> b.setExport(true));
		dbInfoToWrite = dbTableModelFields.stream().filter(b -> b.isExport()).collect(Collectors.toList());
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
					pdaSettings.setFilterField(i, "");
					pdaSettings.setFilterValue(i, "");
					pdaSettings.setFilterOperator(i, FilterOperator.IS_EQUAL_TO);

					isFirst = i == 0;
				}
			}
		}

		if (isFirst && !pdaSettings.getFilterValue(1).isEmpty()) {
			pdaSettings.setFilterField(0, pdaSettings.getFilterField(1));
			pdaSettings.setFilterValue(0, pdaSettings.getFilterValue(1));
			pdaSettings.setFilterOperator(0, pdaSettings.getFilterOperator(1));
			pdaSettings.setFilterField(1, "");
			pdaSettings.setFilterValue(1, "");
			pdaSettings.setFilterOperator(1, FilterOperator.IS_EQUAL_TO);
		}

		isFilterDefined = pdaSettings.isFilterDefined() && !GeneralSettings.getInstance().isNoFilterExport();
		numFilter = 0;

		if (isFilterDefined) {
			numFilter = pdaSettings.getFilterField(1).isEmpty() ? 1 : 2;
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
			default:
				break;
			}
		}

		Object obj = dbRecord.get(field.getFieldName());
		if (obj == null || obj.equals("")) {
			switch (field.getFieldType()) {
			case FLOAT:
				obj = Double.MIN_VALUE;
				break;
			case NUMBER:
				obj = Integer.MIN_VALUE;
				break;
			default:
				obj = "";
			}
		}

		int idx = 0;
		try {
			switch (field.getFieldType()) {
			case DATE:
				if (obj instanceof Date) {
					idx = General.convertDate2DB((Date) obj).compareTo(filterValue);
					break;
				}
			case FLOAT:
				idx = ((Double) obj).compareTo(Double.valueOf(filterValue));
				break;
			case NUMBER:
				idx = ((Integer) obj).compareTo(Integer.valueOf(filterValue));
				break;
			case TIMESTAMP:
				if (obj instanceof Date) {
					idx = General.convertTimestamp2DB((Date) obj).compareTo(filterValue);
					break;
				}
			default:
				idx = obj.toString().compareTo(filterValue);
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
		if (!isFilterDefined) {
			return true;
		}

		if (dbRecord.isEmpty() && filterValue.isEmpty() && operator == FilterOperator.IS_EQUAL_TO) {
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
		return myTotalRecord;
	}

	public List<String> getCategories() {
		return myCategories;
	}

	protected abstract List<Map<String, Object>> getDataListMap() throws Exception;
}
