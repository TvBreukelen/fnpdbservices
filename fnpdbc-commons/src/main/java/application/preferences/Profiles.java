package application.preferences;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import application.interfaces.ExportFile;
import application.interfaces.FilterOperator;
import application.interfaces.TvBSoftware;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.General;

public abstract class Profiles extends Project {
	// General Settings
	private String categoryField = "";
	private String contentsFilter = "";
	private String databaseFromFile;
	private String exportFile = "";
	private String exportUser;
	private String exportPassword;
	private String filterCondition;
	private String keywordFilter;
	private String lastModified;
	private String lastSaved;
	private String notes;
	private String pdaDatabaseName = "";
	private String remainingField = "";
	private String tableName = "";
	private int languageDriver = 3; // xBase

	private String[] filterField = new String[] { "", "" };
	private FilterOperator[] filterOperator = new FilterOperator[2];
	private String[] filterValue = new String[] { "", "" };
	private String[] sortField = new String[] { "", "", "", "" };
	private String[] groupField = new String[] { "", "", "", "" };
	private String[] groupingField = new String[] { "", "", "", "" };
	private List<String> relations = new ArrayList<>();

	private boolean appendRecords;
	private boolean createBackup;
	private boolean exportImages;

	private int imageOption;
	private int imageHeight;
	private int imageWidth;
	private int lastIndex;

	private String profileID = "";

	private List<BasisField> userList = new ArrayList<>();

	// HanDBase Settings
	private String autoInstUser;
	private int exportOption;
	private int importOption;

	// MS-Excel Settings
	private String font = "Arial";
	private int fontSize = 8;

	private boolean boldHeader;
	private boolean lockHeader;
	private boolean lock1stColumn;
	private boolean useHeader; // Also used by text files
	private boolean skipEmptyRecords;

	// Text file Settings
	private String fieldSeparator = ",";
	private String textDelimiter = "\"";
	private String textFileFormat = "";
	private int maxFileSize;
	private boolean useLinebreak;

	protected Preferences child;

	protected Profiles(TvBSoftware software) {
		super(software);
	}

	@SuppressWarnings("unchecked")
	public void setProfile(String profileID) {
		if (profileID.isEmpty()) {
			return;
		}

		child = getParent().node(profileID);
		databaseFromFile = child.get("database.from.file", "");
		pdaDatabaseName = child.get("pda.database.name", "");

		contentsFilter = child.get("contents.filter", "");
		exportFile = child.get("export.file", "");
		exportUser = child.get("export.user", "");
		exportPassword = child.get("export.password", "");

		filterCondition = child.get("filter.condition", "AND");
		filterField[0] = child.get("filter0.field", "");
		filterField[1] = child.get("filter1.field", "");
		filterOperator[0] = FilterOperator
				.getFilterOperator(child.get("filter0.operator", FilterOperator.IS_EQUAL_TO.getValue()));
		filterOperator[1] = FilterOperator
				.getFilterOperator(child.get("filter1.operator", FilterOperator.IS_EQUAL_TO.getValue()));
		filterValue[0] = child.get("filter0.value", "");
		filterValue[1] = child.get("filter1.value", "");

		keywordFilter = child.get("keyword.filter", "");
		notes = child.get("export.notes", "");

		tableName = child.get("table.name", "");
		languageDriver = child.getInt("dbf.language.driver", 3);

		appendRecords = child.getBoolean("append.records", false);
		categoryField = child.get("category.field", "");
		createBackup = child.getBoolean("create.backup", false);
		exportImages = child.getBoolean("export.images", false);
		imageOption = child.getInt("export.image.option", 0);
		imageHeight = child.getInt("export.image.height", 0);
		imageWidth = child.getInt("export.image.width", 0);
		lastIndex = child.getInt("last.index", 0);
		skipEmptyRecords = child.getBoolean("skip.empty.records", false);

		lastModified = child.get("last.modified", "");
		lastSaved = child.get("last.saved", "");

		this.profileID = profileID;
		remainingField = child.get("remaining.field", "");
		sortField[0] = child.get("sort.field0", "");
		sortField[1] = child.get("sort.field1", "");
		sortField[2] = child.get("sort.field2", "");
		sortField[3] = child.get("sort.field3", "");

		groupField[0] = child.get("group.field0", "");
		groupField[1] = child.get("group.field1", "");
		groupField[2] = child.get("group.field2", "");
		groupField[3] = child.get("group.field3", "");

		groupingField[0] = child.get("grouping.field0", "");
		groupingField[1] = child.get("grouping.field1", "");
		groupingField[2] = child.get("grouping.field2", "");
		groupingField[3] = child.get("grouping.field3", "");

		userList.clear();
		relations.clear();

		int i = 0;
		while (true) {
			String element = child.get("relation" + i++, "");
			if (element.isEmpty()) {
				break;
			}
			relations.add(element);
		}

		// HanDBase
		autoInstUser = child.get("autoinst.user", "");
		exportOption = child.getInt("export.option", 0);
		importOption = child.getInt("import.option", 0);

		// MS-Excel
		boldHeader = child.getBoolean("use.bold.headers", true);
		font = child.get("font", "Arial");
		fontSize = child.getInt("font.size", 8);
		lockHeader = child.getBoolean("lock.headers", true);
		lock1stColumn = child.getBoolean("lock.firstcol", true);
		useHeader = child.getBoolean("use.headers", true);

		// Text files
		fieldSeparator = child.get("field.separator", ",");
		textDelimiter = child.get("text.delimiter", "\"");
		textFileFormat = child.get("textfile.format", "standardCsv");
		maxFileSize = child.getInt("textfile.maxsize", 0);
		useLinebreak = child.getBoolean("use.linebreak", true);

		// Files to be imported
		if (!databaseFromFile.isEmpty()) {
			getDbSettings().setNode(databaseFromFile);
		}

		try {
			if (child.nodeExists("userlist")) {
				userList = (List<BasisField>) PrefObj.getObject(child, "userlist");
			}
		} catch (Exception e) {
		}
	}

