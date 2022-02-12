package application.preferences;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import application.interfaces.TvBSoftware;

public class Databases {
	private String databaseID = "";
	private String databaseFile = "";
	private String databaseHost = "";
	private String databasePassword = "";
	private String databaseUser = "";
	private String databaseType = "";
	private String databaseVersion = "";
	private int databasePort = 0;

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
		databaseHost = myPref.get("database.host", "");
		databasePort = myPref.getInt("database.port", 0);
		databaseType = myPref.get("database.type", "");
		databaseUser = myPref.get("database.user", "");
		databaseVersion = myPref.get("database.version", "");

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

	public String getDatabaseHost() {
		return databaseHost;
	}

	public void setDatabaseHost(String databaseHost) {
		PrefUtils.writePref(myPref, "database.host", databaseHost, this.databaseHost, "");
		this.databaseHost = databaseHost;
	}

	public int getDatabasePort() {
		return databasePort;
	}

	public void setDatabasePort(int databasePort) {
		PrefUtils.writePref(myPref, "database.port", databasePort, this.databasePort, 0);
		this.databasePort = databasePort;
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
