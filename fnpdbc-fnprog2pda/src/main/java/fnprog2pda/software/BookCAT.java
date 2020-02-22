package fnprog2pda.software;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
import application.utils.XComparator;

public class BookCAT extends FNProgramvare {
	/**
	 * Title: BookCAT Description: BookCAT Class Copyright: (c) 2003-2011
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private boolean useAuthor;
	private boolean useAuthorSort;
	private boolean useContentsAuthor = true;
	private boolean useContentsOrigTitle = true;
	private boolean useContentsItemTitle = true;
	private boolean useOriginalTitle;
	private boolean useOriginalReleaseNo;
	private boolean useReleaseNo;

	private int myBookID = 0;
	private int myItemCount = 0;
	private String myPerson = "[None]";
	private String myTitle = "";

	private Map<String, FieldTypes> sortList = new LinkedHashMap<>();
	private XComparator comp = new XComparator(sortList);

	public BookCAT(Component myParent) throws Exception {
		super(myParent);
		useContentsAuthor = pdaSettings.isUseContentsPerson();
		useContentsOrigTitle = pdaSettings.isUseContentsOrigTitle();
		useContentsItemTitle = pdaSettings.isUseContentsItemTitle();
		useOriginalTitle = pdaSettings.isUseOriginalTitle();

		personField = new String[] { "Author", "AuthorSort" };
		sortList.put("Contents.Item", FieldTypes.NUMBER);
		sortList.put("Index", FieldTypes.NUMBER);
	}

	@Override
	protected List<String> getSystemFields(List<String> userFields) {
		useOriginalReleaseNo = userFields.contains("OriginalReleaseNo");
		useReleaseNo = userFields.contains("ReleaseNo");
		useAuthor = userFields.contains(personField[0]);
		useAuthorSort = userFields.contains(personField[1]);

		List<String> result = new ArrayList<>();
		if (useOriginalTitle && !userFields.contains("OriginalTitle")) {
			result.add("OriginalTitle");
		} else {
			useOriginalTitle = false;
		}
		return result;
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		List<String> result = new ArrayList<>(15);
		useContents = userFields.contains("Contents");

		if (useContents) {
			if (useContentsItemTitle) {
				result.add("Media.Item");
			}

			result.add("MediaCount");
			result.add("NumberOfSections");
			result.add("ContentsLink.ContentsID");
			result.add("Contents.Item");

			if (useContentsAuthor) {
				if (useAuthor || useAuthorSort) {
					result.add(useAuthor ? "ContentsPerson" : "ContentsPersonSort");
				} else {
					result.add("ContentsPerson");
				}
			}
		}

		return result;
	}

	@Override
	public void setCategories() throws Exception {
		super.setCategories();
		if (useRoles && myTable.equals("Album") || myTable.equals("Contents")) {
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
		myRoles.put("Author", personMap);

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
		myRoles.put("Author", personMap);

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
		getRoles("Author", "AuthorRole", "AuthorRoleID");
		getRoles("Credits", "CreditRole", "CreditRoleID");
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		myBookID = (Integer) dbDataRecord.get(myTableID);
		myLastIndex = Math.max(myLastIndex, myBookID);

		if (useContents) {
			myItemCount = ((Number) dbDataRecord.get("MediaCount")).intValue();
			if (((Number) dbDataRecord.get("NumberOfSections")).intValue() > 0) {
				myPerson = (String) dbDataRecord.get(personField[useAuthorSort ? 1 : 0]);
				myTitle = (String) dbDataRecord.get("Title");
				dbDataRecord.put("Contents", getBookContents(hashTable));
			}
		}

		if (useReleaseNo && ((Number) dbDataRecord.get("ReleaseNo")).intValue() == 0) {
			dbDataRecord.put("ReleaseNo", "");
		}

		if (useOriginalReleaseNo && ((Number) dbDataRecord.get("OriginalReleaseNo")).intValue() == 0) {
			dbDataRecord.put("OriginalReleaseNo", "");
		}

		if (useOriginalTitle) {
			String s = (String) dbDataRecord.get("OriginalTitle");
			if (s != null && !s.isEmpty()) {
				dbDataRecord.put("Title", s);
			}
		}
	}

	private String getBookContents(Map<String, List<Map<String, Object>>> hashTable) throws Exception {
		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get("Contents");

		if (contentsList.isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();

		int item = 0;
		int oldItem = 0;
		int personIndex = 0;

		String title = myTitle == null ? "" : myTitle.toUpperCase();

		// Get Media
		List<Map<String, Object>> mediaList = hashTable.get("Media");

		// Get Persons
		List<Map<String, Object>> personList = hashTable.get("ContentsPerson");
		int maxPersons = personList != null ? personList.size() : 0;

		// Sort Media by Contents.Item
		if (useContentsItemTitle && mediaList != null && mediaList.size() > 1) {
			Collections.sort(contentsList, comp);
		}

		for (Map<String, Object> map : contentsList) {
			String contTitle = (String) map.get("Title");
			String origTitle = (String) map.get("OriginalTitle");

			if (useContentsItemTitle) {
				if (mediaList != null) {
					item = ((Number) map.get("Contents.Item")).intValue();
					if (item != oldItem) {
						Map<String, Object> mapItem = mediaList.get(item - 1);
						String itemTitle = (String) mapItem.get("Title");
						if (myItemCount > 1 || myItemCount == 1 && !itemTitle.toUpperCase().equals(title)) {
							if (item > 1) {
								result.append("  \n");
							}
							result.append(item + " - " + itemTitle + "\n");
						}
					}
				}
			}

			if (origTitle != null && !origTitle.isEmpty() && !origTitle.equals(contTitle)) {
				if (useContentsOrigTitle) {
					contTitle += " / " + origTitle;
				} else {
					if (useOriginalTitle) {
						contTitle = origTitle;
					}
				}
			}

			result.append(contTitle);

			if (useContentsAuthor && !personList.isEmpty() && personIndex < maxPersons) {
				Map<String, Object> mapPerson = personList.get(personIndex);
				boolean isPersonSet = false;
				StringBuilder buf = new StringBuilder();

				while (true) {
					String persons = (String) mapPerson.get(useAuthorSort ? "SortBy" : "Name");
					if (persons != null && !persons.isEmpty()) {
						int roleID = useRoles ? ((Number) mapPerson.get("RoleID")).intValue() : 0;

						if (isPersonSet) {
							buf.append("; ");
						}

						buf.append(getPersonRole("Artist", persons, roleID));
						isPersonSet = true;
					} else {
						break;
					}

					if ((Boolean) mapPerson.get("isAtEnd")) {
						break;
					}

					personIndex++;
					mapPerson = personList.get(personIndex);
				}

				String persons = buf.toString();
				if (!persons.isEmpty() && !persons.equals(myPerson)) {
					result.append(" [");
					result.append(persons);
					result.append("]");
				}
			}

			result.append("\n");
			oldItem = item;
			personIndex++;
		}
		return result.toString();
	}
}