	public void reset() {
		filterCondition = "AND";
		filterField[0] = "";
		filterField[1] = "";
		filterOperator[0] = FilterOperator.IS_EQUAL_TO;
		filterOperator[1] = FilterOperator.IS_EQUAL_TO;
		filterValue[0] = "";
		filterValue[1] = "";
		keywordFilter = "";
		remainingField = "";

		for (int i = 0; i < 4; i++) {
			sortField[i] = "";
			groupField[i] = "";
			groupingField[i] = "";
		}

		relations.clear();
		userList.clear();
	}

	public String getContentsFilter() {
		return contentsFilter;
	}

	public void setContentsFilter(String contentsFilter) {
		PrefUtils.writePref(child, "contents.filter", contentsFilter, this.contentsFilter, "");
		this.contentsFilter = contentsFilter;
	}

	public String getExportFile() {
		return exportFile;
	}

	public String getImportFileProgram() {
		Databases db = getDbSettings();
		if (db.getDatabaseVersion().isEmpty()) {
			return db.getDatabaseTypeAsString();
		}
		return db.getDatabaseTypeAsString() + " (" + db.getDatabaseVersion() + ")";
	}

	public String getImportFile() {
		return getDbSettings().getRemoteDatabase();
	}

	public void setExportFile(String exportFile) {
		PrefUtils.writePref(child, "export.file", exportFile, this.exportFile, "");
		this.exportFile = exportFile;
	}

	public void setRelation(int index, String relation) {
		PrefUtils.writePref(child, "relation" + index + ".field", relation, this.filterField[index], "");
		this.filterField[index] = relation;
	}

	public String getFilterCondition() {
		return filterCondition;
	}

	public void setFilterCondition(String filterCondition) {
		PrefUtils.writePref(child, "filter.condition", filterCondition, this.filterCondition, "AND");
		this.filterCondition = filterCondition;
	}

	public boolean isNoFilters() {
		return filterField[0].length() + contentsFilter.length() + keywordFilter.length() == 0;
	}

	public boolean isFilterDefined() {
		return filterField[0].length() != 0;
	}

	public String getFilterField(int index) {
		return filterField[index];
	}

