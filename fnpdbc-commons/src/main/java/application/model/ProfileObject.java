package application.model;

import application.preferences.Profiles;

public class ProfileObject {
	private String projectID = "";
	private String profileID = "";
	private String exportFile = "";
	private String importFile = "";
	private String importFileProgram = "";
	private String notes = "";
	private String tableName = "";
	private String lastExported = "";
	private Profiles profiles;

	public ProfileObject(String projectID, String profileID, Profiles profiles) {
		this.projectID = projectID;
		this.profileID = profileID;
		this.profiles = profiles;

		if (!profileID.isEmpty()) {
			alignTable();
		}
	}

	public void setProjectID(String projectID) {
		this.projectID = projectID;
	}

	public void setProfileID(String profileID) {
		this.profileID = profileID;
	}

	public String getProjectID() {
		return projectID;
	}

	public String getProfileID() {
		return profileID;
	}

	public String getImportFileProgram() {
		return importFileProgram;
	}

	public String getImportFile() {
		return importFile;
	}

	public String getLastExported() {
		return lastExported;
	}

	public String getNotes() {
		return notes;
	}

	public String getTableName() {
		return tableName;
	}

	public String getExportFile() {
		return exportFile;
	}

	public Profiles getProfiles() {
		return profiles;
	}

	public void alignProfiles() {
		profiles.setProject(projectID);
		profiles.setProfile(profileID);
	}

	public void alignTable() {
		exportFile = profiles.getExportFile();
		importFile = profiles.getImportFile();
		importFileProgram = profiles.getImportFileProgram();
		notes = profiles.getNotes();
		tableName = profiles.getTableName();
		lastExported = profiles.getLastExported();
	}
}
