package application.model;

import application.interfaces.FilterOperator;
import application.interfaces.TvBSoftware;
import application.preferences.Profiles;
import application.utils.General;

public class FilterData {
	private String categoryField = General.EMPTY_STRING;
	private String contentsFilter = General.EMPTY_STRING;
	private String keywordFilter = General.EMPTY_STRING;
	private String filterCondition = "AND";

	private String[] filterField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING };
	private FilterOperator[] filterOperator = new FilterOperator[] { FilterOperator.IS_EQUAL_TO,
			FilterOperator.IS_EQUAL_TO };
	private String[] filterValue = new String[] { General.EMPTY_STRING, General.EMPTY_STRING };

	private String profileID;
	private TvBSoftware tvbSoftware;

	public void loadProfile(Profiles profile) {
		categoryField = profile.getCategoryField();
		contentsFilter = profile.getContentsFilter();
		filterCondition = profile.getFilterCondition();
		keywordFilter = profile.getKeywordFilter();
		filterField[0] = profile.getFilterField(0);
		filterField[1] = profile.getFilterField(1);
		filterOperator[0] = profile.getFilterOperator(0);
		filterOperator[1] = profile.getFilterOperator(1);
		filterValue[0] = profile.getFilterValue(0);
		filterValue[1] = profile.getFilterValue(1);
		profileID = profile.getProfileID();
		tvbSoftware = profile.getTvBSoftware();
	}

	public void saveProfile(Profiles profile) {
		profile.setCategoryField(categoryField);
		profile.setContentsFilter(contentsFilter);
		profile.setFilterCondition(filterCondition);
		profile.setKeywordFilter(keywordFilter);
		profile.setFilterField(0, filterField[0]);
		profile.setFilterField(1, filterField[1]);
		profile.setFilterOperator(0, filterOperator[0]);
		profile.setFilterOperator(1, filterOperator[1]);
		profile.setFilterValue(0, filterValue[0]);
		profile.setFilterValue(1, filterValue[1]);
	}

	public TvBSoftware getTvBSoftware() {
		return tvbSoftware;
	}

	public void setTvBSoftware(TvBSoftware tvbSoftware) {
		this.tvbSoftware = tvbSoftware;
	}

	public String getCategoryField() {
		return categoryField;
	}

	public void setCategoryField(String categoryField) {
		this.categoryField = categoryField;
	}

	public String getContentsFilter() {
		return contentsFilter;
	}

	public void setContentsFilter(String contentsFilter) {
		this.contentsFilter = contentsFilter;
	}

	public String getFilterCondition() {
		return filterCondition;
	}

	public void setFilterCondition(String filterCondition) {
		this.filterCondition = filterCondition;
	}

	public String getKeywordFilter() {
		return keywordFilter;
	}

	public void setKeywordFilter(String keywordFilter) {
		this.keywordFilter = keywordFilter;
	}

	public String getFilterField(int index) {
		return filterField[index];
	}

	public void setFilterField(int index, String filterField) {
		this.filterField[index] = filterField;
	}

	public FilterOperator getFilterOperator(int index) {
		return filterOperator[index];
	}

	public void setFilterOperator(int index, FilterOperator filterOperator) {
		this.filterOperator[index] = filterOperator;
	}

	public String getFilterValue(int index) {
		return filterValue[index];
	}

	public void setFilterValue(int index, String filterValue) {
		this.filterValue[index] = filterValue;
	}

	public String getProfileID() {
		return profileID;
	}

	public void setProfileID(String profileID) {
		this.profileID = profileID;
	}

	public void clearFilterFields() {
		for (int i = 0; i < 2; i++) {
			setFilterField(i, General.EMPTY_STRING);
			setFilterOperator(i, FilterOperator.IS_EQUAL_TO);
			setFilterValue(i, General.EMPTY_STRING);
		}

		setFilterCondition("AND");
		setContentsFilter(General.EMPTY_STRING);
		setKeywordFilter(General.EMPTY_STRING);
	}
}