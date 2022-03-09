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
		}

		settings.setDbcVersion(TvBSoftware.DBCONVERT.getVersion());
	}

	private static void convertToXBase() {
		Databases dbases = Databases.getInstance(TvBSoftware.DBCONVERT);
		String[] bases = dbases.getDatabases();
		for (String db : bases) {
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
}