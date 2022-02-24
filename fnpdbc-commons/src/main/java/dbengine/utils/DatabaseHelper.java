package dbengine.utils;

import application.interfaces.ExportFile;
import application.preferences.Databases;

public class DatabaseHelper {
	private String database;
	private String user = "root";
	private String password = "";
	private String sslPrivateKey = "";
	private String sslCACertificate = "";
	private String sslCertificate = "";
	private String sslCipher = "";
	private boolean useSsl = false;

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
		sslPrivateKey = base.getSslPrivateKey();
		sslCACertificate = base.getSslCACertificate();
		sslCertificate = base.getSslCertificate();
		sslCipher = base.getSslCipher();
		useSsl = base.isUseSsl();
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

	public String getSslPrivateKey() {
		return sslPrivateKey;
	}

	public void setSslPrivateKey(String sslPrivateKey) {
		this.sslPrivateKey = sslPrivateKey;
	}

	public String getSslCACertificate() {
		return sslCACertificate;
	}

	public void setSslCACertificate(String sslCACertificate) {
		this.sslCACertificate = sslCACertificate;
	}

	public String getSslCertificate() {
		return sslCertificate;
	}

	public void setSslCertificate(String sslCertificate) {
		this.sslCertificate = sslCertificate;
	}

	public String getSslCipher() {
		return sslCipher;
	}

	public void setSslCipher(String sslCipher) {
		this.sslCipher = sslCipher;
	}

	public boolean isUseSsl() {
		return useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	public DatabaseHelper createCopy() {
		DatabaseHelper result = new DatabaseHelper(database, databaseType);
		result.setPassword(password);
		result.setUser(user);
		result.setSslPrivateKey(sslPrivateKey);
		result.setSslCACertificate(sslCACertificate);
		result.setSslCertificate(sslCertificate);
		result.setSslCipher(sslCipher);
		result.setUseSsl(useSsl);
		return result;
	}
}
