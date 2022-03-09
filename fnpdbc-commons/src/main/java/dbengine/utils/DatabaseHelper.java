package dbengine.utils;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseHelper;

public class DatabaseHelper implements IDatabaseHelper {
	private String database;
	private String user = "root";
	private String password = "";
	private String sslMode = "";
	private String serverSslCert = "";
	private String serverSslCaCert = ""; // PostgreSQL only
	private String keyStore = "";
	private String keyStorePassword = "";

	private ExportFile databaseType;
	private boolean useSsl = false;

	public DatabaseHelper(String database, ExportFile databaseType) {
		this.database = database;
		this.databaseType = databaseType;

		user = databaseType == ExportFile.POSTGRESQL ? "postgres" : "root";
		sslMode = databaseType == ExportFile.POSTGRESQL ? "prefer" : "trust";
	}

	public DatabaseHelper(IDatabaseHelper helper) {
		update(helper);
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String getDatabase() {
		return database;
	}

	@Override
	public void setDatabase(String database) {
		this.database = database;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public ExportFile getDatabaseType() {
		return databaseType;
	}

	@Override
	public void setDatabaseType(ExportFile databaseType) {
		this.databaseType = databaseType;
	}

	@Override
	public String getKeyStore() {
		return keyStore;
	}

	@Override
	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	@Override
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	@Override
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	@Override
	public String getServerSslCert() {
		return serverSslCert;
	}

	@Override
	public void setServerSslCert(String serverSslCert) {
		this.serverSslCert = serverSslCert;
	}

	@Override
	public String getServerSslCaCert() {
		return serverSslCaCert;
	}

	@Override
	public void setServerSslCaCert(String serverSslCaCert) {
		this.serverSslCaCert = serverSslCaCert;
	}

	@Override
	public String getSslMode() {
		return sslMode;
	}

	@Override
	public void setSslMode(String sslMode) {
		this.sslMode = sslMode;
	}

	@Override
	public boolean isUseSsl() {
		return useSsl;
	}

	@Override
	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	public DatabaseHelper createCopy() {
		return new DatabaseHelper(this);
	}

	public void update(IDatabaseHelper helper) {
		setDatabase(helper.getDatabase());
		setDatabaseType(helper.getDatabaseType());
		setPassword(helper.getPassword());
		setUser(helper.getUser());
		setKeyStore(helper.getKeyStore());
		setKeyStorePassword(helper.getKeyStorePassword());
		setServerSslCert(helper.getServerSslCert());
		setServerSslCaCert(helper.getServerSslCaCert());
		setSslMode(helper.getSslMode());
		setUseSsl(helper.isUseSsl());
	}
}