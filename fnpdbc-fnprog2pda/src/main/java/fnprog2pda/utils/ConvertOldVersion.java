package fnprog2pda.utils;

import application.interfaces.TvBSoftware;
import application.preferences.GeneralSettings;

public class ConvertOldVersion {
	private ConvertOldVersion() {
		// Hide constructor
	}

	public static void convert() {
		GeneralSettings myGeneralSettings = GeneralSettings.getInstance();
		String version = myGeneralSettings.getFnpVersion();

		if (version.equals(TvBSoftware.FNPROG2PDA.getVersion())) {
			return;
		}

		if (!myGeneralSettings.isNoVersionCheck()) {
			myGeneralSettings.setCheckVersionDate();
		}

		myGeneralSettings.setFnpVersion(TvBSoftware.FNPROG2PDA.getVersion());
	}
}
