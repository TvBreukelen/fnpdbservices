package dbconvert.utils;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.ExportFile;
import application.interfaces.TvBSoftware;
import application.preferences.Databases;
import application.preferences.GeneralSettings;

public class ConvertOldVersion {
	static GeneralSettings settings = GeneralSettings.getInstance();

	private ConvertOldVersion() {
		// Hide constructor
	}

	public static void convert() {
		String version = settings.getDbcVersion();

		if (version.equals(TvBSoftware.DBCONVERT.getVersion())) {
			return;
		}

		if (StringUtils.isNotEmpty(version)) {
			double vs = Double.parseDouble(version.substring(0, 3));
			if (vs <= 6.8) {
				// Rename DBase and FoxPro input files to xBase
				convertToXBase();
			} else if ("7.1".equals(version)) {
				// Extract host, port and database fields from database
				convertMariaDB();
			}
		} else if (TvBSoftware.DBCONVERT.getVersion().equals("7.3")) {
			copyOldSettings();
		}

		if (!settings.isNoVersionCheck()) {
			settings.setCheckVersionDate();
		}

		settings.setDbcVersion(TvBSoftware.DBCONVERT.getVersion());
	}

	private static void convertToXBase() {
		Databases dbases = Databases.getInstance(TvBSoftware.DBCONVERT);
		for (String db : dbases.getDatabases()) {
			dbases.setNode(db);
			switch (dbases.getDatabaseType()) {
			case DBASE3:
			case DBASE4:
			case DBASE5:
			case FOXPRO:
				dbases.setDatabaseType(ExportFile.DBASE);
				break;
			default:
				continue;
			}
		}
	}

	private static void convertMariaDB() {
		Databases dbases = Databases.getInstance(TvBSoftware.DBCONVERT);
		for (String db : dbases.getDatabases()) {
			dbases.setNode(db);
			if (dbases.getDatabaseType() == ExportFile.MARIADB) {
				String database = dbases.getDatabase();
				int index = database.indexOf(":");
				dbases.setHost(database.substring(0, index++));
				String portNo = database.substring(index, database.indexOf("/"));
				dbases.setPort(Integer.valueOf(portNo));
				index += portNo.length() + 1;
				dbases.setDatabase(database.substring(index));
			}
		}
	}

	private static void copyOldSettings() {
		try {
			if (!Preferences.userRoot().nodeExists("fnprog2pda")) {
				return;
			}

			String userHome = System.getProperty("user.home", "");
			Preferences myPref = Preferences.userRoot().node("fnprog2pda");
			myPref = myPref.node("general_settings");

			settings.setCheckBoxChecked(myPref.get("checkbox.checked", "Yes"));
			settings.setCheckBoxUnchecked(myPref.get("checkbox.unchecked", ""));
			settings.setDateDelimiter(myPref.get("date.delimiter", "/"));
			settings.setDateFormat(myPref.get("date.format", "dd MM yyyy"));
			settings.setDbcVersion(myPref.get("dbconvert.version", ""));
			settings.setDefaultBackupFolder(myPref.get("default.backup.folder", userHome));
			settings.setDefaultFileFolder(myPref.get("default.file.folder", userHome));
			settings.setDefaultImageFolder(myPref.get("default.image.folder", userHome));
			settings.setDefaultPdaFolder(myPref.get("default.pda.folder", userHome));
			settings.setDurationFormat(myPref.get("duration.format", "h:mm:ss"));
			settings.setHandbaseConversionProgram(myPref.get("handbase.conversion.program", ""));
			settings.setLanguage(myPref.get("language", "English"));
			settings.setLookAndFeel(myPref.get("gui.lookandfeel", "Nimbus"));
			settings.setTimeFormat(myPref.get("time.format", "hh:mm"));
			settings.setHeight(myPref.getInt("frame.height", 500));
			settings.setWidth(myPref.getInt("frame.width", 850));
			settings.setVersionDaysCheck(myPref.getInt("version.days.check", 30));
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
}