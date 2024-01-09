package application.model;

import application.preferences.Profiles;
import application.utils.General;

public class SortData {

	private String categoryField = General.EMPTY_STRING;
	private String remainingField = General.EMPTY_STRING;
	private String[] sortField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING,
			General.EMPTY_STRING };
	private String[] groupField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING,
			General.EMPTY_STRING };
	private String[] groupingField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING,
			General.EMPTY_STRING };

	public void loadProfile(Profiles profile) {
		categoryField = profile.getCategoryField();
		remainingField = profile.getRemainingField();
		for (int i = 0; i < 4; i++) {
			sortField[i] = profile.getSortField(i);
			groupField[i] = profile.getGroupField(i);
			groupingField[i] = profile.getGroupingField(i);
		}
	}

	public String getCategoryField() {
		return categoryField;
	}

	public void setCategoryField(String categoryField) {
		this.categoryField = categoryField;
	}

	public String getSortField(int index) {
		return sortField[index];
	}

	public void setSortField(int index, String sortField) {
		this.sortField[index] = sortField;
	}

	public void clearSortFields() {
		for (int i = 0; i < 4; i++) {
			setSortField(i, General.EMPTY_STRING);
			setGroupField(i, General.EMPTY_STRING);
			setGroupingField(i, General.EMPTY_STRING);
		}
	}

	public String getGroupField(int index) {
		return groupField[index];
	}

	public void setGroupField(int index, String groupField) {
		this.groupField[index] = groupField;
	}

	public String getGroupingField(int index) {
		return groupingField[index];
	}

	public void setGroupingField(int index, String groupingField) {
		this.groupingField[index] = groupingField;
	}

	public void saveProfile(Profiles profile) {
		profile.setCategoryField(categoryField);
		profile.setRemainingField(remainingField);
		for (int i = 0; i < 4; i++) {
			profile.setSortField(i, sortField[i]);
			profile.setGroupField(i, groupField[i]);
			profile.setGroupingField(i, groupingField[i]);
		}
	}

	public String getRemainingField() {
		return remainingField;
	}

	public void setRemainingField(String remainingField) {
		this.remainingField = remainingField;
	}
}
