package application.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseHelper;
import application.interfaces.TvBSoftware;

public class Databases implements IDatabaseHelper {
	private String databaseID = "";
	private String database = "";
	private String user = "";
	private String password = "";
	private String databaseType;
	private String databaseVersion = "";

	// Remote databases
	private String host = "";
	private int port = 0;

	// Remote databases SSH
	private boolean useSsh = false;
	private String sshHost = "";
	private String sshUser = "";
	private String sshPassword = "";
	private String privateKeyFile = "";
	private int sshPort;

	// Remote databases SSL/TSL
	private boolean useSsl = false;
	private boolean trustServerCertificate = false;

	private String serverSslCert = "";
	private String serverSslCaCert = "";
	private String keyStore = "";
	private String keyStorePassword = "";
	private String hostNameInCertificate = "";
	private String sslMode = "";

	// For the import of Text files
	private String fieldSeparator = ",";
	private String textDelimiter = "\"";
	private String textFileFormat = "";

	private static final String DB_FILE = "database.file";
	private static final String DB_TYPE = "database.type";

	private TvBSoftware mySoftware;
	private Map<String, String> nodes = new HashMap<>();

	protected Preferences myPref;
	private Preferences parent;

	private static EnumMap<TvBSoftware, Databases> mapDB = new EnumMap<>(TvBSoftware.class);

	public static Databases getInstance(TvBSoftware software) {
		return mapDB.computeIfAbsent(software, Databases::new);
	}

	private Databases(TvBSoftware software) {
		mySoftware = software;
		parent = Preferences.userRoot().node(mySoftware.getName().toLowerCase());
		parent = parent.node("databases");
		fillNodes();
	}

	public String[] getDatabases() {
		try {
			return parent.childrenNames();
		} catch (Exception e) {
		}
		return new String[0];
	}

	public void setNode(String databaseID) {
		if (databaseID.isEmpty()) {
			return;
		}

		myPref = parent.node(databaseID);
		this.databaseID = databaseID;

		database = myPref.get(DB_FILE, "");
		password = myPref.get("database.password", "");
		databaseType = myPref.get(DB_TYPE, "");
		user = myPref.get("database.user", "");
		databaseVersion = myPref.get("database.version", "");

		host = myPref.get("remote.host", "");
		port = myPref.getInt("remote.port", 0);

		// SSH
		useSsh = myPref.getBoolean("use.ssh", false);
		sshHost = myPref.get("ssh.host", "");
		sshPort = myPref.getInt("ssh.port", 0);
		sshUser = myPref.get("ssh.user", "");
		sshPassword = myPref.get("ssh.password", "");
		privateKeyFile = myPref.get("ssh.key.file", "");

		// SSL
		useSsl = myPref.getBoolean("use.ssl", false);
		sslMode = myPref.get("ssl.mode", "");
		keyStore = myPref.get("ssl.keystore", "");
		keyStorePassword = myPref.get("ssl.keystore.password", "");
		serverSslCert = myPref.get("ssl.server.cert", "");
		serverSslCaCert = myPref.get("ssl.server.ca.cert", "");
		trustServerCertificate = myPref.getBoolean("ssl.trust.server.certificate", false);
		hostNameInCertificate = myPref.get("ssl.hostname.in.certificate", "");

		fieldSeparator = myPref.get("field.separator", ",");
		textDelimiter = myPref.get("text.delimiter", "\"");
		textFileFormat = myPref.get("textfile.format", "");
	}

	public void deleteNode(String databaseID) {
		PrefUtils.deleteNode(parent, databaseID);
		fillNodes();
	}

	public String getNodename(String databaseFile, String databaseType) {
		return nodes.get((databaseFile + "/" + databaseType).toUpperCase());
	}

