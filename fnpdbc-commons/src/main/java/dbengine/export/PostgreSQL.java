package dbengine.export;

import java.sql.DriverManager;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import application.preferences.Profiles;
import application.utils.General;
import dbengine.SqlDB;

public class PostgreSQL extends SqlDB {
	public PostgreSQL(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// close an existing database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain the database connection
		Properties props = new Properties();
		props.setProperty("user", myHelper.getUser());
		props.setProperty("password", General.decryptPassword(myHelper.getPassword()));

		if (myHelper.isUseSsl()) {
			StringBuilder options = new StringBuilder();
			props.put("ssl", "true");
			addOption("sslcert", myHelper.getServerSslCert(), options);
			addOption("sslkey", myHelper.getKeyStore(), options);
			addOption("sslpassword", General.decryptPassword(myHelper.getKeyStorePassword()), options);
			addOption("sslrootcert", myHelper.getServerSslCaCert(), options);
			addOption("sslmode", myHelper.getSslMode(), options);

			if (options.length() > 0) {
				options.delete(0, 1); // remove first comma
				props.setProperty("options", options.toString());
			}
		}

		connection = DriverManager.getConnection("jdbc:postgresql://" + myDatabase, props);
		isConnected = true;
	}

	private void addOption(String key, String value, StringBuilder options) {
		if (StringUtils.isNotBlank(value)) {
			options.append(",").append(key).append("=").append(value);
		}
	}

}
