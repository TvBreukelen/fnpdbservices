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
import dbengine.utils.DatabaseHelper;

public abstract class Profiles extends Project {
	public static final String FROM_DATABASE = "database.from.file";
	public static final String TO_DATABASE = "export.file";
	public static final String USER_LIST = "userlist";

	public static final int ON_CONFLICT_ABORT = 0;
	public static final int ON_CONFLICT_FAIL = 1;
	public static final int ON_CONFLICT_IGNORE = 2;
	public static final int ON_CONFLICT_REPLACE = 3;
	public static final int ON_CONFLICT_ROLLBACK = 4;

	private static final String RELATION = "relation";
	private static final String ARIAL = "Arial";
	private static final String FILTER = "filter";

	// General Settings
	private String categoryField = General.EMPTY_STRING;
	private String contentsFilter = General.EMPTY_STRING;
	private String fromDatabase = General.EMPTY_STRING;
	private String toDatabase = General.EMPTY_STRING;
	private String filterCondition = General.EMPTY_STRING;
	private String keywordFilter = General.EMPTY_STRING;
	private String lastExported = General.EMPTY_STRING;
	private String lastSaved = General.EMPTY_STRING;
	private String notes = General.EMPTY_STRING;
	private String databaseName = General.EMPTY_STRING;
	private String remainingField = General.EMPTY_STRING;
	private String tableName = General.EMPTY_STRING;
	private int languageDriver = 3; // xBase

