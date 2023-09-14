package fnprog2pda.dbengine.export;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.export.CsvFile;

public class BookBuddy extends CsvFile {

	public BookBuddy(Profiles pref) {
		super(pref);
	}

	@Override
	public Object convertDataFields(Object dbValue, FieldDefinition field) {
		if (dbValue == null || dbValue.equals("")) {
			return "";
		}

		switch (field.getFieldAlias()) {
		case "Author":
		case "AuthorSort":
			// Remove roles
			String author = dbValue.toString();
			while (true) {
				int pos = author.indexOf(" [");
				if (pos == -1) {
					return author.replace(",", "").replace(" & ", ",");
				}

				author = author.substring(0, pos) + author.substring(author.indexOf("]") + 1);
			}
		case "DateAcquired":
		case "DateBorrowed":
			return General.convertFussyDate(dbValue.toString(), "yyyy/MM/dd");
		case "ISBN":
			return dbValue.toString().replace("-", "");
		case "Synopsis":
			return dbValue;
		default:
			String text = dbValue.toString();
			return text.contains("; ") ? text.replace("; ", ",") : super.convertDataFields(dbValue, field);
		}
	}
}
