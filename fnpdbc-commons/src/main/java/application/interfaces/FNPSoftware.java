package application.interfaces;

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
	private String[] views;

	private FNPSoftware(String name, String... views) {
		this.name = name;
		this.views = views;
	}

	public static FNPSoftware getSoftware(String id) {
		for (FNPSoftware exp : values()) {
			if (exp.name.equals(id)) {
				return exp;
			}
		}
		return UNDEFINED;
	}

	public String[] getViews() {
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
			enabled = "AlbumBookContentsTrackVideoPlaylist".indexOf(view) != -1;
		}
		return enabled;
	}
}
