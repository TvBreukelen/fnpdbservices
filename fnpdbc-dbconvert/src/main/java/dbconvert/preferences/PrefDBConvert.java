package dbconvert.preferences;

import application.interfaces.TvBSoftware;
import application.preferences.Profiles;

public class PrefDBConvert extends Profiles {
	private static final PrefDBConvert gInstance = new PrefDBConvert();

	private PrefDBConvert() {
		super(TvBSoftware.DBCONVERT);
	}

	public static PrefDBConvert getInstance() {
		return gInstance;
	}
}
