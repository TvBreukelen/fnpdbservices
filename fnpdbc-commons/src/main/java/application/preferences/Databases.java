package application.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import application.interfaces.ExportFile;
import application.interfaces.TvBSoftware;
import application.utils.General;
import dbengine.utils.DatabaseHelper;

public class Databases extends DatabaseHelper {
	private String databaseID = General.EMPTY_STRING;

	// For the import of Text files
	private String fieldSeparator = ",";
	private String textDelimiter = General.TEXT_DELIMITER;
	private String textFileFormat = General.EMPTY_STRING;

	private static final String DB_FILE = "database.file";
	private static final String DB_TYPE = "database.type";

	private TvBSoftware mySoftware;
	private Map<String, String> nodes = new HashMap<>();

	private Preferences myPref;
	private Preferences parent;

	private static EnumMap<TvBSoftware, Databases> mapDB = new EnumMap<>(TvBSoftware.class);

	public static Databases getInstance(TvBSoftware software) {
		return mapDB.computeIfAbsent(software, Databases::new);
	}

	private Databases(TvBSoftware software) {
		super(General.EMPTY_STRING, ExportFile.ACCESS);
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

		database = myPref.get(DB_FILE, General.EMPTY_STRING);
		password = myPref.get("database.password", General.EMPTY_STRING);
		databaseType = myPref.get(DB_TYPE, General.EMPTY_STRING);
		user = myPref.get("database.user", General.EMPTY_STRING);
		databaseVersion = myPref.get("database.version", General.EMPTY_STRING);

		host = myPref.get("remote.host", General.EMPTY_STRING);
		port = myPref.getInt("remote.port", 0);

		// SSH
		useSsh = myPref.getBoolean("use.ssh", false);
		sshHost = myPref.get("ssh.host", General.EMPTY_STRING);
		sshPort = myPref.getInt("ssh.port", 0);
		sshUser = myPref.get("ssh.user", General.EMPTY_STRING);
		sshPassword = myPref.get("ssh.password", General.EMPTY_STRING);
		privateKeyFile = myPref.get("ssh.key.file", General.EMPTY_STRING);

		// SSL
		useSsl = myPref.getBoolean("use.ssl", false);
		sslMode = myPref.get("ssl.mode", General.EMPTY_STRING);
		keyStore = myPref.get("ssl.keystore", General.EMPTY_STRING);
		keyStorePassword = myPref.get("ssl.keystore.password", General.EMPTY_STRING);
		serverSslCert = myPref.get("ssl.server.cert", General.EMPTY_STRING);
		serverSslCaCert = myPref.get("ssl.server.ca.cert", General.EMPTY_STRING);
		trustServerCertificate = myPref.getBoolean("ssl.trust.server.certificate", false);
		hostNameInCertificate = myPref.get("ssl.hostname.in.certificate", General.EMPTY_STRING);

		fieldSeparator = myPref.get("field.separator", ",");
		textDelimiter = myPref.get("text.delimiter", General.TEXT_DELIMITER);
		textFileFormat = myPref.get("textfile.format", General.EMPTY_STRING);
	}

	public void deleteNode(String databaseID) {
		PrefUtils.deleteNode(parent, databaseID);
		fillNodes();
	}

	public String getNodename(String dbFile, ExportFile type) {
		return getNodename(dbFile, type.getName());
	}

