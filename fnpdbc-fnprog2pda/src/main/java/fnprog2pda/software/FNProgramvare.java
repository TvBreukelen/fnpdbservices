package fnprog2pda.software;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Row;

import application.BasicSoft;
import application.interfaces.ExportFile;
import application.interfaces.FNPSoftware;
import application.interfaces.FieldTypes;
import application.interfaces.FilterOperator;
import application.interfaces.TvBSoftware;
import application.model.ViewerModel;
import application.preferences.Databases;
import application.preferences.GeneralSettings;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.XComparator;
import dbengine.GeneralDB;
import dbengine.export.CsvFile;
import dbengine.export.HanDBase;
import dbengine.export.MSAccess;
import dbengine.utils.DatabaseHelper;
import dbengine.utils.MSTable;
import fnprog2pda.preferences.PrefFNProg;

public abstract class FNProgramvare extends BasicSoft {
	/**
	 * Title: FNProgramvare Description: Abstract Class for FNProgramvare software
	 * Copyright: (c) 2003-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	protected String mySoftwareVersion = "";
	protected int myLastIndex = 0;

	protected String myTable;
	protected String myTableID;

	private boolean useCategory;
	private boolean exportImages;

	private boolean isSkipFirstFilter;
	private boolean isSkipLastFilter;
	private boolean isNewModified;
	private boolean isNewRecords;

	protected boolean useRoles;
	protected boolean useContents;
	protected boolean useContentsPerson;
	protected boolean useContentsOrigTitle = true;
	protected boolean useContentsItemTitle = true;
	protected boolean usePersonSort;

	private MSTable msTable;
	private Cursor cursor;
	protected MSAccess msAccess;

	private Map<String, FieldDefinition> dbInfoToExport = new HashMap<>(); // DB definitions of the fields to be
	// exported
	private Map<String, FieldTypes> dbSortList = new LinkedHashMap<>(); // Sort by field -number and -type
	private Predicate<FieldDefinition> filter = field -> field.getFieldType() == FieldTypes.TEXT
			|| field.getFieldType() == FieldTypes.FLOAT || field.getFieldType() == FieldTypes.NUMBER;

	private String myLastModified = "";
	private String imageKey;

	protected String[] personField = new String[] { "", "" };
	private int fileCounter;
	private int imageOption;

	private GeneralDB dbOut;
	private boolean isInputFileOpen = false;
	private boolean isOutputFileOpen = false;
	private boolean isNoImagePath = false;

	protected Map<String, Map<Integer, String>> myRoles = new HashMap<>(30);
	protected static PrefFNProg pdaSettings = PrefFNProg.getInstance();

	private Databases dbSettings = Databases.getInstance(TvBSoftware.FNPROG2PDA);
	private GeneralSettings generalSettings = GeneralSettings.getInstance();

	private static final String ROLE_ID = "RoleID";
	private static final String THUMB = "Thumb";

	private boolean isCatraxx;
	protected DatabaseFactory dbFactory = DatabaseFactory.getInstance();

	protected FNProgramvare() {
		super(pdaSettings);

		isNoImagePath = generalSettings.isNoImagePath();
		mySoftwareVersion = dbSettings.getDatabaseVersion();

		// Initialize "Global" variables
		exportImages = pdaSettings.isExportImages();
		useRoles = pdaSettings.isUseRoles();
		useCategory = !pdaSettings.getCategoryField().isEmpty();
		useContentsPerson = pdaSettings.isUseContentsPerson();
		useContentsOrigTitle = pdaSettings.isUseContentsOrigTitle();
		useContentsItemTitle = pdaSettings.isUseContentsItemTitle();

		imageKey = pdaSettings.getProfileID();
		imageOption = pdaSettings.getImageOption();
		fileCounter = 0;

		myLastIndex = pdaSettings.getLastIndex();
		myLastModified = pdaSettings.getLastModified();
		myImportFile = ExportFile.ACCESS;
	}

	public void setupDBTranslation(boolean isNew) {
		boolean isUserListError = false;

		msTable = dbFactory.getMSTable();
		myTable = msTable.getName();
		myTableID = msTable.getIndex();

		dbFieldDefinition = dbFactory.getDbFieldDefinition();
		dbInfoToExport.clear();
		dbInfoToExport.put(myTableID, dbFieldDefinition.get(myTableID));

		// Add the user's Export fields to the fields to export and to write
		dbUserFields = isNew ? new ArrayList<>() : pdaSettings.getUserList();
		List<String> usrList = new ArrayList<>();
		dbTableModelFields.clear();
		dbSortList.clear();

		for (BasisField field : dbUserFields) {
			FieldDefinition dbField = dbFieldDefinition.get(field.getFieldAlias());
			if (dbField == null) {
				isUserListError = true;
				continue;
			}

			dbField = dbField.copy();
			field.setFieldType(dbField.getFieldType()); // Just incase the type has changed
			dbField.set(field);
			dbTableModelFields.add(dbField);

			usrList.add(dbField.getFieldAlias());
			if (!dbInfoToExport.containsKey(dbField.getFieldAlias())) {
				dbInfoToExport.put(dbField.getFieldAlias(), dbField);
			}
		}

		if (isUserListError) {
			// Correct the list of selected user fields because they don't match the
			// database definition
			validateUserFields(usrList, false);
		}

		// Add special fields for list or XML to the fields to
		// export and to write
		for (String dbField : pdaSettings.getSpecialFields()) {
			if (addField(dbField, usrList, false, false) == null) {
				pdaSettings.removeSortField(dbField);
				pdaSettings.removeGroupField(dbField);
			}
		}

		// Add the Contents specific fields to the fields to export. They will be merged
		// in the "Contents Field", not individually exported
		for (String dbField : getContentsFields(usrList)) {
			addField(dbField, usrList, true, true);
		}

		// Add the System specific fields to the fields to export, but hide them in
		// xViewer and don't export them
		for (String dbField : getSystemFields(usrList)) {
			addField(dbField, usrList, true, false);
		}

		// Add the sort fields to the fields to export
		pdaSettings.getSortFields().forEach(dbField -> {
			FieldDefinition field = dbFieldDefinition.get(dbField);
			if (field != null) {
				addField(dbField, usrList, true, false);
				dbSortList.put(field.getFieldAlias(), field.getFieldType());
			}
		});
	}

	private FieldDefinition addField(String dbField, List<String> usrList, boolean isSystemField,
			boolean isContentsField) {
		FieldDefinition field = dbFieldDefinition.get(dbField);
		if (field != null && !dbInfoToExport.containsKey(field.getFieldAlias())) {
			usrList.add(dbField);
			field = field.copy();
			field.setExport(!isSystemField);
			field.setContentsField(isContentsField);
			dbInfoToExport.put(field.getFieldAlias(), field);
			dbTableModelFields.add(field);
		}
		return field;
	}

	public void openToFile() throws Exception {
		dbOut = GeneralDB.getDatabase(myExportFile, pdaSettings);
		dbOut.setSoftware(this);
		dbOut.openFile(new DatabaseHelper(pdaSettings.getExportFile(), myExportFile), false);
		isOutputFileOpen = true;
	}

	public GeneralDB getDbOut() {
		return dbOut;
	}

	/* Method to connect to the FNProgramvare Access database */
	public void openFile() throws Exception {
		dbFactory.connect2DB(new DatabaseHelper(dbSettings));
		dbFactory.verifyDatabase(dbSettings.getDatabaseFile());
		dbFactory.loadConfiguration(pdaSettings.getTableName());

		isInputFileOpen = true;
		isCatraxx = dbFactory.getDatabaseType() == FNPSoftware.CATRAXX;

		// Get the total number of records to process
		dbFactory.getMSAccess().readTableContents();
		msAccess = dbFactory.getMSAccess();
		msAccess.setSoftware(this);
		totalRecords = msAccess.getTotalRecords();
	}

