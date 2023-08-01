package dbengine.export;

import java.io.FileInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.IConvert;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.transform.recurrence.Frequency;

public class ICalendar extends GeneralDB implements IConvert {
	private int currentRecord;
	private List<CalendarComponent> iCals;
	private Calendar cal;

	private List<Map<String, Object>> dbRecords = new ArrayList<>();

	private static Map<WeekDay, String> weekDays = new HashMap<>();
	private static EnumMap<Frequency, String> frequency = new EnumMap<>(Frequency.class);

	static {
		weekDays.put(WeekDay.MO, "Monday");
		weekDays.put(WeekDay.TU, "Tuesday");
		weekDays.put(WeekDay.WE, "Wednesday");
		weekDays.put(WeekDay.TH, "Thursday");
		weekDays.put(WeekDay.FR, "Friday");
		weekDays.put(WeekDay.SA, "Saturday");
		weekDays.put(WeekDay.SU, "Sunday");

		frequency.put(Frequency.SECONDLY, "second");
		frequency.put(Frequency.MINUTELY, "minute");
		frequency.put(Frequency.HOURLY, "hour");
		frequency.put(Frequency.DAILY, "day");
		frequency.put(Frequency.WEEKLY, "week");
		frequency.put(Frequency.MONTHLY, "month");
		frequency.put(Frequency.YEARLY, "year");
	}

	public ICalendar(Profiles pref) {
		super(pref);
	}

	@Override
	public void closeFile() {
		// Auto close
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		try (FileInputStream fin = new FileInputStream(myDatabase)) {
			CalendarBuilder builder = new CalendarBuilder();
			cal = builder.build(fin);
		}
	}

	@Override
	public void readTableContents() throws Exception {
		dbFieldNames.clear();
		dbFieldTypes.clear();

		String[] fields = new String[] { "Summary", "Description", "Location", "Organizer", "Attendees", "Created",
				"Last Modified", "Start Date", "Start Time", "End Date", "End Time", "Repeat Freq", "Repeat", "End By",
				"End After" };

		FieldTypes[] types = new FieldTypes[] { FieldTypes.TEXT, FieldTypes.MEMO, FieldTypes.TEXT, FieldTypes.TEXT,
				FieldTypes.MEMO, FieldTypes.TIMESTAMP, FieldTypes.TIMESTAMP, FieldTypes.DATE, FieldTypes.TIME,
				FieldTypes.DATE, FieldTypes.TIME, FieldTypes.TEXT, FieldTypes.TEXT, FieldTypes.DATE, FieldTypes.TEXT };

		dbFieldNames.addAll(Arrays.asList(fields));
		dbFieldTypes.addAll(Arrays.asList(types));
	}

	@Override
	public void readInputFile() {
		currentRecord = 0;
		dbRecords.clear();

		iCals = cal.getComponents(Component.VEVENT); // Get all events
		totalRecords = iCals.size();

		// Begin Loop
		for (CalendarComponent comp : iCals) {
			Map<String, Object> map = new HashMap<>();
			PropertyList props = comp.getPropertyList();

			getText(Property.SUMMARY, "Summary", props, map);
			getText(Property.DESCRIPTION, "Description", props, map);
			getText(Property.LOCATION, "Location", props, map);
			getText(Property.ORGANIZER, "Organizer", props, map);
			getAttendees(Property.ATTENDEE, "Attendees", props, map);
			getRules(Property.RRULE, props, map);

			getDateAndTime(Property.CREATED, "Created", null, props, map);
			getDateAndTime(Property.LAST_MODIFIED, "Last Modified", null, props, map);
			getDateAndTime(Property.DTSTART, "Start Date", "Start Time", props, map);
			getDateAndTime(Property.DTEND, "End Date", "End Time", props, map);

			dbRecords.add(map);
		}

	}