	public int noOfFilters() {
		if (isFilterDefined()) {
			return filterField[1].isEmpty() ? 1 : 2;
		}
		return 0;
	}

	public void setFilterField(int index, String filterField) {
		PrefUtils.writePref(child, "filter" + index + ".field", filterField, this.filterField[index], "");
		this.filterField[index] = filterField;
	}

	public FilterOperator getFilterOperator(int index) {
		return filterOperator[index];
	}

	public void setFilterOperator(int index, FilterOperator filterOperator) {
		PrefUtils.writePref(child, "filter" + index + ".operator", filterOperator.getValue(),
				this.filterOperator[index].getValue(), FilterOperator.IS_EQUAL_TO.getValue());
		this.filterOperator[index] = filterOperator;
	}

	public String getFilterValue(int index) {
		return filterValue[index];
	}

	public void setFilterValue(int index, String filterValue) {
		PrefUtils.writePref(child, "filter" + index + ".value", filterValue, this.filterValue[index], "");
		this.filterValue[index] = filterValue;
	}

	public String getKeywordFilter() {
		return keywordFilter;
	}

	public void setKeywordFilter(String keywordFilter) {
		PrefUtils.writePref(child, "keyword.filter", keywordFilter, this.keywordFilter, "");
		this.keywordFilter = keywordFilter;
	}

	public boolean isAppendRecords() {
		return appendRecords;
	}

	public void setAppendRecords(boolean appendRecords) {
		PrefUtils.writePref(child, "append.records", appendRecords, this.appendRecords, false);
		this.appendRecords = appendRecords;
	}

	public boolean isSkipEmptyRecords() {
		return skipEmptyRecords;
	}

	public void setSkipEmptyRecords(boolean skipEmptyRecords) {
		PrefUtils.writePref(child, "skip.empty.records", skipEmptyRecords, this.skipEmptyRecords, false);
		this.skipEmptyRecords = skipEmptyRecords;
	}

	public String getCategoryField() {
		return categoryField;
	}

	public void setCategoryField(String categoryField) {
		PrefUtils.writePref(child, "category.field", categoryField, this.categoryField, "-1");
		this.categoryField = categoryField;
	}

	public boolean isCreateBackup() {
		return createBackup;
	}

	public void setCreateBackup(boolean createBackup) {
		PrefUtils.writePref(child, "create.backup", createBackup, this.createBackup, false);
		this.createBackup = createBackup;
	}

	public boolean isExportImages() {
		return exportImages;
	}

	public void setExportImages(boolean exportImages) {
		PrefUtils.writePref(child, "export.images", exportImages, this.exportImages, false);
		this.exportImages = exportImages;
	}

	public int getLanguageDriver() {
		return languageDriver;
	}

	public void setLanguageDriver(int languageDriver) {
		PrefUtils.writePref(child, "dbf.language.driver", languageDriver, this.languageDriver, 3);
		this.languageDriver = languageDriver;
	}

	public int getImageOption() {
		return imageOption;
	}

	public void setImageOption(int imageOption) {
		PrefUtils.writePref(child, "export.image.option", imageOption, this.imageOption, 0);
		this.imageOption = imageOption;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		PrefUtils.writePref(child, "export.image.height", imageHeight, this.imageHeight, 0);
		this.imageHeight = imageHeight;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		PrefUtils.writePref(child, "export.image.width", imageWidth, this.imageWidth, 0);
		this.imageWidth = imageWidth;
	}

	public int getLastIndex() {
		return lastIndex;
	}

	public void setLastIndex(int lastIndex) {
		PrefUtils.writePref(child, "last.index", lastIndex, this.lastIndex, 0);
		this.lastIndex = lastIndex;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		PrefUtils.writePref(child, "last.modified", lastModified, this.lastModified, "");
		this.lastModified = lastModified;
	}

	public String getLastSaved() {
		return lastSaved;
	}

	public void setLastSaved() {
		lastSaved = General.convertTimestamp(LocalDateTime.now(), General.sdInternalTimestamp);
		PrefUtils.writePref(child, "last.saved", lastSaved, "", "");
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		PrefUtils.writePref(child, "export.notes", notes, this.notes, "");
		this.notes = notes;
	}

