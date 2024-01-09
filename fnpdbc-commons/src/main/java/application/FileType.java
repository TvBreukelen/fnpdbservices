package application;

import java.util.ArrayList;
import java.util.List;

import application.utils.General;

public enum FileType {
	NEW(General.EMPTY_STRING), JSON(" Json (*.json)"), PDB(" Palm database (*.pdb)"),
	TXT(" textfile (*.csv, *.tsv, *.txt)"), XLSX(" MS-Excel (*.xlsx, *.xls)"), ODS("OpenOffice Calc (*.ods)"),
	DB(" SQLite (*.db, *.db3, *.sqlite, *.sqlite3)"), DBF(" Dbase / FoxPro file (*.dbf)"), HOST(General.EMPTY_STRING),
	PARADOX("Paradox (*.db"), XML(" XML File (*.xml)"), MDB(" MS Access Database (*.mdb, *.accdb)"),
	FIREBIRD(" Firebird (*.fdb)"), TRUSTSTORE(" Certificate (*.crt, *.pem)"),
	KEYSTORE(" Java KeyStore (*.jks,*.p12, *.pfx"), PPK(" PuTTy private key ('*.pkk)"),
	ICS("ICalendar (*.ics, *.ical, *.icalendar, *.ifb)"), VCF("VCard (*.vcf, *.hlml, *.json, *.xml)"),
	YAML(" Yaml (*.yml, *.yaml)");

	private String name;

	FileType(String name) {
		this.name = name;
	}

	public List<String> getExtention() {
		List<String> result = new ArrayList<>();
		switch (this) {
		case ICS:
			result.add(".ics");
			result.add(".ical");
			result.add(".icalendar");
			result.add(".ifb");
			break;
		case JSON:
			result.add(".json");
			break;
		case PDB:
			result.add(".pdb");
			break;
		case TXT:
			result.add(".csv");
			result.add(".tsv");
			result.add(".txt");
			break;
		case XLSX:
			result.add(".xlsx");
			result.add(".xls");
			break;
		case ODS:
			result.add(".ods");
			break;
		case FIREBIRD:
			result.add(".fdb");
			break;
		case DB:
			result.add(".db");
			result.add(".db3");
			result.add(".sqlite");
			result.add(".sqlist3");
			break;
		case DBF:
			result.add(".dbf");
			break;
		case PARADOX:
			result.add(".db");
			break;
		case XML:
			result.add(".xml");
			break;
		case MDB:
			result.add(".mdb");
			result.add(".accdb");
			break;
		case TRUSTSTORE:
			result.add(".der");
			result.add(".pem");
			break;
		case KEYSTORE:
			result.add(".jks");
			result.add(".p12");
			result.add(".pfx");
			break;
		case PPK:
			result.add(".ppk");
			break;
		case YAML:
			result.add(".yml");
			result.add(".yaml");
			break;
		case VCF:
			result.add(".vcf");
			result.add(".xml");
			result.add(".html");
			result.add(".json");
			break;
		case HOST:
		case NEW:
			break;
		}
		return result;
	}

	public String getType() {
		return name;
	}
}