	private void getRules(String property, PropertyList props, Map<String, Object> map) {
		Optional<Property> optRule = props.getFirst(property);
		if (optRule.isEmpty()) {
			return;
		}

		RRule<?> rrule = (RRule<?>) optRule.get();
		Recur<?> recur = rrule.getRecur();

		map.put("Repeat Freq", General.capitalizeFirstLetter(recur.getFrequency().toString()));
		getLocalDateOrTime("End By", null, map, recur.getUntil());

		// Get Weekday(s)
		List<WeekDay> days = recur.getDayList();
		StringBuilder buf = new StringBuilder();

		if (CollectionUtils.isNotEmpty(days)) {
			switch (days.size()) {
			case 1:
				WeekDay day = days.get(0);
				buf.append(weekDays.get(day));
				if (day.getOffset() > 0) {
					buf.insert(0, General.ordinal(day.getOffset()) + " ");
				}
				break;
			case 7:
				buf.append("Daily");
				break;
			case 5:
				String weekdays = "MO,TU,WE,TH,FR";
				if (!days.stream().allMatch(wDay -> weekdays.contains(wDay.getDay().toString()))) {
					buf.append("Weekdays");
					break;
				}
			default:
				days.forEach(wDay -> buf.append(weekDays.get(wDay)).append(", "));
				buf.delete(buf.length() - 2, buf.length());
			}
		}

		if (recur.getInterval() > 0) {
			buf.append(buf.length() > 0 ? ", every " : "Every ").append(General.ordinal(recur.getInterval()))
					.append(" ").append(frequency.get(recur.getFrequency()));
		}

		if (recur.getCount() > 0) {
			map.put("End After", recur.getCount() + " occurences");
		}

		map.put("Repeat", buf.toString());

	}

	private void getText(String property, String textField, PropertyList props, Map<String, Object> map) {
		Optional<Property> optText = props.getFirst(property);
		if (optText.isEmpty()) {
			return;
		}

		Property prop = optText.get();
		String value = prop.getValue();
		map.put(textField, value);

		if (prop instanceof Organizer) {
			map.put(textField, getUserAndEmail(prop));
		} else if (value.contains("<table>")) {
			value = value.replace("</p>", "\n"); // Convert HTML to text
			map.put(textField, value.replaceAll("<[^>]*>", "").trim());
		}
	}

	private void getAttendees(String property, String textField, PropertyList props, Map<String, Object> map) {
		StringBuilder result = new StringBuilder();
		List<Property> attendees = props.get(property);
		if (!attendees.isEmpty()) {
			attendees.forEach(attendee -> result.append(getUserAndEmail(attendee)).append("\n"));
			result.delete(result.length() - 1, result.length());
			map.put(textField, result.toString());
		}
	}

	private String getUserAndEmail(Property property) {
		String result = property.getValue();
		if (result.contains(":")) {
			result = result.substring(result.indexOf(":") + 1); // Get E-Mail address
		}

		Optional<Parameter> user = property.getParameter(Parameter.CN);
		if (user.isPresent()) {
			String orgName = user.get().getValue();
			if (!orgName.equals(result)) {
				result = orgName + " [mailto:" + result + "]";
			}
		}

		return result;
	}

	private void getDateAndTime(String property, String dateField, String timeField, PropertyList props,
			Map<String, Object> map) {

		Optional<Property> optDate = props.getFirst(property);
		if (optDate.isEmpty()) {
			return;
		}

		DateProperty<?> dateProp = (DateProperty<?>) optDate.get();
		getLocalDateOrTime(dateField, timeField, map, dateProp.getDate());
	}

	private void getLocalDateOrTime(String dateField, String timeField, Map<String, Object> map, Object obj) {
		if (obj == null) {
			return;
		}

		if (obj instanceof LocalDate) {
			map.put(dateField, obj);
			return;
		}

		LocalDateTime localTime = null;
		if (obj instanceof ZonedDateTime) {
			localTime = ((ZonedDateTime) obj).toLocalDateTime();
		} else if (obj instanceof Instant) {
			localTime = LocalDateTime.parse(obj.toString(),
					DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()));
		}

		if (localTime != null) {
			if (timeField == null && dbFieldTypes.get(dbFieldNames.indexOf(dateField)) == FieldTypes.TIMESTAMP) {
				map.put(dateField, localTime);
			} else {
				map.put(dateField, localTime.toLocalDate());
				if (timeField != null && LocalTime.of(23, 59, 59).isAfter(localTime.toLocalTime())) {
					map.put(timeField, localTime.toLocalTime());
				}
			}
		}
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		return dbRecords.get(currentRecord++);
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		// Not used yet, we are only importing
	}

}
