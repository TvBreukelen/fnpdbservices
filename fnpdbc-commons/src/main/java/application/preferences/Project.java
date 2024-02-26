package application.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import application.interfaces.TvBSoftware;
import application.utils.General;
import dbengine.utils.DatabaseHelper;

public class Project {
	private static Preferences root;
	private Preferences gParent;
	private Preferences parent;

	private TvBSoftware software;
	private Databases dbSettings;

	private String lastProject;
	private String lastProfile;
	private String projectID = General.EMPTY_STRING;

	public Project(TvBSoftware software) {
		this.software = software;
		dbSettings = Databases.getInstance(software);

		root = Preferences.userRoot().node(software.getName().toLowerCase());
		gParent = root.node("projects");

		lastProject = root.get("last.project", General.EMPTY_STRING);
		lastProfile = root.get("last.profile", General.EMPTY_STRING);
		projectID = lastProject;
	}

	public Databases getDbSettings() {
		return dbSettings;
	}

	public TvBSoftware getTvBSoftware() {
		return software;
	}

	public String getProjectID() {
		return projectID;
	}

	public static void backupApplication(String filename) {
		PrefUtils.exportNode(root, filename);
	}

	public static void restoreApplication(String filename) {
		PrefUtils.importNode(filename);
	}

	public List<String> getProjects() {
		List<String> result = new ArrayList<>();
		try {
			for (String project : gParent.childrenNames()) {
				if (getProfiles(project).isEmpty()) {
					removeProject(project);
					continue;
				}
				result.add(project);
			}
		} catch (Exception e) {
			// Should not occur
		}
		return result;
	}

	public String getInitialProfile() {
		return lastProfile;
	}

	public String getInitialProject() {
		return lastProject;
	}

	public Preferences getParent() {
		return parent;
	}

	public void setLastProject() {
		PrefUtils.writePref(root, "last.project", projectID, lastProject, General.EMPTY_STRING);
		lastProject = projectID;
	}

	public void setLastProfile(String profile) {
		PrefUtils.writePref(root, "last.profile", profile, lastProfile, General.EMPTY_STRING);
		lastProfile = profile;
	}

	public void setProject(String project) {
		if (project.isEmpty()) {
			return;
		}

		parent = gParent.node(project);
		projectID = project;
	}

	public DatabaseHelper getDatabase(String dbFile) {
		dbSettings.setNode(dbFile);
		return new DatabaseHelper(dbSettings);
	}

	public String setDatabase(DatabaseHelper helper) {
		String node = dbSettings.getNodename(helper.getDatabase(), helper.getDatabaseType());
		if (node == null) {
			node = dbSettings.getNextDatabaseID();
		}
		dbSettings.setNode(node);
		dbSettings.update(helper);
		return node;
	}

	public boolean projectExists(String project) {
		try {
			return gParent.nodeExists(project);
		} catch (BackingStoreException e) {
			return false;
		}
	}

	public boolean profileExists(String project, String profile) {
		try {
			if (projectExists(project)) {
				Preferences p = gParent.node(project);
				return p.nodeExists(profile);
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public void deleteNode(String project, String profile) {
		if (profileExists(project, profile)) {
			PrefUtils.deleteNode(gParent.node(project), profile);
		}
	}

	public void copyProfile(Preferences child, String project, String profile) throws Exception {
		Preferences p = gParent.node(project);
		PrefUtils.copyNode(child, p, profile);
	}

	public void removeDatabase(String database) {
		try {
			for (String project : gParent.childrenNames()) {
				Preferences p1 = gParent.node(project);
				for (String profile : p1.childrenNames()) {
					Preferences p2 = p1.node(profile);
					String db1 = p2.get(Profiles.FROM_DATABASE, General.EMPTY_STRING);
					String db2 = p2.get(Profiles.TO_DATABASE, General.EMPTY_STRING);
					if (db1.isEmpty() || db1.equals(database) || db2.isEmpty() || db2.equals(database)) {
						p2.removeNode();
					}
				}

				if (p1.childrenNames().length == 0) {
					p1.removeNode();
				}
			}
			gParent.flush();
		} catch (Exception e) {
			// Should not occur
		}
	}

	public void cleanupDatabase(String database) {
		try {
			for (String project : gParent.childrenNames()) {
				Preferences p1 = gParent.node(project);
				for (String profile : p1.childrenNames()) {
					Preferences p2 = p1.node(profile);
					String fromDatabase = p2.get(Profiles.FROM_DATABASE, General.EMPTY_STRING);
					String toDatabase = p2.get(Profiles.TO_DATABASE, General.EMPTY_STRING);
					if (fromDatabase.equals(database) || toDatabase.equals(database)) {
						// Database is still in use
						return;
					}
				}
			}

			// Remove Database setting since it is no longer used
			dbSettings.deleteNode(database);
		} catch (Exception e) {
			// Should not occur
		}
	}

	public void removeProject(String project) {
		try {
			if (gParent.nodeExists(project)) {
				parent = gParent.node(project);
				parent.removeNode();
				gParent.flush();
			}
		} catch (Exception e) {
			// Should not occur
		}

		if (project.equals(projectID)) {
			projectID = General.EMPTY_STRING;
			if (project.equals(lastProject)) {
				lastProject = General.EMPTY_STRING;
				lastProfile = General.EMPTY_STRING;
			}
		}
	}

	public List<String> getProfiles(String projectID) {
		List<String> result = new ArrayList<>();
		try {
			Preferences p1 = gParent.node(projectID);
			String[] profiles = p1.childrenNames();
			for (String profile : profiles) {
				Preferences p2 = p1.node(profile);
				if (p2.get(Profiles.FROM_DATABASE, General.EMPTY_STRING).isEmpty()) {
					PrefUtils.deleteNode(p1, profile);
					if (profile.equals(lastProfile)) {
						lastProfile = General.EMPTY_STRING;
					}

					continue;
				}
				result.add(profile);

			}
		} catch (Exception e) {
			// Should not occur
		}
		return result;
	}
}
