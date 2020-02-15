package dbconvert.preferences;

import application.interfaces.TvBSoftware;
import application.preferences.Databases;

public class DatabasesDBConvert extends Databases {
	private static final DatabasesDBConvert gInstance = new DatabasesDBConvert();

	private DatabasesDBConvert() {
		super(TvBSoftware.DBCONVERT);
	}

	public static DatabasesDBConvert getInstance() {
		return gInstance;
	}
}
