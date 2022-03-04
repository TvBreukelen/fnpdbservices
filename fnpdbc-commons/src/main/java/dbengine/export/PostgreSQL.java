package dbengine.export;

import java.sql.DriverManager;
import java.util.Properties;

import application.preferences.Profiles;
import application.utils.General;
import dbengine.SqlDB;

public class PostgreSQL extends SqlDB {
	public PostgreSQL(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Try to obtain the database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain the database connection
		Properties props = new Properties();
		props.setProperty("user", myHelper.getUser());
		props.setProperty("password", General.decryptPassword(myHelper.getPassword()));

		if (myHelper.isUseSsl()) {
			StringBuilder options = new StringBuilder();

			// SSL certificate
			if (!myHelper.getSslCertificate().isEmpty()) {
				options.append(",sslcert=").append(myHelper.getSslCertificate());
			}

			// SSL private key
			if (!myHelper.getSslPrivateKey().isEmpty()) {
				options.append(",sslkey=").append(myHelper.getSslPrivateKey());
			}

			// SSL root certificate
			if (!myHelper.getSslCACertificate().isEmpty()) {
				options.append(",sslrootcert=").append(myHelper.getSslCACertificate());
			}

			// SSL mode
			if (!myHelper.getSslMode().isEmpty()) {
				options.append(",sslmode=").append(myHelper.getSslMode());
			}

			// Check if additional options are set
			if (options.length() > 0) {
				// Remove first comma
				options.delete(0, 1);
				props.setProperty("options", options.toString());
			} else {
				props.setProperty("ssl", "true"); // Same as verify-full
			}
		}

		connection = DriverManager.getConnection("jdbc:postgresql://" + myDatabase, props);
		isConnected = true;
	}
}
