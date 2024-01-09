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
import application.utils.BasisField;
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
	private static final String COLLECTION_TYPE = "CollectionType";
	private static final String CONTENTS = "Contents";
	private static final String CONTENTS_ITEM = "Contents.Item";
	private static final String CONTENTS_TYPE = "Type";
	private static final String DIRECTOR = "Director";
	private static final String EPISODE_NO = "EpisodeNo";
	private static final String EPISODES = "Episodes";
	private static final String GENRES = "GenreSort";
	private static final String IMDB_ID = "IMDbID";
	private static final String LAST_VIEWED = "LastViewed";
	private static final String PERSON = "Person";
	private static final String PRODUCTION_YEAR = "ProductionYear";
	private static final String RUN_TIME = "Length";
	private static final String SEASON = "Season";
	private static final String SERIES = "Series";
	private static final String STATUS = "WatchingStatus";
	private static final String SUPPORT_CAST = "SupportCast";
	private static final String SYNOPSIS = "Synopsis";
	private static final String TITLE = "Title";
	private static final String TV_CREATOR = "TVCreator";
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
	protected List<BasisField> getBuddyFields() {
		List<BasisField> result = new ArrayList<>();
		if (useMovieBuddy) {
			result.add(new BasisField(TITLE, TITLE, TITLE, FieldTypes.TEXT));
			result.add(new BasisField(CONTENTS_TYPE, CONTENTS_TYPE, "Content Type", FieldTypes.TEXT));
			result.add(new BasisField(COLLECTION_TYPE, COLLECTION_TYPE, "Collection Type", FieldTypes.TEXT));
			result.add(new BasisField(SERIES, SERIES, SERIES, FieldTypes.TEXT));
			result.add(new BasisField(RUN_TIME, RUN_TIME, "Runtime", FieldTypes.DURATION));
			result.add(new BasisField(PRODUCTION_YEAR, PRODUCTION_YEAR, "Release Year", FieldTypes.DURATION));
			result.add(new BasisField(GENRES, GENRES, "Genres", FieldTypes.TEXT));
			result.add(new BasisField(DIRECTOR, DIRECTOR, "Directors", FieldTypes.TEXT));
			result.add(new BasisField(TV_CREATOR, TV_CREATOR, "TV Creators", FieldTypes.TEXT));
			result.add(new BasisField(PERSON, PERSON, "Cast", FieldTypes.MEMO));
			result.add(new BasisField(IMDB_ID, IMDB_ID, "IMDB ID", FieldTypes.TEXT));
			result.add(new BasisField(EPISODES, EPISODES, "TV Episodes", FieldTypes.MEMO));
			result.add(new BasisField(SYNOPSIS, SYNOPSIS, "Summary", FieldTypes.MEMO));
			result.add(new BasisField(STATUS, STATUS, "Status", FieldTypes.TEXT));
			result.add(new BasisField(SEASON, SEASON, "TV Season", FieldTypes.NUMBER));
			result.add(new BasisField(CATEGORY, CATEGORY, CATEGORY, FieldTypes.TEXT));

			useContentsLength = true;
			useSeason = true;
		}

		return result;
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
		useType = userFields.contains(CONTENTS_TYPE);

		if (!useDirector) {
			useDirector = userFields.contains(TV_CREATOR);
		}

		if (useAudio && !userFields.contains(AUDIO_LANGUAGE)) {
			result.add(AUDIO_LANGUAGE);
		}

		if (useCategory && !useType) {
			result.add(CONTENTS_TYPE);
			useType = true;
		}

		if (useStatus && !userFields.contains(LAST_VIEWED)) {
			result.add(LAST_VIEWED);
		}

		if (useDirector && !usePerson) {
			result.add(PERSON);
			result.add(TV_CREATOR);
		}

		if (!userFields.contains(VIDEO_TITLE)) {
			result.add(VIDEO_TITLE);
		}

		if (useSeason && !userFields.contains(SEASON)) {
			result.add(SEASON);
		}

		if (!userFields.contains(SERIES)) {
			result.add(SERIES);
		}

		if (useEpisodeNo && !userFields.contains(EPISODE_NO)) {
			result.add(EPISODE_NO);
		}

		if (useEpisodeNo && !userFields.contains(EPISODES)) {
			result.add(EPISODES);
		}

		if (useContentsLength && !userFields.contains(RUN_TIME)) {
			result.add(RUN_TIME);
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
		result.add("Index");
		result.add(EPISODE_NO);
		result.addAll(sortList);
		return result;
	}

	@Override
	public void setupDbInfoToWrite() {
		super.setupDbInfoToWrite();
		if (useMovieBuddy) {
			validateBuddyHeaders("config/MovieBuddy.yaml");
		}
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		int myVideoID = (Integer) dbDataRecord.get(myTableID);
		lastIndex = Math.max(lastIndex, myVideoID);

		if (useContents) {
			String s = dbDataRecord.get("NumberOfSegments").toString();
			if (Integer.parseInt(s) > 1) {
				dbDataRecord.put(CONTENTS, getVideoContents(s.length(), hashTable));
			}
		}

		if (!myTable.equals(CONTENTS)) {
			return;
		}

		dbDataRecord.put(DIRECTOR, General.EMPTY_STRING);
		dbDataRecord.put(TV_CREATOR, General.EMPTY_STRING);
		dbDataRecord.put(PERSON, General.EMPTY_STRING);

		if (useAudio) {
			getAudioTracks(dbDataRecord, hashTable);
		}

		if (useCategory || useType) {
			getCategoryAndType(dbDataRecord, hashTable);
		}

		if (useSeason) {
			Number season = (Number) dbDataRecord.getOrDefault(SEASON, 0);
			if (season.intValue() == 0) {
				dbDataRecord.put(SEASON, General.EMPTY_STRING);
			}
		}

		if (useEpisodeNo) {
			Number episodeNo = (Number) dbDataRecord.getOrDefault(EPISODE_NO, 0);
			if (episodeNo.intValue() == 0) {
				dbDataRecord.put(EPISODE_NO, General.EMPTY_STRING);
				dbDataRecord.put(EPISODES, General.EMPTY_STRING);
			} else {
				dbDataRecord.put(EPISODES, dbDataRecord.get(TITLE));
				dbDataRecord.put(TITLE, dbDataRecord.get(VIDEO_TITLE));
			}
		}

		if (useStatus) {
			String viewed = dbDataRecord.getOrDefault(LAST_VIEWED, General.EMPTY_STRING).toString();
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
		List<Map<String, Object>> listType = hashTable.getOrDefault(CONTENTS_TYPE, new ArrayList<>());

		if (!listType.isEmpty()) {
			Map<String, Object> typeMap = listType.get(0);
			Object type = typeMap.get("ContentsTypeID");
			if (type != null) {
				dbDataRecord.put(CONTENTS_TYPE, typeMap.get("ContentsType"));
				switch (type.toString()) {
				case "1":
					dbDataRecord.put(CATEGORY, "Movies & TV Shows");
					break;
				case "2":
					dbDataRecord.put(CATEGORY, "Movies & TV Shows");
					dbDataRecord.put(CONTENTS_TYPE, "TV Show");
					dbDataRecord.put(COLLECTION_TYPE, "Single Season");
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

		// Maps by supporting role and their character role
		Map<String, String> castSupportMap = new LinkedHashMap<>();

		// Maps by director
		Set<String> directorSet = new LinkedHashSet<>();

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
						addActorRole(castMap, map, pMap);
					}
					break;
				case 2: // Supporting character
					if (usePerson && useEntireCast) {
						addActorRole(castSupportMap, map, pMap);
					}
					break;
				case 3: // Crew
					if (useDirector) {
						addDirector(directorSet, map, pMap);
					}
					break;
				default:
					// Nothing to do
					break;
				}
			});
		});

		StringBuilder bCast = getCastFromCastMap(castMap);
		StringBuilder bSupportCast = getCastFromCastMap(castSupportMap);

		dbDataRecord.put(SUPPORT_CAST, bSupportCast.toString());
		dbDataRecord.put(PERSON, bCast.toString());

		dbDataRecord.put(dbDataRecord.get(SEASON).toString().isEmpty() ? DIRECTOR : TV_CREATOR,
				getDirector(directorSet));
	}

	private Object getDirector(Set<String> directorSet) {
		if (directorSet.isEmpty()) {
			return General.EMPTY_STRING;
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
					result.append(" [").append(cast.getValue()).append("]");
				}
				result.append("\n");
			}
		}
		return result;
	}

	private void addDirector(Set<String> directorSet, Map<String, Object> map, Map<String, Object> pMap) {
		int roleID = (Integer) pMap.get("RoleID");
		if (roleID == 1) {
			String directorName = map.getOrDefault("Name", General.EMPTY_STRING).toString();

			if (!directorName.isEmpty()) {
				directorSet.add(directorName);
			}
		}
	}

	private void addActorRole(Map<String, String> castMap, Map<String, Object> map, Map<String, Object> pMap) {
		String castName = map.getOrDefault("Name", General.EMPTY_STRING).toString();

		String character = General.EMPTY_STRING;
		if (useRoles) {
			character = pMap.getOrDefault("Character", General.EMPTY_STRING).toString();
		}

		if (!castName.isEmpty()) {
			castMap.putIfAbsent(castName, character);
		}
	}

	// method that returns the Video Contents as String
	private String getVideoContents(int itemLength, Map<String, List<Map<String, Object>>> hashTable) {
		StringBuilder result = new StringBuilder();
		StringBuilder newLine = new StringBuilder();

		String sideATitle = General.EMPTY_STRING;
		String sideBTitle = General.EMPTY_STRING;

		Number sideAPlaytime = 0;
		Number sideBPlaytime = 0;
		String oldSide = General.EMPTY_STRING;

		int item = 0;
		int oldItem = 0;

		boolean isDoubleSided = false;

		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get(CONTENTS);

		if (CollectionUtils.isEmpty(contentsList)) {
			return General.EMPTY_STRING;
		}

		// Get Media
		List<Map<String, Object>> mediaList = hashTable.get("Media");

		// Sort Media by Contents.Item
		if (mediaList != null && mediaList.size() > 1) {
			Collections.sort(contentsList, compContents);
			Collections.sort(mediaList, compMedia);
		}

		for (Map<String, Object> map : contentsList) {
			String title = (String) map.get(TITLE);
			String side = (String) map.get("Side");

			if (mediaList != null) {
				item = ((Number) map.get(CONTENTS_ITEM)).intValue();
				Map<String, Object> mapItem = mediaList.get(item - 1);

				if (item != oldItem) {
					if (item > 1) {
						newLine.append("\n");
					}

					String sideTitle = (String) mapItem.get(TITLE);
					sideATitle = (String) mapItem.get("SideATitle");
					sideBTitle = (String) mapItem.get("SideBTitle");

					sideAPlaytime = (Number) mapItem.get("SideALength");
					sideBPlaytime = (Number) mapItem.get("SideBLength");

					if (sideTitle == null) {
						sideTitle = General.EMPTY_STRING;
					}

					isDoubleSided = sideBPlaytime.intValue() > 0;
					if (mapItem.containsKey("DoubleSided")) {
						isDoubleSided = (Boolean) mapItem.get("DoubleSided");
					}

					if (useContentsItemTitle) {
						newLine.append(item + " - ");
						if (useContentsLength) {
							newLine.append("(" + General.convertDuration((Integer) mapItem.get(RUN_TIME)) + ") ");
						}

						newLine.append(sideTitle);
						addToList(newLine, result);
					}
				}

				if (isDoubleSided && useContentsSide && !side.equals(oldSide)) {
					int sidePlaytime = side.equals("A") ? sideAPlaytime.intValue() : sideBPlaytime.intValue();
					String sideTitle = side.equals("A") ? sideATitle : sideBTitle;
					if (sideTitle == null) {
						sideTitle = General.EMPTY_STRING;
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
				Number season = (Number) map.getOrDefault(SEASON, -1);
				if (season.intValue() != -1) {
					newLine.append("S" + General.addLeadingZeroes(season, 2));
					if (useEpisodeNo) {
						newLine.append("E");
					}
				}
			}

			if (useEpisodeNo) {
				Number episode = (Number) map.getOrDefault(EPISODE_NO, -1);
				if (episode.intValue() != -1) {
					newLine.append(General.convertTrack((Number) map.get(EPISODE_NO), itemLength) + General.SPACE);
				}
			}

			if (useContentsLength) {
				newLine.append("(" + General.convertDuration((Integer) map.get(RUN_TIME)) + ") ");
			}

			newLine.append(title);
			addToList(newLine, result);
			oldItem = item;
			oldSide = side;
		}
		return result.toString();
	}
}