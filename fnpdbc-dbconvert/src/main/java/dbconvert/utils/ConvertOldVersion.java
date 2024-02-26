package dbconvert.utils;

import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.ExportFile;
import application.interfaces.TvBSoftware;
import application.preferences.Databases;
import application.preferences.GeneralSettings;
import application.preferences.PrefUtils;
import application.utils.General;
import dbconvert.preferences.PrefDBConvert;

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
				convertInputFilesToXBase();
			} else if ("7.1".equals(version)) {
				// Extract host, port and database fields from database
				convertMariaDB();
			} else if (vs < 8.0) {
				// Move DBase and FoxPro output files to xBase
				convertOutputFilesToXBase();
			} else if (vs < 8.2) {
				// Move all database files to Database preferences
				mergeInAndExportFiles();
			}
		} else if (TvBSoftware.DBCONVERT.getVersion().equals("7.3")) {
			copyOldSettings();
		}

		if (!settings.isNoVersionCheck()) {
			settings.setCheckVersionDate();
		}

		settings.setDbcVersion(TvBSoftware.DBCONVERT.getVersion());
	}

	private static void convertInputFilesToXBase() {
		Databases dbases = Databases.getInstance(TvBSoftware.DBCONVERT);
		for (String db : dbases.getDatabases()) {
			dbases.setNode(db);
			switch (dbases.getDatabaseTypeAsString()) {
			case "DBase3", "DBase4", "DBase5", "FoxPro":
				dbases.setDatabaseType(ExportFile.DBASE);
				break;
			default:
				continue;
			}
		}
	}

	private static void convertOutputFilesToXBase() {
		PrefDBConvert pref = PrefDBConvert.getInstance();
		moveProfiles(pref, "DBase3");
		moveProfiles(pref, "DBase4");
		moveProfiles(pref, "DBase5");
		moveProfiles(pref, "FoxPro");
	}

	private static void moveProfiles(PrefDBConvert pref, String projectToMove) {
		if (!pref.projectExists(projectToMove)) {
			return;
		}

		final String toProject = ExportFile.DBASE.getName();
		List<String> profiles = pref.getProfiles(projectToMove);
		if (!profiles.isEmpty()) {
			pref.setProject(projectToMove);
			profiles.forEach(profile -> {
				String prof = profile;
				if (pref.profileExists(toProject, prof)) {
					prof += " (" + projectToMove + ")";
				}
				try {
					Preferences copyFrom = pref.getParent().node(profile);
					pref.copyProfile(copyFrom, toProject, prof);
					PrefUtils.deleteNode(pref.getParent(), profile);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
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

			String userHome = System.getProperty("user.home", General.EMPTY_STRING);
			Preferences myPref = Preferences.userRoot().node("fnprog2pda");
			myPref = myPref.node("general_settings");

			settings.setCheckBoxChecked(myPref.get("checkbox.checked", "Yes"));
			settings.setCheckBoxUnchecked(myPref.get("checkbox.unchecked", General.EMPTY_STRING));
			settings.setDateDelimiter(myPref.get("date.delimiter", "/"));
			settings.setDateFormat(myPref.get("date.format", "dd MM yyyy"));
			settings.setDbcVersion(myPref.get("dbconvert.version", General.EMPTY_STRING));
			settings.setDefaultBackupFolder(myPref.get("default.backup.folder", userHome));
			settings.setDefaultFileFolder(myPref.get("default.file.folder", userHome));
			settings.setDefaultImageFolder(myPref.get("default.image.folder", userHome));
			settings.setDefaultPdaFolder(myPref.get("default.pda.folder", userHome));
			settings.setDurationFormat(myPref.get("duration.format", "h:mm:ss"));
			settings.setHandbaseConversionProgram(myPref.get("handbase.conversion.program", General.EMPTY_STRING));
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

	private static void mergeInAndExportFiles() {
		PrefDBConvert pref = PrefDBConvert.getInstance();
		Databases db = pref.getDbSettings();

		pref.getProjects().forEach(project -> {
			List<String> profiles = pref.getProfiles(project);
			if (!profiles.isEmpty()) {
				pref.setProject(project);
				profiles.forEach(profile -> {
					pref.setProfile(profile);
					Preferences child = pref.getChild();
					String dbFile = child.get("export.file", General.EMPTY_STRING);
					String node = db.getNodename(dbFile, project);
					if (node == null) {
						node = db.getNextDatabaseID();
						db.setNode(node);
						db.setDatabase(dbFile);
						db.setDatabaseTypeAsString(project);
						db.setUser(child.get("export.user", General.EMPTY_STRING));
						db.setPassword(child.get("export.password", General.EMPTY_STRING));
					}
					pref.setToDatabase(node);
					child.remove("export.user");
					child.remove("export.password");
				});
			}
		});

	}

}