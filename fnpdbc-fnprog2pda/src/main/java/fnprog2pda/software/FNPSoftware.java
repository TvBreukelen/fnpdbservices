package fnprog2pda.software;

public enum FNPSoftware {
	/**
	 * Title: FNPSoftware Description: Enums for all FNProgramvare Software related
	 * constants
	 *
	 * @author Tom van Breukelen
	 */

	ASSETCAT("AssetCAT", "Asset"), //
	BOOKCAT("BookCAT", "Book", "Contents", "Person", "WantList"), //
	CATRAXX("CATraxx", "Album", "ArtistPerson", "BoxSet", "Playlist", "Track", "WantList"), //
	CATVIDS("CATVids", "Contents", "Video", "WantList"), //
	SOFTCAT("SoftCAT", "App"), //
	STAMPCAT("StampCAT", "Stamp"), //
	UNDEFINED("undefined");

	private String name;
	private String[] views;

	FNPSoftware(String name, String... views) {
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
		case ASSETCAT, SOFTCAT, STAMPCAT:
			break;
		default:
			enabled = "AlbumBookContentsTrackVideoPlaylist".indexOf(view) != -1;
		}
		return enabled;
	}
}
