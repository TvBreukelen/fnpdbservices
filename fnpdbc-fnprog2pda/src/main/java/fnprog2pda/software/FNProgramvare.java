package fnprog2pda.software;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import javax.swing.ImageIcon;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Row;

import application.BasicSoft;
import application.interfaces.ExportFile;
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
import dbengine.utils.DatabaseHelper;
import fnprog2pda.dbengine.MSAccess;
import fnprog2pda.dbengine.utils.MSTable;
import fnprog2pda.dialog.ExportProcess;
import fnprog2pda.preferences.PrefFNProg;

public abstract class FNProgramvare extends BasicSoft {
	/**
	 * Title: FNProgramvare Description: Abstract Class for FNProgramvare software
	 * Copyright: (c) 2003-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	protected String mySoftwareVersion = General.EMPTY_STRING;
	protected int lastIndex = 0;

	protected String myTable;
	protected String myTableID;

	private boolean useCategory;

	private boolean isSkipFirstFilter;
	private boolean isSkipLastFilter;
	private boolean isNewModified;
	private boolean isNewRecords;

	protected boolean useRoles;
	protected boolean useContents;
	protected boolean useContentsPerson;
	protected boolean useContentsOrigTitle;
	protected boolean useContentsItemTitle;

	private MSTable msTable;
	private Cursor cursor;
	protected MSAccess msAccess;

	private Map<String, FieldDefinition> dbInfoToExport = new HashMap<>(); // DB definitions of the fields to be
	// exported
	private Map<String, FieldTypes> dbSortList = new LinkedHashMap<>(); // Sort by field -number and -type
	private Predicate<FieldDefinition> filter = field -> field.getFieldType() == FieldTypes.TEXT
			|| field.getFieldType() == FieldTypes.FLOAT || field.getFieldType() == FieldTypes.NUMBER;

	private String lastExported = General.EMPTY_STRING;

	protected String[] personField = new String[] { General.EMPTY_STRING, General.EMPTY_STRING };

	private boolean isInputFileOpen = false;
	private boolean isOutputFileOpen = false;

	protected Map<String, Map<Integer, String>> myRoles = new HashMap<>(30);
	protected static PrefFNProg mySettings = PrefFNProg.getInstance();

	private Databases dbSettings = Databases.getInstance(TvBSoftware.FNPROG2PDA);
	private GeneralSettings generalSettings = GeneralSettings.getInstance();

	private static final String ROLE_ID = "RoleID";
	private static final String THUMB = "Thumb";

	protected DatabaseFactory dbFactory = DatabaseFactory.getInstance();
	public static FNPSoftware whoAmI;

	protected FNProgramvare() {
		super(mySettings);
		mySoftwareVersion = dbSettings.getDatabaseVersion();

		// Initialize "Global" variables
		useRoles = mySettings.isUseRoles();
		useCategory = !mySettings.getCategoryField().isEmpty();
		useContentsPerson = mySettings.isUseContentsPerson();
		useContentsOrigTitle = mySettings.isUseContentsOrigTitle();
		useContentsItemTitle = mySettings.isUseContentsItemTitle();

		lastIndex = mySettings.getLastIndex();
		lastExported = mySettings.getLastExported();
		myImportFile = ExportFile.ACCESS;
	}

	public void setupDBTranslation(boolean isNew) {
		msTable = dbFactory.getMSTable();
		myTable = msTable.getName();
		myTableID = msTable.getIndex();

		dbFieldDefinition = dbFactory.getDbFieldDefinition();
		dbInfoToExport.clear();
		dbInfoToExport.put(myTableID, dbFieldDefinition.get(myTableID));

		// Add the user's Export fields to the fields to export and to write
		dbUserFields = isNew ? new ArrayList<>() : mySettings.getUserList();
		List<String> usrList = new ArrayList<>();
		dbSortList.clear();

		boolean isUserListError = verifyUserfields(usrList, dbInfoToExport);

		if (isUserListError) {
			// Correct the list of selected user fields because they don't match the
			// database definition
			validateUserFields(usrList, false);
		}

		// Verify sort fields
		verifySortFields();

		// Add special fields for list or XML to the fields to
		// export and to write
		for (String dbField : mySettings.getSpecialFields()) {
			if (addField(dbField, usrList, false, false) == null) {
				mySettings.removeSortField(dbField);
				mySettings.removeGroupField(dbField);
			}
		}

		for (BasisField dbField : getBuddyFields()) {
			// Check if the "Buddy" field isn't assigned yet
			if (dbInfoToExport.values().stream()
					.noneMatch(field -> field.getFieldHeader().equals(dbField.getFieldHeader()))) {

				// We can safely add the new field
				FieldDefinition field = addField(dbField.getFieldAlias(), usrList, false, false);
				if (field != null) {
					field.setFieldHeader(dbField.getFieldHeader());
				}
			}
		}

		// Add the Contents specific fields to the fields to export. They will be merged
		// in the "Contents Field", not individually exported
		getContentsFields(usrList).forEach(dbField -> addField(dbField, usrList, true, true));

		// Add (optional) sort fields to extend and enforce sorting
		getMandatorySortFields(mySettings.getSortFields()).forEach(dbField -> addSortField(usrList, dbField));

		// Add the System specific fields to the fields to export, but hide them in
		// xViewer and don't export them
		getSystemFields(usrList).forEach(dbField -> addField(dbField, usrList, true, false));
	}

	protected List<String> getMandatorySortFields(List<String> sortList) {
		return sortList;
	}

	private void addSortField(List<String> usrList, String dbField) {
		FieldDefinition field = dbFieldDefinition.get(dbField);
		if (field != null) {
			addField(dbField, usrList, true, false);
			dbSortList.putIfAbsent(field.getFieldAlias(), field.getFieldType());
		}
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

			if (!(isSystemField || isContentsField)) {
				dbUserFields.add(field);
			}
		}
		return field;
	}

	@Override
	public void openToFile() throws Exception {
		dbOut = ExportProcess.getDatabase(myExportFile, mySettings);
		dbOut.setSoftware(this);
		dbOut.openFile(new DatabaseHelper(mySettings.getExportFile(), myExportFile), false);
		isOutputFileOpen = true;
	}

	public GeneralDB getDbOut() {
		return dbOut;
	}

	/* Method to connect to the FNProgramvare Access database */
	public void openFile() throws Exception {
		dbFactory.connect2DB(new DatabaseHelper(dbSettings));
		dbFactory.verifyDatabase(dbSettings.getDatabase());
		dbFactory.loadConfiguration(mySettings.getTableName());

		isInputFileOpen = true;

		// Get the total number of records to process
		dbFactory.getInputFile().readTableContents();
		msAccess = (MSAccess) dbFactory.getInputFile();
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

		List<Object> fieldData = dbFactory.getFilterFieldValues(mySettings.getCategoryField());
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

			int index = ((Number) map.get(myTableID)).intValue();
			if (mySettings.getLastIndex() >= index) {
				boolean isOK = true;
				if (isNewModified && !lastExported.isEmpty()) {
					Object obj = map.getOrDefault("LastModified", General.EMPTY_STRING);
					isOK = obj instanceof LocalDateTime;
					if (isOK) {
						String compDate = General.convertTimestamp((LocalDateTime) obj, General.sdInternalTimestamp);
						isOK = compDate.compareTo(lastExported) > 0;
					}
				}

				if (!isOK || isNewRecords) {
					continue;
				}
			}

			if (isFilterDefined) {
				boolean[] isTrue = { true, true };

				for (Entry<Integer, FieldDefinition> entry : hFilterTable.entrySet()) {
					if (entry.getValue().getTable().equals(myTable)) {
						isTrue[entry.getKey()] = isIncludeRecord(map, entry.getValue(),
								mySettings.getFilterValue(entry.getKey()),
								mySettings.getFilterOperator(entry.getKey()));
					} else {
						isTrue[entry.getKey()] = isIncludeRecord(getLinkedRecords(map, hashTable, entry.getValue()),
								entry.getValue(), mySettings.getFilterValue(entry.getKey()),
								mySettings.getFilterOperator(entry.getKey()));
					}
				}

				if (!(mySettings.getFilterCondition().equals("AND") ? isTrue[0] && isTrue[1]
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

				dbRecord.put(field.getFieldAlias(), General.EMPTY_STRING);
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
					String separator = field.isRoleField() ? " & " : "; ";
					if (field.getFieldType() == FieldTypes.MEMO) {
						separator = "\n";
					}
					StringBuilder buf = new StringBuilder(100);
					StringBuilder bufPerson = new StringBuilder(100); // For Book & MusicBuddy

					for (Map<String, Object> lObj : list) {
						String s = msAccess.convertObject(lObj, field).toString();
						if (isPersonRoles) {
							Number roleID = (Number) lObj.getOrDefault(ROLE_ID, -1);
							String role = getPersonRole(field.getTable(), roleID);
							if (role.isEmpty()) {
								buf.append(s).append(separator);
							} else {
								buf.append(s).append(General.SPACE).append(role).append(separator);
							}
							if (roleID != null && roleID.intValue() != -1) {
								bufPerson.append(s).append("[").append(roleID.intValue()).append("]; ");
							}
						} else {
							buf.append(s).append(separator);
						}
					}

					int lastChar = buf.lastIndexOf(separator);
					if (lastChar != -1) {
						buf.delete(lastChar, buf.length());
					}

					if (bufPerson.length() > 2) {
						bufPerson.delete(bufPerson.length() - 2, bufPerson.length());
						dbRecord.put(field.getFieldAlias() + "Credits", bufPerson.toString().trim());
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
						}
					}
				}
			}
		}

