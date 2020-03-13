package application.interfaces;

import java.time.Year;

public enum TvBSoftware {
	/**
	 * Title: TvBSoftware Description: Enums for version and support information
	 * about DBConvert & FNProg2PDA
	 *
	 * @author Tom van Breukelen
	 */

	DBCONVERT("DBConvert", "6.2.2"), FNPROG2PDA("FNProg2PDA", "8.6.2");

	private String version;
	private String name;
	private String support;
	private String download;

	private TvBSoftware(String name, String version) {
		this.name = name;
		this.version = version;
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

	public void setSupport(String support) {
		this.support = support;
	}

	public String getDownload() {
		return download;
	}

	public void setDownload(String download) {
		this.download = download;
	}
}
