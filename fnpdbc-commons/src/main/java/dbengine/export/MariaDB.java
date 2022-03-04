package dbengine.export;

import java.sql.DriverManager;

import application.preferences.Profiles;
import application.utils.General;
import dbengine.SqlDB;

public class MariaDB extends SqlDB {
	public MariaDB(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Try to obtain the database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain the database connection
		StringBuilder url = new StringBuilder();
		url.append("jdbc:mariadb://");
		url.append(myDatabase);

		if (myHelper.isUseSsl()) {
			boolean isServerValidation = false;
			if (!myHelper.getSslCertificate().isEmpty()) {
				url.append(";SSLCERT=").append(myHelper.getSslCertificate());
				isServerValidation = true;
			}

			if (!myHelper.getSslPrivateKey().isEmpty()) {
				url.append(";SSLKEY=").append(myHelper.getSslPrivateKey());
				isServerValidation = true;
			}

			if (!myHelper.getSslCACertificate().isEmpty()) {
				url.append(";SSLCA=").append(myHelper.getSslCACertificate());
				isServerValidation = true;
			}

			if (!myHelper.getSslCipher().isEmpty()) {
				url.append(";SSLCIPHER=").append(myHelper.getSslCipher());
			}

			if (isServerValidation) {
				url.append(";SSLVERIFY=1");
			}
		}

		connection = DriverManager.getConnection(url.toString(), myHelper.getUser(),
				General.decryptPassword(myHelper.getPassword()));
		isConnected = true;
	}
}