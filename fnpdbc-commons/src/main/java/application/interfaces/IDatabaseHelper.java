package application.interfaces;

public interface IDatabaseHelper {
	String getUser();

	void setUser(String user);

	String getDatabase();

	void setDatabase(String database);

	String getPassword();

	void setPassword(String password);

	ExportFile getDatabaseType();

	void setDatabaseType(ExportFile databaseType);

	String getKeyStore();

	void setKeyStore(String keyStore);

	String getKeyStorePassword();

	void setKeyStorePassword(String keyStorePassword);

	String getServerSslCert();

	void setServerSslCert(String serverSslCert);

	String getSslMode();

	void setSslMode(String sslMode);

	boolean isUseSsl();

	void setUseSsl(boolean useSsl);

	String getServerSslCaCert();

	void setServerSslCaCert(String serverSslCaCert);
}