	public void close() {
		if (isInputFileOpen) {
			dbFactory.close();
			isInputFileOpen = false;
		}

		if (isOutputFileOpen) {
			dbOut.closeFile();
			isOutputFileOpen = false;
		}
	}

	public void closeFiles(boolean delete) {
		if (!delete) {
			close();
			return;
		}

		if (isInputFileOpen) {
			dbFactory.close();
			isInputFileOpen = false;
		}

		if (isOutputFileOpen) {
			dbOut.closeFile();
			dbOut.deleteFile();
			isOutputFileOpen = false;
		}
	}

	public boolean isConnected() {
		if (isInputFileOpen && !dbFactory.isConnected()) {
			close();
		}

		return isInputFileOpen;
	}

	public void setCategories() throws Exception {
		if (useRoles && !msAccess.isIndexedSupported()) {
			useRoles = false;
		}

		final int MAX_CATEGORIES = LISTDB_MAX_CATEGORIES + 1;
		myCategories.clear();
		myCategories.add("Unfiled");

		if (!useCategory) {
			return;
		}

		List<Object> fieldData = dbFactory.getDbFieldValues(pdaSettings.getCategoryField());
		final int MAX_RECORDS = fieldData.size();

		int count = 1;
		for (int i = 0; i < MAX_RECORDS && count < MAX_CATEGORIES; i++, count++) {
			String category = fieldData.get(i).toString();
			if (StringUtils.isEmpty(category)) {
				continue;
			}

			if (category.length() > LISTDB_MAX_CATEGORY_LENGTH) {
				category = category.substring(0, LISTDB_MAX_CATEGORY_LENGTH);
			}

			myCategories.add(category);
		}
	}

