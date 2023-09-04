package fnprog2pda.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	private boolean useContentsLength = true;
	private boolean useContentsSide = true;
	private boolean useContentsIndex = true;
	private boolean useDirector = true;
	private boolean useEntireCast = false;
	private boolean usePerson = false;

	private static final String CONTENTS = "Contents";
	private static final String CONTENTS_ITEM = "Contents.Item";
	private static final String DIRECTOR = "Director";
	private static final String DIRECTOR_SORT = "DirectorSort";
	private static final String PERSON = "Person";
	private static final String PERSON_SORT = "Person.PersonSort";
	private static final String SORT_BY = "SortBy";

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

		if (usePerson || useDirector) {
			getPersonWithRoles(dbDataRecord, hashTable);
		}
	}

	private void getPersonWithRoles(Map<String, Object> dbDataRecord,
			Map<String, List<Map<String, Object>>> hashTable) {
		StringBuilder result = new StringBuilder();
		StringBuilder resultSort = new StringBuilder();

		List<Map<String, Object>> creditsList = hashTable.get("CreditsLink");
		List<Map<String, Object>> personList = hashTable.get(PERSON);

		personList.forEach(map -> {
			List<Map<String, Object>> personCredit = creditsList.stream()
					.filter(credit -> credit.get("PersonID").equals(map.get("PersonID"))).collect(Collectors.toList());

			if (!personCredit.isEmpty()) {
				personCredit.forEach(pMap -> {
					byte roleType = (Byte) pMap.get("RoleType");
					boolean isActor = false;
					switch (roleType) {
					case 1: // Leading character
						if (usePerson) {
							isActor = true;
						}
						break;
					case 2: // Supporting character
						if (usePerson && useEntireCast) {
							isActor = true;
						}
						break;
					case 3: // Crew
						if (useDirector) {
							int roleID = (Integer) pMap.get("RoleID");
							if (roleID == 1) {
								dbDataRecord.put(DIRECTOR, map.get("Name"));
								dbDataRecord.put(DIRECTOR_SORT, map.get(SORT_BY));
							}
						}
						break;
					default:
						// Nothing to do
						break;
					}

					if (isActor) {
						result.append(map.get("Name"));
						resultSort.append(map.get(SORT_BY));
						if (useRoles) {
							result.append(" (" + pMap.get("Character") + ")");
							resultSort.append(" (" + pMap.get("Character") + ")");
						}
						result.append("\n");
						resultSort.append("\n");
					}
				});
			}
		});

		dbDataRecord.put(PERSON, result.toString());
		dbDataRecord.put(PERSON_SORT, resultSort.toString());
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