package fnprog2pda.software;

import java.awt.Component;
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
	private boolean useArtist;
	private boolean useArtistSort;
	private boolean useTrackArtist = true;
	private boolean useTrackLength = true;
	private boolean useTrackSide = true;
	private boolean useTrackIndex = true;
	private boolean useTrackItemTitle = true;
	private boolean isPlaylist = false;

	private int myAlbumID = 0;
	private Map<String, FieldTypes> sortList = new LinkedHashMap<>();
	private XComparator comp = new XComparator(sortList);

	public CATraxx(Component myParent) throws Exception {
		super(myParent);
		useTrackArtist = pdaSettings.isUseContentsPerson();
		useTrackLength = pdaSettings.isUseContentsLength();
		useTrackSide = pdaSettings.isUseContentsSide();
		useTrackIndex = pdaSettings.isUseContentsIndex();
		useTrackItemTitle = pdaSettings.isUseContentsItemTitle();

		personField = new String[] { "Artist", "ArtistSort" };
		sortList.put("Tracks.Item", FieldTypes.NUMBER);
		sortList.put("Index", FieldTypes.NUMBER);
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		useArtist = userFields.contains(personField[0]);
		useArtistSort = userFields.contains(personField[1]);
		useContents = userFields.contains("Tracks");
		isPlaylist = myTable.equals("Playlist");

		List<String> result = new ArrayList<>(15);
		if (useContents) {
			result.add("NumberOfTracks");
			result.add("ContentsLink.TrackID");
			result.add("Tracks.Item");

			if (!isPlaylist) {
				result.add("Media.Item");
			}

			if (useTrackArtist) {
				if (useArtist || useArtistSort) {
					result.add(useArtist ? "ContentsPerson" : "ContentsPersonSort");
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
		if (useRoles && (myTable.equals("Album") || myTable.equals("Track"))) {
			switch ((int) Math.floor(Double.parseDouble(mySoftwareVersion))) {
			case 5:
				HashMap<Integer, String> personMap = new HashMap<>();
				myRoles.put("Artist", personMap);
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
		getRoles("Artist", "ArtistRole", "ArtistRoleID");
		getRoles("ProductionPerson", "ProductionRole", "ProductionRoleID");
		getRoles("Studio", "StudioRole", "StudioRoleID");
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		myAlbumID = (Integer) dbDataRecord.get(myTableID);
		myLastIndex = Math.max(myLastIndex, myAlbumID);

		if (useContents) {
			String s = dbDataRecord.get("NumberOfTracks").toString();
			if (Integer.parseInt(s) > 0) {
				try {
					myPerson = dbDataRecord.get(personField[useArtistSort ? 1 : 0]).toString();
				} catch (Exception e) {
					myPerson = "[None]";
				}
				dbDataRecord.put("Tracks", getAlbumTracks(s.length(), hashTable));
			}
		}
	}

	// method that returns the Album Tracks as String
	private String getAlbumTracks(int itemLength, Map<String, List<Map<String, Object>>> hashTable) throws Exception {
		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get("Tracks");

		if (contentsList.isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();

		String side = null;
		String sideTitle = null;
		Number sidePlaytime = null;
		Number sideAPlaytime = null;
		Number sideBPlaytime = null;
		String sideATitle = null;
		String sideBTitle = null;
		String oldSide = "";

		int item = 0;
		int oldItem = 0;
		int index = 0;
		int personIndex = 0;

		boolean isDoubleSided = false;

		// Get Media
		List<Map<String, Object>> mediaList = hashTable.get("Media");

		// Get Persons
		List<Map<String, Object>> personList = hashTable.get("ContentsPerson");
		int maxPersons = personList == null ? 0 : personList.size();

		// Sort Media by Tracks.Item
		if (mediaList != null && mediaList.size() > 1) {
			Collections.sort(contentsList, comp);
		}

		for (Map<String, Object> map : contentsList) {
			String title = (String) map.get("Title");
			side = (String) map.get("Side");

			if (mediaList != null) {
				item = ((Number) map.get("Tracks.Item")).intValue();
				if (item != oldItem) {
					if (item > 1) {
						result.append("\n");
					}

					Map<String, Object> mapItem = mediaList.get(item - 1);
					sideTitle = (String) mapItem.get("Title");
					if (sideTitle == null) {
						sideTitle = "";
					}

					sideATitle = (String) mapItem.get("SideATitle");
					sideBTitle = (String) mapItem.get("SideBTitle");

					sidePlaytime = (Number) mapItem.get("PlayingTime");
					sideAPlaytime = (Number) mapItem.get("SideAPlayingTime");
					sideBPlaytime = (Number) mapItem.get("SideBPlayingTime");

					isDoubleSided = sideBPlaytime.intValue() > 0;
					if (mapItem.containsKey("DoubleSided")) {
						isDoubleSided = (Boolean) mapItem.get("DoubleSided");
					}

					if (useTrackItemTitle) {
						result.append(item + " - ");
						if (useTrackLength) {
							result.append("(" + General.convertDuration((Number) mapItem.get("PlayingTime")) + ") ");
						}

						result.append(sideTitle + "\n");
					}
				}

				if (isDoubleSided && useTrackSide) {
					if (!side.equals(oldSide)) {
						sidePlaytime = side.equals("A") ? sideAPlaytime : sideBPlaytime;
						sideTitle = side.equals("A") ? sideATitle : sideBTitle;
						if (sideTitle == null) {
							sideTitle = "";
						}

						if (useTrackLength) {
							result.append(
									side + " - (" + General.convertDuration(sidePlaytime) + ") " + sideTitle + "\n");
						} else {
							if (!sideTitle.isEmpty()) {
								result.append(side + " - " + sideTitle + "\n");
							} else {
								result.append(side + "\n");
							}
						}
					}
				}
			}

			if (useTrackIndex) {
				if (isPlaylist) {
					result.append(General.convertTrack(index + 1, itemLength) + " ");
				} else {
					result.append(General.convertTrack((Number) map.get("Index"), itemLength) + " ");
				}
			}

			if (useTrackLength) {
				result.append("(" + General.convertDuration((Number) map.get("Length")) + ") ");
			}

			result.append(title);

			if ((Boolean) map.get("Live")) {
				result.append(" [Live]");
			}

			if (useTrackArtist && !personList.isEmpty() && personIndex < maxPersons) {
				Map<String, Object> mapPerson = personList.get(personIndex);
				boolean isPersonSet = false;
				StringBuilder buf = new StringBuilder();

				while (true) {
					String persons = (String) mapPerson.get(useArtistSort ? "SortBy" : "Name");
					if (persons != null && !persons.isEmpty()) {
						int roleID = useRoles ? ((Number) mapPerson.get("RoleID")).intValue() : 0;

						if (isPersonSet) {
							if (roleID < 1) {
								buf.append(" & ");
							}
						}

						buf.append(roleID > 0 ? getPersonRole("Artist", persons, roleID) : persons);
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
			oldSide = side;
			index++;
			personIndex++;
		}
		return result.toString();
	}
}