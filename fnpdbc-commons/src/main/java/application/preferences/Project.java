package application.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import application.interfaces.TvBSoftware;

public class Project {
	private static Preferences root;
	private Preferences gParent;
	private Preferences parent;

	private Databases _dbSettings;
	private TvBSoftware _software;

	private String lastProject = "";
	private String lastProfile = "";
	private String projectID = "";

	public Project(TvBSoftware software, Databases dbSettings) {
		_software = software;
		_dbSettings = dbSettings;

		root = Preferences.userRoot().node(software.getName().toLowerCase());
		gParent = root.node("projects");

		lastProject = root.get("last.project", "");
		lastProfile = root.get("last.profile", "");
		projectID = lastProject;
	}

	public Databases getDbSettings() {
		return _dbSettings;
	}

	public TvBSoftware getTvBSoftware() {
		return _software;
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
		ArrayList<String> result = new ArrayList<>();
		try {
			for (String project : gParent.childrenNames()) {
				if (getProfiles(project).isEmpty()) {
					removeProject(project);
					continue;
				}
				result.add(project);
			}
		} catch (Exception e) {
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
		PrefUtils.writePref(root, "last.project", projectID, lastProject, "");
		lastProject = projectID;
	}

	public void setLastProfile(String profile) {
		PrefUtils.writePref(root, "last.profile", profile, lastProfile, "");
		lastProfile = profile;
	}

	public void setProject(String project) {
		if (project.isEmpty()) {
			return;
		}

		parent = gParent.node(project);
		projectID = project;
	}

	public boolean profileExists(String project, String profile) {
		try {
			if (!gParent.nodeExists(project)) {
				return false;
			}

			Preferences parent = gParent.node(project);
			return parent.nodeExists(profile);
		} catch (Exception e) {
		}
		return false;
	}

	public void deleteNode(String project, String profile) {
		if (profileExists(project, profile)) {
			PrefUtils.deleteNode(gParent.node(project), profile);
		}
	}

	protected void copyProfile(Preferences child, String project, String profile) throws Exception {
		Preferences parent = gParent.node(project);
		PrefUtils.copyNode(child, parent, profile, true);
	}

	public void removeDatabase(String database) {
		try {
			for (String project : gParent.childrenNames()) {
				Preferences p1 = gParent.node(project);
				for (String profile : p1.childrenNames()) {
					Preferences p2 = p1.node(profile);
					String db = p2.get("database.from.file", "");
					if (db.isEmpty() || db.equals(database)) {
						p2.removeNode();
					}
				}

				if (p1.childrenNames().length == 0) {
					p1.removeNode();
				}
			}
			gParent.flush();
		} catch (Exception e) {
		}
	}

	public void cleanupDatabase(String database) {
		try {
			for (String project : gParent.childrenNames()) {
				Preferences p1 = gParent.node(project);
				for (String profile : p1.childrenNames()) {
					Preferences p2 = p1.node(profile);
					if (p2.get("database.from.file", "").equals(database)) {
						// Database is still in use
						return;
					}
				}
			}

			// Remove Database setting since it is no longer used
			getDbSettings().deleteNode(database);
		} catch (Exception e) {
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
		}

		if (projectID.equals(projectID)) {
			lastProject = "";
			lastProfile = "";
			projectID = "";
		}
	}

	public List<String> getProfiles(String projectID) {
		List<String> result = new ArrayList<>();
		try {
			Preferences p1 = gParent.node(projectID);
			String[] profiles = p1.childrenNames();
			for (String profile : profiles) {
				Preferences p2 = p1.node(profile);
				if (p2.get("database.from.file", "").isEmpty()) {
					PrefUtils.deleteNode(p1, profile);
					if (profile.equals(lastProfile)) {
						lastProfile = "";
					}

					continue;
				}
				result.add(profile);

			}
		} catch (Exception e) {
		}
		return result;
	}
}
