package dbengine.export;

import java.sql.DriverManager;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import application.preferences.Profiles;
import application.utils.General;
import dbengine.SqlDB;

public class MariaDB extends SqlDB {
	private Properties info;

	public MariaDB(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Close any existing database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain a new database connection
		info = new Properties();
		info.put("user", myHelper.getUser());
		info.put("password", General.decryptPassword(myHelper.getPassword()));

		if (myHelper.isUseSsl()) {
			info.put("ssl", "true");
			addToProperies("sslMode", myHelper.getSslMode());
			addToProperies("serverSslCert", myHelper.getServerSslCert());
			addToProperies("keyStore", myHelper.getKeyStore());
			addToProperies("keyStorePassword", General.decryptPassword(myHelper.getKeyStorePassword()));
			addToProperies("keyStoreType", getKeyStoreType());
		}

		connection = DriverManager.getConnection("jdbc:mariadb://" + myDatabase, info);
		isConnected = true;
	}

	private void addToProperies(String key, String value) {
		if (StringUtils.isNotBlank(value)) {
			info.put(key, value);
		}
	}

	private String getKeyStoreType() {
		if (StringUtils.isNotBlank(myHelper.getKeyStore())) {
			return myHelper.getKeyStore().toLowerCase().endsWith(".jks") ? "JKS" : "PKCS12";
		}
		return "";
	}
}