	public String getRemainingField() {
		return remainingField;
	}

	public void setRemainingField(String remainingField) {
		PrefUtils.writePref(child, "remaining.field", remainingField, this.remainingField, "");
		this.remainingField = remainingField;
	}

	public String getSortField(int index) {
		return sortField[index];
	}

	public List<String> getSortFields() {
		return getStringList(sortField);
	}

	public boolean isSortFieldDefined() {
		return !getSortFields().isEmpty();
	}

	public void setSortField(int index, String sortField) {
		PrefUtils.writePref(child, "sort.field" + index, sortField, this.sortField[index], "");
		this.sortField[index] = sortField;
	}

	public void removeSortField(String sortField) {
		List<String> aSortfield = new ArrayList<>();
		for (int i = 0; i < this.sortField.length; i++) {
			if (!this.sortField[i].equals(sortField)) {
				aSortfield.add(this.sortField[i]);
			}
			setSortField(i, "");
		}

		for (int i = 0; i < aSortfield.size(); i++) {
			setSortField(i, aSortfield.get(i));
		}
	}

	public List<String> getGroupFields() {
		return getStringList(groupField);
	}

	public boolean isGroupFieldDefined() {
		return !getGroupFields().isEmpty();
	}

	public String getGroupField(int index) {
		return groupField[index];
	}

	public void setGroupField(int index, String groupField) {
		PrefUtils.writePref(child, "group.field" + index, groupField, this.groupField[index], "");
		this.groupField[index] = groupField;
	}

	private List<String> getStringList(String[] array) {
		List<String> result = new ArrayList<>();
		for (String s : array) {
			if (!s.isEmpty()) {
				result.add(s);
			}
		}
		return result;
	}

	public Map<String, String> getGrouping() {
		Map<String, String> result = new LinkedHashMap<>();
		for (int i = 0; i < 4; i++) {
			String group = getGroupField(i);
			String grouping = getGroupingField(i);
			if (!group.isEmpty()) {
				result.put(group, grouping.isEmpty() ? group : grouping);
			}
		}
		return result;
	}

	public void removeGroupField(String groupField) {
		List<String> aGroupfield = new ArrayList<>();
		List<String> aGroupingfield = new ArrayList<>();
		for (int i = 0; i < this.groupField.length; i++) {
			if (!this.groupField[i].equals(groupField)) {
				aGroupfield.add(this.groupField[i]);
				aGroupingfield.add(groupingField[i]);
			}
			setGroupField(i, "");
			setGroupingField(i, "");
		}

		for (int i = 0; i < aGroupfield.size(); i++) {
			setGroupField(i, aGroupfield.get(i));
			setGroupingField(i, aGroupingfield.get(i));
		}
	}

	public String getGroupingField(int index) {
		return groupingField[index];
	}

	public void setGroupingField(int index, String groupingField) {
		PrefUtils.writePref(child, "grouping.field" + index, groupingField, this.groupingField[index], "");
		this.groupingField[index] = groupingField;
	}

	public List<String> getGroupingFields() {
		return getStringList(groupingField);
	}

	public List<String> getRelations() {
		return relations;
	}

	public void setRelations(List<String> relations) {
		int i = 0;
		for (String element : relations) {
			child.put("relation" + i++, element);
		}

		if (i < this.relations.size()) {
			for (int j = i; j < this.relations.size(); j++) {
				child.remove("relation" + j);
			}
		}

		this.relations.clear();
		this.relations.addAll(relations);
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String table, boolean isSave) {
		if (isSave) {
			PrefUtils.writePref(child, "table.name", table, tableName, "");
		}
		tableName = table;
	}

	public String getAutoInstUser() {
		return autoInstUser;
	}

	public void setAutoInstUser(String autoInstUser) {
		PrefUtils.writePref(child, "autoinst.user", autoInstUser, this.autoInstUser, "");
		this.autoInstUser = autoInstUser;
	}

	public int getExportOption() {
		return exportOption;
	}

