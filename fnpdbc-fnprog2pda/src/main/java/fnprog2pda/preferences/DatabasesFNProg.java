package fnprog2pda.preferences;

import application.interfaces.TvBSoftware;
import application.preferences.Databases;

public class DatabasesFNProg extends Databases {
	private static final DatabasesFNProg gInstance = new DatabasesFNProg();

	private DatabasesFNProg() {
		super(TvBSoftware.FNPROG2PDA);
	}

	public static DatabasesFNProg getInstance() {
		return gInstance;
	}
}
