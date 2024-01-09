package application.model;

import application.preferences.Profiles;
import application.utils.General;

public class ProfileObject {
	private String projectID = General.EMPTY_STRING;
	private String profileID = General.EMPTY_STRING;
	private String exportFile = General.EMPTY_STRING;
	private String importFile = General.EMPTY_STRING;
	private String importFileProgram = General.EMPTY_STRING;
	private String notes = General.EMPTY_STRING;
	private String tableName = General.EMPTY_STRING;
	private String lastExported = General.EMPTY_STRING;
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
