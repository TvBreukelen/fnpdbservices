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

	int getSshPort();

	boolean isUseSsh();

	default String getRemoteDatabase() {
		String database = getDatabase();
		if (database.isEmpty()) {
			return database;
		}

		ExportFile exp = getDatabaseType();
		if (exp.isConnectHost()) {
			return getHost() + ":" + getPort() + "/" + database;
		}
		return database;
	}
}