	@Override
	protected List<Map<String, Object>> getDataListMap() throws Exception {
		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, List<Map<String, Object>>> hashTable = new HashMap<>();
		Set<Object> keywordList = new HashSet<>();
		Map<String, Object> hKeyword = new HashMap<>();
		Map<Integer, FieldDefinition> hFilterTable = new LinkedHashMap<>();

		cursor = null;
		setCurrentRecord(0);

		prepareFilters(hFilterTable, keywordList);

		Map<String, Object> map;
		Iterator<Row> iter1 = null;
		Iterator<Object> iter = null;

		boolean isFilterSubset = !keywordList.isEmpty();

		if (isFilterSubset) {
			totalRecords = keywordList.size();
			iter = keywordList.iterator();
		} else {
			if (cursor == null) {
				cursor = msAccess.getCursor(msTable.getName(), null, null, FilterOperator.IS_NOT_EQUAL_TO);
				if (cursor == null) {
					return result;
				}
			}
			iter1 = cursor.iterator();
		}

		while (true) {
			currentRecord++;

			hashTable.clear();
			Map<String, Object> dbRecord = new HashMap<>(); // stores a single record

			if (isFilterSubset) {
				if (!iter.hasNext()) {
					break;
				}
				hKeyword.put(myTableID, iter.next());
				map = msAccess.getSingleRecord(myTable, myTableID, hKeyword);
			} else {
				if (!iter1.hasNext()) {
					break;
				}
				map = iter1.next();
			}

			if (isNewRecords) {
				if (pdaSettings.getLastIndex() > ((Number) map.get(myTableID)).intValue()) {
					continue;
				}
			} else if (isNewModified) {
				boolean isOK = false;
				if (pdaSettings.getLastIndex() > 0) {
					isOK = pdaSettings.getLastIndex() < ((Number) map.get(myTableID)).intValue();
				}

				if (!isOK && !myLastModified.isEmpty() && map.containsKey("LastModified")) {
					Object obj = map.get("LastModified");
					if (obj == null || obj.equals("")) {
						continue;
					}

					String compDate = obj instanceof LocalDateTime
							? General.convertTimestamp((LocalDateTime) obj, General.sdInternalTimestamp)
							: obj.toString();
					if (compDate.compareTo(myLastModified) < 0) {
						continue;
					}
				}
			}

			if (isFilterDefined) {
				boolean[] isTrue = { true, true };

				for (Entry<Integer, FieldDefinition> entry : hFilterTable.entrySet()) {
					if (entry.getValue().getTable().equals(myTable)) {
						isTrue[entry.getKey()] = isIncludeRecord(map, entry.getValue(),
								pdaSettings.getFilterValue(entry.getKey()),
								pdaSettings.getFilterOperator(entry.getKey()));
					} else {
						isTrue[entry.getKey()] = isIncludeRecord(getLinkedRecords(map, hashTable, entry.getValue()),
								entry.getValue(), pdaSettings.getFilterValue(entry.getKey()),
								pdaSettings.getFilterOperator(entry.getKey()));
					}
				}

				if (!(pdaSettings.getFilterCondition().equals("AND") ? isTrue[0] && isTrue[1]
						: isTrue[0] || isTrue[1])) {
					// Filter returned false
					continue;
				}
			}

			for (FieldDefinition field : dbInfoToExport.values()) {
				if (field.getTable().equals(myTable)) {
					dbRecord.put(field.getFieldAlias(), msAccess.convertObject(map, field));
					continue;
				}

				dbRecord.put(field.getFieldAlias(), "");
				boolean isPersonRoles = useRoles && field.isRoleField();

				List<Map<String, Object>> list = getLinkedRecords(map, hashTable, field);
				if (list.isEmpty()) {
					continue;
				}

				if (field.getFieldType() == FieldTypes.IMAGE || field.getFieldType() == FieldTypes.THUMBNAIL) {
					convertImage(field.getFieldAlias(), dbRecord, list);
				} else if (field.isContentsField()) {
					if (!field.getFieldAlias().equals(field.getFieldName())) {
						for (Map<String, Object> lObj : list) {
							lObj.put(field.getFieldAlias(), lObj.get(field.getFieldName()));
						}
					}
				} else if (field.isRoleField() || list.size() > 1) {
					String separator = field.isRoleField() ? " & "
							: field.getFieldType() == FieldTypes.MEMO ? "\n" : "; ";
					StringBuilder buf = new StringBuilder(100);

					for (Map<String, Object> lObj : list) {
						String s = msAccess.convertObject(lObj, field).toString();
						if (isPersonRoles) {
							String role = getPersonRole(field.getTable(), (Number) lObj.get(ROLE_ID));
							if (role.isEmpty()) {
								buf.append(s).append(separator);
							} else {
								buf.append(s).append(" ").append(role).append(" ");
							}
						} else {
							buf.append(s).append(separator);
						}
					}

					int lastChar = buf.lastIndexOf(separator);
					if (lastChar != -1) {
						buf.delete(lastChar, buf.length());
					}

					dbRecord.put(field.getFieldAlias(), buf.toString().trim());
				} else {
					dbRecord.put(field.getFieldAlias(), msAccess.convertObject(list.get(0), field));
				}
			}

			setTableRecord(dbRecord, hashTable, result);
		}

		if (!dbSortList.isEmpty()) {
			Collections.sort(result, new XComparator(dbSortList));
		}
		return result;
	}