		hashTable.put(linkKey, linkList);

		for (Map<String, Object> linkMap : linkList) {
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
			dbRecord.put(field, General.EMPTY_STRING);
		}

		Map<String, Object> map = list.get(0);
		boolean result = false;

		if (field.startsWith(THUMB) || !((boolean) map.get("ImageExternal"))) {
			result = check4InternalImage(field, dbRecord, map);
		} else {
			result = check4ExternalImage(field, dbRecord, map);
		}

		if (!result) {
			dbRecord.put(field, General.EMPTY_STRING);
		}
	}

	// If image field is selected check whether we have load the image from an
	// external file
	private boolean check4ExternalImage(String field, Map<String, Object> dbRecord, Map<String, Object> map) {
		String imageFilename = (String) map.get("ImageFilename");
		if (StringUtils.isEmpty(imageFilename) || !General.existFile(imageFilename)) {
			return false;
		}

		if (mySettings.isExportImages()) {
			// Load image from external file
			try {
				dbRecord.put(field, new ImageIcon(imageFilename));
			} catch (IllegalArgumentException e) {
				// File format is invalid
				return false;
			}
		} else {
			dbRecord.put(field,
					generalSettings.isNoImagePath() ? imageFilename.substring(imageFilename.lastIndexOf('\\') + 1)
							: imageFilename);
		}
		return true;
	}

	private boolean check4InternalImage(String field, Map<String, Object> dbRecord, Map<String, Object> map) {
		if (!mySettings.isExportImages()) {
			return false;
		}

		Object obj = map.get(field.startsWith(THUMB) ? "ImageThumbnail" : "Image");
		if (obj instanceof byte[] byteArray) {
			try {
				BufferedImage image = ImageIO.read(new ByteArrayInputStream(byteArray));
				dbRecord.put(field, new ImageIcon(image));
			} catch (IOException e) {
				// Image format is not valid
				return false;
			}
		}
		return true;

	}

	private void prepareFilters(Map<Integer, FieldDefinition> hFilterTable, Set<Object> idSet) throws Exception {
		isSkipFirstFilter = false;
		isSkipLastFilter = numFilter == 1;
		int index = mySettings.getLastIndex();

		isNewRecords = generalSettings.isNewExport() && index > 0;
		isNewModified = generalSettings.isIncrementalExport() && (index > 0 || !lastExported.isEmpty());

		if (generalSettings.isNoFilterExport()) {
			if (isNewRecords) {
				Object obj = index;
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
					hFilterTable.put(i, dbFactory.getDbFieldDefinition().get(mySettings.getFilterField(i)));
				}
			}
		}
	}

	private void setContentsFilter() {
		if (useContents && !mySettings.getContentsFilter().isEmpty()) {
			try {
				Map<String, Object> map = msAccess.getSingleRecord("ContentsType", null,
						Collections.singletonMap("ContentsType", mySettings.getContentsFilter()));
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
		if (!mySettings.getKeywordFilter().isEmpty()) {
			Object kwFilter = mySettings.getKeywordFilter();

			MSTable table = dbFactory.getMSTable(KEYWORD);
			Map<String, Object> map = msAccess.getSingleRecord(KEYWORD, table.isIndexedColumn(KEYWORD) ? KEYWORD : null,
					Collections.singletonMap(KEYWORD, kwFilter));
			if (!map.isEmpty() && !setIdFilter(idSet, msAccess.getMultipleRecords(table.getFromTable(), KEYWORD_ID,
					Collections.singletonMap(KEYWORD_ID, map.get(KEYWORD_ID))))) {
				mySettings.setKeywordFilter(General.EMPTY_STRING);
				return false;
			}

			return true;
		}
		return false;
	}

	private boolean setStandardFilter(Set<Object> idSet) throws Exception {
		if (mySettings.getFilterCondition().equals("OR")) {
			// This is a bit too complex to handle, we'll use the default filter method
			// instead
			return false;
		}

		FilterOperator[] oper = new FilterOperator[2];
		oper[0] = mySettings.getFilterOperator(0);
		oper[1] = mySettings.getFilterField(1).isEmpty() ? FilterOperator.IS_NOT_EQUAL_TO
				: mySettings.getFilterOperator(1);

		if (oper[0] == FilterOperator.IS_NOT_EQUAL_TO && oper[1] == FilterOperator.IS_NOT_EQUAL_TO) {
			// a bit too complex, we'll use the default filter method instead
			return false;
		}

		int idx = oper[1] == FilterOperator.IS_EQUAL_TO ? 1 : 0;
		int index = oper[0] == FilterOperator.IS_EQUAL_TO ? 0 : idx;
		FieldDefinition field = dbFieldDefinition.get(mySettings.getFilterField(index));
		MSTable table = dbFactory.getMSTable(field.getTable());
		boolean isIndexColumn = table.isIndexedColumn(field.getFieldName());

		if (!isIndexColumn && table.getName().equals(myTable)) {
			// Try the other filter
			index = index == 0 ? 1 : 0;
			if (oper[index] == FilterOperator.IS_NOT_EQUAL_TO) {
				return false;
			}

			field = dbFieldDefinition.get(mySettings.getFilterField(index));
			table = dbFactory.getMSTable(field.getTable());
			isIndexColumn = table.isIndexedColumn(field.getFieldName());
			if (!isIndexColumn && table.getName().equals(myTable)) {
				return false;
			}
		}

		Map<String, Object> hFilter = new HashMap<>();
		hFilter.put(field.getFieldName(), mySettings.getFilterValue(index));

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

		// Verify if the record to write contains any values
		if (pRead.isEmpty() || mySettings.isSkipEmptyRecords() && dbInfoToWrite.stream().noneMatch(field -> !pRead
				.getOrDefault(field.getFieldAlias(), General.EMPTY_STRING).equals(General.EMPTY_STRING))) {
			return;
		}

		pWrite.add(pRead);
		dbInfoToWrite.stream().filter(filter)
				.forEach(field -> field.setSize(pRead.getOrDefault(field.getFieldAlias(), General.EMPTY_STRING)));
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
			throw FNProgException.getException("noRecordsFound", mySettings.getProfileID());
		}

		verifyFilter();
	}

	@Override
	public void runConversionProgram(Component parent) throws Exception {
		super.runConversionProgram(parent);

		// Save last export record
		if (lastIndex > 0) {
			int prevIndex = mySettings.getLastIndex();
			mySettings.setLastIndex(Math.max(lastIndex, prevIndex));
		}
	}

	public DatabaseFactory getDatabaseFactory() {
		return dbFactory;
	}

	protected void getRoles(String table, String roleTable, String roleID) throws Exception {
		List<Map<String, Object>> list = msAccess.getMultipleRecords(roleTable, roleID);
		Map<Integer, String> personMap = new HashMap<>();
		myRoles.put(table, personMap);
		list.forEach(map -> personMap.put((Integer) map.get(roleID), map.get(roleTable).toString()));
	}

	public String getPersonRole(String table, Number pRoleID) {
		if (pRoleID != null && pRoleID.intValue() > 0) {
			Map<Integer, String> map = myRoles.get(table);
			if (map != null) {
				String role = map.get(pRoleID.intValue());
				if (role != null) {
					return table.equals("Artist") ? role : "[" + role + "]";
				}
			}
		}
		return General.EMPTY_STRING;
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
		return getContentsPerson(personList, false);
	}

	protected String getContentsPerson(List<Map<String, Object>> personList, boolean useSort) {
		if (CollectionUtils.isEmpty(personList) || !useContentsPerson) {
			return General.EMPTY_STRING;
		}

		StringBuilder sb = new StringBuilder();
		for (Map<String, Object> mapPerson : personList) {
			String persons = (String) mapPerson.get(useSort ? "SortBy" : "Name");
			if (StringUtils.isNotEmpty(persons)) {
				if (useRoles) {
					String role = getPersonRole(personField[0], (Number) mapPerson.get(ROLE_ID));
					if (role.isEmpty()) {
						sb.append(persons).append(" & ");
					} else {
						sb.append(role).append(General.SPACE).append(persons).append(General.SPACE);
					}
				} else {
					sb.append(persons);
					sb.append(" & ");
				}
			}
		}

		int lastChar = sb.lastIndexOf(" & ");
		if (lastChar != -1) {
			sb.delete(lastChar, lastChar + 2);
		}
		return sb.toString().trim();
	}

	public static FNProgramvare getSoftware(FNPSoftware soft) {
		whoAmI = soft;
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

	public static FNPSoftware whoAmI() {
		return whoAmI;
	}

	@SuppressWarnings("unchecked")
	protected void validateBuddyHeaders(String path) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Map<String, Object> map;
		try {
			map = mapper.readValue(General.getInputStreamReader(path), Map.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ArrayList<LinkedHashMap<String, String>> fields = (ArrayList<LinkedHashMap<String, String>>) map
				.get("Buddy fields");

		Set<String> buddySet = new HashSet<>();
		Map<String, String> buddyMap = new HashMap<>();
		fields.forEach(link -> link.entrySet().forEach(entry -> {
			buddySet.add(entry.getKey());
			buddyMap.put(entry.getValue(), entry.getKey());
		}));

		List<FieldDefinition> invalidHeaders = dbInfoToWrite.stream()
				.filter(field -> !buddySet.contains(field.getFieldHeader())).toList();

		if (!invalidHeaders.isEmpty()) {
			StringBuilder error = new StringBuilder(GUIFactory.getText("buddyInvalid1"));
			dbInfoToWrite.forEach(field -> buddySet.remove(field.getFieldHeader()));

			Iterator<FieldDefinition> iter = invalidHeaders.iterator();
			while (iter.hasNext()) {
				FieldDefinition field = iter.next();
				String validHeader = buddyMap.getOrDefault(field.getFieldAlias(), "Unknown");
				if (!validHeader.equals("Unknown")) {
					error.append(GUIFactory.getText("buddyInvalid2")).append(field.getFieldHeader())
							.append(GUIFactory.getText("buddyInvalid3")).append(validHeader).append("\"<br>");
					field.setFieldHeader(validHeader);
					buddySet.remove(field.getFieldHeader());
					iter.remove();
				}
			}

			if (!invalidHeaders.isEmpty()) {
				invalidHeaders.forEach(field -> error.append(GUIFactory.getText("buddyInvalid4"))
						.append(field.getFieldHeader()).append(GUIFactory.getText("buddyInvalid5")));

				error.append("<br><br>");
				List<String> validFields = new ArrayList<>(buddySet);
				Collections.sort(validFields);
				error.append(GUIFactory.getText("buddyInvalid6"));
				validFields.forEach(field -> error.append(field).append("; "));
				error.delete(error.length() - 2, error.length());
			}

			error.append("</html>");
			General.showMessage(null, error.toString(), GUIFactory.getText("buddyInvalidTitle"), true);
			dbInfoToWrite.removeAll(invalidHeaders);
		}
	}

	protected void addToList(StringBuilder newLine, StringBuilder result) {
		result.append(newLine).append("\n");
		newLine.setLength(0);
	}

	protected List<BasisField> getBuddyFields() {
		// Nothing to do on this level
		return new ArrayList<>();
	}

	protected List<String> getSystemFields(List<String> userFields) {
		// Nothing to do on this level
		return new ArrayList<>();
	}

	protected List<String> getContentsFields(List<String> userFields) {
		// Nothing to do on this level
		return new ArrayList<>();
	}

	protected abstract void setDatabaseData(Map<String, Object> dbDataRecord,
			Map<String, List<Map<String, Object>>> hashTable) throws Exception;
}