	private String[] filterField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING };
	private FilterOperator[] filterOperator = new FilterOperator[2];
	private String[] filterValue = new String[] { General.EMPTY_STRING, General.EMPTY_STRING };
	private String[] sortField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING,
			General.EMPTY_STRING };
	private String[] groupField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING,
			General.EMPTY_STRING };
	private String[] groupingField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING,
			General.EMPTY_STRING };
	private List<String> relations = new ArrayList<>();

	private boolean appendRecords;
	private boolean createBackup;
	private boolean exportImages;
	private boolean isNewProfile;

	private int imageOption;
	private int imageHeight;
	private int imageWidth;

	private String profileID = General.EMPTY_STRING;

	// FNProg2PDA only
	private boolean useContentsIndex;
	private boolean useContentsItemTitle;
	private boolean useContentsLength;
	private boolean useContentsOrigTitle;
	private boolean useContentsPerson;
	private boolean useContentsSide;
	private boolean useEntireCast;
	private boolean useOriginalTitle;
	private boolean useReleaseNo;
	private boolean useSeason;
	private boolean useRoles;

	private int lastIndex;

	private List<BasisField> userList = new ArrayList<>();

	// Sql database options
	private int sqlSelectLimit = 0;
	private boolean pagination;
	private int onConflict = ON_CONFLICT_REPLACE;

	// HanDBase Settings
	private String autoInstUser = General.EMPTY_STRING;
	private int exportOption;
	private int importOption;

	// MS-Excel Settings
	private String font = ARIAL;
	private int fontSize = 8;

	private boolean boldHeader;
	private boolean lockHeader;
	private boolean lock1stColumn;
	private boolean useHeader; // Also used by text files
	private boolean skipEmptyRecords;

	// Text file Settings
	private String fieldSeparator = ",";
	private String textDelimiter = General.TEXT_DELIMITER;
	private String textFileFormat = General.EMPTY_STRING;
	private int maxFileSize;
	private boolean useLinebreak;

	// iCalendar
	private boolean isNotesCompatible;
	private boolean isOutlookCompatible;
	private boolean relaxedParsing;
	private boolean relaxedUnfolding;
	private boolean relaxedValidation;

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
		fromDatabase = child.get(FROM_DATABASE, General.EMPTY_STRING);
		databaseName = child.get("pda.database.name", General.EMPTY_STRING);

		contentsFilter = child.get("contents.filter", General.EMPTY_STRING);
		toDatabase = child.get(TO_DATABASE, General.EMPTY_STRING);

		filterCondition = child.get("filter.condition", "AND");
		filterField[0] = child.get("filter0.field", General.EMPTY_STRING);
		filterField[1] = child.get("filter1.field", General.EMPTY_STRING);
		filterOperator[0] = FilterOperator
				.getFilterOperator(child.get("filter0.operator", FilterOperator.IS_EQUAL_TO.getValue()));
		filterOperator[1] = FilterOperator
				.getFilterOperator(child.get("filter1.operator", FilterOperator.IS_EQUAL_TO.getValue()));
		filterValue[0] = child.get("filter0.value", General.EMPTY_STRING);
		filterValue[1] = child.get("filter1.value", General.EMPTY_STRING);

		keywordFilter = child.get("keyword.filter", General.EMPTY_STRING);
		notes = child.get("export.notes", General.EMPTY_STRING);

		tableName = child.get("table.name", General.EMPTY_STRING);
		languageDriver = child.getInt("dbf.language.driver", 3);

		appendRecords = child.getBoolean("append.records", false);
		categoryField = child.get("category.field", General.EMPTY_STRING);
		createBackup = child.getBoolean("create.backup", false);
		exportImages = child.getBoolean("export.images", false);
		imageOption = child.getInt("export.image.option", 0);
		imageHeight = child.getInt("export.image.height", 0);
		imageWidth = child.getInt("export.image.width", 0);
		skipEmptyRecords = child.getBoolean("skip.empty.records", false);

		lastExported = child.get("last.modified", General.EMPTY_STRING);
		lastSaved = child.get("last.saved", General.EMPTY_STRING);

		this.profileID = profileID;
		remainingField = child.get("remaining.field", General.EMPTY_STRING);
		sortField[0] = child.get("sort.field0", General.EMPTY_STRING);
		sortField[1] = child.get("sort.field1", General.EMPTY_STRING);
		sortField[2] = child.get("sort.field2", General.EMPTY_STRING);
		sortField[3] = child.get("sort.field3", General.EMPTY_STRING);

		groupField[0] = child.get("group.field0", General.EMPTY_STRING);
		groupField[1] = child.get("group.field1", General.EMPTY_STRING);
		groupField[2] = child.get("group.field2", General.EMPTY_STRING);
		groupField[3] = child.get("group.field3", General.EMPTY_STRING);

		groupingField[0] = child.get("grouping.field0", General.EMPTY_STRING);
		groupingField[1] = child.get("grouping.field1", General.EMPTY_STRING);
		groupingField[2] = child.get("grouping.field2", General.EMPTY_STRING);
		groupingField[3] = child.get("grouping.field3", General.EMPTY_STRING);

		userList.clear();
		relations.clear();

		int i = 0;
		while (true) {
			String element = child.get(RELATION + i++, General.EMPTY_STRING);
			if (element.isEmpty()) {
				break;
			}
			relations.add(element);
		}

		// HanDBase
		autoInstUser = child.get("autoinst.user", General.EMPTY_STRING);
		exportOption = child.getInt("export.option", 0);
		importOption = child.getInt("import.option", 0);

		// MS-Excel
		boldHeader = child.getBoolean("use.bold.headers", true);
		font = child.get("font", ARIAL);
		fontSize = child.getInt("font.size", 8);
		lockHeader = child.getBoolean("lock.headers", true);
		lock1stColumn = child.getBoolean("lock.firstcol", true);
		useHeader = child.getBoolean("use.headers", true);

		// Text files
		fieldSeparator = child.get("field.separator", ",");
		textDelimiter = child.get("text.delimiter", General.TEXT_DELIMITER);
		textFileFormat = child.get("textfile.format", "standardCsv");
		maxFileSize = child.getInt("textfile.maxsize", 0);
		useLinebreak = child.getBoolean("use.linebreak", true);

		// iCalendar
		isNotesCompatible = child.getBoolean("icalendar.notes.compatible", false);
		isOutlookCompatible = child.getBoolean("icalendar.outlook.compatible", false);
		relaxedUnfolding = child.getBoolean("icalendar.relaxed.unfolding", false);
		relaxedParsing = child.getBoolean("icalendar.relaxed.parsing", true);
		relaxedValidation = child.getBoolean("icalendar.relaxed.validation", false);

		// Sql Databases
		sqlSelectLimit = child.getInt("sql.select.limit", 0);
		pagination = child.getBoolean("database.pagination", false);
		onConflict = child.getInt("onconflict.action", ON_CONFLICT_REPLACE);

		// FNProg2PDA
		useContentsIndex = child.getBoolean("use.contents.index", false);
		useContentsItemTitle = child.getBoolean("use.contents.itemtitle", false);
		useContentsLength = child.getBoolean("use.contents.length", false);
		useContentsOrigTitle = child.getBoolean("use.contents.origtitle", false);
		useContentsPerson = child.getBoolean("use.contents.person", false);
		useContentsSide = child.getBoolean("use.contents.side", false);
		useEntireCast = child.getBoolean("use.entire.cast", false);
		useOriginalTitle = child.getBoolean("use.original.title", false);
		useReleaseNo = child.getBoolean("use.releaseno", false);
		useRoles = child.getBoolean("use.roles", false);
		useSeason = child.getBoolean("use.season", false);
		lastIndex = child.getInt("last.index", 0);

		// Files to be imported
		if (!fromDatabase.isEmpty()) {
			getDbSettings().setNode(fromDatabase);
		}

		try {
			if (child.nodeExists(USER_LIST)) {
				userList = (List<BasisField>) PrefObj.getObject(child, USER_LIST);
			}
		} catch (Exception e) {
			// Should not occur
		}
	}

	public void reset() {
		filterCondition = "AND";
		filterField[0] = General.EMPTY_STRING;
		filterField[1] = General.EMPTY_STRING;
		filterOperator[0] = FilterOperator.IS_EQUAL_TO;
		filterOperator[1] = FilterOperator.IS_EQUAL_TO;
		filterValue[0] = General.EMPTY_STRING;
		filterValue[1] = General.EMPTY_STRING;
		keywordFilter = General.EMPTY_STRING;
		remainingField = General.EMPTY_STRING;

		for (int i = 0; i < 4; i++) {
			sortField[i] = General.EMPTY_STRING;
			groupField[i] = General.EMPTY_STRING;
			groupingField[i] = General.EMPTY_STRING;
		}

		relations.clear();
		userList.clear();
	}

	public String getContentsFilter() {
		return contentsFilter;
	}

	public void setContentsFilter(String contentsFilter) {
		PrefUtils.writePref(child, "contents.filter", contentsFilter, this.contentsFilter, General.EMPTY_STRING);
		this.contentsFilter = contentsFilter;
	}

	public String getImportFileProgram() {
		Databases db = getDbSettings();
		if (db.getDatabaseVersion().isEmpty()) {
			return db.getDatabaseTypeAsString();
		}
		return db.getDatabaseTypeAsString() + " (" + db.getDatabaseVersion() + ")";
	}

	public DatabaseHelper getFromDatabase() {
		return getDatabase(fromDatabase);
	}

	public void setFromDatabase(String fromDatabase) {
		PrefUtils.writePref(child, FROM_DATABASE, fromDatabase, this.fromDatabase, General.EMPTY_STRING);
		this.fromDatabase = fromDatabase;
	}

	public DatabaseHelper getToDatabase() {
		return getDatabase(toDatabase);
	}

	public void setToDatabase(String toDatabase) {
		PrefUtils.writePref(child, TO_DATABASE, toDatabase, this.toDatabase, General.EMPTY_STRING);
		this.toDatabase = toDatabase;
	}

	public void setRelation(int index, String relation) {
		PrefUtils.writePref(child, RELATION + index + ".field", relation, filterField[index], General.EMPTY_STRING);
		filterField[index] = relation;
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
		PrefUtils.writePref(child, FILTER + index + ".field", filterField, this.filterField[index],
				General.EMPTY_STRING);
		this.filterField[index] = filterField;
	}

	public FilterOperator getFilterOperator(int index) {
		return filterOperator[index];
	}

	public void setFilterOperator(int index, FilterOperator filterOperator) {
		PrefUtils.writePref(child, FILTER + index + ".operator", filterOperator.getValue(),
				this.filterOperator[index].getValue(), FilterOperator.IS_EQUAL_TO.getValue());
		this.filterOperator[index] = filterOperator;
	}

	public String getFilterValue(int index) {
		return filterValue[index];
	}

	public void setFilterValue(int index, String filterValue) {
		PrefUtils.writePref(child, FILTER + index + ".value", filterValue, this.filterValue[index],
				General.EMPTY_STRING);
		this.filterValue[index] = filterValue;
	}

	public String getKeywordFilter() {
		return keywordFilter;
	}

	public void setKeywordFilter(String keywordFilter) {
		PrefUtils.writePref(child, "keyword.filter", keywordFilter, this.keywordFilter, General.EMPTY_STRING);
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

	public String getLastExported() {
		return lastExported;
	}

	public void setLastExported(String lastExported) {
		PrefUtils.writePref(child, "last.modified", lastExported, this.lastExported, General.EMPTY_STRING);
		this.lastExported = lastExported;
	}

	public String getLastSaved() {
		return lastSaved;
	}

	public void setLastSaved() {
		lastSaved = General.convertTimestamp(LocalDateTime.now(), General.sdInternalTimestamp);
		PrefUtils.writePref(child, "last.saved", lastSaved, General.EMPTY_STRING, General.EMPTY_STRING);
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		PrefUtils.writePref(child, "export.notes", notes, this.notes, General.EMPTY_STRING);
		this.notes = notes;
	}

	public String getRemainingField() {
		return remainingField;
	}

	public void setRemainingField(String remainingField) {
		PrefUtils.writePref(child, "remaining.field", remainingField, this.remainingField, General.EMPTY_STRING);
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
		PrefUtils.writePref(child, "sort.field" + index, sortField, this.sortField[index], General.EMPTY_STRING);
		this.sortField[index] = sortField;
	}

	public void removeSortField(String sortField) {
		List<String> aSortfield = new ArrayList<>();
		for (int i = 0; i < this.sortField.length; i++) {
			if (!this.sortField[i].equals(sortField)) {
				aSortfield.add(this.sortField[i]);
			}
			setSortField(i, General.EMPTY_STRING);
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
		PrefUtils.writePref(child, "group.field" + index, groupField, this.groupField[index], General.EMPTY_STRING);
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
			setGroupField(i, General.EMPTY_STRING);
			setGroupingField(i, General.EMPTY_STRING);
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
		PrefUtils.writePref(child, "grouping.field" + index, groupingField, this.groupingField[index],
				General.EMPTY_STRING);
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
			child.put(RELATION + i++, element);
		}

		if (i < this.relations.size()) {
			for (int j = i; j < this.relations.size(); j++) {
				child.remove(RELATION + j);
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
			PrefUtils.writePref(child, "table.name", table, tableName, General.EMPTY_STRING);
		}
		tableName = table;
	}

	public String getAutoInstUser() {
		return autoInstUser;
	}

	public void setAutoInstUser(String autoInstUser) {
		PrefUtils.writePref(child, "autoinst.user", autoInstUser, this.autoInstUser, General.EMPTY_STRING);
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

	public int getOnConflict() {
		return onConflict;
	}

	public void setOnConflict(int onConflict) {
		PrefUtils.writePref(child, "onconflict.action", onConflict, this.onConflict, ON_CONFLICT_REPLACE);
		this.onConflict = onConflict;
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
		PrefUtils.writePref(child, "font", font, this.font, ARIAL);
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
		PrefUtils.writePref(child, "text.delimiter", textDelimiter, this.textDelimiter, General.TEXT_DELIMITER);
		this.textDelimiter = textDelimiter;
	}

	public String getTextFileFormat() {
		return textFileFormat;
	}

	public void setTextFileFormat(String textFileFormat) {
		PrefUtils.writePref(child, "textfile.format", textFileFormat, this.textFileFormat, "standardCsv");
		this.textFileFormat = textFileFormat;
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

	public void cleanupNodes() {
		getDbSettings().cleanupNodes(this);
	}

	// Text import text file
	public String getImportFieldSeparator() {
		getDbSettings().setNode(fromDatabase);
		return getDbSettings().getFieldSeparator();
	}

	public String getImportTextDelimiter() {
		getDbSettings().setNode(fromDatabase);
		return getDbSettings().getTextDelimiter();
	}

	public String getImportTextFileFormat() {
		getDbSettings().setNode(fromDatabase);
		return getDbSettings().getTextFileFormat();
	}

	// Text import text file
	public void setImportTextfields(String separator, String delimiter, String format) {
		getDbSettings().setNode(fromDatabase);
		getDbSettings().setFieldSeparator(separator);
		getDbSettings().setTextDelimiter(delimiter);
		getDbSettings().setTextFileFormat(format);
	}

	// iCalendar import
	public boolean isNotesCompatible() {
		return isNotesCompatible;
	}

	public void setNotesCompatible(boolean isNotesCompatible) {
		PrefUtils.writePref(child, "icalendar.notes.compatible", isNotesCompatible, this.isNotesCompatible, false);
		this.isNotesCompatible = isNotesCompatible;
	}

	public boolean isOutlookCompatible() {
		return isOutlookCompatible;
	}

	public boolean isStandardCompatible() {
		return !(isNotesCompatible || isOutlookCompatible);
	}

	public void setOutlookCompatible(boolean isOutlookCompatible) {
		PrefUtils.writePref(child, "icalendar.outlook.compatible", isOutlookCompatible, this.isOutlookCompatible,
				false);
		this.isOutlookCompatible = isOutlookCompatible;
	}

	public boolean isRelaxedParsing() {
		return relaxedParsing;
	}

	public void setRelaxedParsing(boolean relaxedParsing) {
		PrefUtils.writePref(child, "icalendar.relaxed.parsing", relaxedParsing, this.relaxedParsing, true);
		this.relaxedParsing = relaxedParsing;
	}

	public boolean isRelaxedUnfolding() {
		return relaxedUnfolding;
	}

	public void setRelaxedUnfolding(boolean relaxedUnfolding) {
		PrefUtils.writePref(child, "icalendar.relaxed.unfolding", relaxedUnfolding, this.relaxedUnfolding, false);
		this.relaxedUnfolding = relaxedUnfolding;
	}

	public boolean isRelaxedValidation() {
		return relaxedValidation;
	}

	public void setRelaxedValidation(boolean relaxedValidation) {
		PrefUtils.writePref(child, "icalendar.relaxed.validation", relaxedValidation, this.relaxedValidation, false);
		this.relaxedValidation = relaxedUnfolding;
	}

	public boolean isUseContentsIndex() {
		return useContentsIndex;
	}

	public void setUseContentsIndex(boolean useContentsIndex) {
		PrefUtils.writePref(child, "use.contents.index", useContentsIndex, this.useContentsIndex, false);
		this.useContentsIndex = useContentsIndex;
	}

	public boolean isUseContentsLength() {
		return useContentsLength;
	}

	public void setUseContentsLength(boolean useContentsLength) {
		PrefUtils.writePref(child, "use.contents.length", useContentsLength, this.useContentsLength, false);
		this.useContentsLength = useContentsLength;
	}

	public boolean isUseContentsSide() {
		return useContentsSide;
	}

	public void setUseContentsSide(boolean useContentsSide) {
		PrefUtils.writePref(child, "use.contents.side", useContentsSide, this.useContentsSide, false);
		this.useContentsSide = useContentsSide;
	}

	public boolean isUseContentsItemTitle() {
		return useContentsItemTitle;
	}

	public void setUseContentsItemTitle(boolean useContentsItemTitle) {
		PrefUtils.writePref(child, "use.contents.itemtitle", useContentsItemTitle, this.useContentsItemTitle, false);
		this.useContentsItemTitle = useContentsItemTitle;
	}

	public boolean isUseContentsOrigTitle() {
		return useContentsOrigTitle;
	}

	public void setUseContentsOrigTitle(boolean useContentsOrigTitle) {
		PrefUtils.writePref(child, "use.contents.origtitle", useContentsOrigTitle, this.useContentsOrigTitle, false);
		this.useContentsOrigTitle = useContentsOrigTitle;
	}

	public boolean isUseOriginalTitle() {
		return useOriginalTitle;
	}

	public void setUseOriginalTitle(boolean useOriginalTitle) {
		PrefUtils.writePref(child, "use.original.title", useOriginalTitle, this.useOriginalTitle, false);
		this.useOriginalTitle = useOriginalTitle;
	}

	public boolean isUseContentsPerson() {
		return useContentsPerson;
	}

	public void setUseContentsPerson(boolean useContentsPerson) {
		PrefUtils.writePref(child, "use.contents.person", useContentsPerson, this.useContentsPerson, false);
		this.useContentsPerson = useContentsPerson;
	}

	public boolean isUseEntireCast() {
		return useEntireCast;
	}

	public void setUseEntireCast(boolean useEntireCast) {
		PrefUtils.writePref(child, "use.entire.cast", useEntireCast, this.useEntireCast, false);
		this.useEntireCast = useEntireCast;
	}

	public boolean isUseReleaseNo() {
		return useReleaseNo;
	}

	public void setUseReleaseNo(boolean useReleaseNo) {
		PrefUtils.writePref(child, "use.releaseno", useReleaseNo, this.useReleaseNo, false);
		this.useReleaseNo = useReleaseNo;
	}

	public boolean isUseRoles() {
		return useRoles;
	}

	public void setUseRoles(boolean useRoles) {
		PrefUtils.writePref(child, "use.roles", useRoles, this.useRoles, false);
		this.useRoles = useRoles;
	}

	public boolean isUseSeason() {
		return useSeason;
	}

	public void setUseSeason(boolean useSeason) {
		PrefUtils.writePref(child, "use.season", useSeason, this.useSeason, false);
		this.useSeason = useSeason;
	}

	public int getLastIndex() {
		return lastIndex;
	}

	public void setLastIndex(int lastIndex) {
		PrefUtils.writePref(child, "last.index", lastIndex, this.lastIndex, 0);
		this.lastIndex = lastIndex;
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
		setLastExported(General.EMPTY_STRING);
		setLastSaved();
		setLastIndex(0);
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
		PrefUtils.copyNode(child, getParent(), newProfileID);
	}

	public boolean profileExists(String profile) {
		try {
			return getParent().nodeExists(profile);
		} catch (Exception e) {
			return false;
		}
	}

	public Preferences getChild() {
		return child;
	}

	public void deleteNode(String profileID) {
		if (profileExists(profileID)) {
			Preferences p = getParent().node(profileID);
			String database = p.get(FROM_DATABASE, General.EMPTY_STRING);
			PrefUtils.deleteNode(getParent(), profileID);
			if (database.isEmpty()) {
				// No database has been setup yet for this profile
				return;
			}

			// Cleanup Database record in Registry
			cleanupDatabase(database);
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

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		PrefUtils.writePref(child, "pda.database.name", databaseName, this.databaseName, General.EMPTY_STRING);
		this.databaseName = databaseName;
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
			PrefObj.putObject(child, USER_LIST, userList);
		} catch (Exception e) {
			// Should not occur
		}
	}

	public int getSqlSelectLimit() {
		return sqlSelectLimit;
	}

	public void setSqlSelectLimit(int sqlSelectLimit) {
		PrefUtils.writePref(child, "sql.select.limit", sqlSelectLimit, this.sqlSelectLimit, 0);
		this.sqlSelectLimit = sqlSelectLimit;
	}

	public boolean isPagination() {
		return pagination;
	}

	public void setPagination(boolean pagination) {
		PrefUtils.writePref(child, "database.pagination", pagination, this.pagination, false);
		this.pagination = pagination;
	}

	public boolean isNewProfile() {
		return isNewProfile;
	}

	public void setNewProfile(boolean isNewProfile) {
		this.isNewProfile = isNewProfile;
	}
}
