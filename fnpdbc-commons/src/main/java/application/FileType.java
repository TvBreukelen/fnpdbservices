package application;

import java.util.ArrayList;
import java.util.List;

public enum FileType {
	JSON(" Json (*.json)"), PDB(" Palm database (*.pdb)"), TXT(" textfile (*.csv, *.tsv, *.txt)"),
	XLSX(" MS-Excel (*.xlsx, *.xls)"), ODS("OpenOffice Calc (*.ods)"), DB(" SQLite (*.db, *.db3, *.sqlite, *.sqlite3)"),
	DBF(" Dbase or FoxPro file (*.dbf)"), HOST(""), XML(" XML File (*.xml)"),
	MDB(" MS Access Database (*.mdb, *.accdb)"), YAML(" Yaml (*.yml, *.yaml)");

	private String name;

	FileType(String name) {
		this.name = name;
	}

	public List<String> getExtention() {
		List<String> result = new ArrayList<>();
		switch (this) {
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
		case DB:
			result.add(".db");
			result.add(".db3");
			result.add(".sqlite");
			result.add(".sqlist3");
			break;
		case DBF:
			result.add(".dbf");
			break;
		case XML:
			result.add(".xml");
			break;
		case MDB:
			result.add(".mdb");
			result.add(".accdb");
			break;
		case YAML:
			result.add(".yml");
			result.add(".yaml");
			break;
		case HOST:
			break;
		}
		return result;
	}

	public String getType() {
		return name;
	}
}
