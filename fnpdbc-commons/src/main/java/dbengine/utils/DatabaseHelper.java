package dbengine.utils;

import application.interfaces.ExportFile;
import application.preferences.Databases;

public class DatabaseHelper {
	private String database;
	private String user = "root";
	private String password = "";
	private String host = "127.0.0.1";
	private int port = 3306;
	private ExportFile databaseType;

	public DatabaseHelper(String database, ExportFile databaseType) {
		this.database = database;
		this.databaseType = databaseType;
	}

	public DatabaseHelper(Databases base) {
		database = base.getDatabaseFile();
		user = base.getDatabaseUser();
		password = base.getDatabasePassword();
		host = base.getDatabaseHost();
		port = base.getDatabasePort();
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

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public ExportFile getDatabaseType() {
		return databaseType;
	}

	public void setDatabaseType(ExportFile databaseType) {
		this.databaseType = databaseType;
	}

	@Override
	public String toString() {
		return databaseType.isConnectHost() ? host + ":" + port + "/" + database : database;
	}

	public DatabaseHelper createCopy() {
		DatabaseHelper result = new DatabaseHelper(database, databaseType);
		result.setHost(host);
		result.setPassword(password);
		result.setPort(port);
		result.setUser(user);
		return result;
	}
}
