package fnprog2pda.dbengine.export;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.export.CsvFile;

public class MovieBuddy extends CsvFile {
	private static final String DIRECTOR = "Director";
	private static final String DIRECTOR_SORT = "DirectorSort";
	private static final String EPISODES = "Episodes";
	private static final String LAST_VIEWED = "LastViewed";
	private static final String PERSON = "Person";
	private static final String PERSON_SORT = "Person.PersonSort";
	private static final String SEASON = "Season";
	private static final String SUPPORT_CAST = "SupportCast";
	private static final String SUPPORT_CAST_SORT = "SupportCastSort";
	private static final String TITLE = "Title";
	private static final String VIDEO_TITLE = "Video.TitleSort";

	private Map<String, Object> recordMap = new LinkedHashMap<>();
	private String oldRecord = "";

	private Map<String, LinkedHashMap<String, String>> personMap = new HashMap<>();

	public MovieBuddy(Profiles pref) {
		super(pref);
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		if (dbRecord.isEmpty()) {
			return;
		}

		boolean hasSeason = !dbRecord.get(SEASON).toString().isEmpty();

		if (hasSeason) {
			if (recordMap.isEmpty()) {
				recordMap.putAll(dbRecord);
				oldRecord = getRecordID(dbRecord);
			}

			String iD = getRecordID(dbRecord);
			if (iD.equals(oldRecord)) {
				updateActions(dbRecord, true);
			} else {
				writeOutputFile(recordMap);
				recordMap.putAll(dbRecord);
				mergeEpisodes(dbRecord);
				oldRecord = iD;
			}
		} else {
			updateActions(dbRecord, false);
			writeOutputFile(dbRecord);
		}
	}

	private void updateActions(Map<String, Object> dbRecord, boolean isTV) {
		mergeCast(PERSON, dbRecord);
		mergeCast(PERSON_SORT, dbRecord);
		mergeCast(SUPPORT_CAST, dbRecord);
		mergeCast(SUPPORT_CAST_SORT, dbRecord);
		mergeEpisodes(dbRecord);

		if (isTV) {
			updateTitle(dbRecord);
			mergeDirector(dbRecord);
		}
	}

	@Override
	public void closeData() throws Exception {
		writeOutputFile(recordMap);
	}

	private void mergeDirector(Map<String, Object> dbRecord) {
		String director = dbRecord.getOrDefault(DIRECTOR, "").toString();
		String directorSort = dbRecord.getOrDefault(DIRECTOR_SORT, "").toString();
		String oldDirector = recordMap.getOrDefault(DIRECTOR, "").toString();
		String oldDirectorSort = recordMap.getOrDefault(DIRECTOR_SORT, "").toString();

		if (director.isEmpty()) {
			director = oldDirector;
		}

		if (directorSort.isEmpty()) {
			directorSort = oldDirectorSort;
		}

		if (!director.equals(oldDirector)) {
			mergeDirectors(DIRECTOR, director, oldDirector);
		}

		if (!directorSort.equals(oldDirectorSort)) {
			mergeDirectors(DIRECTOR_SORT, directorSort, oldDirectorSort);
		}
	}

	private void mergeEpisodes(Map<String, Object> dbRecord) {
		StringBuilder episodes = new StringBuilder();
		String newEpisodes = dbRecord.getOrDefault(EPISODES, "").toString();
		String oldEpisodes = recordMap.getOrDefault(EPISODES, "").toString();

		if (!newEpisodes.equals(oldEpisodes)) {
			episodes.append(oldEpisodes);
		}

		Duration length = (Duration) dbRecord.get("Length");
		long runtime = length.toMinutes();

		if (episodes.length() == 0) {
			dbRecord.put("Length", runtime);
			return;
		}

		String airdate = dbRecord.getOrDefault("OrigAirDate", "").toString();
		if (!airdate.isEmpty()) {
			airdate = airdate.substring(0, 4) + "\\/" + airdate.substring(4, 6) + "\\/" + airdate.substring(6);
		}

		episodes.append("{\"title\":\"").append(dbRecord.get("EpisodeNo")).append(". ").append(newEpisodes)
				.append("\",\"airdate\":\"").append(airdate).append("\",\"runtime\":").append(runtime).append("},");
		recordMap.put(EPISODES, episodes.toString());
	}

