package fnprog2pda.preferences;

import application.interfaces.TvBSoftware;
import application.preferences.Profiles;

public class PrefFNProg extends Profiles {
	private static final PrefFNProg gInstance = new PrefFNProg();

	private PrefFNProg() {
		super(TvBSoftware.FNPROG2PDA);
	}

	public static PrefFNProg getInstance() {
		return gInstance;
	}
}
