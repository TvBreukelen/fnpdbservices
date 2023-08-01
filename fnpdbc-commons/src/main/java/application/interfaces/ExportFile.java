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
	ACCESS("MS-Access", FileType.MDB, 255, 255, 255), //
	EXCEL("MS-Excel", FileType.XLSX, 256, 32767, 32767), //
	CALC("Calc", FileType.ODS, 256, 1048576, 1048576), //
	TEXTFILE("Text File", FileType.TXT, 256, 32767, 32767), //
	XML("Xml", FileType.XML, 32767, 32767, 32767), //
	JSON("Json", FileType.JSON, 32767, 32767, 32767), //
	YAML("Yaml", FileType.YAML, 32767, 32767, 32767), //
	SQLITE("SQLite", FileType.DB, 255, 255, 255), //
	MARIADB("MariaDB", FileType.HOST, 32767, 32767, 32767), //
	POSTGRESQL("PostgreSQL", FileType.HOST, 32767, 32767, 32767), //
	FIREBIRD("Firebird", FileType.HOST, 32767, 32767, 32767), //
	SQLSERVER("SQL Server", FileType.HOST, 32767, 32767, 32767), //
	ICAL("iCalendar", FileType.ICS, 256, 3000, 256), //
	VCARD("VCard", FileType.VCF, 256, 3000, 256), //
	DBASE("xBase", FileType.DBF, 254, 32737, 128), //
	DBASE3("DBase3", FileType.DBF, 254, 32737, 128), //
	DBASE4("DBase4", FileType.DBF, 254, 32767, 255), //
	DBASE5("DBase5", FileType.DBF, 254, 32767, 1024), //
	FOXPRO("FoxPro", FileType.DBF, 254, 32767, 255), //
	PARADOX("Paradox", FileType.PARADOX, 254, 32737, 128), //
	HANDBASE("HanDBase", FileType.PDB, 256, 2000, 100), //
	JFILE("JFile5", FileType.PDB, 256, 10000, 50), //
	LIST("List", FileType.PDB, 4095, 4095, 32767), //
	MOBILEDB("MobileDB", FileType.PDB, 256, 1000, 20), //
	PILOTDB("Pilot-DB", FileType.PDB, 256, 3000, 256);

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

		if (isImport || !General.IS_WINDOWS) {
			result.remove(HANDBASE.name);
		}

		if (isImport) {
			result.remove(DBASE3.name);
			result.remove(DBASE4.name);
			result.remove(DBASE5.name);
			result.remove(FOXPRO.name);
		} else {
			result.remove(DBASE.name);
			result.remove(ACCESS.name);
			result.remove(FIREBIRD.name);
			result.remove(MARIADB.name);
			result.remove(PARADOX.name);
			result.remove(POSTGRESQL.name);
			result.remove(SQLITE.name);
			result.remove(SQLSERVER.name);
			result.remove(VCARD.name);
			result.remove(ICAL.name);
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
		case FIREBIRD:
		case MARIADB:
		case PARADOX:
		case POSTGRESQL:
		case SQLITE:
		case SQLSERVER:
			return true;
		default:
			return false;
		}
	}

	public boolean isxBase() {
		switch (this) {
		case DBASE:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
			return true;
		default:
			return false;
		}
	}

	public boolean isSqlDatabase() {
		switch (this) {
		case ACCESS:
		case FIREBIRD:
		case MARIADB:
		case PARADOX:
		case POSTGRESQL:
		case SQLITE:
		case SQLSERVER:
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

	public boolean isDurationExport() {
		return this == CALC;
	}

	public boolean isDateExport() {
		switch (this) {
		case HANDBASE:
		case JSON:
		case LIST:
		case TEXTFILE:
		case ICAL:
		case VCARD:
		case XML:
		case YAML:
			return false;
		default:
			return true;
		}
	}

	public boolean isTimeExport() {
		switch (this) {
		case CALC:
		case EXCEL:
		case JFILE:
		case MOBILEDB:
		case PILOTDB:
			return true;
		default:
			return isDatabase() || isxBase();
		}
	}

	public boolean isTimestampExport() {
		switch (this) {
		case CALC:
		case EXCEL:
			return true;
		default:
			return isDatabase() || isxBase();
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
		return type == FileType.HOST;
	}

	public boolean isPasswordSupported() {
		return this == HANDBASE || isConnectHost();
	}

	public int getPort() {
		switch (this) {
		case FIREBIRD:
			return 3050;
		case MARIADB:
			return 3306;
		case POSTGRESQL:
			return 5432;
		case SQLSERVER:
			return 1433;
		default:
			return 0;
		}
	}

	public String getUser() {
		switch (this) {
		case FIREBIRD:
			return "sysdba";
		case MARIADB:
			return "root";
		case POSTGRESQL:
			return "postgres";
		default:
			return "";
		}
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
		case JFILE:
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
		case JFILE:
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
		case PARADOX:
			return "T";
		case SQLITE:
		case HANDBASE:
		case JFILE:
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
		case PARADOX:
			return "F";
		case SQLITE:
		case HANDBASE:
		case JFILE:
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