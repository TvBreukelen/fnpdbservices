package application;

public enum FileType {
	PDB(" Palm or Pocket PC database (*.pdb)"), TXT(" textfile (*.csv, *.txt)"), XLS(" MS-Excel file (*.xls)"),
	XLSX(" MS-Excel file (*.xls, *.xlsx)"), DB(" SQLite (*.db, *.db3, *.sqlite, *.sqlite3)"), DBF(" Dbase or FoxPro file (*.dbf)"),
	XML(" XML File (*.xml)"), MDB(" MS Access Database (*.mdb, *.accdb)");

	private String name;

	private FileType(String name) {
		this.name = name;
	}

	public String[] getExtention() {
		switch (this) {
		case PDB:
			return new String[] { "pdb" };
		case TXT:
			return new String[] { "csv", "txt" };
		case XLS:
			return new String[] { "xls" };
		case XLSX:
			return new String[] { "xls", "xlsx" };
		case DB:
			return new String[] { "db", "db3", "sqlite", "sqlite3", "*" };
		case DBF:
			return new String[] { "dbf" };
		case XML:
			return new String[] { "xml" };
		case MDB:
			return new String[] { "mdb", "accdb" };
		}
		return null;
	}

	public String getType() {
		return name;
	}
}
