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
	ACCESS("MS-Access", FileType.MDB, 255, 255, 255), EXCEL("MS-Excel", FileType.XLSX, 256, 32767, 32767),
	CALC("Calc", FileType.ODS, 256, 1048576, 1048576), TEXTFILE("Text File", FileType.TXT, 256, 32767, 32767),
	XML("Xml", FileType.XML, 32767, 32767, 32767), JSON("Json", FileType.JSON, 32767, 32767, 32767),
	YAML("Yaml", FileType.YAML, 32767, 32767, 32767), SQLITE("SQLite", FileType.DB, 255, 255, 255),
	MARIADB("MariaDB", FileType.HOST, 32767, 32767, 32767),
	POSTGRESQL("PostgreSQL", FileType.HOST, 32767, 32767, 32767), VCARD("VCard", FileType.VCF, 256, 3000, 256),
	DBASE("xBase", FileType.DBF, 254, 32737, 128), DBASE3("DBase3", FileType.DBF, 254, 32737, 128),
	DBASE4("DBase4", FileType.DBF, 254, 32767, 255), DBASE5("DBase5", FileType.DBF, 254, 32767, 1024),
	FOXPRO("FoxPro", FileType.DBF, 254, 32767, 255), HANDBASE("HanDBase", FileType.PDB, 256, 2000, 100),
	JFILE3("JFile3", FileType.PDB, 64, 500, 20), JFILE4("JFile4", FileType.PDB, 256, 10000, 50),
	JFILE5("JFile5", FileType.PDB, 256, 10000, 50), LIST("List", FileType.PDB, 4095, 4095, 32767),
	MOBILEDB("MobileDB", FileType.PDB, 256, 1000, 20), PILOTDB("Pilot-DB", FileType.PDB, 256, 3000, 256);

	private String name;
	private FileType type;
	private int maxTextSize;
	private int maxMemoSize;
	private int maxFields;

	ExportFile(String name, FileType type, int maxTextSize, int maxMemoSize, int maxFields) {
		this.name = name;
		this.type = type;
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
		List<String> result = new ArrayList<>();
		for (ExportFile exp : values()) {
			result.add(exp.name);
		}

		if (!General.IS_WINDOWS) {
			result.remove(HANDBASE.name);
		}

		if (isImport) {
			result.remove(DBASE3.name);
			result.remove(DBASE4.name);
			result.remove(DBASE5.name);
			result.remove(FOXPRO.name);
		} else {
			result.remove(DBASE.name);
			result.remove(SQLITE.name);
			result.remove(ACCESS.name);
			result.remove(MARIADB.name);
			result.remove(POSTGRESQL.name);
			result.remove(VCARD.name);
		}

		return result.toArray(new String[result.size()]);
	}

	public boolean isSpreadSheet() {
		switch (this) {
		case CALC:
		case EXCEL:
			return true;
		default:
			return false;
		}
	}

	public boolean isDatabase() {
		switch (this) {
		case ACCESS:
		case MARIADB:
		case POSTGRESQL:
		case SQLITE:
			return true;
		default:
			return false;
		}
	}

	public boolean isBooleanExport() {
		switch (this) {
		case LIST:
		case TEXTFILE:
		case VCARD:
		case XML:
			return false;
		default:
			return true;
		}
	}

	public boolean isTextExport() {
		switch (this) {
		case LIST:
		case TEXTFILE:
		case VCARD:
		case XML:
			return true;
		default:
			return false;
		}
	}

	public boolean isDurationExport() {
		return this == CALC;
	}

	public boolean isDateExport() {
		switch (this) {
		case HANDBASE:
		case JSON:
		case LIST:
		case TEXTFILE:
		case XML:
		case VCARD:
		case YAML:
			return false;
		default:
			return true;
		}
	}

	public boolean isImageExport() {
		switch (this) {
		case HANDBASE:
		case ACCESS:
			return true;
		default:
			return false;
		}
	}

	public boolean isTimeExport() {
		switch (this) {
		case CALC:
		case JFILE3:
		case JFILE4:
		case JFILE5:
		case MOBILEDB:
		case PILOTDB:
		case EXCEL:
		case ACCESS:
		case MARIADB:
		case SQLITE:
		case POSTGRESQL:
			return true;
		default:
			return false;
		}
	}

	public boolean isAppend() {
		switch (this) {
		case CALC:
		case EXCEL:
		case JSON:
		case YAML:
		case TEXTFILE:
		case XML:
			return false;
		default:
			return true;
		}
	}

	public boolean isConnectHost() {
		return this == MARIADB || this == POSTGRESQL;
	}

	public boolean isPasswordSupported() {
		return this == HANDBASE || this == MARIADB || this == POSTGRESQL;
	}

	public boolean isSpecialFieldSort() {
		switch (this) {
		case LIST:
		case JSON:
		case YAML:
		case XML:
			return true;
		default:
			return false;
		}
	}

	public String getDbType() {
		switch (this) {
		case SQLITE:
			return "SQLite";
		case HANDBASE:
			return "DATA";
		case JFILE3:
			return "JbDb";
		case JFILE4:
			return "JfDb";
		case JFILE5:
			return "JfD5";
		case LIST:
			return "DATA";
		case MOBILEDB:
			return "Mdb1";
		case PILOTDB:
			return "DB00";
		default:
			return "";
		}
	}

	public String getCreator() {
		switch (this) {
		case HANDBASE:
			return "HanD";
		case JFILE3:
			return "JBas";
		case JFILE4:
			return "JFil";
		case JFILE5:
			return "JFi5";
		case LIST:
			return "LSdb";
		case MOBILEDB:
			return "Mdb1";
		case PILOTDB:
			return "DBOS";
		default:
			return "";
		}
	}

	public List<String> getFileExtention() {
		return type.getExtention();
	}

	public String getFileType() {
		return type.getType();
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
		switch (this) {
		case DBASE:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
			return "T";
		case SQLITE:
		case HANDBASE:
		case JFILE3:
		case JFILE4:
		case JFILE5:
		case PILOTDB:
			return "1";
		default:
			return "true";
		}
	}

	public String getFalseValue() {
		switch (this) {
		case DBASE:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
			return "F";
		case SQLITE:
		case HANDBASE:
		case JFILE3:
		case JFILE4:
		case JFILE5:
		case PILOTDB:
			return "0";
		default:
			return "false";
		}
	}

	@Override
	public String toString() {
		return name;
	}
}