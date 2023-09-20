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
			return formatAuthor(dbValue.toString(), false);
		case "AuthorSort":
			return formatAuthor(dbValue.toString(), true);
		case "DateAcquired":
		case "DateBorrowed":
			return General.convertFussyDate(dbValue.toString(), "yyyy/MM/dd");
		case "ISBN":
			return dbValue.toString().replace("-", "");
		case "PersonalRating":
		case "Synopsis":
			return dbValue;
		default:
			String text = dbValue.toString();
			return text.contains("; ") ? text.replace("; ", ",") : super.convertDataFields(dbValue, field);
		}
	}

	private String formatAuthor(String author, boolean isAuthorSort) {
		// Remove roles because BookBuddy doesn't "like" then
		while (true) {
			int pos = author.indexOf(" [");
			if (pos == -1) {
				break;
			}

			author = author.substring(0, pos) + author.substring(author.indexOf("]") + 1);
		}

		author = author.replace(", Jr", " Jr");

		// BookBuddy only allows one author in the sorted field
		if (isAuthorSort && author.contains(" & ")) {
			author = author.substring(0, author.indexOf(" & "));
		} else {
			author = author.replace(" & ", ",");
		}

		return author;
	}
}
