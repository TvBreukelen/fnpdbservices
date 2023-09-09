package fnprog2pda.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import application.interfaces.FieldTypes;
import application.utils.XComparator;

public class BookCAT extends FNProgramvare {
	/**
	 * BookCAT
	 *
	 * @author Tom van Breukelen
	 * @version 10
	 */
	private boolean useAuthor;
	private boolean useOriginalTitle;
	private boolean useOriginalReleaseNo;
	private boolean useReleaseNo;
	private boolean inclReleaseNo;

	private int myItemCount = 0;
	private String myPerson = "[None]";
	private String myTitle = "";

	private static final String AUTHOR = "Author";
	private static final String CONTENTS = "Contents";
	private static final String CONTENTS_ITEM = "Contents.Item";
	private static final String CONTENTS_PERSON = "ContentsPerson";
	private static final String ORIGINAL_RELEASE_NO = "OriginalReleaseNo";
	private static final String ORIGINAL_SERIES = "OriginalSeries";
	private static final String ORIGINAL_SERIES_SORT = "OriginalSeriesSort";
	private static final String ORIGINAL_TITLE = "OriginalTitle";
	private static final String RELEASE_NO = "ReleaseNo";
	private static final String SERIES = "Series";
	private static final String SERIES_SORT = "SeriesSort";
	private static final String TITLE = "Title";

	private Map<String, FieldTypes> sortContents = new LinkedHashMap<>();
	private Map<String, FieldTypes> sortMedia = new LinkedHashMap<>();
	private XComparator compContents = new XComparator(sortContents);
	private XComparator compMedia = new XComparator(sortMedia);

	public BookCAT() {
		super();
		useOriginalTitle = pdaSettings.isUseOriginalTitle();
		inclReleaseNo = pdaSettings.isUseReleaseNo();
		personField = new String[] { AUTHOR, "AuthorSort" };
		sortContents.put(CONTENTS_ITEM, FieldTypes.NUMBER);
		sortContents.put("Index", FieldTypes.NUMBER);
		sortMedia.put("Index", FieldTypes.NUMBER);
	}

	@Override
	protected List<String> getSystemFields(List<String> userFields) {
		List<String> result = new ArrayList<>();
		boolean useOriginalSeries = userFields.contains(ORIGINAL_SERIES) || userFields.contains(ORIGINAL_SERIES_SORT);
		boolean useSeries = userFields.contains(SERIES) || userFields.contains(SERIES_SORT);
		useOriginalReleaseNo = userFields.contains(ORIGINAL_RELEASE_NO);
		useReleaseNo = userFields.contains(RELEASE_NO);
		inclReleaseNo = inclReleaseNo && (useOriginalSeries || useSeries);

		if (inclReleaseNo) {
			if (useOriginalSeries && !useOriginalReleaseNo) {
				result.add(ORIGINAL_RELEASE_NO);
				useOriginalReleaseNo = true;
			}

			if (useSeries && !useReleaseNo) {
				result.add(RELEASE_NO);
				useReleaseNo = true;
			}
		}

		useAuthor = userFields.contains(personField[0]);
		usePersonSort = userFields.contains(personField[1]);

		if (useOriginalTitle && !userFields.contains(ORIGINAL_TITLE)) {
			result.add(ORIGINAL_TITLE);
		} else {
			useOriginalTitle = false;
		}
		return result;
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		List<String> result = new ArrayList<>(15);
		useContents = userFields.contains(CONTENTS);

		if (useContents) {
			if (useContentsItemTitle) {
				result.add("Media.Item");
			}

			result.add("MediaCount");
			result.add("NumberOfSections");
			result.add("ContentsLink.ContentsID");
			result.add(CONTENTS_ITEM);

			if (useContentsPerson) {
				if (useAuthor || usePersonSort) {
					result.add(useAuthor ? CONTENTS_PERSON : "ContentsPersonSort");
				} else {
					result.add(CONTENTS_PERSON);
				}
			}
		}

		return result;
	}

	@Override
	public void setCategories() throws Exception {
		super.setCategories();
		if (useRoles && myTable.equals("Book") || myTable.equals(CONTENTS)) {
			switch ((int) Math.floor(Double.parseDouble(mySoftwareVersion))) {
			case 6:
				getVersion6Roles();
				break;
			case 7:
				getVersion7Roles();
				break;
			default:
				getVersion8Roles();
			}
		}
	}

	private void getVersion6Roles() throws Exception {
		// Get roles
		List<String> roles = new ArrayList<>();
		roles.add("MainAuthor");
		roles.add("CoAuthor");
		roles.add("Ghostwriter");
		roles.add("Contributor");
		roles.add("Editor");
		roles.add("AuthorCustom1");
		roles.add("AuthorCustom2");
		roles.add("AuthorCustom3");
		roles.add("AuthorCustom4");
		roles.add("AuthorCustom5");
		roles.add("Pseudonym");
		roles.add("AuthorCustom6");
		roles.add("AuthorCustom7");
		roles.add("AuthorCustom8");
		roles.add("AuthorCustom9");

		Object obj = myTable;
		List<Map<String, Object>> list = msAccess.getMultipleRecords("FieldDefinitions", null,
				Collections.singletonMap("Table", obj));
		HashMap<Integer, String> personMap = new HashMap<>();
		myRoles.put(AUTHOR, personMap);

		for (Map<String, Object> map : list) {
			Object field = map.get("Field");
			int index = roles.indexOf(field);
			if (index != -1) {
				personMap.put(index, map.get("UserName").toString());
			}
		}
	}

