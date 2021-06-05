package fnprog2pda.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
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
	private boolean isPlaylist = false;
	private boolean isBoxSet = false;

	private static final String ARTIST = "Artist";
	private static final String CONTENTS_PERSON = "ContentsPerson";
	private static final String TRACKS = "Tracks";
	private static final String TRACKS_ITEM = "Tracks.Item";
	private static final String BOX_ITEM = "BoxSetIndex";
	private static final String ALBUM = "Album";
	private static final String TITLE = "Title";

	private Map<String, FieldTypes> sortList = new LinkedHashMap<>();
	private XComparator comp = new XComparator(sortList);

	public CATraxx() {
		super();
		useContentsLength = pdaSettings.isUseContentsLength();
		useContentsSide = pdaSettings.isUseContentsSide();
		useContentsIndex = pdaSettings.isUseContentsIndex();

		personField = new String[] { ARTIST, "ArtistSort" };
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		List<String> result = new ArrayList<>(15);

		boolean useArtist = userFields.contains(personField[0]);
		isBoxSet = myTable.equals("BoxSet");
		useContentsPerson = useContentsPerson || isBoxSet;
		usePersonSort = userFields.contains(personField[1]);
		useContents = userFields.contains(TRACKS) || isBoxSet;
		isPlaylist = myTable.equals("Playlist");

		if (useContents) {
			if (useContentsPerson) {
				if (useArtist || usePersonSort) {
					result.add(useArtist ? CONTENTS_PERSON : "ContentsPersonSort");
				} else {
					result.add(CONTENTS_PERSON);
				}
			}

			if (isBoxSet) {
				result.add("Album.BoxSetID");
				result.add("Format.FormatID");
				result.add("ContentsLink.AlbumID");
				sortList.put(BOX_ITEM, FieldTypes.NUMBER);
				useRoles = true;
				return result;
			}

			result.add("NumberOfTracks");
			result.add("ContentsLink.TrackID");
			result.add(TRACKS_ITEM);

			if (isPlaylist) {
				result.add("PlaylistType");
			} else {
				result.add("Media.Item");
			}

			sortList.put(TRACKS_ITEM, FieldTypes.NUMBER);
			sortList.put("Index", FieldTypes.NUMBER);

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
					myPerson = dbDataRecord.get(personField[usePersonSort ? 1 : 0]).toString();
				} catch (Exception e) {
					myPerson = "[None]";
				}
				dbDataRecord.put(TRACKS, getAlbumTracks(s.length(), hashTable));
			}
		}
	}

	// Method that returns the BoxSet Albums as a strings
	private String getBoxSetAlbums(Map<String, List<Map<String, Object>>> hashTable) {
		List<Map<String, Object>> contentsList = hashTable.get(ALBUM);
		StringBuilder result = new StringBuilder();
		final String FORMAT = "Format";

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

		Collections.sort(contentsList, comp);
		StringBuilder newLine = new StringBuilder();

		for (Map<String, Object> map : contentsList) {
			newLine.append("[").append(map.get(BOX_ITEM)).append("] ");
			newLine.append(getContentsPerson(albumPersons.get(map.get("AlbumID"))));
			newLine.append(" / ").append(map.get(TITLE)).append(", ").append(map.get(FORMAT)).append(", ")
					.append(General.convertFussyDate(map.get("Released").toString()));

			addToList(newLine, result);
		}
		return result.toString();
	}

	// Method that returns the Album Tracks as a list of strings
	private String getAlbumTracks(int itemLength, Map<String, List<Map<String, Object>>> hashTable) {
		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get(TRACKS);
		StringBuilder result = new StringBuilder();

		if (contentsList.isEmpty()) {
			return "";
		}

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
			Collections.sort(contentsList, comp);
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
					sideTitle = (String) mapItem.get(TITLE);
					if (sideTitle == null) {
						sideTitle = "";
					}

					sideATitle = (String) mapItem.get("SideATitle");
					sideBTitle = (String) mapItem.get("SideBTitle");
					sideAPlaytime = (Integer) mapItem.get("SideAPlayingTime");
					sideBPlaytime = (Integer) mapItem.get("SideBPlayingTime");

					isDoubleSided = sideBPlaytime.intValue() > 0;
					if (mapItem.containsKey("DoubleSided")) {
						isDoubleSided = (Boolean) mapItem.get("DoubleSided");
					}

					if (useContentsItemTitle) {
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

			if (!persons.isEmpty() && !persons.equals(myPerson)) {
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