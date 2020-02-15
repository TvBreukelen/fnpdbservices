package application.interfaces;

import java.util.Arrays;
import java.util.List;

public enum FNPSoftware {
	/**
	 * Title: FNPSoftware Description: Enums for all FNProgramvare Software related
	 * constants
	 *
	 * @author Tom van Breukelen
	 */

	ASSETCAT("AssetCAT", "Asset"), BOOKCAT("BookCAT", "Book", "Contents", "Person", "Wantlist"),
	CATRAXX("CATraxx", "Album", "Artist", "Boxset", "Playlist", "Track", "Wantlist"),
	CATVIDS("CATVids", "Contents", "Video", "Wantlist"), SOFTCAT("SoftCAT", "App"), STAMPCAT("StampCAT", "Stamp"),
	UNDEFINED("undefined");

	private String name;
	private List<String> views;

	private FNPSoftware(String name, String... views) {
		this.name = name;
		this.views = Arrays.asList(views);
	}

	public static FNPSoftware getSoftware(String ID) {
		for (FNPSoftware exp : values()) {
			if (exp.name.equals(ID)) {
				return exp;
			}
		}
		return UNDEFINED;
	}

	public List<String> getViews() {
		return views;
	}

	public String getName() {
		return name;
	}

	public boolean isUseMisc(String view) {
		boolean enabled = false;
		switch (this) {
		case ASSETCAT:
		case SOFTCAT:
		case STAMPCAT:
			break;
		default:
			enabled = true && "AlbumBookContentsTrackVideoPlaylist".indexOf(view) != -1;
		}
		return enabled;
	}
}
