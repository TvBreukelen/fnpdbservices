package fnprog2pda.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
	private boolean useAudio;
	private boolean useCategory;
	private boolean useContentsLength;
	private boolean useContentsSide;
	private boolean useEpisodeNo;
	private boolean useDirector;
	private boolean useEntireCast;
	private boolean usePerson;
	private boolean useStatus;
	private boolean useSeason;
	private boolean useType;
	private boolean useMovieBuddy;

	private static final String AUDIO = "Audio";
	private static final String AUDIO_LANGUAGE = "AudioLanguageSort";
	private static final String CATEGORY = "Category";
	private static final String CONTENTS = "Contents";
	private static final String CONTENTS_ITEM = "Contents.Item";
	private static final String DIRECTOR = "Director";
	private static final String DIRECTOR_SORT = "DirectorSort";
	private static final String EPISODE_NO = "EpisodeNo";
	private static final String EPISODES = "Episodes";
	private static final String LAST_VIEWED = "LastViewed";
	private static final String LENGTH = "Length";
	private static final String PERSON = "Person";
	private static final String PERSON_SORT = "Person.PersonSort";
	private static final String SEASON = "Season";
	private static final String SORT_BY = "SortBy";
	private static final String STATUS = "WatchingStatus";
	private static final String SUPPORT_CAST = "SupportCast";
	private static final String SUPPORT_CAST_SORT = "SupportCastSort";
	private static final String TYPE = "Type";
	private static final String VIDEO_TITLE = "Video.TitleSort";

	private Map<String, FieldTypes> sortContents = new LinkedHashMap<>();
	private Map<String, FieldTypes> sortMedia = new LinkedHashMap<>();
	private XComparator compContents = new XComparator(sortContents);
	private XComparator compMedia = new XComparator(sortMedia);

	public CATVids() {
		super();
		useContentsLength = pdaSettings.isUseContentsLength();
		useContentsSide = pdaSettings.isUseContentsSide();
		useEpisodeNo = pdaSettings.isUseContentsIndex();
		useSeason = pdaSettings.isUseSeason();
		useEntireCast = pdaSettings.isUseEntireCast();
		useMovieBuddy = pdaSettings.getTextFileFormat().equals("buddyCsv");

		sortContents.put(CONTENTS_ITEM, FieldTypes.NUMBER);
		sortContents.put(EPISODE_NO, FieldTypes.NUMBER);
		sortMedia.put("DiscNo", FieldTypes.NUMBER);
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		List<String> result = new ArrayList<>();
		useContents = userFields.contains(CONTENTS);

		if (useContents) {
			result.add(CONTENTS_ITEM);
			result.add("Media.Item");
			result.add("NumberOfSegments");
		}

		return result;
	}

	@Override
	protected List<String> getSystemFields(List<String> userFields) {
		List<String> result = new ArrayList<>();
		if (!myTable.equals(CONTENTS)) {
			return result;
		}

		useAudio = userFields.contains(AUDIO);
		usePerson = userFields.contains(PERSON);
		useDirector = userFields.contains(DIRECTOR);
		useCategory = userFields.contains(CATEGORY);
		useStatus = userFields.contains(STATUS);
		useType = userFields.contains(TYPE);

		if (useAudio && !userFields.contains(AUDIO_LANGUAGE)) {
			result.add(AUDIO_LANGUAGE);
		}

		if (useCategory && !useType) {
			result.add("Type");
			useType = true;
		}

		if (!usePerson) {
			usePerson = userFields.contains(PERSON_SORT);
		}

		if (!useDirector) {
			useDirector = userFields.contains(DIRECTOR_SORT);
		}

		if (useStatus && !userFields.contains(LAST_VIEWED)) {
			result.add(LAST_VIEWED);
		}

		if (useDirector && !usePerson) {
			result.add(PERSON);
		}

		if (!userFields.contains(VIDEO_TITLE)) {
			result.add(VIDEO_TITLE);
		}

		if (useSeason && !userFields.contains(SEASON)) {
			result.add(SEASON);
		}

		if (!userFields.contains("Series")) {
			result.add("Series");
		}

		if (useEpisodeNo && !userFields.contains(EPISODE_NO)) {
			result.add(EPISODE_NO);
		}

		if (useEpisodeNo && !userFields.contains(EPISODES)) {
			result.add(EPISODES);
		}

		if (useContentsLength && !userFields.contains(LENGTH)) {
			result.add(LENGTH);
		}

		if (useContentsLength && !userFields.contains("OrigAirDate")) {
			result.add("OrigAirDate");
		}
		return result;
	}

	@Override
	protected List<String> getMandatorySortFields(List<String> sortList) {
		if (!useMovieBuddy) {
			return sortList;
		}

		List<String> result = new ArrayList<>();
		result.add(VIDEO_TITLE);
		result.add("DiscNo");
		result.add(EPISODE_NO);
		result.addAll(sortList);
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

		if (!myTable.equals(CONTENTS)) {
			return;
		}

		dbDataRecord.put(DIRECTOR, "");
		dbDataRecord.put(DIRECTOR_SORT, "");
		dbDataRecord.put(PERSON, "");
		dbDataRecord.put(PERSON_SORT, "");

		if (useAudio) {
			getAudioTracks(dbDataRecord, hashTable);
		}

		if (useCategory || useType) {
			getCategoryAndType(dbDataRecord, hashTable);
		}

		if (useSeason) {
			Number season = (Number) dbDataRecord.getOrDefault(SEASON, 0);
			if (season.intValue() == 0) {
				dbDataRecord.put(SEASON, "");
			}
		}

		if (useEpisodeNo) {
			Number episodeNo = (Number) dbDataRecord.getOrDefault(EPISODE_NO, 0);
			if (episodeNo.intValue() == 0) {
				dbDataRecord.put(EPISODE_NO, "");
				dbDataRecord.put(EPISODES, "");
			} else {
				dbDataRecord.put(EPISODES, dbDataRecord.get("Title"));
				dbDataRecord.put("Title", dbDataRecord.get(VIDEO_TITLE));
			}
		}

		if (useStatus) {
			String viewed = dbDataRecord.getOrDefault(LAST_VIEWED, "").toString();
			dbDataRecord.put(STATUS, StringUtils.isEmpty(viewed) ? "Not Watched" : "Watched");
		}

		if (usePerson || useDirector) {
			getPersonWithRoles(dbDataRecord, hashTable);
		}
	}

	private void getAudioTracks(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable) {
		List<Map<String, Object>> listAudio = hashTable.getOrDefault(AUDIO, new ArrayList<>());
		if (!listAudio.isEmpty()) {
			List<Map<String, Object>> listLanguage = hashTable.getOrDefault("AudioLanguage", new ArrayList<>());
			if (listLanguage.size() == listAudio.size()) {
				StringBuilder result = new StringBuilder();
				for (int i = 0; i < listLanguage.size(); i++) {
					result.append(listLanguage.get(i).get("Language")).append(" (");
					result.append(listAudio.get(i).get(AUDIO)).append("); ");
				}
				result.delete(result.length() - 1, result.length());
				dbDataRecord.put(AUDIO, result.toString());
			}
		}
	}

	private void getCategoryAndType(Map<String, Object> dbDataRecord,
			Map<String, List<Map<String, Object>>> hashTable) {
		List<Map<String, Object>> listType = hashTable.getOrDefault(TYPE, new ArrayList<>());

		if (!listType.isEmpty()) {
			Map<String, Object> typeMap = listType.get(0);
			Object type = typeMap.get("ContentsTypeID");
			if (type != null) {
				dbDataRecord.put(TYPE, typeMap.get("ContentsType"));
				switch (type.toString()) {
				case "1":
					dbDataRecord.put(CATEGORY, "Movies & TV Shows");
					break;
				case "2":
					dbDataRecord.put(CATEGORY, "Movies & TV Shows");
					dbDataRecord.put(TYPE, "Single Season");
					break;
				default:
					dbDataRecord.put(CATEGORY, typeMap.get("ContentsType"));
				}
			}
		}
	}

	private void getPersonWithRoles(Map<String, Object> dbDataRecord,
			Map<String, List<Map<String, Object>>> hashTable) {

		// Maps by leading role and their character role
		Map<String, String> castMap = new LinkedHashMap<>();
		Map<String, String> castSortMap = new LinkedHashMap<>();

		// Maps by supporting role and their character role
		Map<String, String> castSupportMap = new LinkedHashMap<>();
		Map<String, String> castSupportSortMap = new LinkedHashMap<>();

		// Maps by director
		Set<String> directorSet = new LinkedHashSet<>();
		Set<String> directorSortSet = new LinkedHashSet<>();

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
						addActorRole(castMap, castSortMap, map, pMap);
					}
					break;
				case 2: // Supporting character
					if (usePerson && useEntireCast) {
						addActorRole(castSupportMap, castSupportSortMap, map, pMap);
					}
					break;
				case 3: // Crew
					if (useDirector) {
						addDirector(directorSet, directorSortSet, map, pMap);
					}
					break;
				default:
					// Nothing to do
					break;
				}
			});
		});

		StringBuilder bCast = getCastFromCastMap(castMap);
		StringBuilder bCastSort = getCastFromCastMap(castSortMap);
		StringBuilder bSupportCast = getCastFromCastMap(castSupportMap);
		StringBuilder bSupportCastSort = getCastFromCastMap(castSupportSortMap);

		dbDataRecord.put(SUPPORT_CAST, bSupportCast.toString());
		dbDataRecord.put(SUPPORT_CAST_SORT, bSupportCastSort.toString());
		dbDataRecord.put(PERSON, bCast.toString());
		dbDataRecord.put(PERSON_SORT, bCastSort.toString());
		dbDataRecord.put(DIRECTOR, getDirector(directorSet));
		dbDataRecord.put(DIRECTOR_SORT, getDirector(directorSortSet));
	}

	private Object getDirector(Set<String> directorSet) {
		if (directorSet.isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		directorSet.forEach(d -> result.append(d).append("; "));
		result.delete(result.length() - 2, result.length());
		return result.toString();
	}

	private StringBuilder getCastFromCastMap(Map<String, String> castMap) {
		StringBuilder result = new StringBuilder();
		if (!castMap.isEmpty()) {
			for (Entry<String, String> cast : castMap.entrySet()) {
				result.append(cast.getKey());
				if (!cast.getValue().isEmpty()) {
					result.append(" (").append(cast.getValue()).append(")");
				}
				result.append("\n");
			}
		}
		return result;
	}

	private void addDirector(Set<String> directorSet, Set<String> directorSortSet, Map<String, Object> map,
			Map<String, Object> pMap) {
		int roleID = (Integer) pMap.get("RoleID");
		if (roleID == 1) {
			String directorName = map.getOrDefault("Name", "").toString();
			String directorSortName = map.getOrDefault(SORT_BY, "").toString();

			if (!directorName.isEmpty()) {
				directorSet.add(directorName);
			}

			if (!directorSortName.isEmpty()) {
				directorSortSet.add(directorSortName);
			}
		}
	}

	private void addActorRole(Map<String, String> castMap, Map<String, String> castSortMap, Map<String, Object> map,
			Map<String, Object> pMap) {

		String castName = map.getOrDefault("Name", "").toString();
		String castSortName = map.getOrDefault(SORT_BY, "").toString();

		String character = "";
		if (useRoles) {
			character = pMap.getOrDefault("Character", "").toString();
		}

		if (!castName.isEmpty()) {
			castMap.putIfAbsent(castName, character);
		}

		if (!castSortName.isEmpty()) {
			castSortMap.putIfAbsent(castSortName, character);
		}
	}

	// method that returns the Video Contents as String
	private String getVideoContents(int itemLength, Map<String, List<Map<String, Object>>> hashTable) {
		StringBuilder result = new StringBuilder();
		StringBuilder newLine = new StringBuilder();

		String sideATitle = "";
		String sideBTitle = "";

		Number sideAPlaytime = 0;
		Number sideBPlaytime = 0;
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
			Collections.sort(contentsList, compContents);
			Collections.sort(mediaList, compMedia);
		}

		for (Map<String, Object> map : contentsList) {
			String title = (String) map.get("Title");
			String side = (String) map.get("Side");

			if (mediaList != null) {
				item = ((Number) map.get(CONTENTS_ITEM)).intValue();
				Map<String, Object> mapItem = mediaList.get(item - 1);

				if (item != oldItem) {
					if (item > 1) {
						newLine.append("\n");
					}

					String sideTitle = (String) mapItem.get("Title");
					sideATitle = (String) mapItem.get("SideATitle");
					sideBTitle = (String) mapItem.get("SideBTitle");

					sideAPlaytime = (Number) mapItem.get("SideALength");
					sideBPlaytime = (Number) mapItem.get("SideBLength");

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
							newLine.append("(" + General.convertDuration((Integer) mapItem.get(LENGTH)) + ") ");
						}

						newLine.append(sideTitle);
						addToList(newLine, result);
					}
				}

				if (isDoubleSided && useContentsSide && !side.equals(oldSide)) {
					int sidePlaytime = side.equals("A") ? sideAPlaytime.intValue() : sideBPlaytime.intValue();
					String sideTitle = side.equals("A") ? sideATitle : sideBTitle;
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

			if (useSeason) {
				Number season = (Number) map.getOrDefault(SEASON, 0);
				if (season.intValue() > 0) {
					newLine.append("S" + General.convertTrack(season, 2));
					if (useEpisodeNo) {
						newLine.append("E");
					}
				}
			}

			if (useEpisodeNo) {
				Number episode = (Number) map.getOrDefault(EPISODE_NO, 0);
				if (episode.intValue() > 0) {
					newLine.append(General.convertTrack((Number) map.get(EPISODE_NO), itemLength) + " ");
				}
			}

			if (useContentsLength) {
				newLine.append("(" + General.convertDuration((Integer) map.get(LENGTH)) + ") ");
			}

			newLine.append(title);
			addToList(newLine, result);
			oldItem = item;
			oldSide = side;
		}
		return result.toString();
	}
}