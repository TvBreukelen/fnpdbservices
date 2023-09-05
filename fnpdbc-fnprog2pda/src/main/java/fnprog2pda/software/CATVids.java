package fnprog2pda.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import application.interfaces.FieldTypes;
import application.utils.General;
import application.utils.XComparator;

public class CATVids extends FNProgramvare {
	/**
	 * Title: CATVids Description: CATVids Class Copyright: (c) 2003-2011
	 *
	 * @author Tom van Breukelen
	 * @version 8.0
	 */
	private boolean useCategory = false;
	private boolean useContentsLength = true;
	private boolean useContentsSide = true;
	private boolean useContentsIndex = true;
	private boolean useDirector = true;
	private boolean useEntireCast = false;
	private boolean usePerson = false;
	private boolean useStatus = false;

	private static final String CATEGORY = "Category";
	private static final String CONTENTS = "Contents";
	private static final String CONTENTS_ITEM = "Contents.Item";
	private static final String DIRECTOR = "Director";
	private static final String DIRECTOR_SORT = "DirectorSort";
	private static final String PERSON = "Person";
	private static final String PERSON_SORT = "Person.PersonSort";
	private static final String SORT_BY = "SortBy";
	private static final String STATUS = "WatchingStatus";

	private Map<String, FieldTypes> sortList = new LinkedHashMap<>();
	private XComparator comp = new XComparator(sortList);

	public CATVids() {
		super();
		useContentsLength = pdaSettings.isUseContentsLength();
		useContentsSide = pdaSettings.isUseContentsSide();
		useContentsIndex = pdaSettings.isUseContentsIndex();
		useEntireCast = pdaSettings.isUseEntireCast();

		sortList.put(CONTENTS_ITEM, FieldTypes.NUMBER);
		sortList.put("Index", FieldTypes.NUMBER);
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		List<String> result = new ArrayList<>();

		useContents = userFields.contains(CONTENTS);
		usePerson = userFields.contains(PERSON);
		if (!usePerson) {
			usePerson = userFields.contains(PERSON_SORT);
		}

		useDirector = userFields.contains(DIRECTOR);
		if (!useDirector) {
			useDirector = userFields.contains(DIRECTOR_SORT);
		}

		useCategory = userFields.contains(CATEGORY);
		useStatus = userFields.contains(STATUS);

		if (useContents) {
			result.add(CONTENTS_ITEM);
			result.add("Media.Item");
			result.add("NumberOfSegments");
		}

		if (useDirector && !usePerson) {
			result.add(PERSON);
		}

		return result;
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		int myVideoID = (Integer) dbDataRecord.get(myTableID);
		myLastIndex = Math.max(myLastIndex, myVideoID);

		if (useContents) {
			String s = dbDataRecord.get("NumberOfSegments").toString();
			if (Integer.parseInt(s) > 1) {
				dbDataRecord.put(CONTENTS, getVideoContents(s.length(), hashTable));
			}
		}

		dbDataRecord.put(DIRECTOR, "");
		dbDataRecord.put(DIRECTOR_SORT, "");
		dbDataRecord.put(PERSON, "");
		dbDataRecord.put(PERSON_SORT, "");

		// Category
		if (useCategory) {
			getCategory(dbDataRecord, hashTable);
		}

		if (useStatus) {
			List<Map<String, Object>> listContent = hashTable.getOrDefault(CONTENTS, new ArrayList<>());
			if (!listContent.isEmpty()) {
				Map<String, Object> contentMap = listContent.get(0);
				Object viewed = contentMap.get("LastViewed");
				dbDataRecord.put(STATUS, viewed == null ? "Not Watched" : "Watched");
			}
		}

		if (usePerson || useDirector) {
			getPersonWithRoles(dbDataRecord, hashTable);
		}
	}

	private void getCategory(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable) {
		List<Map<String, Object>> listType = hashTable.getOrDefault("Type", new ArrayList<>());
		if (!listType.isEmpty()) {
			Map<String, Object> typeMap = listType.get(0);
			Object type = typeMap.get("ContentsType");
			if (type == null) {
				dbDataRecord.put(CATEGORY, "Movies & TV Shows");
			} else {
				switch (type.toString()) {
				case "Movie":
				case "TV Shows":
					dbDataRecord.put(CATEGORY, "Movies & TV Shows");
					break;
				default:
					dbDataRecord.put(CATEGORY, type);
				}
			}
		}
	}

