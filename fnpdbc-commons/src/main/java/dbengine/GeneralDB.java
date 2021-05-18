package dbengine;

import java.io.EOFException;
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
import dbengine.export.Referencer;
import dbengine.export.SQLite;
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
	protected boolean isInputFile;

	protected String handbaseProgram;
	protected Profiles myPref;

	public GeneralDB(Profiles pref) {
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

		openFile(helper, createBackup, isInputFile);
	}

	public void openFile(DatabaseHelper helper, boolean createBackup, boolean isInputFile) throws Exception {
		if (isInputFile) {
			encoding = myPref.getImportFileEncoding(); // Input file encoding
		}

		myFilename = helper.getDatabase();
		myDatabaseHelper = helper;

		try {
			openFile(createBackup, isInputFile);
		} catch (EOFException e) {
			throw FNProgException.getException("fileOpenError", myFilename,
					"Cannot read file beyond EOF (File is empty or corrupt)");
		} catch (Exception e) {
			throw FNProgException.getException("fileOpenError", myFilename, e.getMessage());
		}
	}

	public String getPdaDatabase() {
		// Valid for CSV- and xBase files
		return null;
	}

	public List<String> getTableNames() {
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
		if (dbValue == null) {
			return "";
		}

		if (field.getFieldType() == FieldTypes.IMAGE || field.getFieldType() == FieldTypes.TEXT || dbValue.equals("")) {
			return dbValue;
		}

		switch (field.getFieldType()) {
		case BOOLEAN:
			boolean b = (Boolean) dbValue;
			if (!isBooleanExport || field.isOutputAsText()) {
				return b ? booleanTrue : booleanFalse;
			}
			return b;
		case DATE:
			if (!isDateExport || field.isOutputAsText()) {
				return General.convertDate(dbValue.toString());
			}
			return convertDate(dbValue.toString());
		case DURATION:
			return General.convertDuration((Number) dbValue);
		case FUSSY_DATE:
			return General.convertFussyDate(dbValue.toString());
		case MEMO:
			// Check whether the value in field found doesn't exceed the maximum field size
			if (dbValue.toString().length() > myExportFile.getMaxMemoSize()) {
				// Truncate field
				return dbValue.toString().substring(0, myExportFile.getMaxMemoSize() - 15) + " truncated...";
			}
			return dbValue;
		case TIME:
			return General.convertTime(dbValue.toString());
		case TIMESTAMP:
			return General.convertTimestamp(dbValue.toString());
		case UNKNOWN:
			return "";
		default:
			return dbValue;
		}
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
		return General.convertDate(pDate);
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
		case REFERENCER:
			return new Referencer(profile);
		case EXCEL:
			return new Excel(profile);
		case TEXTFILE:
			return new CsvFile(profile);
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

	protected abstract void openFile(boolean createBackup, boolean isInputFile) throws Exception;

	public abstract void closeFile();

	public abstract void deleteFile();

	public abstract void processData(Map<String, Object> dbRecord) throws Exception;

	public abstract void createDbHeader() throws Exception;
}