	private List<Map<String, Object>> getLinkedRecords(Map<String, Object> map,
			Map<String, List<Map<String, Object>>> hashTable, FieldDefinition field) throws Exception {
		String key = field.getTable();
		MSTable table = dbFactory.getMSTable(key);

		boolean isPersonRoles = useRoles && field.isRoleField();

		if (!field.getIndexField().isEmpty()) {
			table.getColumnValues().put(field.getIndexField(), field.getIndexValue());
			key += field.getIndexValue();
		}

		List<Map<String, Object>> result = getMainTableRecord(table, map, hashTable, key);
		if (!result.isEmpty()) {
			return result;
		}

		String linkKey = table.getFromTable();
		MSTable link = dbFactory.getMSTable(linkKey);

		List<Map<String, Object>> linkList = getMainTableRecord(link, map, hashTable, linkKey);
		hashTable.put(key, result);

		if (linkList.isEmpty()) {
			// Shit, now it gets really complicated since our linked table isn't directly
			// linked to the main table
			String linkedKey = link.getFromTable();
			if (linkedKey.isEmpty()) {
				// No link found
				throw FNProgException.getException("noForeignKey", key);

			}

			linkList = new ArrayList<>();
			MSTable linkedLink = dbFactory.getMSTable(linkedKey);
			List<Map<String, Object>> linkedList = getMainTableRecord(linkedLink, map, hashTable, linkedKey);
			if (!linkedList.isEmpty()) {
				for (Map<String, Object> linkMap : linkedList) {
					if (link.setColumnValues(linkMap)) {
						List<Map<String, Object>> lList = msAccess.getMultipleRecords(link.getName(), link.getIndex(),
								link.getColumnValues());
						if (!lList.isEmpty()) {
							linkList.addAll(lList);
						} else if (field.isRoleField()) {
							Map<String, Object> lObj = new LinkedHashMap<>();
							lObj.put("AtEnd", true);
							linkList.add(lObj);
						}
					}
				}
			}
		}

		hashTable.put(linkKey, linkList);

		for (Map<String, Object> linkMap : linkList) {
			if (linkMap.get("AtEnd") != null) {
				result.add(linkMap);
				continue;
			}

			if (table.setColumnValues(linkMap)) {
				List<Map<String, Object>> list = msAccess.getMultipleRecords(table.getName(), table.getIndex(),
						table.getColumnValues());
				if (list == null || list.isEmpty()) {
					continue;
				}

				for (Map<String, Object> lObj : list) {
					if (isPersonRoles) {
						lObj.put(ROLE_ID, linkMap.get(ROLE_ID));
					}

					if (link.isNoDupIndex()) {
						// Verify if the "main table" doesn't have the same index value as the linked
						// table
						Object index1 = map.get(link.getIndex());
						Object index2 = linkMap.get(table.getFromIndex());
						if (index1.equals(index2)) {
							continue;
						}
					}

					result.add(lObj);
				}
			}
		}
		return result;
	}

