package application.interfaces;

public enum TvBSoftware {
	/**
	 * Title: TvBSoftware Description: Enums for version and support information
	 * about DBConvert & FNProg2PDA
	 *
	 * @author Tom van Breukelen
	 */

	DBCONVERT("DBConvert", "8.5.2", "dbconvert", "dbconvert/files", "dbconvert/best_release.json"),
	FNPROG2PDA("FNProg2PDA", "10.8", "fnprog2pda", "fnprog2pda/files", "fnprog2pda/best_release.json");

	private static final String SOURCEFORGE = "https://sourceforge.net/projects/";
	private static final String COPYRIGHT = "Copyright 2003-2024 Tom van Breukelen.";

	private String version;
	private String name;
	private String support;
	private String download;
	private String releaseInfo;

	TvBSoftware(String name, String version, String support, String download, String releaseInfo) {
		this.name = name;
		this.version = version;
		this.support = SOURCEFORGE + support;
		this.download = SOURCEFORGE + download;
		this.releaseInfo = SOURCEFORGE + releaseInfo;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getCopyright() {
		return COPYRIGHT;
	}

	public String getSupport() {
		return support;
	}

	public String getDownload() {
		return download;
	}

	public String getReleaseInfo() {
		return releaseInfo;
	}
}
