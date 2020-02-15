package application.model;

import application.preferences.Profiles;

public class SortData {

	private boolean forceSort = false;
	private String categoryField = "";
	private String[] sortField = new String[] { "", "", "", "" };

	public void loadProfile(Profiles profile) {
		forceSort = profile.isForceSort();
		categoryField = profile.getCategoryField();
		sortField[0] = profile.getSortField(0);
		sortField[1] = profile.getSortField(1);
		sortField[2] = profile.getSortField(2);
		sortField[3] = profile.getSortField(3);
	}

	public boolean isForceSort() {
		return forceSort;
	}

	public void setForceSort(boolean forceSort) {
		this.forceSort = forceSort;
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
			setSortField(i, "");
		}
	}

	public void saveProfile(Profiles profile) {
		profile.setForceSort(forceSort);
		profile.setCategoryField(categoryField);
		profile.setSortField(0, sortField[0]);
		profile.setSortField(1, sortField[1]);
		profile.setSortField(2, sortField[2]);
		profile.setSortField(3, sortField[3]);
	}
}
