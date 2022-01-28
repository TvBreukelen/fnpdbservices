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
import dbengine.export.Calc;
import dbengine.export.CsvFile;
import dbengine.export.DBaseFile;
import dbengine.export.Excel;
import dbengine.export.HanDBase;
import dbengine.export.JFile3;
import dbengine.export.JFile4;
import dbengine.export.JFile5;
import dbengine.export.ListDB;
import dbengine.export.MobileDB;
import dbengine.export.PilotDB;
import dbengine.export.SQLite;
import dbengine.export.YamlFile;
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
	protected DatabaseHelper myDatabaseHelper;
	protected String myFilename;
	protected String booleanTrue;
	protected String booleanFalse;
	protected String encoding;

	protected List<FieldDefinition> dbInfo2Write = new ArrayList<>();
	protected List<String> dbFieldNames = new ArrayList<>();
	protected List<FieldTypes> dbFieldTypes = new ArrayList<>();

	protected ExportFile myExportFile;
	protected ExportFile myImportFile;

	private List<FieldDefinition> myDBDefinition;
	private boolean createBackup;

	protected int myTotalRecords = 0;

	protected boolean useImages;
	protected boolean useAppend;
	protected boolean hasBackup;

	private boolean isBooleanExport;
	private boolean isDateExport;
	private boolean isTimeExport;
	private String fileOpenWarning = "";

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
		encoding = myPref.getEncoding(); // Output file encoding

		hasBackup = false;
		isInputFile = false;

		isBooleanExport = myExportFile.isBooleanExport();
		isDateExport = myExportFile.isDateExport();
		isTimeExport = myExportFile.isTimeExport();

		GeneralSettings generalProps = GeneralSettings.getInstance();
		booleanTrue = isBooleanExport ? myExportFile.getTrueValue() : generalProps.getCheckBoxChecked();
		booleanFalse = isBooleanExport ? myExportFile.getFalseValue() : generalProps.getCheckBoxUnchecked();
		handbaseProgram = generalProps.getHandbaseConversionProgram();
	}

	public String getBooleanFalse() {
		return booleanFalse;
	}

	public String getBooleanTrue() {
		return booleanTrue;
	}

	public void openFile(DatabaseHelper helper, boolean isInputFile) throws Exception {
		if (helper.getDatabase().isEmpty()) {
			throw FNProgException.getException("noDatabaseDefined");
		}

		if (isInputFile && !General.existFile(helper.getDatabase())) {
			throw FNProgException.getException("noDatabaseExists", helper.getDatabase());
		}

		hasBackup = false;
		if (isInputFile) {
			encoding = myPref.getImportFileEncoding(); // Input file encoding
		} else if (createBackup) {
			hasBackup = General.copyFile(myFilename, myFilename + ".bak");
		}

		myFilename = helper.getDatabase();
		myDatabaseHelper = helper;
		FNProgException exception = null;

		try {
			openFile(isInputFile);
		} catch (EOFException e) {
			exception = FNProgException.getException("fileOpenError", myFilename,
					"Cannot read file beyond EOF (File is empty or corrupt)");
		} catch (Exception e) {
			exception = FNProgException.getException("fileOpenError", myFilename, e.getMessage());
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
		// valid for MS-Access and SQL based databases only
		return new ArrayList<>();
	}

	public List<String> getSheetNames() {
		// valid for Calc and MS-Excel only
		return new ArrayList<>();
	}

	public int getTotalRecords() {
		return myTotalRecords;
	}

	public Object convertDataFields(Object dbValue, FieldDefinition field) {
		if (dbValue == null || dbValue.equals("")) {
			return "";
		}

		if (field.getFieldType() == FieldTypes.IMAGE || field.getFieldType() == FieldTypes.TEXT || dbValue.equals("")) {
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
		case FLOAT:
			return ((Number) dbValue).doubleValue();
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
		case NUMBER:
			return ((Number) dbValue).intValue();
		case TIME:
			return convertTime(dbValue, field);
		case TIMESTAMP:
			return convertTimestamp(dbValue, field);
		case UNKNOWN:
			return "";
		default:
			return convertString(dbValue);
		}
	}

	protected Object convertBoolean(Object dbValue, FieldDefinition field) {
		boolean b = (Boolean) dbValue;
		if (!isBooleanExport || field.isOutputAsText()) {
			return b ? booleanTrue : booleanFalse;
		}
		return b;
	}

	protected Object convertDate(Object dbValue, FieldDefinition field) {
		return General.convertDate((LocalDate) dbValue,
				!isDateExport || field.isOutputAsText() ? General.getSimpleDateFormat() : General.sdInternalDate);
	}

	protected Object convertDuration(Object dbValue, FieldDefinition field) {
		return General.convertDuration((Duration) dbValue);
	}

	protected String convertString(Object dbValue) {
		// Change Tabs to spaces, remove carriage returns and the trailing line feed
		// char
		String result = dbValue.toString().replace("\t", "  ");
		result = result.replace("\r", "");
		if (result.endsWith("\n")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	protected Object convertTime(Object dbValue, FieldDefinition field) {
		return General.convertTime((LocalTime) dbValue,
				!isTimeExport || field.isOutputAsText() ? General.getSimpleTimeFormat() : General.sdInternalTime);
	}

	protected Object convertTimestamp(Object dbValue, FieldDefinition field) {
		return General.convertTimestamp((LocalDateTime) dbValue,
				!isTimeExport || field.isOutputAsText() ? General.getSimpleTimestampFormat()
						: General.sdInternalTimestamp);
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

	protected Object convertDate(String pDate) {
		return General.convertDate2DB(pDate, General.sdInternalDate);
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

	public static GeneralDB getDatabase(ExportFile db, Profiles profile) {
		switch (db) {
		case ACCESS:
			return new MSAccess(profile);
		case CALC:
			return new Calc(profile);
		case JSON:
			return new JsonFile(profile);
		case YAML:
			return new YamlFile(profile);
		case HANDBASE:
			return new HanDBase(profile);
		case JFILE5:
			return new JFile5(profile);
		case JFILE3:
			return new JFile3(profile);
		case JFILE4:
			return new JFile4(profile);
		case LIST:
			return new ListDB(profile);
		case MOBILEDB:
			return new MobileDB(profile);
		case PILOTDB:
			return new PilotDB(profile);
		case EXCEL:
			return new Excel(profile);
		case TEXTFILE:
			return new CsvFile(profile);
		case DBASE:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
			return new DBaseFile(profile);
		case XML:
			return new XmlFile(profile);
		case SQLITE:
			return new SQLite(profile);
		}
		return null;
	}

	protected abstract void openFile(boolean isInputFile) throws Exception;

	public abstract void closeFile();

	public void deleteFile() {
		closeFile();
		File outFile = new File(myFilename);

		if (outFile.exists()) {
			outFile.delete();
		}
		if (hasBackup) {
			File backupFile = new File(myFilename + ".bak");
			backupFile.renameTo(outFile);
		}
	}

	public abstract void processData(Map<String, Object> dbRecord) throws Exception;

	public abstract void createDbHeader() throws Exception;
}