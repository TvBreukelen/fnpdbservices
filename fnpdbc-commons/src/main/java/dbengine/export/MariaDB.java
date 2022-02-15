package dbengine.export;

import application.preferences.Profiles;
import dbengine.SqlDB;

public class MariaDB extends SqlDB {
	public MariaDB(Profiles pref) {
		super(pref);
	}
}