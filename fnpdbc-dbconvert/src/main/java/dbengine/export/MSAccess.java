package dbengine.export;

import java.sql.DriverManager;

import application.preferences.Profiles;
import dbengine.SqlDB;
import net.ucanaccess.jdbc.UcanaccessDriver;

public class MSAccess extends SqlDB {

	public MSAccess(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Close any existing database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain a new database connection
		String url = UcanaccessDriver.URL_PREFIX + myHelper.getDatabase();
		connection = DriverManager.getConnection(url);
		isConnected = true;
	}

}
