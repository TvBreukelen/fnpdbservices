package dbconvert.utils;

import application.interfaces.TvBSoftware;
import application.preferences.GeneralSettings;

public class ConvertOldVersion {
	private ConvertOldVersion() {
		// Hide constructor
	}

	public static void convert() {
		GeneralSettings myGeneralSettings = GeneralSettings.getInstance();
		String version = myGeneralSettings.getDbcVersion();

		if (version.equals(TvBSoftware.DBCONVERT.getVersion())) {
			return;
		}

		if (!myGeneralSettings.isNoVersionCheck()) {
			myGeneralSettings.setCheckVersionDate();
		}

		myGeneralSettings.setDbcVersion(TvBSoftware.DBCONVERT.getVersion());
	}
}
