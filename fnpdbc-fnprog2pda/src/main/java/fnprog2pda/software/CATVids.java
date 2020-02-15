package fnprog2pda.software;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private boolean useContentsItemTitle = true;

	private LinkedHashMap<String, FieldTypes> sortList = new LinkedHashMap<>();
	private XComparator comp = new XComparator(sortList);

	private int myVideoID = 0;

	public CATVids(Component myParent) throws Exception {
		super(myParent);
		useContentsLength = pdaSettings.isUseContentsLength();
		useContentsSide = pdaSettings.isUseContentsSide();
		useContentsIndex = pdaSettings.isUseContentsIndex();
		useContentsItemTitle = pdaSettings.isUseContentsItemTitle();

		sortList.put("Contents.Item", FieldTypes.NUMBER);
		sortList.put("Index", FieldTypes.NUMBER);
	}

	@Override
	protected List<String> getContentsFields(List<String> userFields) {
		List<String> result = new ArrayList<>();
		useContents = userFields.contains("Contents");

		if (useContents) {
			result.add("Contents.Item");
			result.add("Media.Item");
			result.add("NumberOfSegments");
		}
		return result;
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		myVideoID = (Integer) dbDataRecord.get(myTableID);
		myLastIndex = Math.max(myLastIndex, myVideoID);

		if (useContents) {
			String s = dbDataRecord.get("NumberOfSegments").toString();
			if (Integer.parseInt(s) > 1) {
				dbDataRecord.put("Contents", getVideoContents(s.length(), hashTable));
			}
		}
	}

	// method that returns the Video Contents as String
	private String getVideoContents(int itemLength, Map<String, List<Map<String, Object>>> hashTable) throws Exception {
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

		boolean isDoubleSided = false;

		// Get Media
		List<Map<String, Object>> mediaList = hashTable.get("Media");

		// Get Contents
		List<Map<String, Object>> contentsList = hashTable.get("Contents");

		if (contentsList == null || contentsList.isEmpty()) {
			return "";
		}

		// Sort Media by Contents.Item
		if (mediaList != null && mediaList.size() > 1) {
			Collections.sort(contentsList, comp);
		}

		for (Map<String, Object> map : contentsList) {
			String title = (String) map.get("Title");
			side = (String) map.get("Side");

			if (mediaList != null) {
				item = ((Number) map.get("Contents.Item")).intValue();
				Map<String, Object> mapItem = mediaList.get(item - 1);

				if (item != oldItem) {
					if (item > 1) {
						result.append("\n");
					}

					sideTitle = (String) mapItem.get("Title");
					sideATitle = (String) mapItem.get("SideATitle");
					sideBTitle = (String) mapItem.get("SideBTitle");

					sidePlaytime = (Number) mapItem.get("Length");
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
						result.append(item + " - ");
						if (useContentsLength) {
							result.append("(" + General.convertDuration((Number) mapItem.get("Length")) + ") ");
						}

						result.append(sideTitle + "\n");
					}
				}

				if (isDoubleSided && useContentsSide) {
					if (!side.equals(oldSide)) {
						sidePlaytime = side.equals("A") ? sideAPlaytime : sideBPlaytime;
						sideTitle = side.equals("A") ? sideATitle : sideBTitle;
						if (sideTitle == null) {
							sideTitle = "";
						}

						if (useContentsLength) {
							result.append(
									side + " - (" + General.convertDuration(sidePlaytime) + ") " + sideTitle + "\n");
						} else {
							if (sideTitle.length() > 0) {
								result.append(side + " - " + sideTitle + "\n");
							} else {
								result.append(side + "\n");
							}
						}
					}
				}
			}

			if (useContentsIndex) {
				result.append(General.convertTrack((Number) map.get("Index"), itemLength) + " ");
			}

			if (useContentsLength) {
				result.append("(" + General.convertDuration((Number) map.get("Length")) + ") ");
			}

			result.append(title);
			result.append("\n");
			oldItem = item;
			oldSide = side;
		}
		return result.toString();
	}
}