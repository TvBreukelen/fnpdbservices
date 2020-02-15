package dbengine.utils;

public class DatabaseHelper {
	private String database;
	private String user = "";
	private String password = "";

	public DatabaseHelper(String database) {
		this.database = database;
	}

	public DatabaseHelper(String database, String user, String password) {
		this.database = database;
		this.user = user;
		this.password = password;
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
}
