package fnprog2pda.preferences;

import application.interfaces.TvBSoftware;
import application.preferences.PrefUtils;
import application.preferences.Profiles;

public class PrefFNProg extends Profiles {
	private static final PrefFNProg gInstance = new PrefFNProg();

	private boolean useContentsIndex;
	private boolean useContentsItemTitle;
	private boolean useContentsLength;
	private boolean useContentsOrigTitle;
	private boolean useContentsPerson;
	private boolean useContentsSide;
	private boolean useOriginalTitle;
	private boolean useReleaseNo;

	private boolean useRoles;

	private PrefFNProg() {
		super(TvBSoftware.FNPROG2PDA);
	}

	public static PrefFNProg getInstance() {
		return gInstance;
	}

	@Override
	public void setProfile(String profileID) {
		super.setProfile(profileID);
		useContentsIndex = getChild().getBoolean("use.contents.index", false);
		useContentsItemTitle = getChild().getBoolean("use.contents.itemtitle", false);
		useContentsLength = getChild().getBoolean("use.contents.length", false);
		useContentsOrigTitle = getChild().getBoolean("use.contents.origtitle", false);
		useContentsPerson = getChild().getBoolean("use.contents.person", false);
		useContentsSide = getChild().getBoolean("use.contents.side", false);
		useOriginalTitle = getChild().getBoolean("use.original.title", false);
		useReleaseNo = getChild().getBoolean("use.releaseno", false);
		useRoles = getChild().getBoolean("use.roles", false);
	}

	public boolean isUseContentsIndex() {
		return useContentsIndex;
	}

	public void setUseContentsIndex(boolean useContentsIndex) {
		PrefUtils.writePref(getChild(), "use.contents.index", useContentsIndex, this.useContentsIndex, false);
		this.useContentsIndex = useContentsIndex;
	}

	public boolean isUseContentsLength() {
		return useContentsLength;
	}

	public void setUseContentsLength(boolean useContentsLength) {
		PrefUtils.writePref(getChild(), "use.contents.length", useContentsLength, this.useContentsLength, false);
		this.useContentsLength = useContentsLength;
	}

	public boolean isUseContentsSide() {
		return useContentsSide;
	}

	public void setUseContentsSide(boolean useContentsSide) {
		PrefUtils.writePref(getChild(), "use.contents.side", useContentsSide, this.useContentsSide, false);
		this.useContentsSide = useContentsSide;
	}

	public boolean isUseContentsItemTitle() {
		return useContentsItemTitle;
	}

	public void setUseContentsItemTitle(boolean useContentsItemTitle) {
		PrefUtils.writePref(getChild(), "use.contents.itemtitle", useContentsItemTitle, this.useContentsItemTitle,
				false);
		this.useContentsItemTitle = useContentsItemTitle;
	}

	public boolean isUseContentsOrigTitle() {
		return useContentsOrigTitle;
	}

	public void setUseContentsOrigTitle(boolean useContentsOrigTitle) {
		PrefUtils.writePref(getChild(), "use.contents.origtitle", useContentsOrigTitle, this.useContentsOrigTitle,
				false);
		this.useContentsOrigTitle = useContentsOrigTitle;
	}

	public boolean isUseOriginalTitle() {
		return useOriginalTitle;
	}

	public void setUseOriginalTitle(boolean useOriginalTitle) {
		PrefUtils.writePref(getChild(), "use.original.title", useOriginalTitle, this.useOriginalTitle, false);
		this.useOriginalTitle = useOriginalTitle;
	}

	public boolean isUseContentsPerson() {
		return useContentsPerson;
	}

	public void setUseContentsPerson(boolean useContentsPerson) {
		PrefUtils.writePref(getChild(), "use.contents.person", useContentsPerson, this.useContentsPerson, false);
		this.useContentsPerson = useContentsPerson;
	}

	public boolean isUseReleaseNo() {
		return useReleaseNo;
	}

	public void setUseReleaseNo(boolean useReleaseNo) {
		PrefUtils.writePref(getChild(), "use.releaseno", useReleaseNo, this.useReleaseNo, false);
		this.useReleaseNo = useReleaseNo;
	}

	public boolean isUseRoles() {
		return useRoles;
	}

	public void setUseRoles(boolean useRoles) {
		PrefUtils.writePref(getChild(), "use.roles", useRoles, this.useRoles, false);
		this.useRoles = useRoles;
	}
}