	private List<Map<String, Object>> getMainTableRecord(MSTable table, Map<String, Object> map,
			Map<String, List<Map<String, Object>>> hashTable, String tableKey) throws Exception {
		if (table == null) {
			throw FNProgException.getException("noTable", tableKey);
		}

		List<Map<String, Object>> result = hashTable.get(tableKey);
		if (result != null) {
			return result;
		}

		if (table.getFromTable().equals(myTable)) {
			if (table.setColumnValues(map)) {
				result = msAccess.getMultipleRecords(table.getName(), table.getIndex(), table.getColumnValues(),
						table.isMultiColumnIndex());
			} else {
				result = new ArrayList<>();
			}
			hashTable.put(tableKey, result);
			return result;
		}
		return new ArrayList<>();
	}

	private void convertImage(String field, Map<String, Object> dbRecord, List<Map<String, Object>> list)
			throws Exception {
		if (list.isEmpty()) {
			dbRecord.put(field, "");
		}

		Map<String, Object> map = list.get(0);
		boolean result = false;

		if (field.startsWith(THUMB) || !((boolean) map.get("ImageExternal"))) {
			result = check4InternalImage(field, dbRecord, map);
		} else {
			result = check4ExternalImage(field, dbRecord, map);
		}

		if (!result) {
			dbRecord.put(field, "");
		}
	}

	// If Cover field is selected check whether we have load the Image from an
	// external file
	private boolean check4ExternalImage(String field, Map<String, Object> dbRecord, Map<String, Object> map)
			throws Exception {
		String imageFilename = (String) map.get("ImageFilename");
		if (imageFilename != null && !imageFilename.isEmpty()) {
			// Return fully qualified external filename
			if (exportImages) {
				// Load image from external file (works with JPEG and GIF files only ?)
				if (General.existFile(imageFilename)) {
					try {
						BufferedImage image = ImageIO.read(new File(imageFilename));
						if (convertImage(field, image, dbRecord)) {
							return true;
						}
					} catch (IllegalArgumentException e) {
						// File format is invalid
						return false;
					}
				} else {
					return false;
				}
			} else {
				dbRecord.put(field,
						isNoImagePath ? imageFilename.substring(imageFilename.lastIndexOf('\\') + 1) : imageFilename);
			}
		}
		return true;
	}

	private boolean check4InternalImage(String field, Map<String, Object> dbRecord, Map<String, Object> map)
			throws Exception {
		if (exportImages) {
			Object obj = map.get(field.startsWith(THUMB) ? "ImageThumbnail" : "Image");
			if (obj != null) {
				try {
					BufferedImage image = ImageIO.read(new ByteArrayInputStream((byte[]) obj));
					if (convertImage(field, image, dbRecord)) {
						return true;
					}
				} catch (IllegalArgumentException e) {
					// Image format is not valid
				}
			}
		}
		return false;
	}

	private boolean convertImage(String field, BufferedImage image, Map<String, Object> dbRecord) throws Exception {
		String[] types = { ".bmp", ".jpg", ".png" };

		StringBuilder buf = new StringBuilder(100);
		buf.append(generalSettings.getDefaultImageFolder());
		buf.append("/");
		buf.append(imageKey);
		buf.append("_");
		buf.append(field);
		buf.append("_");
		buf.append(fileCounter++);
		buf.append(types[pdaSettings.getImageOption()]);

		if (General.convertImage(image, myExportFile, pdaSettings, buf.toString(), field.startsWith(THUMB))) {
			if (imageOption != 0 && myExportFile == ExportFile.HANDBASE) {
				buf.delete(0, generalSettings.getDefaultImageFolder().length());
				buf.insert(0, generalSettings.getDefaultPdaFolder());
			}
			dbRecord.put(field, buf.toString());
			return true;
		}
		return false;
	}