	private void getPersonWithRoles(Map<String, Object> dbDataRecord,
			Map<String, List<Map<String, Object>>> hashTable) {

		StringBuilder bCast = new StringBuilder();
		StringBuilder bCastSort = new StringBuilder();
		StringBuilder bSupportCast = new StringBuilder();
		StringBuilder bSupportCastSort = new StringBuilder();
		StringBuilder bDirector = new StringBuilder();
		StringBuilder bDirectorSort = new StringBuilder();

		List<Map<String, Object>> creditsList = hashTable.get("CreditsLink");
		List<Map<String, Object>> personList = hashTable.get(PERSON);
		Set<Integer> rowSet = new HashSet<>();

		personList.forEach(map -> {
			Integer personID = (Integer) map.get("PersonID");
			List<Map<String, Object>> personCredit = creditsList.stream()
					.filter(credit -> personID.equals(credit.get("PersonID")) && !rowSet.contains(personID))
					.collect(Collectors.toList());
			rowSet.add(personID);

			personCredit.forEach(pMap -> {
				byte roleType = (Byte) pMap.get("RoleType");

				switch (roleType) {
				case 1: // Leading role
					if (usePerson) {
						addActorRole(bCast, bCastSort, map, pMap);
					}
					break;
				case 2: // Supporting character
					if (usePerson && useEntireCast) {
						addActorRole(bSupportCast, bSupportCastSort, map, pMap);
					}
					break;
				case 3: // Crew
					if (useDirector) {
						int roleID = (Integer) pMap.get("RoleID");
						if (roleID == 1) {
							bDirector.append(map.get("Name")).append("; ");
							bDirectorSort.append(map.get(SORT_BY)).append("; ");
						}
					}
					break;
				default:
					// Nothing to do
					break;
				}
			});
		});

		bCast.append(bSupportCast.toString());
		bCastSort.append(bSupportCastSort.toString());
		dbDataRecord.put(PERSON, bCast.toString());
		dbDataRecord.put(PERSON_SORT, bCastSort.toString());

		if (bDirector.length() > 2) {
			bDirector.delete(bDirector.length() - 2, bDirector.length());
			bDirectorSort.delete(bDirectorSort.length() - 2, bDirectorSort.length());
			dbDataRecord.put(DIRECTOR, bDirector.toString());
			dbDataRecord.put(DIRECTOR_SORT, bDirectorSort.toString());
		}
	}

	private void addActorRole(StringBuilder bCast, StringBuilder bCastSort, Map<String, Object> map,
			Map<String, Object> pMap) {
		bCast.append(map.get("Name"));
		bCastSort.append(map.get(SORT_BY));
		if (useRoles) {
			bCast.append(" (" + pMap.get("Character") + ")");
			bCastSort.append(" (" + pMap.get("Character") + ")");
		}
		bCast.append("\n");
		bCastSort.append("\n");
	}

	// method that returns the Video Contents as String
	private String getVideoContents(int itemLength, Map<String, List<Map<String, Object>>> hashTable) {
		StringBuilder result = new StringBuilder();
		StringBuilder newLine = new StringBuilder();

		String side = null;
		String sideTitle = null;
		Integer sidePlaytime = null;
		Integer sideAPlaytime = null;
		Integer sideBPlaytime = null;
		String sideATitle = null;
		String sideBTitle = null;
		String oldSide = "";

		int item = 0;
		int oldItem = 0;

		boolean isDoubleSided = false;

		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get(CONTENTS);

		if (CollectionUtils.isEmpty(contentsList)) {
			return "";
		}

		// Get Media
		List<Map<String, Object>> mediaList = hashTable.get("Media");

		// Sort Media by Contents.Item
		if (mediaList != null && mediaList.size() > 1) {
			Collections.sort(contentsList, comp);
		}

		for (Map<String, Object> map : contentsList) {
			String title = (String) map.get("Title");
			side = (String) map.get("Side");

			if (mediaList != null) {
				item = ((Number) map.get(CONTENTS_ITEM)).intValue();
				Map<String, Object> mapItem = mediaList.get(item - 1);

				if (item != oldItem) {
					if (item > 1) {
						newLine.append("\n");
					}

					sideTitle = (String) mapItem.get("Title");
					sideATitle = (String) mapItem.get("SideATitle");
					sideBTitle = (String) mapItem.get("SideBTitle");

					sideAPlaytime = (Integer) mapItem.get("SideALength");
					sideBPlaytime = (Integer) mapItem.get("SideBLength");

					if (sideTitle == null) {
						sideTitle = "";
					}

					isDoubleSided = sideBPlaytime.intValue() > 0;
					if (mapItem.containsKey("DoubleSided")) {
						isDoubleSided = (Boolean) mapItem.get("DoubleSided");
					}

					if (useContentsItemTitle) {
						newLine.append(item + " - ");
						if (useContentsLength) {
							newLine.append("(" + General.convertDuration((Integer) mapItem.get("Length")) + ") ");
						}

						newLine.append(sideTitle);
						addToList(newLine, result);
					}
				}

				if (isDoubleSided && useContentsSide && !side.equals(oldSide)) {
					sidePlaytime = side.equals("A") ? sideAPlaytime : sideBPlaytime;
					sideTitle = side.equals("A") ? sideATitle : sideBTitle;
					if (sideTitle == null) {
						sideTitle = "";
					}

					if (useContentsLength) {
						newLine.append(side + " - (" + General.convertDuration(sidePlaytime) + ") " + sideTitle);
					} else {
						if (sideTitle.length() > 0) {
							newLine.append(side + " - " + sideTitle);
						} else {
							newLine.append(side);
						}
					}
					addToList(newLine, result);
				}
			}

			if (useContentsIndex) {
				newLine.append(General.convertTrack((Number) map.get("Index"), itemLength) + " ");
			}

			if (useContentsLength) {
				newLine.append("(" + General.convertDuration((Integer) map.get("Length")) + ") ");
			}

			newLine.append(title);
			addToList(newLine, result);
			oldItem = item;
			oldSide = side;
		}
		return result.toString();
	}
}