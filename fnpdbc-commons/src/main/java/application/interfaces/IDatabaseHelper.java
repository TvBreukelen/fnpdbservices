package application.interfaces;

public interface IDatabaseHelper {
	String getUser();

	String getDatabase();

	String getHost();

	int getPort();

	String getPassword();

	ExportFile getDatabaseType();

	String getKeyStore();

	String getKeyStorePassword();

	String getServerSslCert();

	String getSslMode();

	boolean isUseSsl();

	String getServerSslCaCert();

	String getSshHost();

	String getSshUser();

	String getSshPassword();

	String getPrivateKeyFile();

	String getHostNameInCertificate();

	boolean isTrustServerCertificate();

	int getSshPort();

	boolean isUseSsh();

	void update(IDatabaseHelper helper);
}