package dbengine.utils;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseHelper;
import application.utils.General;

public class DatabaseHelper implements IDatabaseHelper {
	private String database;
	private String user = General.EMPTY_STRING;
	private String host = General.EMPTY_STRING;
	private String password = General.EMPTY_STRING;
	private String sslMode = General.EMPTY_STRING;
	private String serverSslCert = General.EMPTY_STRING;
	private String serverSslCaCert = General.EMPTY_STRING; // PostgreSQL only
	private String keyStore = General.EMPTY_STRING;
	private String keyStorePassword = General.EMPTY_STRING;

	private String sshHost = General.EMPTY_STRING;
	private String sshUser = General.EMPTY_STRING;
	private String sshPassword = General.EMPTY_STRING;

	private String privateKeyFile = General.EMPTY_STRING;
	private String hostNameInCertificate = General.EMPTY_STRING; // SQL Server only

	private int port;
	private int sshPort;

	private ExportFile databaseType;
	private boolean useSsl = false;
	private boolean useSsh = false;
	private boolean trustServerCertificate = false; // SQLServer only

	public DatabaseHelper(String database, ExportFile databaseType) {
		this.database = database;
		this.databaseType = databaseType;
	}

	public DatabaseHelper(IDatabaseHelper helper) {
		update(helper);
	}

	@Override
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public ExportFile getDatabaseType() {
		return databaseType;
	}

	public void setDatabaseType(ExportFile databaseType) {
		this.databaseType = databaseType;
	}

	@Override
	public String getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	@Override
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	@Override
	public String getServerSslCert() {
		return serverSslCert;
	}

	public void setServerSslCert(String serverSslCert) {
		this.serverSslCert = serverSslCert;
	}

	@Override
	public String getServerSslCaCert() {
		return serverSslCaCert;
	}

	public void setServerSslCaCert(String serverSslCaCert) {
		this.serverSslCaCert = serverSslCaCert;
	}

	@Override
	public String getSslMode() {
		return sslMode;
	}

	public void setSslMode(String sslMode) {
		this.sslMode = sslMode;
	}

	@Override
	public boolean isUseSsl() {
		return useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	@Override
	public String getSshHost() {
		return sshHost;
	}

	public void setSshHost(String sshHost) {
		this.sshHost = sshHost;
	}

	@Override
	public String getSshUser() {
		return sshUser;
	}

	public void setSshUser(String shellUser) {
		sshUser = shellUser;
	}

	@Override
	public String getSshPassword() {
		return sshPassword;
	}

	public void setSshPassword(String shellPassword) {
		sshPassword = shellPassword;
	}

	@Override
	public String getPrivateKeyFile() {
		return privateKeyFile;
	}

	public void setPrivateKeyFile(String privateKeyFile) {
		this.privateKeyFile = privateKeyFile;
	}

	@Override
	public String getHostNameInCertificate() {
		return hostNameInCertificate;
	}

	public void setHostNameInCertificate(String hostNameInCertificate) {
		this.hostNameInCertificate = hostNameInCertificate;
	}

	@Override
	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}

	@Override
	public boolean isUseSsh() {
		return useSsh;
	}

	public void setUseSsh(boolean useSsh) {
		this.useSsh = useSsh;
	}

	@Override
	public boolean isTrustServerCertificate() {
		return trustServerCertificate;
	}

	public void setTrustServerCertificate(boolean trustServerCertificate) {
		this.trustServerCertificate = trustServerCertificate;
	}

	@Override
	public void update(IDatabaseHelper helper) {
		boolean isRemoteDb = helper.getDatabaseType().isConnectHost();
		boolean isPasswordSupported = helper.getDatabaseType().isPasswordSupported();
		boolean isNotSsh = isRemoteDb && !helper.isUseSsh();
		boolean isNotSsl = isRemoteDb && !helper.isUseSsl();

		setDatabase(helper.getDatabase());
		setDatabaseType(helper.getDatabaseType());
		setUser(helper.getUser());
		setPassword(isPasswordSupported ? helper.getPassword() : General.EMPTY_STRING);

		setHost(isRemoteDb ? General.EMPTY_STRING : helper.getHost());
		setHostNameInCertificate(isNotSsl ? General.EMPTY_STRING : helper.getHostNameInCertificate());
		setPort(isRemoteDb ? 0 : helper.getPort());
		setUseSsh(isNotSsh ? !isNotSsh : helper.isUseSsh());
		setSshHost(isNotSsh ? General.EMPTY_STRING : helper.getSshHost());
		setSshPort(isNotSsh ? 0 : helper.getSshPort());
		setSshUser(isNotSsh ? General.EMPTY_STRING : helper.getSshUser());
		setSshPassword(isNotSsh ? General.EMPTY_STRING : helper.getSshPassword());
		setPrivateKeyFile(isRemoteDb ? General.EMPTY_STRING : helper.getPrivateKeyFile());
		setUseSsl(isNotSsl ? !isRemoteDb : helper.isUseSsl());
		setKeyStore(isNotSsl ? General.EMPTY_STRING : helper.getKeyStore());
		setKeyStorePassword(isNotSsl ? General.EMPTY_STRING : helper.getKeyStorePassword());
		setServerSslCert(isNotSsl ? General.EMPTY_STRING : helper.getServerSslCert());
		setServerSslCaCert(isNotSsl ? General.EMPTY_STRING : helper.getServerSslCaCert());
		setTrustServerCertificate(isNotSsl ? !isRemoteDb : helper.isTrustServerCertificate());
		setSslMode(isNotSsl ? General.EMPTY_STRING : helper.getSslMode());
	}

	public String getDatabaseName() {
		String result = getDatabase();
		if (result.isEmpty()) {
			return result;
		}

		if (databaseType.isConnectHost()) {
			return getHost() + ":" + getPort() + "/" + result;
		}
		return result;
	}

}