	private void updateTitle(Map<String, Object> dbRecord) {
		String series = dbRecord.get("Series").toString();
		String season = dbRecord.get(SEASON).toString();
		String title = dbRecord.get(TITLE).toString();

		if (!series.isEmpty()) {
			title = series;
		}

		if (!season.isEmpty()) {
			title = title + ": Season " + season;
		}

		recordMap.put(TITLE, title);
	}

	private void mergeCast(String person, Map<String, Object> dbRecord) {
		String cast = dbRecord.getOrDefault(person, "").toString();
		if (!cast.isEmpty()) {
			Map<String, String> mapNewCast = getCastMembers(cast);
			Map<String, String> mapOldCast = personMap.computeIfAbsent(person, k -> new LinkedHashMap<>());
			mapNewCast.entrySet().forEach(entry -> mapOldCast.putIfAbsent(entry.getKey(), entry.getValue()));
		}
	}

	private Map<String, String> getCastMembers(String cast) {
		Map<String, String> result = new LinkedHashMap<>();
		String[] actors = cast.split("\n");
		for (String actor : actors) {
			int pos = actor.indexOf(" (");
			String character = "";
			if (pos != -1) {
				character = actor.substring(pos + 2, actor.length() - 1);
				actor = actor.substring(0, pos);
			}
			result.put(actor, character);
		}
		return result;
	}

	private void mergeDirectors(String person, String director, String oldDirector) {
		String[] oldDir = oldDirector.split("; ");
		String[] newDir = director.split("; ");

		Set<String> set1 = new LinkedHashSet<>(Arrays.asList(oldDir));
		Set<String> set2 = new LinkedHashSet<>(Arrays.asList(newDir));

		set1.addAll(set2);
		StringBuilder buf = new StringBuilder();
		set1.forEach(entry -> buf.append(entry).append("; "));
		if (buf.length() > 2) {
			buf.delete(buf.length() - 2, buf.length());
		}

		recordMap.put(person, buf.toString());
	}

	private void writeOutputFile(Map<String, Object> dbRecord) throws IOException {
		if (dbRecord.isEmpty()) {
			// Nothing to do
			return;
		}

		// Convert persons
		dbRecord.put(PERSON, convertPersons(PERSON, SUPPORT_CAST));
		dbRecord.put(PERSON_SORT, convertPersons(PERSON_SORT, SUPPORT_CAST_SORT));

		// Convert episodes
		String episodes = dbRecord.getOrDefault(EPISODES, "").toString();
		if (!episodes.isEmpty()) {
			episodes = episodes.substring(0, episodes.length() - 1) + "]";
			dbRecord.put(EPISODES, episodes);
		}

		List<String> list = dbInfo2Write.stream()
				.map(field -> convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString())
				.collect(Collectors.toList());

		super.writeOutputFile(list);

		personMap.clear();
		dbRecord.clear();
	}

	private String getRecordID(Map<String, Object> dbRecord) {
		StringBuilder result = new StringBuilder();
		result.append(dbRecord.get(VIDEO_TITLE));
		result.append(dbRecord.get(SEASON));
		return result.toString();
	}

	@Override
	public Object convertDataFields(Object dbValue, FieldDefinition field) {
		if (dbValue == null || dbValue.equals("")) {
			return "";
		}

		switch (field.getFieldAlias()) {
		case "IMDbID":
			return "tt" + dbValue;
		case "DateAcquired":
		case "DateBorrowed":
		case LAST_VIEWED:
			return General.convertFussyDate(dbValue.toString(), "yyyy/MM/dd");
		case "Length":
			return dbValue;
		default:
			String text = dbValue.toString();
			return text.contains("; ") ? text.replace("; ", ",") : super.convertDataFields(dbValue, field);
		}
	}

	private String convertPersons(String cast, String support) {
		Optional<Map<String, String>> mapOpt = Optional.ofNullable(personMap.get(cast));
		if (mapOpt.isEmpty()) {
			return "";
		}

		Map<String, String> map = mapOpt.get();
		mapOpt = Optional.ofNullable(personMap.get(support));
		if (mapOpt.isPresent()) {
			// Merge leading roles and support cast
			map.putAll(mapOpt.get());
		}

		StringBuilder buf = new StringBuilder();
		buf.append("[");

		map.entrySet().forEach(entry -> {
			buf.append("{\"actor\":\"").append(entry.getKey());
			if (!entry.getValue().isEmpty()) {
				buf.append("\",\"character\":").append("\"").append(entry.getValue().replace("\"", "'")).append("\"},");
			}
		});

		buf.deleteCharAt(buf.length() - 1);
		buf.append("]");
		return buf.toString();
	}

}
