package application.preferences;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import application.interfaces.ExportFile;
import application.interfaces.FilterOperator;
import application.interfaces.IEncoding;
import application.interfaces.TvBSoftware;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.General;

public abstract class Profiles extends Project implements IEncoding {
	// General Settings
	private String categoryField = "";
	private String contentsFilter = "";
	private String databaseFromFile;
	private String encoding = "";
	private String exportFile = "";
	private String exportUser;
	private String exportPassword;
	private String filterCondition;
	private String keywordFilter;
	private String lastModified;
	private String lastSaved;
	private String notes;
	private String pdaDatabaseName = "";
	private String tableName = "";

	private String[] filterField = new String[] { "", "" };
	private FilterOperator[] filterOperator = new FilterOperator[2];
	private String[] filterValue = new String[] { "", "" };
	private String[] sortField = new String[] { "", "", "", "" };

	private boolean appendRecords;
	private boolean createBackup;
	private boolean exportImages;
	private boolean forceSort;

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

	// P.Referencer Settings
	private boolean useParagraphHeader;
	private boolean useTitleHeader;

	// Text file Settings
	private String fieldSeparator = ",";
	private String textDelimiter = "\"";
	private String textFileFormat = "";
	private int maxFileSize;
	private boolean useLinebreak;

	private Preferences child;
	private GeneralSettings generalSettings = GeneralSettings.getInstance();

	public Profiles(TvBSoftware software) {
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
		encoding = child.get("encoding.charset", generalSettings.getEncoding());
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

		appendRecords = child.getBoolean("append.records", false);
		categoryField = child.get("category.field", "");
		createBackup = child.getBoolean("create.backup", false);
		exportImages = child.getBoolean("export.images", false);
		forceSort = child.getBoolean("force.sort", true);
		imageOption = child.getInt("export.image.option", 0);
		imageHeight = child.getInt("export.image.height", 0);
		imageWidth = child.getInt("export.image.width", 0);
		lastIndex = child.getInt("last.index", 0);

		lastModified = child.get("last.modified", "");
		lastSaved = child.get("last.saved", "");

		this.profileID = profileID;
		sortField[0] = child.get("sort.field0", "");
		sortField[1] = child.get("sort.field1", "");
		sortField[2] = child.get("sort.field2", "");
		sortField[3] = child.get("sort.field3", "");

		userList.clear();

		// HanDBase
		autoInstUser = child.get("autoinst.user", "");
		exportOption = child.getInt("export.option", 0);
		importOption = child.getInt("import.option", 0);

		// MS-Excel
		boldHeader = child.getBoolean("bold.headers", true);
		font = child.get("font", "Arial");
		fontSize = child.getInt("font.size", 8);
		lockHeader = child.getBoolean("lock.headers", true);
		lock1stColumn = child.getBoolean("lock.firstcol", true);
		useHeader = child.getBoolean("use.headers", true);

		// P.Referencer
		useParagraphHeader = child.getBoolean("use.paragraph.header", false);
		useTitleHeader = child.getBoolean("use.title.header", false);

		// Text files
		fieldSeparator = child.get("field.separator", ",");
		textDelimiter = child.get("text.delimiter", "\"");
		textFileFormat = child.get("textfile.format", "");
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
			return db.getDatabaseType();
		}
		return db.getDatabaseType() + " (" + db.getDatabaseVersion() + ")";
	}

	public String getImportFile() {
		return getDbSettings().getDatabaseFile();
	}

	public void setExportFile(String exportFile) {
		PrefUtils.writePref(child, "export.file", exportFile, this.exportFile, "");
		this.exportFile = exportFile;
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

	@Override
	public String getEncoding() {
		return encoding;
	}

	@Override
	public void setEncoding(String encoding) {
		PrefUtils.writePref(child, "encoding.charset", encoding, this.encoding, generalSettings.getEncoding());
		this.encoding = encoding;
	}

	public boolean isExportImages() {
		return exportImages;
	}

	public void setExportImages(boolean exportImages) {
		PrefUtils.writePref(child, "export.images", exportImages, this.exportImages, false);
		this.exportImages = exportImages;
	}

	public boolean isForceSort() {
		return forceSort;
	}

	public void setForceSort(boolean forceSort) {
		PrefUtils.writePref(child, "force.sort", forceSort, this.forceSort, true);
		this.forceSort = forceSort;
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
		lastSaved = General.convertTimestamp2DB(LocalDateTime.now());
		PrefUtils.writePref(child, "last.saved", lastSaved, "", "");
	}

	public void setLastSaved(Date date) {
		lastSaved = General.convertTimestamp2DB(date);
		PrefUtils.writePref(child, "last.saved", lastSaved, "", "");
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		PrefUtils.writePref(child, "export.notes", notes, this.notes, "");
		this.notes = notes;
	}

	public String getSortField(int index) {
		return sortField[index];
	}

	public boolean isSortFieldDefined() {
		for (String s : sortField) {
			if (!s.isEmpty()) {
				return true;
			}
		}
		return false;
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

	// P.Referencer
	public boolean isUseParagraphHeader() {
		return useParagraphHeader;
	}

	public void setUseParagraphHeader(boolean useParagraphHeader) {
		PrefUtils.writePref(child, "use.paragraph.header", useParagraphHeader, useParagraphHeader, false);
		this.useParagraphHeader = useParagraphHeader;
	}

	public boolean isUseTitleHeader() {
		return useTitleHeader;
	}

	public void setUseTitleHeader(boolean useTitleHeader) {
		PrefUtils.writePref(child, "use.title.header", useTitleHeader, this.useTitleHeader, false);
		this.useTitleHeader = useTitleHeader;
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
		PrefUtils.writePref(child, "textfile.format", textfileFormat, textFileFormat, "");
		textFileFormat = textfileFormat;
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

	public String getImportFileEncoding() {
		return getDbSettings().getEncoding();
	}

	public void setImportFileEncoding(String encoding) {
		getDbSettings().setEncoding(encoding);
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

		HashSet<String> set = new HashSet<>();
		for (BasisField field : userList) {
			set.add(field.getFieldAlias());
		}

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
