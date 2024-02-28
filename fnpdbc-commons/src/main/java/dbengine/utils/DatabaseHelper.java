package dbengine.utils;

import application.interfaces.ExportFile;
import application.utils.General;

public class DatabaseHelper {
	protected String database = General.EMPTY_STRING;
	protected String databaseVersion = General.EMPTY_STRING;
	protected String user = General.EMPTY_STRING;
	protected String password = General.EMPTY_STRING;

	// For remote databases only
	protected String host = General.EMPTY_STRING;
	protected String sslMode = General.EMPTY_STRING;
	protected String serverSslCert = General.EMPTY_STRING;
	protected String serverSslCaCert = General.EMPTY_STRING; // PostgreSQL only
	protected String keyStore = General.EMPTY_STRING;
	protected String keyStorePassword = General.EMPTY_STRING;

	protected String sshHost = General.EMPTY_STRING;
	protected String sshUser = General.EMPTY_STRING;
	protected String sshPassword = General.EMPTY_STRING;

	protected String privateKeyFile = General.EMPTY_STRING;
	protected String hostNameInCertificate = General.EMPTY_STRING; // SQL Server only

	protected int port = 0;
	protected int sshPort = 0;

	protected String databaseType = General.EMPTY_STRING;
	protected boolean useSsl = false;
	protected boolean useSsh = false;
	protected boolean trustServerCertificate = false; // SQLServer only

	public DatabaseHelper(String database, ExportFile databaseType) {
		this.database = database;
		this.databaseType = databaseType.getName();
	}

	public DatabaseHelper(DatabaseHelper helper) {
		update(helper);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
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
		this.database = extractDatabase(database);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ExportFile getDatabaseType() {
		return ExportFile.getExportFile(databaseType);
	}

	public String getDatabaseTypeAsString() {
		return databaseType;
	}

	public void setDatabaseType(Object obj) {
		if (obj instanceof ExportFile dbType) {
			databaseType = dbType.getName();
		} else {
			databaseType = obj.toString();
		}
	}

	public String getDatabaseVersion() {
		return databaseVersion;
	}

	public void setDatabaseVersion(String databaseVersion) {
		this.databaseVersion = databaseVersion;
	}

	public String getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getServerSslCert() {
		return serverSslCert;
	}

	public void setServerSslCert(String serverSslCert) {
		this.serverSslCert = serverSslCert;
	}

	public String getServerSslCaCert() {
		return serverSslCaCert;
	}

	public void setServerSslCaCert(String serverSslCaCert) {
		this.serverSslCaCert = serverSslCaCert;
	}

	public String getSslMode() {
		return sslMode;
	}

	public void setSslMode(String sslMode) {
		this.sslMode = sslMode;
	}

	public boolean isUseSsl() {
		return useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	public String getSshHost() {
		return sshHost;
	}

	public void setSshHost(String sshHost) {
		this.sshHost = sshHost;
	}

	public String getSshUser() {
		return sshUser;
	}

	public void setSshUser(String shellUser) {
		sshUser = shellUser;
	}

	public String getSshPassword() {
		return sshPassword;
	}

	public void setSshPassword(String shellPassword) {
		sshPassword = shellPassword;
	}

	public String getPrivateKeyFile() {
		return privateKeyFile;
	}

	public void setPrivateKeyFile(String privateKeyFile) {
		this.privateKeyFile = privateKeyFile;
	}

	public String getHostNameInCertificate() {
		return hostNameInCertificate;
	}

	public void setHostNameInCertificate(String hostNameInCertificate) {
		this.hostNameInCertificate = hostNameInCertificate;
	}

	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}

	public boolean isUseSsh() {
		return useSsh;
	}

	public void setUseSsh(boolean useSsh) {
		this.useSsh = useSsh;
	}

	public boolean isTrustServerCertificate() {
		return trustServerCertificate;
	}

	public void setTrustServerCertificate(boolean trustServerCertificate) {
		this.trustServerCertificate = trustServerCertificate;
	}

	public void update(DatabaseHelper helper) {
		boolean isRemoteDb = helper.getDatabaseType().isConnectHost();
		boolean isNotSsh = isRemoteDb && !helper.isUseSsh();
		boolean isNotSsl = isRemoteDb && !helper.isUseSsl();

		setDatabase(extractDatabase(helper.getDatabase()));
		setDatabaseType(helper.getDatabaseTypeAsString());
		setDatabaseVersion(helper.getDatabaseVersion());
		setUser(helper.getUser());
		setPassword(helper.getPassword());

		setHost(helper.getHost());
		setHostNameInCertificate(helper.getHostNameInCertificate());
		setPort(helper.getPort());
		setUseSsh(helper.isUseSsh());
		setSshHost(isNotSsh ? General.EMPTY_STRING : helper.getSshHost());
		setSshPort(isNotSsh ? 0 : helper.getSshPort());
		setSshUser(isNotSsh ? General.EMPTY_STRING : helper.getSshUser());
		setSshPassword(isNotSsh ? General.EMPTY_STRING : helper.getSshPassword());
		setPrivateKeyFile(helper.getPrivateKeyFile());
		setUseSsl(helper.isUseSsl());
		setKeyStore(isNotSsl ? General.EMPTY_STRING : helper.getKeyStore());
		setKeyStorePassword(isNotSsl ? General.EMPTY_STRING : helper.getKeyStorePassword());
		setServerSslCert(isNotSsl ? General.EMPTY_STRING : helper.getServerSslCert());
		setServerSslCaCert(isNotSsl ? General.EMPTY_STRING : helper.getServerSslCaCert());
		setTrustServerCertificate(isNotSsl ? !isNotSsl : helper.isTrustServerCertificate());
		setSslMode(isNotSsl ? General.EMPTY_STRING : helper.getSslMode());
	}

	public static String extractDatabase(String database) {
		if (database.contains("/")) {
			return database.substring(database.lastIndexOf('/') + 1);
		}
		return database;
	}

	public String getDatabaseName() {
		String result = database;
		if (result.isEmpty()) {
			return result;
		}

		if (getDatabaseType().isConnectHost()) {
			return host + ":" + port + "/" + result;
		}
		return result;
	}
}