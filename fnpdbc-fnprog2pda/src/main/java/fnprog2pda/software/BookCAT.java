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
import application.utils.BasisField;
import application.utils.General;
import application.utils.XComparator;

public class BookCAT extends FNProgramvare {
	/**
	 * BookCAT
	 *
	 * @author Tom van Breukelen
	 * @version 10
	 */
	private boolean useOriginalTitle;
	private boolean useOriginalReleaseNo;
	private boolean useReleaseNo;
	private boolean inclReleaseNo;
	private boolean useBookBuddy;
	private boolean useSpecialRoles;
	private boolean useStatus;
	private boolean useRating;
	private boolean useYearPublished;

	private int myItemCount = 0;
	private String myPerson = "[None]";
	private String myTitle = "";

	private static final String AUTHOR = "Author";
	private static final String AUTHOR_SORT = "AuthorSort";
	private static final String CONTENTS = "Contents";
	private static final String CONTENTS_ITEM = "Contents.Item";
	private static final String CONTENTS_PERSON = "ContentsPerson";
	private static final String EDITOR = "Editor";
	private static final String ILLUSTRATOR = "Illustrator";
	private static final String ISBN = "ISBN";
	private static final String LANGUAGE = "Language";
	private static final String LAST_READ = "LastRead";
	private static final String ORIGINAL_RELEASE_NO = "OriginalReleaseNo";
	private static final String ORIGINAL_SERIES = "OriginalSeries";
	private static final String ORIGINAL_SERIES_SORT = "OriginalSeriesSort";
	private static final String ORIGINAL_TITLE = "OriginalTitle";
	private static final String PHOTOGRAPHER = "Photographer";
	private static final String RATING = "PersonalRating";
	private static final String RELEASE_NO = "ReleaseNo";
	private static final String SERIES = "Series";
	private static final String SERIES_SORT = "SeriesSort";
	private static final String STATUS = "ReadStatus";
	private static final String SYNOPSIS = "Synopsis";
	private static final String TITLE = "Title";
	private static final String TITLE_SORT = "TitleSort";
	private static final String TRANSLATOR = "Translator";
	private static final String YEAR_PUBLISHED = "YearPublished";

	private Map<String, FieldTypes> sortContents = new LinkedHashMap<>();
	private Map<String, FieldTypes> sortMedia = new LinkedHashMap<>();
	private XComparator compContents = new XComparator(sortContents);
	private XComparator compMedia = new XComparator(sortMedia);

	public BookCAT() {
		super();
		useOriginalTitle = pdaSettings.isUseOriginalTitle();
		inclReleaseNo = pdaSettings.isUseReleaseNo();
		useBookBuddy = pdaSettings.getTextFileFormat().equals("buddyCsv");

		personField = new String[] { AUTHOR, AUTHOR_SORT };
		sortContents.put(CONTENTS_ITEM, FieldTypes.NUMBER);
		sortContents.put("Index", FieldTypes.NUMBER);
		sortMedia.put("Index", FieldTypes.NUMBER);
	}

	@Override
	protected List<BasisField> getBuddyFields() {
		List<BasisField> result = new ArrayList<>();
		if (useBookBuddy) {
			result.add(new BasisField(AUTHOR, AUTHOR, AUTHOR, FieldTypes.TEXT));
			result.add(new BasisField(AUTHOR_SORT, AUTHOR_SORT, "Author (Last, First)", FieldTypes.TEXT));
			result.add(new BasisField(ISBN, ISBN, ISBN, FieldTypes.TEXT));
			result.add(new BasisField(LANGUAGE, LANGUAGE, LANGUAGE, FieldTypes.TEXT));
			result.add(new BasisField("Pages", "Pages", "Number of Pages", FieldTypes.NUMBER));
			result.add(new BasisField("PrimaryGenre", "PrimaryGenre", "Genre", FieldTypes.TEXT));
			result.add(new BasisField("Publisher", "Publisher", "Publisher", FieldTypes.TEXT));
			result.add(new BasisField(RELEASE_NO, RELEASE_NO, "Volume", FieldTypes.TEXT));
			result.add(new BasisField(SERIES, SERIES, SERIES, FieldTypes.TEXT));
			result.add(new BasisField(STATUS, STATUS, "Status", FieldTypes.TEXT));
			result.add(new BasisField(SYNOPSIS, SYNOPSIS, "Summary", FieldTypes.MEMO));
			result.add(new BasisField(TITLE, TITLE, TITLE, FieldTypes.TEXT));
			result.add(new BasisField(YEAR_PUBLISHED, YEAR_PUBLISHED, "Year Published", FieldTypes.TEXT));

			inclReleaseNo = false;
		}

		return result;
	}

	@Override
	protected List<String> getMandatorySortFields(List<String> sortList) {
		if (!useBookBuddy) {
			return sortList;
		}

		List<String> result = new ArrayList<>();
		result.add(AUTHOR_SORT);
		result.add(SERIES);
		result.add(RELEASE_NO);
		result.add(TITLE_SORT);

		result.addAll(sortList);
		return result;
	}

