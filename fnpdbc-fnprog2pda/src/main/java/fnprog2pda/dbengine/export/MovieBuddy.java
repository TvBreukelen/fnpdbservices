package fnprog2pda.dbengine.export;

import java.time.Duration;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.export.CsvFile;

public class MovieBuddy extends CsvFile {

	public MovieBuddy(Profiles pref) {
		super(pref);
	}

	@Override
	public Object convertDataFields(Object dbValue, FieldDefinition field) {
		if (dbValue == null || dbValue.equals("")) {
			return "";
		}

		switch (field.getFieldAlias()) {
		case "Director":
		case "DirectorSort":
		case "Genre":
		case "GenreSort":
			String genres = dbValue.toString();
			return genres.replace("; ", ",");
		case "IMDbID":
			return "tt" + dbValue;
		case "DateAcquired":
		case "DateBorrowed":
		case "LastViewed":
			return General.convertFussyDate(dbValue.toString(), "yyyy/MM/dd");
		case "Length":
			Duration length = (Duration) dbValue;
			return length.toMinutes();
		case "Person":
		case "PersonSort":
			StringBuilder buf = new StringBuilder();
			String[] actors = dbValue.toString().split("\n");
			buf.append("[");
			for (String actor : actors) {
				buf.append("{\"actor\":\"");
				String character = "";

				int pos = actor.indexOf(" (");
				if (pos != -1) {
					character = actor.substring(pos + 2, actor.length() - 1);
					actor = actor.substring(0, pos);
				}

				buf.append(actor);
				if (!character.isEmpty()) {
					buf.append("\",\"character\":").append("\"").append(character);
				}
				buf.append("\"},");
			}

			buf.deleteCharAt(buf.length() - 1);
			buf.append("]");
			return buf.toString();
		}

		return super.convertDataFields(dbValue, field);
	}

}
