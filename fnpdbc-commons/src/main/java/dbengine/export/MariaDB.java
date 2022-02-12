package dbengine.export;

import java.sql.ResultSet;

import application.preferences.Profiles;
import dbengine.SqlDB;

public class MariaDB extends SqlDB {
	public MariaDB(Profiles pref) {
		super(pref);
	}

	@Override
	protected Object getObject(int colType, int colNo, ResultSet rs) throws Exception {
		return rs.getString(colNo);
	}
}