	public String getNextDatabaseID() {
		int index = 0;
		try {
			String[] nodeNames = parent.childrenNames();
			for (String element : nodeNames) {
				if (Integer.parseInt(element) == index) {
					index++;
				} else {
					break;
				}
			}
		} catch (Exception e) {
		}

		StringBuilder result = new StringBuilder(String.valueOf(index));
		int i = result.length();
		while (i++ < 6) {
			result.insert(0, "0");
		}
		return result.toString();
	}

	public List<String> getDatabaseFiles(ExportFile myImportFile) {
		List<String> result = new ArrayList<>();
		result.add("");

		for (String db : getDatabases()) {
			setNode(db);
			if (getDatabaseType() == myImportFile) {
				result.add(getDatabase());
			}
		}

		Collections.sort(result);
		return result;
	}

	public String getDatabaseID() {
		return databaseID;
	}

	@Override
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		PrefUtils.writePref(myPref, "remote.host", host, this.host, "");
		this.host = host;
	}

	@Override
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		PrefUtils.writePref(myPref, "remote.port", port, this.port, 0);
		this.port = port;
	}

	@Override
	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		PrefUtils.writePref(myPref, DB_FILE, database, this.database, "");
		this.database = database;
		fillNodes();
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(String databasePassword) {
		PrefUtils.writePref(myPref, "database.password", databasePassword, this.password, "");
		this.password = databasePassword;
	}

	@Override
	public String getUser() {
		return user;
	}

	public void setUser(String databaseUser) {
		PrefUtils.writePref(myPref, "database.user", databaseUser, this.user, "");
		this.user = databaseUser;
	}

	@Override
	public ExportFile getDatabaseType() {
		return ExportFile.getExportFile(databaseType);
	}

	public void setDatabaseType(ExportFile databaseType) {
		PrefUtils.writePref(myPref, DB_TYPE, databaseType.getName(), this.databaseType, "");
		this.databaseType = databaseType.getName();
	}

	public String getDatabaseTypeAsString() {
		return databaseType;
	}

	public void setDatabaseTypeAsString(String databaseType) {
		PrefUtils.writePref(myPref, DB_TYPE, databaseType, this.databaseType, "");
		this.databaseType = databaseType;
	}

	public String getDatabaseVersion() {
		return databaseVersion;
	}

	public void setDatabaseVersion(String databaseVersion) {
		PrefUtils.writePref(myPref, "database.version", databaseVersion, this.databaseVersion, "");
		this.databaseVersion = databaseVersion;
	}

	@Override
	public boolean isUseSsl() {
		return useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		PrefUtils.writePref(myPref, "use.ssl", useSsl, this.useSsl, false);
		this.useSsl = useSsl;
	}

	@Override
	public String getSslMode() {
		return sslMode;
	}

	public void setSslMode(String sslMode) {
		PrefUtils.writePref(myPref, "ssl.mode", sslMode, this.sslMode, "");
		this.sslMode = sslMode;
	}

	@Override
	public String getServerSslCert() {
		return serverSslCert;
	}

	public void setServerSslCert(String serverSslCert) {
		PrefUtils.writePref(myPref, "ssl.server.cert", serverSslCert, this.serverSslCert, "");
		this.serverSslCert = serverSslCert;
	}

	@Override
	public String getServerSslCaCert() {
		return serverSslCaCert;
	}

	public void setServerSslCaCert(String serverSslCaCert) {
		PrefUtils.writePref(myPref, "ssl.server.ca.cert", serverSslCaCert, this.serverSslCaCert, "");
		this.serverSslCaCert = serverSslCaCert;
	}

	@Override
	public String getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(String keyStore) {
		PrefUtils.writePref(myPref, "ssl.keystore", keyStore, this.keyStore, "");
		this.keyStore = keyStore;
	}

	@Override
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		PrefUtils.writePref(myPref, "ssl.keystore.password", keyStorePassword, this.keyStorePassword, "");
		this.keyStorePassword = keyStorePassword;
	}

	@Override
	public boolean isUseSsh() {
		return useSsh;
	}

	public void setUseSsh(boolean useSsh) {
		PrefUtils.writePref(myPref, "use.ssh", useSsh, this.useSsh, false);
		this.useSsh = useSsh;
	}

	@Override
	public String getSshHost() {
		return sshHost;
	}

	public void setSshHost(String sshHost) {
		PrefUtils.writePref(myPref, "ssh.host", sshHost, this.sshHost, "");
		this.sshHost = sshHost;
	}

	@Override
	public String getSshUser() {
		return sshUser;
	}

	public void setSshUser(String shellUser) {
		PrefUtils.writePref(myPref, "ssh.user", shellUser, this.sshUser, "");
		this.sshUser = shellUser;
	}

	@Override
	public String getSshPassword() {
		return sshPassword;
	}

	public void setSshPassword(String shellPassword) {
		PrefUtils.writePref(myPref, "ssh.password", shellPassword, this.sshPassword, "");
		this.sshPassword = shellPassword;
	}

	@Override
	public String getPrivateKeyFile() {
		return privateKeyFile;
	}

	public void setPrivateKeyFile(String privateKeyFile) {
		PrefUtils.writePref(myPref, "ssh.key.file", privateKeyFile, this.privateKeyFile, "");
		this.privateKeyFile = privateKeyFile;
	}

	@Override
	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		PrefUtils.writePref(myPref, "ssh.port", sshPort, this.sshPort, 0);
		this.sshPort = sshPort;
	}

	@Override
	public boolean isTrustServerCertificate() {
		return trustServerCertificate;
	}

	public void setTrustServerCertificate(boolean trustServerCertificate) {
		PrefUtils.writePref(myPref, "ssl.trust.server.certificate", trustServerCertificate, this.trustServerCertificate,
				false);
		this.trustServerCertificate = trustServerCertificate;
	}

	@Override
	public String getHostNameInCertificate() {
		return hostNameInCertificate;
	}

	public void setHostNameInCertificate(String hostNameInCertificate) {
		PrefUtils.writePref(myPref, "ssl.hostname.in.certificate", hostNameInCertificate, this.hostNameInCertificate,
				"");
		this.hostNameInCertificate = hostNameInCertificate;
	}

	public String getFieldSeparator() {
		return fieldSeparator;
	}

	public void setFieldSeparator(String fieldSeparator) {
		PrefUtils.writePref(myPref, "field.separator", fieldSeparator, this.fieldSeparator, ",");
		this.fieldSeparator = fieldSeparator;
	}

	public String getTextDelimiter() {
		return textDelimiter;
	}

	public void setTextDelimiter(String textDelimiter) {
		PrefUtils.writePref(myPref, "text.delimiter", textDelimiter, this.textDelimiter, "\"");
		this.textDelimiter = textDelimiter;
	}

	public String getTextFileFormat() {
		return textFileFormat;
	}

	public void setTextFileFormat(String textfileFormat) {
		PrefUtils.writePref(myPref, "textfile.format", textfileFormat, textFileFormat, "");
		textFileFormat = textfileFormat;
	}

	public void cleanupNodes(Profiles pdaSettings) {
		try {
			String[] child = parent.childrenNames();
			for (String element : child) {
				Preferences p = parent.node(element);
				String dbFile = p.get(DB_FILE, "");
				if (dbFile.isEmpty()) {
					PrefUtils.deleteNode(parent, element);
					pdaSettings.removeDatabase(element);
				} else {
					pdaSettings.cleanupDatabase(element);
				}
				parent.flush();
			}
		} catch (Exception e) {
		}
		fillNodes();
	}

	private void fillNodes() {
		nodes.clear();
		try {
			String[] child = parent.childrenNames();
			for (String element : child) {
				Preferences p = parent.node(element);
				String dbFile = p.get(DB_FILE, "") + "/" + p.get(DB_TYPE, "");
				nodes.put(dbFile.toUpperCase(), element);
			}
		} catch (Exception e) {
		}
	}
}
