package application;

import java.util.ArrayList;
import java.util.List;

public enum FileType {
	PDB(" Palm or Pocket PC database (*.pdb)"), TXT(" textfile (*.csv, *.txt)"), XLS(" MS-Excel file (*.xls)"),
	XLSX(" MS-Excel file (*.xls, *.xlsx)"), DB(" SQLite (*.db, *.db3, *.sqlite, *.sqlite3)"),
	DBF(" Dbase or FoxPro file (*.dbf)"), XML(" XML File (*.xml)"), MDB(" MS Access Database (*.mdb, *.accdb)");

	private String name;

	private FileType(String name) {
		this.name = name;
	}

	public List<String> getExtention() {
		List<String> result = new ArrayList<>();
		switch (this) {
		case PDB:
			result.add(".pdb");
			break;
		case TXT:
			result.add(".csv");
			result.add(".txt");
			break;
		case XLS:
			result.add(".xls");
			break;
		case XLSX:
			result.add(".xls");
			result.add(".xlsx");
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
		}
		return result;
	}

	public String getType() {
		return name;
	}
}