	public String getNodename(String dbFile, String type) {
		return nodes.get((dbFile + "/" + type).toUpperCase());
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

	public List<String> getDatabaseFiles(ExportFile myImportFile, boolean mustExist) {
		List<String> result = new ArrayList<>();
		result.add(General.EMPTY_STRING);

		for (String db : getDatabases()) {
			setNode(db);
			String database = getDatabaseName();

			if (getDatabaseType() == myImportFile
					&& (myImportFile.isConnectHost() || mustExist && General.existFile(database))) {
				result.add(database);
			}
		}

		Collections.sort(result);
		return result;
	}

	public String getDatabaseID() {
		return databaseID;
	}

	@Override
	public void setHost(String host) {
		PrefUtils.writePref(myPref, "remote.host", host, this.host, General.EMPTY_STRING);
		this.host = host;
	}

	@Override
	public void setPort(int port) {
		PrefUtils.writePref(myPref, "remote.port", port, this.port, 0);
		this.port = port;
	}

	@Override
	public void setDatabase(String database) {
		database = extractDatabase(database);
		PrefUtils.writePref(myPref, DB_FILE, database, this.database, General.EMPTY_STRING);
		this.database = database;
		fillNodes();
	}

	@Override
	public void setPassword(String databasePassword) {
		PrefUtils.writePref(myPref, "database.password", databasePassword, password, General.EMPTY_STRING);
		password = databasePassword;
	}

	@Override
	public void setUser(String databaseUser) {
		PrefUtils.writePref(myPref, "database.user", databaseUser, user, General.EMPTY_STRING);
		user = databaseUser;
	}

	@Override
	public void setDatabaseType(Object obj) {
		String result = obj instanceof ExportFile dbType ? dbType.getName() : obj.toString();
		PrefUtils.writePref(myPref, DB_TYPE, result, this.databaseType, General.EMPTY_STRING);
		this.databaseType = result;
	}

	@Override
	public void setDatabaseVersion(String databaseVersion) {
		PrefUtils.writePref(myPref, "database.version", databaseVersion, this.databaseVersion, General.EMPTY_STRING);
		this.databaseVersion = databaseVersion;
	}

	@Override
	public void setUseSsl(boolean useSsl) {
		PrefUtils.writePref(myPref, "use.ssl", useSsl, this.useSsl, false);
		this.useSsl = useSsl;
	}

	@Override
	public void setSslMode(String sslMode) {
		PrefUtils.writePref(myPref, "ssl.mode", sslMode, this.sslMode, General.EMPTY_STRING);
		this.sslMode = sslMode;
	}

	@Override
	public void setServerSslCert(String serverSslCert) {
		PrefUtils.writePref(myPref, "ssl.server.cert", serverSslCert, this.serverSslCert, General.EMPTY_STRING);
		this.serverSslCert = serverSslCert;
	}

	@Override
	public void setServerSslCaCert(String serverSslCaCert) {
		PrefUtils.writePref(myPref, "ssl.server.ca.cert", serverSslCaCert, this.serverSslCaCert, General.EMPTY_STRING);
		this.serverSslCaCert = serverSslCaCert;
	}

	@Override
	public void setKeyStore(String keyStore) {
		PrefUtils.writePref(myPref, "ssl.keystore", keyStore, this.keyStore, General.EMPTY_STRING);
		this.keyStore = keyStore;
	}

	@Override
	public void setKeyStorePassword(String keyStorePassword) {
		PrefUtils.writePref(myPref, "ssl.keystore.password", keyStorePassword, this.keyStorePassword,
				General.EMPTY_STRING);
		this.keyStorePassword = keyStorePassword;
	}

	@Override
	public void setUseSsh(boolean useSsh) {
		PrefUtils.writePref(myPref, "use.ssh", useSsh, this.useSsh, false);
		this.useSsh = useSsh;
	}

	@Override
	public void setSshHost(String sshHost) {
		PrefUtils.writePref(myPref, "ssh.host", sshHost, this.sshHost, General.EMPTY_STRING);
		this.sshHost = sshHost;
	}

	@Override
	public void setSshUser(String shellUser) {
		PrefUtils.writePref(myPref, "ssh.user", shellUser, sshUser, General.EMPTY_STRING);
		sshUser = shellUser;
	}

	@Override
	public void setSshPassword(String shellPassword) {
		PrefUtils.writePref(myPref, "ssh.password", shellPassword, sshPassword, General.EMPTY_STRING);
		sshPassword = shellPassword;
	}

	@Override
	public void setPrivateKeyFile(String privateKeyFile) {
		PrefUtils.writePref(myPref, "ssh.key.file", privateKeyFile, this.privateKeyFile, General.EMPTY_STRING);
		this.privateKeyFile = privateKeyFile;
	}

	@Override
	public void setSshPort(int sshPort) {
		PrefUtils.writePref(myPref, "ssh.port", sshPort, this.sshPort, 0);
		this.sshPort = sshPort;
	}

	@Override
	public void setTrustServerCertificate(boolean trustServerCertificate) {
		PrefUtils.writePref(myPref, "ssl.trust.server.certificate", trustServerCertificate, this.trustServerCertificate,
				false);
		this.trustServerCertificate = trustServerCertificate;
	}

	@Override
	public void setHostNameInCertificate(String hostNameInCertificate) {
		PrefUtils.writePref(myPref, "ssl.hostname.in.certificate", hostNameInCertificate, this.hostNameInCertificate,
				General.EMPTY_STRING);
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
		PrefUtils.writePref(myPref, "text.delimiter", textDelimiter, this.textDelimiter, General.TEXT_DELIMITER);
		this.textDelimiter = textDelimiter;
	}

	public String getTextFileFormat() {
		return textFileFormat;
	}

	public void setTextFileFormat(String textfileFormat) {
		PrefUtils.writePref(myPref, "textfile.format", textfileFormat, textFileFormat, General.EMPTY_STRING);
		textFileFormat = textfileFormat;
	}

	public void cleanupNodes(Profiles pdaSettings) {
		try {
			String[] child = parent.childrenNames();
			for (String element : child) {
				Preferences p = parent.node(element);
				String dbFile = p.get(DB_FILE, General.EMPTY_STRING);
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
				String dbFile = p.get(DB_FILE, General.EMPTY_STRING) + "/" + p.get(DB_TYPE, General.EMPTY_STRING);
				nodes.put(dbFile.toUpperCase(), element);
			}
		} catch (Exception e) {
		}
	}
}
