package fnprog2pda.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import application.interfaces.FieldTypes;
import application.utils.BasisField;
import application.utils.General;
import application.utils.XComparator;

public class CATraxx extends FNProgramvare {
	/**
	 * Title: CATraxx Description: CATraxx Class Copyright: (c) 2003-2011
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private String myPerson = "[None]";
	private boolean useContentsLength = true;
	private boolean useContentsSide = true;
	private boolean useContentsIndex = true;

	private boolean useCategory = false;
	private boolean useComposers = false;
	private boolean useConductors = false;
	private boolean useGenresAndStyles = false;
	private boolean useOrchestras = false;
	private boolean useWriters = false;
	private boolean useRating = false;
	private boolean useReleaseYear = false;
	private boolean usePerformers = false;

	private boolean isPlaylist = false;
	private boolean isBoxSet = false;

	private boolean useMusicBuddy;

	private static final String ALBUM = "Album";
	private static final String ARTIST = "Artist";
	private static final String ARTIST_PERFORMER = "ArtistPerformer";
	private static final String ARTIST_PERSON_ID = "ArtistPersonID";
	private static final String ARTIST_SORT = "ArtistSort";
	private static final String BAR_CODE = "BarCode";
	private static final String BOX_ITEM = "BoxSetIndex";
	private static final String CATEGORY = "Category";
	private static final String COMPOSERS = "Composers";
	private static final String CONDUCTORS = "Conductors";
	private static final String CONTENTS_PERSON = "ContentsPerson";
	private static final String FORMAT = "Format";
	private static final String GENRES = "PrimaryGenre.MainGenre";
	private static final String INSTRUMENT = "Instrument";
	private static final String INSTRUMENT_ID = "InstrumentID";
	private static final String ORCHESTRAS = "Orchestras";
	private static final String PERFORMERS = "Performers";
	private static final String RATING = "PersonalRating";
	private static final String RELEASED = "Released";
	private static final String RELEASE_YEAR = "ReleaseYear";
	private static final String STYLES = "Genre";
	private static final String TITLE = "Title";
	private static final String TITLE_SORT = "TitleSort";
	private static final String TRACKS = "Tracks";
	private static final String TRACKS_ITEM = "Tracks.Item";
	private static final String WRITERS = "Writers";

	private Map<String, FieldTypes> sortTracks = new LinkedHashMap<>();
	private Map<String, FieldTypes> sortMedia = new LinkedHashMap<>();
	private XComparator compTracks = new XComparator(sortTracks);
	private XComparator compMedia = new XComparator(sortMedia);

	public CATraxx() {
		super();
		useContentsLength = pdaSettings.isUseContentsLength();
		useContentsSide = pdaSettings.isUseContentsSide();
		useContentsIndex = pdaSettings.isUseContentsIndex();
		useMusicBuddy = pdaSettings.getTextFileFormat().equals("buddyCsv");

		personField = new String[] { ARTIST, ARTIST_SORT };
	}

	@Override
	protected List<BasisField> getBuddyFields() {
		List<BasisField> result = new ArrayList<>();
		if (useMusicBuddy) {
			result.add(new BasisField(ARTIST, ARTIST, ARTIST, FieldTypes.TEXT));
			result.add(new BasisField(ARTIST_SORT, ARTIST_SORT, "Artist (Last, First)", FieldTypes.TEXT));
			result.add(new BasisField(BAR_CODE, BAR_CODE, "UPC-EAN13", FieldTypes.TEXT));
			result.add(new BasisField(FORMAT, FORMAT, "Media", FieldTypes.TEXT));
			result.add(new BasisField(CATEGORY, CATEGORY, CATEGORY, FieldTypes.TEXT));
			result.add(new BasisField("FormatGroup", "FormatGroup", "Content Type", FieldTypes.TEXT));
			result.add(new BasisField(GENRES, GENRES, "Genres", FieldTypes.TEXT));
			result.add(new BasisField(PERFORMERS, PERFORMERS, PERFORMERS, FieldTypes.MEMO));
			result.add(new BasisField(RELEASE_YEAR, RELEASE_YEAR, "Release Year", FieldTypes.TEXT));
			result.add(new BasisField(STYLES, STYLES, "Styles", FieldTypes.TEXT));
			result.add(new BasisField(TITLE, TITLE, TITLE, FieldTypes.TEXT));
			result.add(new BasisField(TRACKS, TRACKS, TRACKS, FieldTypes.MEMO));
		}

		return result;
	}

	@Override
	protected List<String> getMandatorySortFields(List<String> sortList) {
		if (!useMusicBuddy) {
			return sortList;
		}

		List<String> result = new ArrayList<>();
		result.add(ARTIST_SORT);
		result.add(TITLE_SORT);
		result.addAll(sortList);
		return result;
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		List<String> result = new ArrayList<>();

		isBoxSet = myTable.equals("BoxSet");
		useContentsPerson = useContentsPerson || isBoxSet;
		useContents = userFields.contains(TRACKS) || isBoxSet;
		isPlaylist = myTable.equals("Playlist");

		if (useContents) {
			if (useContentsPerson) {
				result.add("ContentsPersonSort");
				result.add(CONTENTS_PERSON);
			}

			if (isBoxSet) {
				result.add("Album.BoxSetID");
				result.add("Format.FormatID");
				result.add("ContentsLink.AlbumID");
				sortTracks.put(BOX_ITEM, FieldTypes.NUMBER);
				useRoles = true;
				return result;
			}

			result.add("NumberOfTracks");
			result.add("DiscCount");
			result.add("ContentsLink.TrackID");
			result.add(TRACKS_ITEM);

			if (isPlaylist) {
				result.add("PlaylistType");
			} else {
				result.add("Media.Item");
			}

			sortTracks.put(TRACKS_ITEM, FieldTypes.NUMBER);
			sortTracks.put("Index", FieldTypes.NUMBER);
			sortMedia.put("DiscNo", FieldTypes.NUMBER);
		}
		return result;
	}

	@Override
	protected List<String> getSystemFields(List<String> userFields) {
		List<String> result = new ArrayList<>();
		result.add(TITLE);

		useCategory = userFields.contains(CATEGORY);
		useComposers = userFields.contains(COMPOSERS);
		useConductors = userFields.contains(CONDUCTORS);
		useOrchestras = userFields.contains(ORCHESTRAS);

		useReleaseYear = userFields.contains(RELEASE_YEAR);
		useRating = userFields.contains(RATING);
		usePerformers = userFields.contains(PERFORMERS);
		useWriters = userFields.contains(WRITERS);
		useGenresAndStyles = useMusicBuddy;

		if (useCategory) {
			result.add(GENRES);
		}

		if (useConductors) {
			result.add("ArrangerTrackLink.RoleID");
			result.add("ArtistArranger");
		}

		if (!useGenresAndStyles) {
			useGenresAndStyles = userFields.contains(GENRES) && userFields.contains(STYLES);
		}

		if (useReleaseYear) {
			result.add(RELEASED);
		}

		if (useRating) {
			result.add("PersonalRatingID");
		}

		if (usePerformers || useOrchestras) {
			result.add("MusicianTrackLink.RoleType");
			result.add(ARTIST_PERFORMER);
			result.add(INSTRUMENT);
			useRoles = true;
		}

		if (useComposers || useWriters) {
			result.add("AuthorTrackLink.RoleID");
			result.add("ArtistAuthor");
		}

		if (useMusicBuddy) {
			useContentsLength = true;
			useContentsIndex = true;
		}

		return result;
	}

	@Override
	public void setCategories() throws Exception {
		super.setCategories();
		if (useRoles) {
			switch ((int) Math.floor(Double.parseDouble(mySoftwareVersion))) {
			case 5:
				HashMap<Integer, String> personMap = new HashMap<>();
				myRoles.put(ARTIST, personMap);
				personMap.put(1, "feat.");
				personMap.put(2, "vs.");
				personMap.put(3, "with");
				getProductionRoles();
				break;
			case 6:
				getVersion6Roles();
				getProductionRoles();
				break;
			default:
				getVersion7Roles();
			}
		}
	}

	private void getProductionRoles() {
		Map<Integer, String> personMap = new HashMap<>();
		myRoles.put("ProductionPerson", personMap);
		personMap.put(1, "Assistent Engineer");
		personMap.put(2, "Associate Producer");
		personMap.put(3, "Co Producer");
		personMap.put(4, "Engineer");
		personMap.put(5, "Executive Producer");
		personMap.put(6, "Mastering");
		personMap.put(7, "Mixing");
		personMap.put(8, "Producer");
		personMap.put(9, "Programming");
		personMap.put(10, "Remixing");

		Map<Integer, String> studioMap = new HashMap<>();
		myRoles.put("Studio", studioMap);

		studioMap.put(1, "Mastering Location");
		studioMap.put(2, "Mixing Location");
		studioMap.put(3, "Recording Location");
	}

	private void getVersion6Roles() throws Exception {
		// Obtain the Roles
		Object obj = "FieldDefinitions_" + myTable;
		Map<String, Object> map = msAccess.getSingleRecord("Settings", "ID", Collections.singletonMap("ID", obj));
		String data = new String((byte[]) map.get("Data"));
		int count = 0;
		int index1 = data.indexOf("ArtistMain") - 1;
		int index2 = data.indexOf("ArtistCustom1") - 1;
		data = data.substring(index1, index2);

		Map<Integer, String> personMap = new HashMap<>();
		myRoles.put("µ", personMap);

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

	private void getVersion7Roles() throws Exception {
		getRoles(ARTIST, "ArtistRole", "ArtistRoleID");
		getRoles("ProductionPerson", "ProductionRole", "ProductionRoleID");
		getRoles("Studio", "StudioRole", "StudioRoleID");
	}

	@Override
	public void setupDbInfoToWrite() {
		super.setupDbInfoToWrite();
		if (useMusicBuddy) {
			validateBuddyHeaders("config/MusicBuddy.yaml");
		}
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {

		int myAlbumID = (Integer) dbDataRecord.get(myTableID);
		myLastIndex = Math.max(myLastIndex, myAlbumID);

		if (isPlaylist) {
			Integer typeID = (Integer) dbDataRecord.get("PlaylistType");
			dbDataRecord.put("Type", typeID == 0 ? "Manual" : "Dynamic");
		}

		if (useContents) {
			if (isBoxSet) {
				dbDataRecord.put(ALBUM, getBoxSetAlbums(hashTable));
				return;
			}

			String s = dbDataRecord.get("NumberOfTracks").toString();
			if (Integer.parseInt(s) > 0) {
				try {
					myPerson = dbDataRecord.get(personField[0]).toString();
				} catch (Exception e) {
					myPerson = "[None]";
				}

				Integer discCount = (Integer) dbDataRecord.getOrDefault("DiscCount", 1);
				String albumTitle = dbDataRecord.get(TITLE).toString();
				dbDataRecord.put(TRACKS, getAlbumTracks(s.length(), discCount, albumTitle, hashTable));
			}
		}

		if (useCategory) {
			String genre = dbDataRecord.getOrDefault(GENRES, "").toString();
			dbDataRecord.put(CATEGORY, genre.equals("Classical") ? "Classical" : "Modern");
		}

		if (useConductors) {
			getConductors(dbDataRecord, hashTable);
		}

		if (useOrchestras) {
			getOrchestras(dbDataRecord, hashTable);
		}

		if (usePerformers) {
			getPerformers(dbDataRecord, hashTable);
		}

		if (useWriters || useComposers) {
			getAuthors(dbDataRecord, hashTable);
		}

		if (useRating && useMusicBuddy) {
			Integer rating = (Integer) dbDataRecord.getOrDefault("PersonalRatingID", -1);
			if (rating == -1 || rating > 5) {
				dbDataRecord.put(RATING, "");
			} else {
				dbDataRecord.put(RATING, 6 - rating);
			}
		}

		if (useReleaseYear) {
			String released = dbDataRecord.getOrDefault(RELEASED, "").toString();
			if (released.length() > 3) {
				dbDataRecord.put(RELEASE_YEAR, released.substring(0, 4));
			}
		}

		if (useGenresAndStyles) {
			String genres = dbDataRecord.getOrDefault(GENRES, "").toString();
			String styles = dbDataRecord.getOrDefault(STYLES, "").toString();

			if (genres.equals(styles)) {
				dbDataRecord.put(STYLES, "");
			}
		}
	}

	private void getConductors(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable) {
		dbDataRecord.put(CONDUCTORS, "");

		List<Map<String, Object>> arrangerTrackList = hashTable.getOrDefault("ArrangerTrackLink", new ArrayList<>());
		if (arrangerTrackList.isEmpty()) {
			return;
		}

		// Get the arranger(s)
		List<Integer> arrangers = arrangerTrackList.stream()
				.filter(map -> (Integer) map.getOrDefault("RoleID", -1) == 2)
				.map(x -> (Integer) x.getOrDefault(ARTIST_PERSON_ID, -1)).collect(Collectors.toList());
		if (arrangers.isEmpty()) {
			return;
		}

		List<Map<String, Object>> arranger = hashTable.getOrDefault("ArtistArranger", new ArrayList<>());
		if (arranger.isEmpty()) {
			return;
		}

		// Remove duplicate persons
		Map<Integer, String> mapArranger = mapPersons(arranger);

		StringBuilder result = new StringBuilder();
		mapArranger.entrySet().forEach(entry -> {
			if (arrangers.contains(entry.getKey())) {
				result.append(entry.getValue()).append(", ");
				result.delete(result.lastIndexOf(", "), result.length());
				result.append("\n");
			}
		});

		dbDataRecord.put(CONDUCTORS, result.toString());
	}

	private void getAuthors(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable) {
		dbDataRecord.put(COMPOSERS, "");
		dbDataRecord.put(WRITERS, "");

		List<Map<String, Object>> authorTrackList = hashTable.getOrDefault("AuthorTrackLink", new ArrayList<>());
		if (authorTrackList.isEmpty()) {
			return;
		}

		List<Map<String, Object>> author = hashTable.getOrDefault("ArtistAuthor", new ArrayList<>());
		if (author.isEmpty()) {
			return;
		}

		// Remove duplicate persons
		Map<Integer, String> mapAuthor = mapPersons(author);

		StringBuilder bComposers = new StringBuilder();
		StringBuilder bAuthors = new StringBuilder();

		mapAuthor.entrySet().forEach(entry -> {
			Set<Integer> list = authorTrackList.stream()
					.filter(artist -> artist.getOrDefault(ARTIST_PERSON_ID, -1).equals(entry.getKey()))
					.map(x -> (Integer) x.get("RoleID")).collect(Collectors.toSet());

			if (!list.isEmpty()) {
				list.forEach(id -> {
					switch (id) {
					case 2:
						bComposers.append(entry.getValue()).append("\n");
						break;
					case 1:
					case 4:
					case 5:
					case 7:
					case 8:
						bAuthors.append(entry.getValue()).append("\n");
						break;
					default:
						break;
					}
				});
			}
		});

		dbDataRecord.put(COMPOSERS, bComposers.toString().trim());
		dbDataRecord.put(WRITERS, bAuthors.toString().trim());
	}

	private void getPerformers(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable) {
		dbDataRecord.put(PERFORMERS, "");
		List<Map<String, Object>> instrumentList = hashTable.getOrDefault(INSTRUMENT, new ArrayList<>());
		if (instrumentList.isEmpty()) {
			return;
		}

		List<Map<String, Object>> musicianTrackList = hashTable.getOrDefault("MusicianTrackLink", new ArrayList<>());
		if (musicianTrackList.isEmpty()) {
			return;
		}

		List<Map<String, Object>> performer = hashTable.getOrDefault(ARTIST_PERFORMER, new ArrayList<>());
		if (performer.isEmpty()) {
			return;
		}

		StringBuilder result = new StringBuilder();

		// Remove duplicate instruments
		Map<Integer, String> mapInstrument = new HashMap<>();
		instrumentList.forEach(map -> {
			Integer instrumentID = (Integer) map.get(INSTRUMENT_ID);
			String instrument = map.get(INSTRUMENT).toString();
			mapInstrument.putIfAbsent(instrumentID, instrument);
		});

		// Remove duplicate persons
		Map<Integer, String> mapPerformer = mapPersons(performer);

		mapPerformer.entrySet().forEach(entry -> {
			Set<Integer> list = musicianTrackList.stream().filter(
					map -> map.get(ARTIST_PERSON_ID).equals(entry.getKey()) && (Integer) map.get(INSTRUMENT_ID) != -1)
					.map(x -> (Integer) x.get(INSTRUMENT_ID)).collect(Collectors.toCollection(LinkedHashSet::new));

			if (!list.isEmpty()) {
				result.append(entry.getValue()).append(" - ");
				list.forEach(instrument -> result.append(mapInstrument.getOrDefault(instrument, "")).append(", "));
				result.delete(result.lastIndexOf(", "), result.length());
				result.append("\n");
			}
		});

		dbDataRecord.put(PERFORMERS, result.toString());
	}

	private void getOrchestras(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable) {
		dbDataRecord.put(ORCHESTRAS, "");
		List<Map<String, Object>> musicianTrackList = hashTable.getOrDefault("MusicianTrackLink", new ArrayList<>());
		if (musicianTrackList.isEmpty()) {
			return;
		}

		// Get the orchestra(s)
		List<Integer> orchestras = musicianTrackList.stream()
				.filter(map -> (Integer) map.getOrDefault(INSTRUMENT_ID, -1) == 64)
				.map(x -> (Integer) x.getOrDefault(ARTIST_PERSON_ID, -1)).collect(Collectors.toList());
		if (orchestras.isEmpty()) {
			return;
		}

		List<Map<String, Object>> performer = hashTable.getOrDefault(ARTIST_PERFORMER, new ArrayList<>());
		if (performer.isEmpty()) {
			return;
		}

		StringBuilder result = new StringBuilder();

		// Remove duplicate persons
		Map<Integer, String> mapPerformer = mapPersons(performer);

		mapPerformer.entrySet().forEach(entry -> {
			if (orchestras.contains(entry.getKey())) {
				result.append(entry.getValue()).append(", ");
				result.delete(result.lastIndexOf(", "), result.length());
				result.append("\n");
			}
		});

		dbDataRecord.put(ORCHESTRAS, result.toString());
	}

	private Map<Integer, String> mapPersons(List<Map<String, Object>> persons) {
		Map<Integer, String> result = new LinkedHashMap<>();
		persons.forEach(map -> {
			Integer personID = (Integer) map.getOrDefault(ARTIST_PERSON_ID, -1);
			if (personID != -1) {
				String person = map.get("Name").toString();
				result.putIfAbsent(personID, person);
			}
		});
		return result;
	}

	// Method that returns the BoxSet Albums as a strings
	private String getBoxSetAlbums(Map<String, List<Map<String, Object>>> hashTable) {
		List<Map<String, Object>> contentsList = hashTable.get(ALBUM);
		StringBuilder result = new StringBuilder();

		if (contentsList.isEmpty()) {
			return "";
		}

		// Get ContentsPerson and Linkage
		List<Map<String, Object>> personList = hashTable.get(CONTENTS_PERSON);
		List<Map<String, Object>> linkList = hashTable.get("ArtistAlbumLink");

		// Split ContentsPerson by AlbumID
		Map<Integer, List<Map<String, Object>>> albumPersons = getContentsPersonIndex(linkList, personList, "AlbumID");

		// Get Format
		List<Map<String, Object>> formatList = hashTable.get(FORMAT);
		for (int i = 0; i < formatList.size(); i++) {
			Map<String, Object> format = formatList.get(i);
			contentsList.get(i).put(FORMAT, format.get(FORMAT));
		}

		Collections.sort(contentsList, compTracks);
		StringBuilder newLine = new StringBuilder();

		for (Map<String, Object> map : contentsList) {
			newLine.append("[").append(map.get(BOX_ITEM)).append("] ");
			newLine.append(getContentsPerson(albumPersons.get(map.get("AlbumID"))));
			newLine.append(" / ").append(map.get(TITLE)).append(", ").append(map.get(FORMAT)).append(", ")
					.append(General.convertFussyDate(map.get(RELEASED).toString()));

			addToList(newLine, result);
		}
		return result.toString();
	}

	// Method that returns the Album Tracks as a list of strings
	private String getAlbumTracks(int itemLength, int discCount, String albumTitle,
			Map<String, List<Map<String, Object>>> hashTable) {
		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get(TRACKS);

		if (CollectionUtils.isEmpty(contentsList)) {
			return "";
		}

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
		int index = 0;

		boolean isDoubleSided = false;

		// Get ContentPersons and Linkage
		List<Map<String, Object>> personList = hashTable.get(CONTENTS_PERSON);
		List<Map<String, Object>> linkList = hashTable.get("ContentsLink");

		// Split ContentsPerson by TrackID
		Map<Integer, List<Map<String, Object>>> trackPersons = getContentsPersonIndex(linkList, personList, "TrackID");

		// Get Media
		List<Map<String, Object>> mediaList = hashTable.get("Media");

		// Sort Media by Tracks.Item
		if (mediaList != null && mediaList.size() > 1) {
			Collections.sort(contentsList, compTracks);
			Collections.sort(mediaList, compMedia);
		}

		for (Map<String, Object> map : contentsList) {
			String title = (String) map.get(TITLE);
			side = (String) map.get("Side");

			if (mediaList != null) {
				item = ((Number) map.get(TRACKS_ITEM)).intValue();
				if (item != oldItem) {
					if (item > 1) {
						addToList(newLine, result);
					}

					Map<String, Object> mapItem = mediaList.get(item - 1);
					sideTitle = (String) mapItem.getOrDefault(TITLE, "");
					sideATitle = (String) mapItem.get("SideATitle");
					sideBTitle = (String) mapItem.get("SideBTitle");
					sideAPlaytime = (Integer) mapItem.get("SideAPlayingTime");
					sideBPlaytime = (Integer) mapItem.get("SideBPlayingTime");

					isDoubleSided = sideBPlaytime.intValue() > 0;
					if (mapItem.containsKey("DoubleSided")) {
						isDoubleSided = (Boolean) mapItem.get("DoubleSided");
					}

					if (useContentsItemTitle && !sideTitle.equalsIgnoreCase(albumTitle)) {
						newLine.append(item + " - ");
						if (useContentsLength) {
							newLine.append("(" + General.convertDuration((Integer) mapItem.get("PlayingTime")) + ") ");
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
						newLine.append(side + " - (" + General.convertDuration(sidePlaytime) + ") " + sideTitle + "\n");
					} else {
						if (!sideTitle.isEmpty()) {
							newLine.append(side + " - " + sideTitle);
						} else {
							newLine.append(side);
						}
						addToList(newLine, result);
					}
				}
			}

			if (useContentsIndex) {
				if (discCount > 1) {
					newLine.append(item).append(".");
				}

				if (isPlaylist) {
					newLine.append(General.convertTrack(index + 1, itemLength) + " ");
				} else {
					newLine.append(General.convertTrack((Number) map.get("Index"), itemLength) + " ");
				}
			}

			if (useContentsLength) {
				newLine.append("(" + General.convertDuration((Integer) map.get("Length")) + ") ");
			}

			newLine.append(title);

			if ((boolean) map.get("Live")) {
				newLine.append(" [Live]");
			}

			String persons = getContentsPerson(trackPersons.get(map.get("TrackID")));

			if (StringUtils.isNotEmpty(persons) && !persons.equals(myPerson)) {
				newLine.append(" [");
				newLine.append(persons);
				newLine.append("]");
			}

			addToList(newLine, result);
			oldItem = item;
			oldSide = side;
			index++;
		}
		return result.toString();
	}
}