	private void prepareFilters(Map<Integer, FieldDefinition> hFilterTable, Set<Object> idSet) throws Exception {
		isSkipFirstFilter = false;
		isSkipLastFilter = numFilter == 1;
		isNewRecords = generalSettings.isNewExport() && myLastIndex > 0;
		isNewModified = generalSettings.isIncrementalExport() && (myLastIndex > 0 || !myLastModified.isEmpty());

		if (generalSettings.isNoFilterExport()) {
			if (isNewRecords) {
				Object obj = myLastIndex;
				try {
					cursor = msAccess.getCursor(myTable, myTableID, Collections.singletonMap(myTableID, obj),
							FilterOperator.IS_GREATER_THAN);
					isNewRecords = false;
				} catch (Exception e) {
					// Log the error
				}
			}
		} else {
			setContentsFilter();
			setKeywordFilter(idSet);
			if (idSet.isEmpty() && isFilterDefined) {
				setStandardFilter(idSet);
			}

			if (isFilterDefined) {
				ArrayList<Integer> hTemp = new ArrayList<>();
				if (isSkipLastFilter) {
					hTemp.add(0);
				} else if (isSkipFirstFilter) {
					hTemp.add(1);
				} else {
					hTemp.add(0);
					hTemp.add(1);
				}
				for (int i : hTemp) {
					hFilterTable.put(i, dbFactory.getDbFieldDefinition().get(pdaSettings.getFilterField(i)));
				}
			}
		}
	}

	private void setContentsFilter() {
		if (useContents && !pdaSettings.getContentsFilter().isEmpty()) {
			try {
				Map<String, Object> map = msAccess.getSingleRecord("ContentsType", null,
						Collections.singletonMap("ContentsType", pdaSettings.getContentsFilter()));
				if (!map.isEmpty()) {
					MSTable table = dbFactory.getMSTable("Contents");
					table.getColumnValues().put("TypeID", map.get("ContentsTypeID"));
				}
			} catch (Exception e) {
				// Log error
			}
		}
	}

	private boolean setKeywordFilter(Set<Object> idSet) throws Exception {
		final String KEYWORD = "Keyword";
		final String KEYWORD_ID = "KeywordID";

		idSet.clear();
		if (!pdaSettings.getKeywordFilter().isEmpty()) {
			Object kwFilter = pdaSettings.getKeywordFilter();

			MSTable table = dbFactory.getMSTable(KEYWORD);
			Map<String, Object> map = msAccess.getSingleRecord(KEYWORD, table.isIndexedColumn(KEYWORD) ? KEYWORD : null,
					Collections.singletonMap(KEYWORD, kwFilter));
			if (!map.isEmpty() && !setIdFilter(idSet, msAccess.getMultipleRecords(table.getFromTable(), KEYWORD_ID,
					Collections.singletonMap(KEYWORD_ID, map.get(KEYWORD_ID))))) {
				pdaSettings.setKeywordFilter("");
				return false;
			}

			return true;
		}
		return false;
	}

