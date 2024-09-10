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
	ACCESS("MS-Access", FileType.MDB, 255, 255), //
	EXCEL("MS-Excel", FileType.XLSX, 32767, 32767), //
	CALC("Calc", FileType.ODS, 1048576, 1048576), //
	TEXTFILE("Text File", FileType.TXT, 32767, 32767), //
	XML("Xml", FileType.XML, 32767, 32767), //
	JSON("Json", FileType.JSON, 32767, 32767), //
	YAML("Yaml", FileType.YAML, 32767, 32767), //
	SQLITE("SQLite", FileType.DB, 255, 255), //
	MARIADB("MariaDB", FileType.HOST, 32767, 32767), //
	POSTGRESQL("PostgreSQL", FileType.HOST, 32767, 32767), //
	FIREBIRD("Firebird", FileType.HOST, 32767, 32767), //
	SQLSERVER("SQL Server", FileType.HOST, 32767, 32767), //
	ICAL("iCalendar", FileType.ICS, 3000, 256), //
	VCARD("VCard", FileType.VCF, 3000, 256), //
	DBASE("xBase", FileType.DBF, 32737, 128), //
	PARADOX("Paradox", FileType.PARADOX, 32737, 128), //
	HANDBASE("HanDBase", FileType.PDB, 2000, 100), //
	JFILE("JFile5", FileType.PDB, 10000, 50), //
	LIST("List", FileType.PDB, 4095, 32767), //
	MOBILEDB("MobileDB", FileType.PDB, 1000, 20), //
	PILOTDB("Pilot-DB", FileType.PDB, 3000, 256);

	private String name;
	private FileType type;
	private int maxMemoSize;
	private int maxFields;

	ExportFile(String name, FileType type, int maxMemoSize, int maxFields) {
		this.name = name;
		this.type = type;
		this.maxMemoSize = maxMemoSize;
		this.maxFields = maxFields;
	}

	public static ExportFile getExportFile(String id) {
		for (ExportFile exp : values()) {
			if (exp.name.equals(id)) {
				return exp;
			}
		}
		return ACCESS;
	}

	public static String[] getExportFilenames(boolean isImport) {
		List<String> result = new ArrayList<>();
		for (ExportFile exp : values()) {
			result.add(exp.name);
		}

		if (isImport || !General.IS_WINDOWS) {
			result.remove(HANDBASE.name);
		}

		if (!isImport) {
			result.remove(JFILE.name);
			result.remove(LIST.name);
			result.remove(MOBILEDB.name);
			result.remove(PARADOX.name);
			result.remove(PILOTDB.name);
			result.remove(VCARD.name);
			result.remove(ICAL.name);
		}

		return result.toArray(new String[result.size()]);
	}

	public boolean isSpreadSheet() {
		return this == CALC || this == EXCEL;
	}

	public boolean isSqlDatabase() {
		return this == ACCESS || this == FIREBIRD || this == MARIADB || this == PARADOX || this == POSTGRESQL
				|| this == SQLITE || this == SQLSERVER;
	}

	public boolean isxBase() {
		return this == DBASE;
	}

	public boolean isBooleanExport() {
		return !(this == LIST || this == TEXTFILE || this == VCARD || this == XML);
	}

	public boolean isDurationExport() {
		return this == CALC;
	}

	public boolean isDateExport() {
		return !(this == HANDBASE || this == JSON || this == LIST || this == TEXTFILE || this == ICAL || this == VCARD
				|| this == XML || this == YAML);
	}

	public boolean isTimeExport() {
		return this == JFILE || this == MOBILEDB || this == PILOTDB || isTimestampExport();
	}

	public boolean isTimestampExport() {
		return isSpreadSheet() || isSqlDatabase() || isxBase();
	}

	public boolean isImageExport() {
		return this == HANDBASE || this == PARADOX || this == ACCESS || this == POSTGRESQL || this == SQLSERVER;
	}

	public boolean isAppend() {
		return !(this == JSON || this == YAML || this == TEXTFILE || this == XML);
	}

	public boolean isConnectHost() {
		return type == FileType.HOST;
	}

	public boolean isPasswordSupported() {
		return this == HANDBASE;
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
			return General.EMPTY_STRING;
		}
	}

	public boolean isSpecialFieldSort() {
		return this == JSON || this == YAML || this == XML;
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
			return General.EMPTY_STRING;
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
			return General.EMPTY_STRING;
		}
	}

	public List<String> getFileExtention() {
		return this == FIREBIRD ? FileType.FIREBIRD.getExtention() : type.getExtention();
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

	public String getName() {
		return name;
	}

	public String getTrueValue() {
		switch (this) {
		case DBASE, PARADOX:
			return "T";
		case SQLITE, HANDBASE, JFILE, PILOTDB:
			return "1";
		default:
			return "true";
		}
	}

	public String getFalseValue() {
		switch (this) {
		case DBASE, PARADOX:
			return "F";
		case SQLITE, HANDBASE, JFILE, PILOTDB:
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