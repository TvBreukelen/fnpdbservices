package application.preferences;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseHelper;
import application.interfaces.TvBSoftware;

public class Databases implements IDatabaseHelper {
	private String databaseID = "";
	private String databaseFile = "";
	private String databasePassword = "";
	private String databaseUser = "";
	private String databaseType;
	private String databaseVersion = "";

	// Remote databases
	private boolean useSsl = false;
	private String serverSslCert = "";
	private String serverSslCaCert = "";
	private String keyStore = "";
	private String keyStorePassword = "";
	private String sslMode = "";

	// For the import of Text files
	private String fieldSeparator = ",";
	private String textDelimiter = "\"";
	private String textFileFormat = "";

	private static final String DB_FILE = "database.file";

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

		databaseFile = myPref.get(DB_FILE, "");
		databasePassword = myPref.get("database.password", "");
		databaseType = myPref.get("database.type", "");
		databaseUser = myPref.get("database.user", "");
		databaseVersion = myPref.get("database.version", "");

		serverSslCert = myPref.get("ssl.server.cert", "");
		serverSslCaCert = myPref.get("ssl.server.ca.cert", "");
		keyStore = myPref.get("ssl.keystore", "");
		keyStorePassword = myPref.get("ssl.keystore.password", "");
		sslMode = myPref.get("ssl.mode", "");
		useSsl = myPref.getBoolean("use.ssl", false);

		fieldSeparator = myPref.get("field.separator", ",");
		textDelimiter = myPref.get("text.delimiter", "\"");
		textFileFormat = myPref.get("textfile.format", "");
	}

	public void deleteNode(String databaseID) {
		PrefUtils.deleteNode(parent, databaseID);
		fillNodes();
	}

	public String getNodename(String databaseFile) {
		return nodes.get(databaseFile.toUpperCase());
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

	public String getDatabaseID() {
		return databaseID;
	}

	@Override
	public String getDatabase() {
		return databaseFile;
	}

	@Override
	public void setDatabase(String databaseFile) {
		PrefUtils.writePref(myPref, DB_FILE, databaseFile, this.databaseFile, "");
		this.databaseFile = databaseFile;
		fillNodes();
	}

	@Override
	public String getPassword() {
		return databasePassword;
	}

	@Override
	public void setPassword(String databasePassword) {
		PrefUtils.writePref(myPref, "database.password", databasePassword, this.databasePassword, "");
		this.databasePassword = databasePassword;
	}

	@Override
	public String getUser() {
		return databaseUser;
	}

	@Override
	public void setUser(String databaseUser) {
		PrefUtils.writePref(myPref, "database.user", databaseUser, this.databaseUser, "");
		this.databaseUser = databaseUser;
	}

	@Override
	public ExportFile getDatabaseType() {
		return ExportFile.getExportFile(databaseType);
	}

	@Override
	public void setDatabaseType(ExportFile databaseType) {
		PrefUtils.writePref(myPref, "database.type", databaseType.getName(), this.databaseType, "");
		this.databaseType = databaseType.getName();
	}

	public String getDatabaseTypeAsString() {
		return databaseType;
	}

	public void setDatabaseTypeAsString(String databaseType) {
		PrefUtils.writePref(myPref, "database.type", databaseType, this.databaseType, "");
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

	@Override
	public void setUseSsl(boolean useSsl) {
		PrefUtils.writePref(myPref, "use.ssl", useSsl, this.useSsl, false);
		this.useSsl = useSsl;
	}

	@Override
	public String getSslMode() {
		return sslMode;
	}

	@Override
	public void setSslMode(String sslMode) {
		PrefUtils.writePref(myPref, "ssl.mode", sslMode, this.sslMode, "");
		this.sslMode = sslMode;
	}

	@Override
	public String getServerSslCert() {
		return serverSslCert;
	}

	@Override
	public void setServerSslCert(String serverSslCert) {
		PrefUtils.writePref(myPref, "ssl.server.cert", serverSslCert, this.serverSslCert, "");
		this.serverSslCert = serverSslCert;
	}

	@Override
	public String getServerSslCaCert() {
		return serverSslCaCert;
	}

	@Override
	public void setServerSslCaCert(String serverSslCaCert) {
		PrefUtils.writePref(myPref, "ssl.server.ca.cert", serverSslCaCert, this.serverSslCaCert, "");
		this.serverSslCaCert = serverSslCaCert;
	}

	@Override
	public String getKeyStore() {
		return keyStore;
	}

	@Override
	public void setKeyStore(String keyStore) {
		PrefUtils.writePref(myPref, "ssl.keystore", keyStore, this.keyStore, "");
		this.keyStore = keyStore;
	}

	@Override
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	@Override
	public void setKeyStorePassword(String keyStorePassword) {
		PrefUtils.writePref(myPref, "ssl.keystore.password", keyStorePassword, this.keyStorePassword, "");
		this.keyStorePassword = keyStorePassword;
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
				final String dbFile = p.get(DB_FILE, "");
				nodes.put(dbFile.toUpperCase(), element);
			}
		} catch (Exception e) {
		}
	}
}
