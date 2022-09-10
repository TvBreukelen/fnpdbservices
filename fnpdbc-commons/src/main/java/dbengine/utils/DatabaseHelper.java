package dbengine.utils;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseHelper;

public class DatabaseHelper implements IDatabaseHelper {
	private String database;
	private String user = "";
	private String host = "";
	private String password = "";
	private String sslMode = "";
	private String serverSslCert = "";
	private String serverSslCaCert = ""; // PostgreSQL only
	private String keyStore = "";
	private String keyStorePassword = "";

	private String sshHost = "";
	private String sshUser = "";
	private String sshPassword = "";

	private String privateKeyFile = "";
	private String hostNameInCertificate = ""; // SQL Server only

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
		this.sshUser = shellUser;
	}

	@Override
	public String getSshPassword() {
		return sshPassword;
	}

	public void setSshPassword(String shellPassword) {
		this.sshPassword = shellPassword;
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

	public void update(IDatabaseHelper helper) {
		setDatabase(helper.getDatabase());
		setDatabaseType(helper.getDatabaseType());
		setHost(helper.getHost());
		setHostNameInCertificate(helper.getHostNameInCertificate());
		setPort(helper.getPort());
		setUser(helper.getUser());
		setPassword(helper.getPassword());
		setUseSsh(helper.isUseSsh());
		setSshHost(helper.getSshHost());
		setSshPort(helper.getSshPort());
		setSshUser(helper.getSshUser());
		setSshPassword(helper.getSshPassword());
		setPrivateKeyFile(helper.getPrivateKeyFile());
		setUseSsl(helper.isUseSsl());
		setKeyStore(helper.getKeyStore());
		setKeyStorePassword(helper.getKeyStorePassword());
		setServerSslCert(helper.getServerSslCert());
		setServerSslCaCert(helper.getServerSslCaCert());
		setTrustServerCertificate(helper.isTrustServerCertificate());
		setSslMode(helper.getSslMode());
	}
}