	private boolean setStandardFilter(Set<Object> idSet) throws Exception {
		if (pdaSettings.getFilterCondition().equals("OR")) {
			// This is a bit too complex to handle, we'll use the default filter method
			// instead
			return false;
		}

		FilterOperator[] oper = new FilterOperator[2];
		oper[0] = pdaSettings.getFilterOperator(0);
		oper[1] = pdaSettings.getFilterField(1).isEmpty() ? FilterOperator.IS_NOT_EQUAL_TO
				: pdaSettings.getFilterOperator(1);

		if (oper[0] == FilterOperator.IS_NOT_EQUAL_TO && oper[1] == FilterOperator.IS_NOT_EQUAL_TO) {
			// a bit too complex, we'll use the default filter method instead
			return false;
		}

		int idx = oper[1] == FilterOperator.IS_EQUAL_TO ? 1 : 0;
		int index = oper[0] == FilterOperator.IS_EQUAL_TO ? 0 : idx;
		FieldDefinition field = dbFieldDefinition.get(pdaSettings.getFilterField(index));
		MSTable table = dbFactory.getMSTable(field.getTable());
		boolean isIndexColumn = table.isIndexedColumn(field.getFieldName());

		if (!isIndexColumn && table.getName().equals(myTable)) {
			// Try the other filter
			index = index == 0 ? 1 : 0;
			if (oper[index] == FilterOperator.IS_NOT_EQUAL_TO) {
				return false;
			}

			field = dbFieldDefinition.get(pdaSettings.getFilterField(index));
			table = dbFactory.getMSTable(field.getTable());
			isIndexColumn = table.isIndexedColumn(field.getFieldName());
			if (!isIndexColumn && table.getName().equals(myTable)) {
				return false;
			}
		}

		Map<String, Object> hFilter = new HashMap<>();
		hFilter.put(field.getFieldName(), pdaSettings.getFilterValue(index));

		if (field.getTable().equals(myTable)) {
			cursor = msAccess.getCursor(myTable, field.getFieldName(), hFilter, oper[index]);
			setFilterStatus(index);
			return false;
		}

		MSTable link = dbFactory.getMSTable(table.getFromTable());
		String indexName = table.getFromIndex().isEmpty() ? table.getIndex() : table.getFromIndex();
		if (!link.isIndexedColumn(indexName)) {
			return false;
		}

		if (oper[index] != FilterOperator.IS_EQUAL_TO) {
			if (!isIndexColumn) {
				return false;
			}

			List<Map<String, Object>> list1 = msAccess.getMultipleRecords(table.getName(), field.getFieldName(),
					hFilter, false, oper[index]);

			if (!list1.isEmpty()) {
				setFilterStatus(index);
				hFilter.clear();
				for (Map<String, Object> map : list1) {
					hFilter.put(table.getFromIndex(), map.get(table.getFromIndex()));
					setIdFilter(idSet, msAccess.getMultipleRecords(link.getName(), table.getFromIndex(), hFilter));
				}
				return !idSet.isEmpty();
			}

			return false;
		}

		Map<String, Object> map = msAccess.getSingleRecord(table.getName(), isIndexColumn ? field.getFieldName() : null,
				hFilter);
		if (!map.isEmpty()) {
			setFilterStatus(index);
			hFilter.clear();
			hFilter.put(indexName, map.get(table.getIndex()));

			if (table.getFromTable().equals(myTable)) {
				cursor = msAccess.getCursor(myTable, indexName, hFilter, FilterOperator.IS_EQUAL_TO);
			} else {
				isFilterDefined = true;
			}
		}
		return false;
	}

	private void setFilterStatus(int index) {
		if (numFilter == 1) {
			isFilterDefined = false;
		} else {
			isSkipFirstFilter = index == 0;
			isSkipLastFilter = !isSkipFirstFilter;
		}
	}

	private boolean setIdFilter(Set<Object> idSet, List<Map<String, Object>> list) {
		if (CollectionUtils.isEmpty(list)) {
			return false;
		}

		for (Map<String, Object> keyMap : list) {
			Object obj = keyMap.get(myTableID);
			if (obj != null) {
				idSet.add(keyMap.get(myTableID));
			}
		}
		return !idSet.isEmpty();
	}

	private void setTableRecord(Map<String, Object> pRead, Map<String, List<Map<String, Object>>> hashTable,
			List<Map<String, Object>> pWrite) throws Exception {

		// Additional formating for Contents, Tracks, Segments, Authors, etc.
		setDatabaseData(pRead, hashTable);
		pWrite.add(pRead);

		dbTableModelFields.stream().filter(filter)
				.forEach(field -> field.setSize(pRead.getOrDefault(field.getFieldAlias(), "")));
	}

	public void checkNumberOfFields(boolean isExport, ViewerModel model) throws Exception {
		int userFields = !isExport ? model.getColumnCount() : dbInfoToWrite.size();

		if (userFields == 0) {
			throw FNProgException.getException("noFieldsDefined", myExportFile.getName());
		}

		if (isExport) {
			totalRecords = model == null ? 0 : model.getRowCount();
			if (userFields > myExportFile.getMaxFields()) {
				throw FNProgException.getException("maxFieldsOverride", Integer.toString(userFields),
						myExportFile.getName(), Integer.toString(myExportFile.getMaxFields()));
			}
		}

		// Check if there are any records to process
		if (totalRecords == 0) {
			throw FNProgException.getException("noRecordsFound", pdaSettings.getProfileID());
		}

		verifyFilter();
	}

