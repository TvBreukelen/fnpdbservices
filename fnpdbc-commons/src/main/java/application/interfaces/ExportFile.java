package application.interfaces;

import java.util.ArrayList;
import java.util.List;

import application.FileType;
import application.utils.General;

public enum ExportFile {
	/**
	 * Title: ExportFile Description: Enums for all Exportfile related constants
	 *
	 * @author Tom van Breukelen
	 */

	HANDBASE("HanDBase", "DATA", "HanD", "1", "0", FileType.PDB, 256, 2000, 100),
	JFILE3("JFile3", "JbDb", "JBas", "1", "0", FileType.PDB, 64, 500, 20),
	JFILE4("JFile4", "JfDb", "JFil", "1", "0", FileType.PDB, 256, 10000, 50),
	JFILE5("JFile5", "JfD5", "JFi5", "1", "0", FileType.PDB, 256, 10000, 50),
	LIST("List", "DATA", "LSdb", "true", "false", FileType.PDB, 4095, 4095, 32767),
	MOBILEDB("MobileDB", "Mdb1", "Mdb1", "true", "false", FileType.PDB, 256, 1000, 20),
	PILOTDB("Pilot-DB", "DB00", "DBOS", "1", "0", FileType.PDB, 256, 3000, 256),
	REFERENCER("P. Referencer", "", "", "true", "false", FileType.XLS, 256, 32767, 32767),
	SQLITE("SQLite", "SQLite", "", "1", "0", FileType.DB, 255, 255, 255),
	ACCESS("MS-Access", "", "", "true", "false", FileType.MDB, 255, 255, 255),
	EXCEL("MS-Excel", "", "", "true", "false", FileType.XLSX, 256, 32767, 32767),
	TEXTFILE("Text File", "", "", "true", "false", FileType.TXT, 256, 32767, 32767),
	DBASE3("DBase3", "", "", "T", "F", FileType.DBF, 254, 32737, 128),
	DBASE4("DBase4", "", "", "T", "F", FileType.DBF, 254, 32767, 255),
	DBASE5("DBase5", "", "", "T", "F", FileType.DBF, 254, 32767, 1024),
	FOXPRO("FoxPro", "", "", "T", "F", FileType.DBF, 254, 32767, 255),
	XML("Xml", "", "", "true", "false", FileType.XML, 32767, 32767, 32767);

	private String name;
	private String dbType;
	private String creator;
	private String trueValue;
	private String falseValue;
	private List<String> fileExtention;
	private String fileType;
	private int maxTextSize;
	private int maxMemoSize;
	private int maxFields;

	private ExportFile(String name, String dbType, String creator, String trueValue, String falseValue, FileType type,
			int maxTextSize, int maxMemoSize, int maxFields) {
		this.name = name;
		this.dbType = dbType;
		this.creator = creator;
		this.trueValue = trueValue;
		this.falseValue = falseValue;
		fileExtention = type.getExtention();
		fileType = type.getType();
		this.maxTextSize = maxTextSize;
		this.maxMemoSize = maxMemoSize;
		this.maxFields = maxFields;
	}

	public static ExportFile getExportFile(String id) {
		for (ExportFile exp : values()) {
			if (exp.name.equals(id)) {
				return exp;
			}
		}
		return TEXTFILE;
	}

	public static String[] getExportFilenames(boolean isImport) {
		List<String> exportList = new ArrayList<>();
		for (ExportFile exp : values()) {
			exportList.add(exp.name);
		}

		if (!General.IS_WINDOWS) {
			exportList.remove(HANDBASE.name);
		}

		if (isImport) {
			exportList.remove(REFERENCER.name);
		} else {
			exportList.remove(SQLITE.name);
			exportList.remove(ACCESS.name);
		}

		String[] result = new String[exportList.size()];
		exportList.toArray(result);
		return result;
	}

	public boolean isBooleanExport() {
		switch (this) {
		case HANDBASE:
		case JFILE3:
		case JFILE4:
		case JFILE5:
		case MOBILEDB:
		case PILOTDB:
		case EXCEL:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
		case ACCESS:
		case SQLITE:
			return true;
		default:
			return false;
		}
	}

	public boolean isTextExport() {
		switch (this) {
		case TEXTFILE:
		case XML:
			return true;
		default:
			return false;
		}
	}

	public boolean isDateExport() {
		switch (this) {
		case JFILE3:
		case JFILE4:
		case JFILE5:
		case MOBILEDB:
		case PILOTDB:
		case EXCEL:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
		case ACCESS:
		case SQLITE:
			return true;
		default:
			return false;
		}
	}

	public boolean isImageExport() {
		switch (this) {
		case HANDBASE:
		case ACCESS:
		case TEXTFILE:
		case REFERENCER:
			return true;
		default:
			return false;
		}
	}

	public boolean isTimeExport() {
		switch (this) {
		case JFILE3:
		case JFILE4:
		case JFILE5:
		case MOBILEDB:
		case PILOTDB:
		case EXCEL:
		case ACCESS:
		case SQLITE:
			return true;
		default:
			return false;
		}
	}

	public boolean isEncodingSupported() {
		switch (this) {
		case TEXTFILE:
		case XML:
			return true;
		default:
			return false;
		}
	}

	public boolean isAppend() {
		switch (this) {
		case HANDBASE:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
		case JFILE3:
		case JFILE4:
		case JFILE5:
		case LIST:
		case MOBILEDB:
		case PILOTDB:
		case ACCESS:
		case SQLITE:
			return true;
		default:
			return false;
		}
	}

	public boolean isPasswordSupported() {
		return this == HANDBASE;
	}

	public boolean isSpecialFieldSort() {
		switch (this) {
		case LIST:
		case REFERENCER:
		case XML:
			return true;
		default:
			return false;
		}
	}

	public boolean hasCategories() {
		return this == LIST;
	}

	public String getCreator() {
		return creator;
	}

	public String getDbType() {
		return dbType;
	}

	public String getFalseValue() {
		return falseValue;
	}

	public List<String> getFileExtention() {
		return fileExtention;
	}

	public String getFileType() {
		return fileType;
	}

	public int getMaxFields() {
		return maxFields;
	}

	public int getMaxMemoSize() {
		return maxMemoSize;
	}

	public int getMaxSortFields() {
		return this == ExportFile.LIST ? 2 : 4;
	}

	public int getMaxTextSize() {
		return maxTextSize;
	}

	public String getName() {
		return name;
	}

	public String getTrueValue() {
		return trueValue;
	}
}