package fnprog2pda.dbengine.export;

import java.util.List;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.export.CsvFile;

public class MusicBuddy extends CsvFile {

	public MusicBuddy(Profiles pref) {
		super(pref);
	}

	@Override
	public Object convertDataFields(Object dbValue, FieldDefinition field) {
		if (dbValue == null || dbValue.equals("")) {
			return "";
		}

		switch (field.getFieldAlias()) {
		case "Artist":
		case "ArtistSort":
		case "Notes":
		case "PersonalRating":
		case "Synopsis":
			return dbValue;
		case "BarCode":
			return dbValue.toString().replace("-", "");
		case "Composers":
		case "Writers":
			return dbValue.toString().replace("\n", ",");
		case "DateAcquired":
		case "DateBorrowed":
			return General.convertFussyDate(dbValue.toString(), "yyyy/MM/dd");
		case "Performers":
			return convertPerformers(dbValue.toString());
		case "Tracks":
			return convertTracks(dbValue.toString());
		default:
			String text = dbValue.toString();
			return text.contains("; ") ? text.replace("; ", ",") : super.convertDataFields(dbValue, field);
		}
	}

	private String convertPerformers(String performers) {
		List<String> data = General.convertStringToList(performers);
		StringBuilder result = new StringBuilder();
		data.forEach(persInst -> {
			int index = persInst.indexOf(" - ");
			List<String> instruments = General.convertStringToList(persInst.substring(index + 3), ", ");
			String person = persInst.substring(0, index);
			instruments.forEach(instrument -> result.append(instrument).append(" - ").append(person).append(","));
		});

		result.deleteCharAt(result.length() - 1);
		return result.toString();
	}

	private String convertTracks(String tracks) {
		List<String> data = General.convertStringToList(tracks);
		StringBuilder result = new StringBuilder("[");
		for (String entry : data) {
			if (!entry.isBlank()) {
				long seconds = 0;
				String item = "";
				String trackTitle = entry.substring(entry.indexOf(") ") + 2);

				if (!entry.contains("- (")) {
					item = entry.substring(0, entry.indexOf(". (")).replace(".", "-") + ". ";
					String runtime = entry.substring(entry.indexOf("(") + 1, entry.indexOf(") "));
					seconds = General.convertDuration2DB(runtime).toSeconds();
				}

				result.append("{\"title\":\"").append(item).append(General.convertDoubleQuotes(trackTitle))
						.append("\",\"duration\":").append(seconds).append("},");
			}
		}
		result.deleteCharAt(result.length() - 1);
		result.append("]");
		return result.toString();
	}
}
