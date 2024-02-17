package dbengine;

import java.io.EOFException;
import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import application.BasicSoft;
import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.export.CsvFile;
import dbengine.utils.DatabaseHelper;

public abstract class GeneralDB {
	/**
	 * Title: GeneralDB Description: Abstract Class for the PDA databases Copyright:
	 * (c) 2003-2011
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	protected BasicSoft mySoft;
	protected DatabaseHelper myHelper;
	protected String myDatabase;
	protected String booleanTrue;
	protected String booleanFalse;

	protected List<FieldDefinition> dbInfo2Write = new ArrayList<>();
	protected List<String> dbFieldNames = new ArrayList<>();
	protected List<FieldTypes> dbFieldTypes = new ArrayList<>();

	protected ExportFile myExportFile;
	protected ExportFile myImportFile;

	private List<FieldDefinition> myDBDefinition;
	private boolean createBackup;

	protected int totalRecords = 0;

	protected boolean useImages;
	protected boolean useAppend;
	protected boolean hasBackup;

	private boolean isBooleanExport;
	private boolean isDateExport;
	private boolean isTimeExport;
	private String fileOpenWarning = General.EMPTY_STRING;

	protected boolean isInputFile;

	protected String handbaseProgram;
	protected Profiles myPref;

	protected GeneralDB(Profiles pref) {
		myPref = pref;
	}

	public void setSoftware(BasicSoft pSoft) {
		dbInfo2Write = pSoft.getDbInfoToWrite();
		mySoft = pSoft;
		myDBDefinition = null;

		myImportFile = pSoft.getImportFile();
		myExportFile = pSoft.getExportFile();

		createBackup = myPref.isCreateBackup();
		useAppend = myPref.isAppendRecords();
		useImages = myPref.isExportImages();

		hasBackup = false;
		isInputFile = false;

		isBooleanExport = myExportFile.isBooleanExport();
		isDateExport = myExportFile.isDateExport();
		isTimeExport = myExportFile.isSqlDatabase();

		GeneralSettings generalProps = GeneralSettings.getInstance();
		booleanTrue = isBooleanExport ? myExportFile.getTrueValue() : generalProps.getCheckBoxChecked();
		booleanFalse = isBooleanExport ? myExportFile.getFalseValue() : generalProps.getCheckBoxUnchecked();
		handbaseProgram = generalProps.getHandbaseConversionProgram();
	}

	public ExportFile getImportFile() {
		return myImportFile;
	}

	public String getBooleanFalse() {
		return booleanFalse;
	}

	public String getBooleanTrue() {
		return booleanTrue;
	}

	public void openFile(DatabaseHelper helper, boolean isInputFile) throws Exception {
		this.isInputFile = isInputFile;

		if (helper.getDatabase().isEmpty()) {
			throw FNProgException.getException("noDatabaseDefined");
		}

		myDatabase = helper.getRemoteDatabase();
		if (isInputFile && !helper.getDatabaseType().isConnectHost() && !General.existFile(getDbFile())) {
			throw FNProgException.getException("noDatabaseExists", getDbFile());
		}

		hasBackup = false;
		if (!isInputFile && createBackup) {
			hasBackup = General.copyFile(getDbFile(), getDbFile() + ".bak");
		}

		myHelper = helper;
		FNProgException exception = null;

		try {
			openFile(isInputFile);
		} catch (EOFException e) {
			exception = FNProgException.getException(isInputFile ? "cannotOpen" : "cannotWrite", getDbFile(),
					"Cannot read file beyond EOF (File is empty or corrupt)");
		} catch (Exception e) {
			exception = FNProgException.getException(isInputFile ? "cannotOpen" : "cannotWrite", getDbFile(),
					e.getMessage());
		}

		if (exception != null) {
			throw exception;
		}
	}

	public String getPdaDatabase() {
		// Valid for CSV- and xBase files
		return null;
	}

	public List<String> getTableOrSheetNames() {
		// valid for Calc, Excel, MS-Access and SQL based databases only
		return new ArrayList<>();
	}

	public int getTotalRecords() throws Exception {
		return totalRecords;
	}

	public Object convertDataFields(Object dbValue, FieldDefinition field) {
		if (dbValue == null || dbValue.equals(General.EMPTY_STRING)) {
			return General.EMPTY_STRING;
		}

		if (field.getFieldType() == FieldTypes.IMAGE || field.getFieldType() == FieldTypes.TEXT) {
			return dbValue;
		}

		String dbField = dbValue.toString();
		switch (field.getFieldType()) {
		case BOOLEAN:
			return convertBoolean(dbValue, field);
		case DATE:
			return convertDate(dbValue, field);
		case DURATION:
			return convertDuration(dbValue, field);
		case FUSSY_DATE:
			return General.convertFussyDate(dbValue.toString().trim());
		case MEMO:
			if (myExportFile == ExportFile.JSON) {
				return General.convertStringToList(dbField);
			}

			if (dbField.length() > myExportFile.getMaxMemoSize()) {
				// Truncate field
				return dbField.substring(0, myExportFile.getMaxMemoSize() - 15) + " truncated...";
			}
			return dbField;
		case BIG_DECIMAL, FLOAT, NUMBER:
			return dbValue;
		case TIME:
			return convertTime(dbValue, field);
		case TIMESTAMP:
			return convertTimestamp(dbValue, field);
		case YEAR:
			return ((LocalDate) dbValue).getYear();
		case UNKNOWN:
			return General.EMPTY_STRING;
		default:
			return convertString(dbValue);
		}
	}

	protected void validateAppend(List<FieldDefinition> dbFields) throws FNProgException {
		int numFields = dbInfo2Write.size();
		if (numFields != dbFields.size()) {
			throw FNProgException.getException("noMatchFieldsDatabase", Integer.toString(numFields),
					Integer.toString(dbFields.size()));
		}

		// Verify if fields match in type and size
		for (int i = 0; i < numFields; i++) {
			FieldDefinition field1 = dbFields.get(i);
			FieldDefinition field2 = dbInfo2Write.get(i);
			String fieldName = myExportFile.isSqlDatabase() ? getSqlFieldName(field2.getFieldHeader(), true)
					: field2.getFieldHeader();

			if (!field1.getFieldName().equals(fieldName)) {
				throw FNProgException.getException("noMatchFieldName", Integer.toString(i + 1), field1.getFieldName(),
						field2.getFieldHeader());
			}

			boolean isField1Text = field1.getFieldType() == FieldTypes.MEMO || field1.getFieldType() == FieldTypes.TEXT;
			boolean isField2Text = field2.isOutputAsText() || field2.getFieldType() == FieldTypes.MEMO
					|| field2.getFieldType() == FieldTypes.TEXT;

			if (field1.getFieldType() != field2.getFieldType() && isField1Text != isField2Text) {
				throw FNProgException.getException("noMatchFieldType", field1.getFieldName(), field2.getFieldHeader());
			}

			if (myExportFile == ExportFile.SQLITE || myExportFile.isSpreadSheet()) {
				// No need to check the field sizes
				return;
			}

			if (field1.getSize() < field2.getSize()) {
				throw FNProgException.getException("noMatchFieldLength", field1.getFieldName(),
						Integer.toString(field1.getSize()), Integer.toString(field2.getSize()));
			}
		}
	}

	protected String getSqlFieldName(String value, boolean noDot) {
		return noDot ? getSqlFieldName(value).replace(".", "") : getSqlFieldName(value);
	}

	protected String getSqlFieldName(String value) {
		if (isNotReservedWord(value) && value.matches("^[a-zA-Z0-9_.]*$")) {
			return value;
		}

		return "[" + value + "]";
	}

	protected boolean isNotReservedWord(String value) {
		return !"user".equalsIgnoreCase(value);
	}

	protected Object convertBoolean(Object dbValue, FieldDefinition field) {
		boolean b = (Boolean) dbValue;
		if (!isBooleanExport || field.isOutputAsText()) {
			return b ? booleanTrue : booleanFalse;
		}
		return b;
	}

	protected Object convertDate(Object dbValue, FieldDefinition field) {
		if (!isDateExport || field.isOutputAsText()) {
			return General.convertDate((LocalDate) dbValue, General.getSimpleDateFormat());
		}
		return dbValue;
	}

	protected Object convertDuration(Object dbValue, FieldDefinition field) {
		return General.convertDuration((Duration) dbValue);
	}

	protected String convertString(Object dbValue) {
		// Change Tabs to spaces, remove carriage returns and the trailing line feed
		// char
		String result = dbValue.toString().replace("\t", "  ");
		result = result.replace("\r", General.EMPTY_STRING);
		if (result.endsWith("\n")) {
			result = result.substring(0, result.length() - 1);
		}

		if (this instanceof CsvFile) {
			// Replace " with '
			return result.replace(General.TEXT_DELIMITER, "'");
		}
		return result;
	}

	protected Object convertTime(Object dbValue, FieldDefinition field) {
		if (!isTimeExport || field.isOutputAsText()) {
			return General.convertTime((LocalTime) dbValue, General.getSimpleTimeFormat());
		}
		return dbValue;
	}

	protected Object convertTimestamp(Object dbValue, FieldDefinition field) {
		if (!isTimeExport || field.isOutputAsText()) {
			return General.convertTimestamp((LocalDateTime) dbValue);
		}
		return dbValue;
	}

	public List<FieldDefinition> getTableModelFields() {
		if (myDBDefinition == null) {
			int index = 0;
			myDBDefinition = new ArrayList<>();
			for (String name : dbFieldNames) {
				myDBDefinition.add(new FieldDefinition(name, name, dbFieldTypes.get(index++)));
			}
		}
		return myDBDefinition;
	}

	public String getFileOpenWarning() {
		return fileOpenWarning;
	}

	public void setFileOpenWarning(String fileOpenWarning) {
		this.fileOpenWarning = fileOpenWarning;
	}

	public void closeData() throws Exception {
		// Nothing to do here on this level
	}

	protected abstract void openFile(boolean isInputFile) throws Exception;

	public abstract void closeFile();

	public void deleteFile() {
		closeFile();
		File outFile = new File(getDbFile());

		if (outFile.exists()) {
			outFile.delete();
		}
		if (hasBackup) {
			File backupFile = new File(getDbFile() + ".bak");
			backupFile.renameTo(outFile);
		}
	}

	public abstract int processData(Map<String, Object> dbRecord) throws Exception;

	public void createDbHeader() throws Exception {
		// Nothing to do here
	}

	public String getDbFile() {
		return myDatabase;
	}
}