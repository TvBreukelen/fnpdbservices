package application.preferences;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import application.interfaces.TvBSoftware;

public class Databases {
	private String databaseID = "";
	private String databaseFile = "";
	private String databasePassword = "";
	private String databaseUser = "";
	private String databaseType = "";
	private String databaseVersion = "";

	// Remote databases
	private String sslPrivateKey = "";
	private String sslCACertificate = "";
	private String sslCertificate = "";
	private String sslCipher = "";
	private boolean useSsl = false;

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

		sslPrivateKey = myPref.get("ssl.private.key", "");
		sslCACertificate = myPref.get("ssl.ca.certificate", "");
		sslCertificate = myPref.get("ssl.certificate", "");
		sslCipher = myPref.get("ssl.cipher", "");
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

	public String getDatabaseFile() {
		return databaseFile;
	}

	public void setDatabaseFile(String databaseFile) {
		PrefUtils.writePref(myPref, DB_FILE, databaseFile, this.databaseFile, "");
		this.databaseFile = databaseFile;
		fillNodes();
	}

	public String getDatabasePassword() {
		return databasePassword;
	}

	public void setDatabasePassword(String databasePassword) {
		PrefUtils.writePref(myPref, "database.password", databasePassword, this.databasePassword, "");
		this.databasePassword = databasePassword;
	}

	public String getDatabaseUser() {
		return databaseUser;
	}

	public void setDatabaseUser(String databaseUser) {
		PrefUtils.writePref(myPref, "database.user", databaseUser, this.databaseUser, "");
		this.databaseUser = databaseUser;
	}

	public String getDatabaseType() {
		return databaseType;
	}

	public void setDatabaseType(String databaseType) {
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

	public String getSslPrivateKey() {
		return sslPrivateKey;
	}

	public void setSslPrivateKey(String sslPrivateKey) {
		PrefUtils.writePref(myPref, "ssl.private.key", sslPrivateKey, this.sslPrivateKey, "");
		this.sslPrivateKey = sslPrivateKey;
	}

	public String getSslCACertificate() {
		return sslCACertificate;
	}

	public void setSslCACertificate(String sslCACertificate) {
		PrefUtils.writePref(myPref, "ssl.ca.certificate", sslCACertificate, this.sslCACertificate, "");
		this.sslCACertificate = sslCACertificate;
	}

	public String getSslCertificate() {
		return sslCertificate;
	}

	public void setSslCertificate(String sslCertificate) {
		PrefUtils.writePref(myPref, "ssl.certificate", sslCertificate, this.sslCertificate, "");
		this.sslCertificate = sslCertificate;
	}

	public String getSslCipher() {
		return sslCipher;
	}

	public void setSslCipher(String sslCipher) {
		PrefUtils.writePref(myPref, "ssl.cipher", sslCipher, this.sslCipher, "");
		this.sslCipher = sslCipher;
	}

	public boolean isUseSsl() {
		return useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		PrefUtils.writePref(myPref, "use.ssl", useSsl, this.useSsl, false);
		this.useSsl = useSsl;
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
