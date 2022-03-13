package dbconvert.utils;

import application.interfaces.ExportFile;
import application.interfaces.TvBSoftware;
import application.preferences.Databases;
import application.preferences.GeneralSettings;

public class ConvertOldVersion {
	private ConvertOldVersion() {
		// Hide constructor
	}

	public static void convert() {
		GeneralSettings settings = GeneralSettings.getInstance();
		String version = settings.getDbcVersion();

		if (version.equals(TvBSoftware.DBCONVERT.getVersion())) {
			return;
		}

		if (!settings.isNoVersionCheck()) {
			settings.setCheckVersionDate();
		}

		double vs = Double.parseDouble(version.substring(0, 3));
		if (vs <= 6.8) {
			// Rename DBase and FoxPro input files to xBase
			convertToXBase();
		} else if ("7.1".equals(version)) {
			// Extract host, port and database fields from database
			convertMariaDB();
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

}