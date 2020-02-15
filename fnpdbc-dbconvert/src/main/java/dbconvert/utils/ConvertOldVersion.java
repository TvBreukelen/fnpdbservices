package dbconvert.utils;

import java.util.Date;
import java.util.List;

import application.interfaces.TvBSoftware;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import dbconvert.preferences.PrefDBConvert;

public class ConvertOldVersion {
	private static Profiles pdaSettings = PrefDBConvert.getInstance();

	public static void convert() throws Exception {
		GeneralSettings myGeneralSettings = GeneralSettings.getInstance();
		String version = myGeneralSettings.getDbcVersion();

		if (version.equals(TvBSoftware.DBCONVERT.getVersion())) {
			return;
		}

		if (!myGeneralSettings.isNoVersionCheck()) {
			myGeneralSettings.setCheckVersionDate();
		}

		convertDateFields();
		myGeneralSettings.setDbcVersion(version);
	}

	private static void convertDateFields() {
		List<String> projects = pdaSettings.getProjects();

		if (projects.isEmpty()) {
			// Nothing to do
			return;
		}

		for (String project : projects) {
			for (String profile : pdaSettings.getProfiles(project)) {
				pdaSettings.setProject(project);
				pdaSettings.setProfile(profile);

				String lastSaved = pdaSettings.getLastSaved();
				if (lastSaved.isEmpty()) {
					// Nothing to do
					continue;
				}

				Date date;
				try {
					date = new Date(Long.valueOf(lastSaved));
				} catch (Exception e) {
					date = new Date();
				}
				pdaSettings.setLastSaved(date);
			}
		}
	}
}
