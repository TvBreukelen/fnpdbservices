package dbengine.utils;

import application.interfaces.ExportFile;
import application.preferences.Databases;

public class DatabaseHelper {
	private String database;
	private String user = "root";
	private String password = "";
	private ExportFile databaseType;

	public DatabaseHelper(String database, ExportFile databaseType) {
		this.database = database;
		this.databaseType = databaseType;
	}

	public DatabaseHelper(Databases base) {
		database = base.getDatabaseFile();
		user = base.getDatabaseUser();
		password = base.getDatabasePassword();
		databaseType = ExportFile.getExportFile(base.getDatabaseType());
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ExportFile getDatabaseType() {
		return databaseType;
	}

	public void setDatabaseType(ExportFile databaseType) {
		this.databaseType = databaseType;
	}

	public DatabaseHelper createCopy() {
		DatabaseHelper result = new DatabaseHelper(database, databaseType);
		result.setPassword(password);
		result.setUser(user);
		return result;
	}
}