	public void setExportOption(int exportOption) {
		PrefUtils.writePref(child, "export.option", exportOption, this.exportOption, 0);
		this.exportOption = exportOption;
	}

	public int getImportOption() {
		return importOption;
	}

	public void setImportOption(int importOption) {
		PrefUtils.writePref(child, "import.option", importOption, this.importOption, 0);
		this.importOption = importOption;
	}

	// MS-Excel
	public boolean isBoldHeader() {
		return boldHeader;
	}

	public void setBoldHeader(boolean useBoldHeader) {
		PrefUtils.writePref(child, "use.bold.headers", useBoldHeader, boldHeader, true);
		boldHeader = useBoldHeader;
	}

	public String getFont() {
		return font;
	}

	public void setFont(String font) {
		PrefUtils.writePref(child, "font", font, this.font, "Arial");
		this.font = font;
	}

	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		PrefUtils.writePref(child, "font.size", fontSize, this.fontSize, 8);
		this.fontSize = fontSize;
	}

	public boolean isLock1stColumn() {
		return lock1stColumn;
	}

	public void setLock1stColumn(boolean lock1stColumn) {
		PrefUtils.writePref(child, "lock.firstcol", lock1stColumn, this.lock1stColumn, true);
		this.lock1stColumn = lock1stColumn;
	}

	public boolean isLockHeader() {
		return lockHeader;
	}

	public void setLockHeader(boolean lockHeader) {
		PrefUtils.writePref(child, "lock.headers", lockHeader, this.lockHeader, true);
		this.lockHeader = lockHeader;
	}

	public boolean isUseHeader() {
		return useHeader;
	}

	public void setUseHeader(boolean useHeader) {
		PrefUtils.writePref(child, "use.headers", useHeader, this.useHeader, true);
		this.useHeader = useHeader;
	}

	// Text export file
	public String getFieldSeparator() {
		return fieldSeparator;
	}

	public void setFieldSeparator(String fieldSeparator) {
		PrefUtils.writePref(child, "field.separator", fieldSeparator, this.fieldSeparator, ",");
		this.fieldSeparator = fieldSeparator;
	}

	public String getTextDelimiter() {
		return textDelimiter;
	}

	public void setTextDelimiter(String textDelimiter) {
		PrefUtils.writePref(child, "text.delimiter", textDelimiter, this.textDelimiter, "\"");
		this.textDelimiter = textDelimiter;
	}

	public String getTextFileFormat() {
		return textFileFormat;
	}

	public void setTextFileFormat(String textfileFormat) {
		PrefUtils.writePref(child, "textfile.format", textfileFormat, this.textFileFormat, "standardCsv");
		this.textFileFormat = textfileFormat;
	}

	public int getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(int maxFileSize) {
		PrefUtils.writePref(child, "textfile.maxsize", maxFileSize, this.maxFileSize, 0);
		this.maxFileSize = maxFileSize;
	}

	public boolean isUseLinebreak() {
		return useLinebreak;
	}

	public void setUseLinebreak(boolean useLinebreak) {
		PrefUtils.writePref(child, "use.linebreak", useLinebreak, this.useLinebreak, true);
		this.useLinebreak = useLinebreak;
	}

	// Text import text file
	public String getImportFieldSeparator() {
		return getDbSettings().getFieldSeparator();
	}

	public String getImportTextDelimiter() {
		return getDbSettings().getTextDelimiter();
	}

	public String getImportTextFileFormat() {
		return getDbSettings().getTextFileFormat();
	}

	// Text import text file
	public void setImportFieldSeparator(String separator) {
		getDbSettings().setFieldSeparator(separator);
	}

	public void setImportTextDelimiter(String delimiter) {
		getDbSettings().setTextDelimiter(delimiter);
	}

	public void setImportTextFileFormat(String textFormat) {
		getDbSettings().setTextFileFormat(textFormat);
	}

	// General Methods
	public void cloneCurrentProfile(String newProject, String newProfile) throws FNProgException {
		if (profileExists(newProject, newProfile)) {
			throw FNProgException.getException("profileExists", newProfile);
		}

		try {
			copyProfile(child, newProject, newProfile);
		} catch (Exception e) {
			throw FNProgException.getException("profileCloneError", profileID, e.getMessage());
		}

		setProject(newProject);
		setProfile(newProfile);
		setLastIndex(0);
		setLastModified("");
		setLastSaved();
	}

	public void renameCurrentNode(String newProfile) throws FNProgException {
		if (newProfile.equals(profileID)) {
			return;
		}

		if (profileExists(newProfile)) {
			throw FNProgException.getException("profileExists", newProfile);
		}

		try {
			copyProfile(newProfile);
			deleteNode(profileID);
		} catch (Exception e) {
			throw FNProgException.getException("profileRenameError", profileID, newProfile, e.getMessage());
		}

		setProfile(newProfile);
	}

	private void copyProfile(String newProfileID) throws Exception {
		PrefUtils.copyNode(child, getParent(), newProfileID, true);
	}

	public boolean profileExists(String profile) {
		try {
			return getParent().nodeExists(profile);
		} catch (Exception e) {
		}
		return false;
	}

	public Preferences getChild() {
		return child;
	}

	public ExportFile getExportFileEnum() {
		return ExportFile.getExportFile(getProjectID());
	}

	public String getExportUser() {
		return exportUser;
	}

	public void setExportUser(String exportUser) {
		PrefUtils.writePref(child, "export.user", exportUser, this.exportUser, "");
		this.exportUser = exportUser;
	}

	public String getExportPassword() {
		return General.decryptPassword(exportPassword);
	}

	public void setExportPassword(char[] password) {
		String expPassword = "";
		if (getExportFileEnum().isPasswordSupported()) {
			expPassword = General.encryptPassword(password);
		}

		PrefUtils.writePref(child, "export.password", expPassword, exportPassword, "");
		exportPassword = expPassword;
	}

	public void deleteNode(String profileID) {
		if (profileExists(profileID)) {
			Preferences p = getParent().node(profileID);
			String database = p.get("database.from.file", "");
			try {
				PrefUtils.deleteNode(getParent(), profileID);
				if (database.isEmpty()) {
					// No database has been setup yet for this profile
					return;
				}

				// Cleanup Database record in Registry
				cleanupDatabase(database);
			} catch (Exception e) {
			}
		}
	}

	public Set<String> getSpecialFields() {
		Set<String> result = new HashSet<>();
		ExportFile exp = ExportFile.getExportFile(getProjectID());

		if (userList.isEmpty() || !exp.isSpecialFieldSort()) {
			return result;
		}

		Set<String> set = new HashSet<>();
		for (BasisField field : userList) {
			set.add(field.getFieldAlias());
		}

		// Add sort (or grouping) fields to user fieldList
		List<String> sortList = exp == ExportFile.LIST ? getSortFields() : getGroupFields();
		sortList.forEach(field -> {
			if (!set.contains(field)) {
				result.add(field);
			}
		});

		// Add Category field to user fields
		if (!categoryField.isEmpty() && !set.contains(categoryField)) {
			result.add(categoryField);
		}
		return result;
	}

	public String getDatabaseFromFile() {
		return databaseFromFile;
	}

	public void setDatabaseFromFile(String databaseFromFile) {
		child.put("database.from.file", databaseFromFile);
		this.databaseFromFile = databaseFromFile;
	}

	public String getPdaDatabaseName() {
		return pdaDatabaseName;
	}

	public void setPdaDatabaseName(String pdaDatabaseName) {
		PrefUtils.writePref(child, "pda.database.name", pdaDatabaseName, this.pdaDatabaseName, "");
		this.pdaDatabaseName = pdaDatabaseName;
	}

	public String getProfileID() {
		return profileID;
	}

	public List<BasisField> getUserList() {
		return userList;
	}

	public void setUserList(List<BasisField> userlist) {
		userList = userlist;
		try {
			PrefObj.putObject(child, "userlist", userList);
		} catch (Exception e) {
		}
	}

	public void updateTofile(String exportFile, String exportUser, char[] filePassword) {
		setExportFile(exportFile);
		setExportUser(exportUser);
		setExportPassword(filePassword);
	}
}
