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
	ACCESS("MS-Access", FileType.MDB, 255), EXCEL("MS-Excel", FileType.XLSX, 16384), CALC("Calc", FileType.ODS, 16384),
	TEXTFILE("Text File", FileType.TXT, 32767), XML("Xml", FileType.XML, 32767), JSON("Json", FileType.JSON, 32767),
	YAML("Yaml", FileType.YAML, 32767), SQLITE("SQLite", FileType.DB, 2000), MARIADB("MariaDB", FileType.HOST, 4096),
	POSTGRESQL("PostgreSQL", FileType.HOST, 1600), FIREBIRD("Firebird", FileType.HOST, 16000),
	SQLSERVER("SQL Server", FileType.HOST, 1024), ICAL("iCalendar", FileType.ICS, 32767),
	VCARD("VCard", FileType.VCF, 32767), DBASE("xBase", FileType.DBF, 254), PARADOX("Paradox", FileType.PARADOX, 128),
	HANDBASE("HanDBase", FileType.PDB, 100);

	private String name;
	private FileType type;
	private int maxFields;

	ExportFile(String name, FileType type, int maxFields) {
		this.name = name;
		this.type = type;
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
			result.remove(PARADOX.name);
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
		return !(this == TEXTFILE || this == VCARD || this == XML);
	}

	public boolean isDurationExport() {
		return this == CALC;
	}

	public boolean isDateExport() {
		return !(this == HANDBASE || this == JSON || this == TEXTFILE || this == ICAL || this == VCARD || this == XML
				|| this == YAML);
	}

	public boolean isTimeExport() {
		return isTimestampExport();
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
		default:
			return General.EMPTY_STRING;
		}
	}

	public String getCreator() {
		switch (this) {
		case HANDBASE:
			return "HanD";
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

	public String getName() {
		return name;
	}

	public String getTrueValue() {
		switch (this) {
		case DBASE, PARADOX:
			return "T";
		case SQLITE, HANDBASE:
			return "1";
		default:
			return "true";
		}
	}

	public String getFalseValue() {
		switch (this) {
		case DBASE, PARADOX:
			return "F";
		case SQLITE, HANDBASE:
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