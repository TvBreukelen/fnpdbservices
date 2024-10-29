package fnprog2pda.utils;

import java.util.List;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.ExportFile;
import application.interfaces.TvBSoftware;
import application.preferences.Databases;
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

		if (StringUtils.isNotEmpty(version)) {
			if (General.compareVersions("10.0", version) > 0) {
				// Move DBase and FoxPro output files to xBase
				convertOutputFilesToXBase();
			}

			if (General.compareVersions("10.5", version) > 0) {
				// Move DBase and FoxPro output files to xBase
				mergeInAndExportFiles();
			}

			if (General.compareVersions("10.8", version) > 0) {
				renameArtistView();
			}
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

	private static void mergeInAndExportFiles() {
		PrefFNProg pref = PrefFNProg.getInstance();
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
						db.setDatabaseType(project);
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

	private static void renameArtistView() {
		PrefFNProg pref = PrefFNProg.getInstance();
		pref.getProjects().forEach(project -> {
			List<String> profiles = pref.getProfiles(project);
			if (!profiles.isEmpty()) {
				pref.setProject(project);
				profiles.forEach(profile -> {
					pref.setProfile(profile);
					if (pref.getTableName().equals("Artist")) {
						pref.setTableName("ArtistPerson", true);
					}
				});
			}
		});

	}

}
