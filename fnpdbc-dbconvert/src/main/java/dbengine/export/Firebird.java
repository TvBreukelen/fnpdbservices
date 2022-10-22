package dbengine.export;

import java.sql.DriverManager;
import java.util.Properties;

import application.preferences.Profiles;
import application.utils.General;
import dbengine.SqlDB;

public class Firebird extends SqlDB {

	public Firebird(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Close any existing database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain a new database connection
		Properties info = new Properties();
		info.put("user", myHelper.getUser());
		info.put("password", General.decryptPassword(myHelper.getPassword()));
		info.put("charSet", "UTF8");

		// Try to obtain the database connection
		StringBuilder url = new StringBuilder();
		url.append("jdbc:firebirdsql://").append(myHelper.getHost()).append(":").append(myHelper.getPort()).append("/")
				.append(myHelper.getDatabase());
		connection = DriverManager.getConnection(url.toString(), info);
		isConnected = true;
	}
}
