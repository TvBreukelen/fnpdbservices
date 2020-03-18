package application.interfaces;

import java.time.Year;

public enum TvBSoftware {
	/**
	 * Title: TvBSoftware Description: Enums for version and support information
	 * about DBConvert & FNProg2PDA
	 *
	 * @author Tom van Breukelen
	 */

	DBCONVERT("DBConvert", "6.2.2", "https://sourceforge.net/projects/dbconvert",
			"https://sourceforge.net/projects/dbconvert/files"),
	FNPROG2PDA("FNProg2PDA", "8.6.2", "https://sourceforge.net/projects/fnprog2pda",
			"https://sourceforge.net/projects/fnprog2pda/files");

	private String version;
	private String name;
	private String support;
	private String download;

	private TvBSoftware(String name, String version, String support, String download) {
		this.name = name;
		this.version = version;
		this.support = support;
		this.download = download;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getCopyright() {
		return "Copyright © 2003-" + Year.now().getValue() + " Tom van Breukelen.";
	}

	public String getSupport() {
		return support;
	}

	public String getDownload() {
		return download;
	}
}