	public void runConversionProgram(Component parent) throws Exception {
		if (myExportFile == ExportFile.HANDBASE) {
			((HanDBase) dbOut).runConversionProgram(pdaSettings);
		}

		if (myExportFile == ExportFile.TEXTFILE) {
			General.showMessage(parent,
					GUIFactory.getMessage("createdFiles",
							((CsvFile) dbOut).getExportFiles(GUIFactory.getText("file"), GUIFactory.getText("files"))),
					GUIFactory.getTitle("information"), false);
		} else {
			General.showMessage(parent, GUIFactory.getMessage("createdFile", pdaSettings.getExportFile()),
					GUIFactory.getTitle("information"), false);
		}

		// Save last export date and record
		if (myLastIndex > 0) {
			pdaSettings.setLastIndex(myLastIndex);
			pdaSettings.setLastModified(General.convertTimestamp(LocalDateTime.now(), General.sdInternalTimestamp));
		}
	}

	public DatabaseFactory getDatabaseFactory() {
		return dbFactory;
	}

	protected void getRoles(String table, String roleTable, String roleID) throws Exception {
		List<Map<String, Object>> list = msAccess.getMultipleRecords(roleTable, roleID);
		Map<Integer, String> personMap = new HashMap<>();
		myRoles.put(table, personMap);

		if (!list.isEmpty()) {
			for (Map<String, Object> map : list) {
				personMap.put((Integer) map.get(roleID), map.get(roleTable).toString());
			}
		}
	}

	public String getPersonRole(String table, Number pRoleID) {
		if (pRoleID != null && pRoleID.intValue() > 0) {
			Map<Integer, String> map = myRoles.get(table);
			if (map != null) {
				String role = map.get(pRoleID.intValue());
				if (role != null) {
					return isCatraxx ? role : "[" + role + "]";
				}
			}
		}
		return "";
	}

	protected Map<Integer, List<Map<String, Object>>> getContentsPersonIndex(List<Map<String, Object>> linkList,
			List<Map<String, Object>> personList, String key) {
		Map<Integer, List<Map<String, Object>>> result = new HashMap<>();

		if (linkList == null || personList == null) {
			return result;
		}

		for (int i = 0; i < linkList.size(); i++) {
			Integer cIndex = (Integer) linkList.get(i).get(key);
			List<Map<String, Object>> mapPerson = result.computeIfAbsent(cIndex, ArrayList::new);
			mapPerson.add(personList.get(i));
		}
		return result;
	}

	protected String getContentsPerson(List<Map<String, Object>> personList) {
		if (CollectionUtils.isEmpty(personList) || !useContentsPerson) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Map<String, Object> mapPerson : personList) {
			String persons = (String) mapPerson.get(usePersonSort ? "SortBy" : "Name");
			if (StringUtils.isNotEmpty(persons)) {
				if (useRoles) {
					String role = getPersonRole(personField[0], (Number) mapPerson.get(ROLE_ID));
					if (role.isEmpty()) {
						sb.append(persons).append(" & ");
					} else {
						sb.append(persons).append(" ").append(role).append(" ");
					}
				} else {
					sb.append(persons);
					sb.append(" & ");
				}
			}
		}

		int lastChar = sb.lastIndexOf("&");
		if (lastChar != -1) {
			sb.deleteCharAt(lastChar);
		}
		return sb.toString().trim();
	}

	public static FNProgramvare getSoftware(FNPSoftware soft) {
		switch (soft) {
		case ASSETCAT:
			return new AssetCAT();
		case BOOKCAT:
			return new BookCAT();
		case CATRAXX:
			return new CATraxx();
		case CATVIDS:
			return new CATVids();
		case SOFTCAT:
			return new SoftCAT();
		case STAMPCAT:
			return new StampCAT();
		default:
			return null;
		}
	}

	protected void addToList(StringBuilder newLine, StringBuilder result) {
		result.append(newLine).append("\n");
		newLine.setLength(0);
	}

	protected List<String> getSystemFields(List<String> userFields) {
		// Nothing to do on this level
		return new ArrayList<>(userFields);
	}

	protected List<String> getContentsFields(List<String> userFields) {
		// Nothing to do on this level
		return new ArrayList<>(userFields);
	}

	protected abstract void setDatabaseData(Map<String, Object> dbDataRecord,
			Map<String, List<Map<String, Object>>> hashTable) throws Exception;
}