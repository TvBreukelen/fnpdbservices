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

	protected List<FieldDefinition> dbInfo2Write = new ArrayList<>(50);
	protected List<String> dbFieldNames = new ArrayList<>(50);
	protected List<FieldTypes> dbFieldTypes = new ArrayList<>(50);

	protected ExportFile myExportFile;
	protected ExportFile myImportFile;

	private List<FieldDefinition> myDBDefinition;
	private boolean createBackup;

	protected int myTotalRecords = 0;

	protected boolean useAppend;
	protected boolean useBoolean = true;
	protected boolean useDate = true;
	protected boolean useImages;
	protected boolean useTime;
	protected boolean hasBackup;
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
		useBoolean = myPref.isExportBoolean();
		useDate = myPref.isExportDate();
		useImages = myPref.isExportImages();
		useTime = myPref.isExportTime();
		encoding = myPref.getEncoding(); // Output file encoding

		hasBackup = false;
		isInputFile = false;

		GeneralSettings generalProps = GeneralSettings.getInstance();
		booleanTrue = useBoolean ? myExportFile.getTrueValue() : generalProps.getCheckBoxChecked();
		booleanFalse = useBoolean ? myExportFile.getFalseValue() : generalProps.getCheckBoxUnchecked();
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

	public String[] getTableNames() {
		// valid for MS-Access and SQL based databases only
		return null;
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
			return useBoolean ? b : b ? booleanTrue : booleanFalse;
		case DATE:
			return useDate ? convertDate(dbValue.toString()) : General.convertDate(dbValue.toString());
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

	public List<FieldDefinition> getTableModelFields() throws Exception {
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

	public static GeneralDB getDatabase(ExportFile db, Profiles profile, boolean isDBConvert) {
		switch (db) {
		case ACCESS:
			return new MSAccess(profile);
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
		case SQLite:
			return new SQLite(profile);
		}
		return null;
	}

	abstract protected void openFile(boolean createBackup, boolean isInputFile) throws Exception;

	abstract public void closeFile();

	abstract public void deleteFile();

	abstract public void processData(Map<String, Object> dbRecord) throws Exception;

	abstract public void createDbHeader() throws Exception;
}