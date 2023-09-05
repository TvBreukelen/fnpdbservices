package fnprog2pda.utils;

import java.util.List;
import java.util.prefs.Preferences;

import application.interfaces.ExportFile;
import application.interfaces.TvBSoftware;
import application.preferences.GeneralSettings;
import application.preferences.PrefUtils;
import application.utils.General;
import fnprog2pda.preferences.PrefFNProg;

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

		if (!version.isEmpty() && General.compareVersions("10.0", version) > 0) {
			// Move DBase and FoxPro output files to xBase
			convertOutputFilesToXBase();
		}

		if (!myGeneralSettings.isNoVersionCheck()) {
			myGeneralSettings.setCheckVersionDate();
		}

		myGeneralSettings.setFnpVersion(TvBSoftware.FNPROG2PDA.getVersion());
	}

	private static void convertOutputFilesToXBase() {
		PrefFNProg pref = PrefFNProg.getInstance();
		moveProfiles(pref, "DBase3");
		moveProfiles(pref, "DBase4");
		moveProfiles(pref, "DBase5");
		moveProfiles(pref, "FoxPro");
	}

	private static void moveProfiles(PrefFNProg pref, String projectToMove) {
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

}
