package fnprog2pda.model;

import fnprog2pda.preferences.PrefFNProg;

public class MiscellaneousData {
	private boolean useContentsIndex = false;
	private boolean useContentsItemTitle = false;
	private boolean useContentsLength = false;
	private boolean useContentsOrigTitle = false;
	private boolean useContentsPerson = false;
	private boolean useContentsSide = false;
	private boolean useOriginalTitle = false;
	private boolean useRoles = false;
	private String tableName;
	private String profileID;

	public void loadProfile(PrefFNProg profile) {
		useContentsIndex = profile.isUseContentsIndex();
		useContentsItemTitle = profile.isUseContentsItemTitle();
		useContentsLength = profile.isUseContentsLength();
		useContentsOrigTitle = profile.isUseContentsOrigTitle();
		useContentsPerson = profile.isUseContentsPerson();
		useOriginalTitle = profile.isUseOriginalTitle();
		useRoles = profile.isUseRoles();
		tableName = profile.getTableName();
		profileID = profile.getProfileID();
	}
	
	public void saveProfile(PrefFNProg profile) {
		profile.setUseContentsIndex(useContentsIndex);
		profile.setUseContentsItemTitle(useContentsItemTitle);
		profile.setUseContentsLength(useContentsLength);
		profile.setUseContentsOrigTitle(useContentsOrigTitle);
		profile.setUseContentsPerson(useContentsPerson);
		profile.setUseOriginalTitle(useOriginalTitle);
		profile.setUseRoles(useRoles);
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getProfileID() {
		return profileID;
	}

	public void setProfileID(String profileID) {
		this.profileID = profileID;
	}

	public boolean isUseContentsIndex() {
		return useContentsIndex;
	}
	
	public void setUseContentsIndex(boolean useContentsIndex) {
		this.useContentsIndex = useContentsIndex;
	}
	
	public boolean isUseContentsItemTitle() {
		return useContentsItemTitle;
	}
	
	public void setUseContentsItemTitle(boolean useContentsItemTitle) {
		this.useContentsItemTitle = useContentsItemTitle;
	}
	
	public boolean isUseContentsLength() {
		return useContentsLength;
	}
	
	public void setUseContentsLength(boolean useContentsLength) {
		this.useContentsLength = useContentsLength;
	}
	
	public boolean isUseContentsOrigTitle() {
		return useContentsOrigTitle;
	}
	
	public void setUseContentsOrigTitle(boolean useContentsOrigTitle) {
		this.useContentsOrigTitle = useContentsOrigTitle;
	}
	
	public boolean isUseContentsPerson() {
		return useContentsPerson;
	}
	
	public void setUseContentsPerson(boolean useContentsPerson) {
		this.useContentsPerson = useContentsPerson;
	}
	
	public boolean isUseContentsSide() {
		return useContentsSide;
	}
	
	public void setUseContentsSide(boolean useContentsSide) {
		this.useContentsSide = useContentsSide;
	}
	
	public boolean isUseOriginalTitle() {
		return useOriginalTitle;
	}
	
	public void setUseOriginalTitle(boolean useOriginalTitle) {
		this.useOriginalTitle = useOriginalTitle;
	}
	
	public boolean isUseRoles() {
		return useRoles;
	}
	
	public void setUseRoles(boolean useRoles) {
		this.useRoles = useRoles;
	}
}
