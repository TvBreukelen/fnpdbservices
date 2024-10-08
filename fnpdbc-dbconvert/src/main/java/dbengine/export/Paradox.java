package dbengine.export;

import java.nio.file.FileSystems;
import java.sql.DriverManager;

import application.preferences.Profiles;
import application.utils.General;
import dbengine.SqlDB;

public class Paradox extends SqlDB {

	public Paradox(Profiles pref) {
		super(pref);
		reservedWords.clear();
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Try to obtain the database connection
		if (isConnected) {
			closeFile();
		}

		// Try to obtain the database connection
		StringBuilder url = new StringBuilder();
		url.append("jdbc:paradox:");

		// URL should not point to the database file, but to the database directory
		url.append(myDatabase.substring(0, myDatabase.lastIndexOf(FileSystems.getDefault().getSeparator())));
		connection = DriverManager.getConnection(url.toString());
		isConnected = true;
	}

	@Override
	protected String getSqlFieldName(String value) {
		if (value.matches("^[a-zA-Z0-9_.]*$")) {
			return value;
		}

		return General.TEXT_DELIMITER + value + General.TEXT_DELIMITER;
	}

	@Override
	protected String getPaginationSqlString() {
		StringBuilder b = new StringBuilder(sqlQuery);
		if (myPref.getSqlSelectLimit() > 0) {
			b.append("\nLIMIT ").append(myPref.getSqlSelectLimit()).append("\nOFFSET ").append(offset);
		}
		return b.toString();
	}
}