	private void getVersion7Roles() throws Exception {
		// Obtain the Roles
		Object obj = "FieldDefinitions_" + myTable;
		Map<String, Object> map = msAccess.getSingleRecord("Settings", "ID", Collections.singletonMap("ID", obj));
		String data = new String((byte[]) map.get("Data"));
		int count = 0;
		int index1 = data.indexOf("MainAuthor") - 1;
		int index2 = data.indexOf("AuthorDetail") - 1;
		data = data.substring(index1, index2);

		HashMap<Integer, String> personMap = new HashMap<>();
		myRoles.put(AUTHOR, personMap);

		for (String s : data.split("\0\0\7")) {
			index1 = s.indexOf("\1\1\6") + 3;
			index2 = s.charAt(index1) + index1;

			String stdName = s.substring(++index1, ++index2);
			String altName = "";

			index1 = index2 + 2;
			index2 = s.charAt(index1) + index1;
			index1 = s.indexOf("\0\1\6", index2 + 1);

			if (index1 != -1) {
				altName = s.substring(index1 + 4, s.length());
			}

			personMap.put(count++, altName.isEmpty() ? stdName : altName);
		}
	}

	private void getVersion8Roles() throws Exception {
		getRoles(AUTHOR, "AuthorRole", "AuthorRoleID");
		getRoles("Credits", "CreditRole", "CreditRoleID");
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		int myBookID = (Integer) dbDataRecord.get(myTableID);
		myLastIndex = Math.max(myLastIndex, myBookID);

		if (useContents) {
			myItemCount = ((Number) dbDataRecord.get("MediaCount")).intValue();
			if (((Number) dbDataRecord.get("NumberOfSections")).intValue() > 0) {
				myPerson = (String) dbDataRecord.get(personField[usePersonSort ? 1 : 0]);
				myTitle = (String) dbDataRecord.get(TITLE);
				dbDataRecord.put(CONTENTS, getBookContents(hashTable));
			}
		}

		if (useOriginalReleaseNo) {
			convertSeries(dbDataRecord, ORIGINAL_SERIES, ORIGINAL_SERIES_SORT, ORIGINAL_RELEASE_NO);
		}

		if (useReleaseNo) {
			convertSeries(dbDataRecord, SERIES, SERIES_SORT, RELEASE_NO);
		}

		if (useOriginalTitle) {
			String s = (String) dbDataRecord.get(ORIGINAL_TITLE);
			if (StringUtils.isNotEmpty(s)) {
				dbDataRecord.put(TITLE, s);
			}
		}
	}

	private void convertSeries(Map<String, Object> dbDataRecord, String seriesId, String seriesSortId,
			String releaseId) {

		int releaseNo = ((Number) dbDataRecord.get(releaseId)).intValue();
		dbDataRecord.put(releaseId, releaseNo == 0 ? "" : releaseNo);

		if (!inclReleaseNo || releaseNo == 0) {
			// Nothing more to do
			return;
		}

		String[] searchId = { seriesId, seriesSortId };
		for (String id : searchId) {
			String s1 = (String) dbDataRecord.get(id);
			if (StringUtils.isNotBlank(s1)) {
				dbDataRecord.put(id, s1 + " (" + releaseNo + ")");
			}
		}
	}

	private String getBookContents(Map<String, List<Map<String, Object>>> hashTable) {
		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get(CONTENTS);

		if (CollectionUtils.isEmpty(contentsList)) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		StringBuilder newLine = new StringBuilder();

		int item = 0;
		int oldItem = 0;
		String title = myTitle == null ? "" : myTitle.toUpperCase();

		// Get Media
		List<Map<String, Object>> mediaList = hashTable.get("Media");

		// Get Persons
		List<Map<String, Object>> personList = hashTable.get(CONTENTS_PERSON);
		List<Map<String, Object>> linkList = hashTable.get("ContentsLink");

		// Split ContentsPerson by ContentsID
		Map<Integer, List<Map<String, Object>>> trackPersons = getContentsPersonIndex(linkList, personList,
				"ContentsID");

		// Sort Media by Contents.Item
		if (useContentsItemTitle && mediaList != null && mediaList.size() > 1) {
			Collections.sort(contentsList, compContents);
			Collections.sort(mediaList, compMedia);
		}

		for (Map<String, Object> map : contentsList) {
			String contTitle = (String) map.get(TITLE);
			String origTitle = (String) map.get(ORIGINAL_TITLE);

			if (useContentsItemTitle && mediaList != null) {
				item = ((Number) map.get(CONTENTS_ITEM)).intValue();
				if (item != oldItem) {
					Map<String, Object> mapItem = mediaList.get(item - 1);
					String itemTitle = (String) mapItem.get(TITLE);
					if (myItemCount > 1 || myItemCount == 1 && !itemTitle.equalsIgnoreCase(title)) {
						if (item > 1) {
							addToList(newLine, result);
						}
						newLine.append(item + " - " + itemTitle);
						addToList(newLine, result);
					}
				}
			}

			if (StringUtils.isNotEmpty(origTitle) && !origTitle.equals(contTitle)) {
				if (useContentsOrigTitle) {
					contTitle += " / " + origTitle;
				} else {
					if (useOriginalTitle) {
						contTitle = origTitle;
					}
				}
			}

			newLine.append(contTitle);

			String persons = getContentsPerson(trackPersons.get(map.get("ContentID")));
			if (StringUtils.isNotEmpty(persons) && !persons.equals(myPerson)) {
				newLine.append(" [");
				newLine.append(persons);
				newLine.append("]");
			}

			addToList(newLine, result);
			oldItem = item;
		}
		return result.toString();
	}
}