	@Override
	protected List<String> getSystemFields(List<String> userFields) {
		List<String> result = new ArrayList<>();
		boolean useOriginalSeries = userFields.contains(ORIGINAL_SERIES) || userFields.contains(ORIGINAL_SERIES_SORT);
		boolean useSeries = userFields.contains(SERIES) || userFields.contains(SERIES_SORT);

		useOriginalReleaseNo = userFields.contains(ORIGINAL_RELEASE_NO);
		useReleaseNo = userFields.contains(RELEASE_NO);
		inclReleaseNo = inclReleaseNo && (useOriginalSeries || useSeries);
		useStatus = userFields.contains(STATUS);
		useRating = userFields.contains(RATING);
		useYearPublished = userFields.contains(YEAR_PUBLISHED);

		if (inclReleaseNo && useOriginalSeries && !useOriginalReleaseNo) {
			result.add(ORIGINAL_RELEASE_NO);
			useOriginalReleaseNo = true;
		}

		if (useRating) {
			result.add("PersonalRatingID");
		}

		if (useSeries && !useReleaseNo) {
			result.add(RELEASE_NO);
			useReleaseNo = true;
		}

		if (useStatus && !userFields.contains(LAST_READ)) {
			result.add(LAST_READ);
		}

		if (useYearPublished) {
			result.add("PublishDate");
		}

		useSpecialRoles = userFields.contains(EDITOR) || userFields.contains(ILLUSTRATOR)
				|| userFields.contains(PHOTOGRAPHER) || userFields.contains(TRANSLATOR);

		if (useSpecialRoles) {
			result.add("Credits");
			useRoles = true;
		}

		if (useOriginalTitle) {
			result.add(ORIGINAL_TITLE);
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
				result.add("ContentsPersonSort");
				result.add(CONTENTS_PERSON);
			}
		}

		return result;
	}

	@Override
	public void setCategories() throws Exception {
		super.setCategories();
		if (myTable.equals("Book") || myTable.equals(CONTENTS)) {
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
		roles.add(EDITOR);
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
	public void setupDbInfoToWrite() {
		super.setupDbInfoToWrite();
		if (useBookBuddy) {
			validateBuddyHeaders("config/BookBuddy.yaml");
		}
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		int myBookID = (Integer) dbDataRecord.get(myTableID);
		lastIndex = Math.max(lastIndex, myBookID);

		if (useContents) {
			myItemCount = ((Number) dbDataRecord.get("MediaCount")).intValue();
			if (((Number) dbDataRecord.get("NumberOfSections")).intValue() > 0) {
				myPerson = (String) dbDataRecord.get(personField[0]);
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
				dbDataRecord.put(ORIGINAL_TITLE, "");
			}
		}

		if (useRating && useBookBuddy) {
			Integer rating = (Integer) dbDataRecord.getOrDefault("PersonalRatingID", -1);
			if (rating == -1 || rating > 5) {
				dbDataRecord.put(RATING, "");
			} else {
				dbDataRecord.put(RATING, 6 - rating);
			}
		}

		if (useStatus) {
			String viewed = dbDataRecord.getOrDefault(LAST_READ, "").toString();
			dbDataRecord.put(STATUS, StringUtils.isEmpty(viewed) ? "Unread" : "Read");
		}

		if (useSpecialRoles) {
			convertCreditRoles(dbDataRecord);
		}

		if (useYearPublished) {
			String published = dbDataRecord.getOrDefault("PublishDate", "").toString();
			if (published.length() > 3) {
				dbDataRecord.put(YEAR_PUBLISHED, published.substring(0, 4));
			}
		}
	}

	private void convertCreditRoles(Map<String, Object> dbDataRecord) {

		StringBuilder bEditor = new StringBuilder();
		StringBuilder bIllustrator = new StringBuilder();
		StringBuilder bPhotographer = new StringBuilder();
		StringBuilder bTransator = new StringBuilder();

		List<String> authorCredits = General
				.convertStringToList((String) dbDataRecord.getOrDefault("AuthorCredits", ""), "; ");
		List<String> credits = General.convertStringToList((String) dbDataRecord.getOrDefault("CreditCredits", ""),
				"; ");

		authorCredits.forEach(person -> {
			if (!person.isEmpty()) {
				int pos = person.indexOf("[");
				String role = person.substring(pos + 1, person.lastIndexOf("]"));
				person = person.substring(0, pos);

				if (role.equals("4")) {
					bEditor.append(person).append("; ");
				} else if (role.equals("6")) {
					bIllustrator.append(person).append("; ");
				}
			}
		});

		credits.forEach(person -> {
			if (!person.isEmpty()) {
				int pos = person.indexOf("[");
				String role = person.substring(pos + 1, person.lastIndexOf("]"));
				person = person.substring(0, pos);
				switch (role) {
				case "3":
					bIllustrator.append(person).append("; ");
					break;
				case "4":
					bPhotographer.append(person).append("; ");
					break;
				case "5":
					bTransator.append(person).append("; ");
					break;
				default:
					break;
				}
			}
		});

		setSpecialRoles(EDITOR, dbDataRecord, bEditor);
		setSpecialRoles(ILLUSTRATOR, dbDataRecord, bIllustrator);
		setSpecialRoles(PHOTOGRAPHER, dbDataRecord, bPhotographer);
		setSpecialRoles(TRANSLATOR, dbDataRecord, bTransator);
	}

	private void setSpecialRoles(String field, Map<String, Object> dbDataRecord, StringBuilder bRole) {
		if (bRole.length() > 2) {
			bRole.delete(bRole.length() - 2, bRole.length());
		}
		dbDataRecord.put(field, bRole.toString());
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