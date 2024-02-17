package dbengine.export;

import java.io.FileInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.microsoft.sqlserver.jdbc.StringUtils;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
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
import net.fortuna.ical4j.model.component.VAvailability;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VJournal;
import net.fortuna.ical4j.model.component.VResource;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.FreeBusy;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RequestStatus;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.transform.recurrence.Frequency;

public class ICalendar extends GeneralDB implements IConvert {
	private int currentRecord;
	private List<CalendarComponent> iCals;
	private Map<String, FieldTypes> fields;
	private Map<String, Object> map;
	private List<Map<String, Object>> dbRecords = new ArrayList<>();

	private static final String ALARM = "Alarm";
	private static final String COMPLETED = "Completed";
	private static final String DATE_END = "DateEnd";
	private static final String DATE_START = "DateStart";
	private static final String EVENT = "Event";
	private static final String ORGANIZER = "Organizer";
	private static final String PARTICIPANT = "Participant";
	private static final String REPEAT = "Repeat";
	private static final String REQUEST_STATUS = "RequestStatus";
	private static final String STATUS = "Status";
	private static final String TIME_END = "TimeEnd";
	private static final String TIME_START = "TimeStart";

	private static Map<WeekDay, String> weekDays = new HashMap<>();
	private static EnumMap<Frequency, String> frequency = new EnumMap<>(Frequency.class);
	private static Map<String, String> eventStatus = new HashMap<>();
	private static Map<String, String> reqStatus = new HashMap<>();

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

		eventStatus.put(Status.VALUE_CANCELLED, "Cancelled");
		eventStatus.put(Status.VALUE_COMPLETED, COMPLETED);
		eventStatus.put(Status.VALUE_CONFIRMED, "Confirmed");
		eventStatus.put(Status.VALUE_DRAFT, "Draft");
		eventStatus.put(Status.VALUE_FINAL, "Final");
		eventStatus.put(Status.VALUE_IN_PROCESS, "In Process");
		eventStatus.put(Status.VALUE_NEEDS_ACTION, "Needs Action");
		eventStatus.put(Status.VALUE_TENTATIVE, "Tentative");

		reqStatus.put(RequestStatus.CLIENT_ERROR, "Client Error");
		reqStatus.put(RequestStatus.PRELIM_SUCCESS, "Prelimary Success");
		reqStatus.put(RequestStatus.SCHEDULING_ERROR, "Scheduling Error");
		reqStatus.put(RequestStatus.SUCCESS, "Successful");
	}

	public ICalendar(Profiles pref) {
		super(pref);

		System.setProperty("ical4j.compatibility.notes", Boolean.toString(myPref.isNotesCompatible()));
		System.setProperty("ical4j.compatibility.outlook", Boolean.toString(myPref.isOutlookCompatible()));
		System.setProperty("ical4j.parsing.relaxed", Boolean.toString(myPref.isRelaxedParsing()));
		System.setProperty("ical4j.unfolding.relaxed", Boolean.toString(myPref.isRelaxedUnfolding()));
		System.setProperty("ical4j.validation.relaxed", Boolean.toString(myPref.isRelaxedValidation()));
	}

	@Override
	public void closeFile() {
		// Auto close
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		currentRecord = 0;
		dbRecords.clear();

		try (FileInputStream fin = new FileInputStream(getDbFile())) {
			CalendarBuilder builder = new CalendarBuilder();
			Calendar cal = builder.build(fin);
			iCals = cal.getComponents();
		}

		totalRecords = iCals.size();
		if (totalRecords == 0) {
			throw FNProgException.getException("noFields", getDbFile());
		}
	}

	@Override
	public void readTableContents() throws Exception {
		dbFieldNames.clear();
		dbFieldTypes.clear();
		fields = new HashMap<>();

		// Begin Loop
		for (CalendarComponent comp : iCals) {
			map = new HashMap<>();
			String eventType = EVENT;

			switch (comp.getName()) {
			case Component.VALARM:
				eventType = ALARM;
				processProperties(comp.getPropertyList(), General.EMPTY_STRING);
				break;
			case Component.VAVAILABILITY:
				eventType = "Availability";
				processAvailability((VAvailability) comp);
				break;
			case Component.VEVENT:
				processEvent((VEvent) comp);
				break;
			case Component.VFREEBUSY:
				eventType = "FreeBusy";
				processProperties(comp.getPropertyList(), General.EMPTY_STRING);
				break;
			case Component.VJOURNAL:
				eventType = "Journal";
				processJournal((VJournal) comp);
				break;
			case Component.VTIMEZONE:
				continue;
			case Component.VTODO:
				eventType = "Task";
				processToDo((VToDo) comp);
				break;
			default:
				break;
			}

			if (!map.isEmpty()) {
				dbRecords.add(map);
				map.put(EVENT, eventType);
			}
			fields.putIfAbsent(EVENT, FieldTypes.TEXT);
		}

		totalRecords = dbRecords.size();
		fields.keySet().forEach(field -> dbFieldNames.add(field));
		Collections.sort(dbFieldNames);
		dbFieldNames.forEach(field -> dbFieldTypes.add(fields.get(field)));
	}

	private void processEvent(VEvent event) {
		processProperties(event.getPropertyList(), General.EMPTY_STRING);
		event.getAlarms().forEach(alarm -> processProperties(alarm.getPropertyList(), ALARM));
		event.getParticipants().forEach(participant -> processProperties(participant.getPropertyList(), PARTICIPANT));
		processResources(event.getResources());
	}

	private void processAvailability(VAvailability availability) {
		availability.getAvailable().forEach(avail -> {
			map.put(EVENT, "Availability");
			processProperties(availability.getPropertyList(), General.EMPTY_STRING);
			processProperties(avail.getPropertyList(), "Avail");

			dbRecords.add(map);
			map = new HashMap<>();
		});
	}

	private void processJournal(VJournal journal) {
		processProperties(journal.getPropertyList(), General.EMPTY_STRING);
		journal.getParticipants().forEach(participant -> processProperties(participant.getPropertyList(), PARTICIPANT));
		processResources(journal.getResources());
	}

	private void processToDo(VToDo todo) {
		processProperties(todo.getPropertyList(), General.EMPTY_STRING);
		todo.getAlarms().forEach(alarm -> processProperties(alarm.getPropertyList(), ALARM));
		todo.getParticipants().forEach(participant -> processProperties(participant.getPropertyList(), PARTICIPANT));
		processResources(todo.getResources());
	}

	@SuppressWarnings("java:S1479")
	private void processProperties(PropertyList props, String prefix) {
		final Set<String> propMap = new HashSet<>();
		props.getAll().forEach(prop -> {
			switch (prop.getName()) {
			case Property.ACTION:
				getText(prop, prefix + "Action", true);
				break;
			case Property.BUSYTYPE:
				getText(prop, prefix + "BusyType", false);
				break;
			case Property.CATEGORIES:
				getCategories((Categories) prop, prefix + "Categories");
				break;
			case Property.CLASS:
				getText(prop, prefix + "Classification", true);
				break;
			case Property.CONTACT:
				getText(prop, prefix + "Contact", false);
				break;
			case Property.CREATED:
				getDateAndTime(prop, prefix + "Created", null, true);
				break;
			case Property.COMPLETED:
				getDateAndTime(prop, prefix + COMPLETED, null, false);
				break;
			case Property.DESCRIPTION:
				getText(prop, prefix + "Description", false);
				break;
			case Property.DUE:
				getDateAndTime(prop, prefix + "DueDate", null, false);
				break;
			case Property.DURATION:
				getDuration(prop, prefix + "Duration");
				break;
			case Property.DTEND:
				getDateAndTime(prop, DATE_END, TIME_END, false);
				break;
			case Property.DTSTAMP:
				getDateAndTime(prop, prefix + "Timestamp", null, true);
				break;
			case Property.DTSTART:
				getDateAndTime(prop, DATE_START, TIME_START, false);
				break;
			case Property.FREEBUSY:
				getFreeBusy(prop, prefix + "FreeBusyStart", prefix + "FreeBusyEnd", prefix + "FreeBusyType");
				break;
			case Property.GEO:
				getText(prop, prefix + "Geo", false);
				break;
			case Property.LAST_MODIFIED:
				getDateAndTime(prop, prefix + "LastModified", null, true);
				break;
			case Property.LOCATION:
				getText(prop, prefix + "Location", false);
				break;
			case Property.ORGANIZER:
				getText(prop, prefix + ORGANIZER, false);
				break;
			case Property.PARTICIPANT_TYPE:
				getText(prop, prefix + "Type", false);
				break;
			case Property.PERCENT_COMPLETE:
				getText(prop, prefix + "PercentComplete", false);
				break;
			case Property.PRIORITY:
				getText(prop, prefix + "Priority", false);
				break;
			case Property.RECURRENCE_ID:
				getText(prop, prefix + "RecurrenceId", false);
				break;
			case Property.RDATE:
				getRDates(prop, prefix + "RDateStart", "RDateEnd");
				break;
			case Property.RELATED_TO:
				getText(prop, prefix + "RelatedTo", false);
				break;
			case Property.REPEAT:
				getText(prop, prefix + REPEAT, true);
				break;
			case Property.REQUEST_STATUS:
				getText(prop, prefix + REQUEST_STATUS, false);
				break;
			case Property.RRULE:
				getRules(prop, General.EMPTY_STRING);
				break;
			case Property.STATUS:
				getText(prop, prefix + STATUS, false);
				break;
			case Property.SUMMARY:
				getText(prop, prefix + "Summary", false);
				break;
			case Property.TRANSP:
				getText(prop, prefix + "Transp", true);
				break;
			case Property.TRIGGER:
				getDateAndTime(prop, prefix + "Trigger", null, true);
				calculateDuration(prefix + "TriggerStart", ((Trigger) prop).getDuration());
				break;
			case Property.UID:
				getText(prop, prefix + "Uid", false);
				break;
			case Property.URL:
				getText(prop, prefix + "Url", false);
				break;
			default:
				propMap.add(prop.getName());
				break;
			}
		});

		if (propMap.contains(Property.ATTACH)) {
			getList(props.get(Property.ATTACH), prefix + "Attachments");
		}

		if (propMap.contains(Property.ATTENDEE)) {
			getAttendees(props.get(Property.ATTENDEE), prefix + "Attendees");
		}

		if (propMap.contains(Property.COMMENT)) {
			getList(props.get(Property.COMMENT), prefix + "Comments");
		}

		if (propMap.contains(Property.EXDATE)) {
			getExDates(props.get(Property.EXDATE), prefix + "ExDate");
		}
	}

	private void processResources(List<VResource> resources) {
		if (resources.isEmpty()) {
			return;
		}

		StringBuilder result = new StringBuilder();
		resources.forEach(resource -> result.append(resource.getValue()).append("\n"));
		map.put("Resources", result.toString().trim());
	}

	private void getDuration(Property duration, String durationField) {
		calculateDuration(durationField, ((Duration) duration).getDuration());
	}

	private void calculateDuration(String durationField, TemporalAmount duration) {
		if (duration == null) {
			return;
		}

		boolean add = duration.get(duration instanceof Period ? ChronoUnit.DAYS : ChronoUnit.SECONDS) >= 0;

		LocalDate date = (LocalDate) map.get(add ? DATE_START : DATE_END);
		LocalTime time = (LocalTime) map.get(add ? TIME_START : TIME_END);
		if (date != null && time != null) {
			LocalDateTime ldt = time.atDate(date).plus(duration);
			map.put(durationField, ldt);
			fields.putIfAbsent(durationField, FieldTypes.TIMESTAMP);
		}
	}

	private void getFreeBusy(Property freeBusyProp, String fbStartField, String fbEndField, String fbTypeField) {

		StringBuilder resultStart = new StringBuilder();
		StringBuilder resultEnd = new StringBuilder();
		FreeBusy freeBusy = (FreeBusy) freeBusyProp;
		freeBusy.getIntervals().forEach(interval -> {
			LocalDateTime startDt = LocalDateTime.parse(interval.getStart().toString(),
					DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()));
			LocalDateTime endDt = LocalDateTime.parse(interval.getEnd().toString(),
					DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()));

			resultStart.append(General.convertTimestamp(startDt)).append("\n");
			resultEnd.append(General.convertTimestamp(endDt)).append("\n");
		});

		Optional<Parameter> typeOpt = freeBusy.getParameter(Parameter.FBTYPE);
		if (typeOpt.isPresent()) {
			map.put(fbTypeField, General.capitalizeFirstLetter(typeOpt.get().getValue()));
			fields.putIfAbsent(fbTypeField, FieldTypes.TEXT);
		}

		map.put(fbStartField, resultStart.toString().trim());
		map.put(fbEndField, resultEnd.toString().trim());
		fields.putIfAbsent(fbStartField, FieldTypes.MEMO);
		fields.putIfAbsent(fbEndField, FieldTypes.MEMO);
	}

	private void getAttendees(List<Attendee> attendees, String textField) {
		if (attendees.isEmpty()) {
			return;
		}

		StringBuilder result = new StringBuilder();
		attendees.forEach(attendee -> result.append(getUserAndEmail(attendee)).append("\n"));
		result.delete(result.length() - 1, result.length());
		map.put(textField, result.toString());
		fields.putIfAbsent(textField, FieldTypes.MEMO);
	}

	private void getList(List<? extends Property> list, String textField) {
		if (list.isEmpty()) {
			return;
		}

		StringBuilder result = new StringBuilder();
		list.forEach(prop -> result.append(prop.getValue()).append("\n"));
		result.delete(result.length() - 1, result.length());
		map.put(textField, result.toString());
		fields.putIfAbsent(textField, FieldTypes.MEMO);
	}

	private void getCategories(Categories list, String textField) {
		String[] elements = list.getValue().split(",");
		for (int i = 0; i < elements.length; i++) {
			elements[i] = General.capitalizeFirstLetter(elements[i]);
		}

		map.put(textField, General.convertListToString(Arrays.asList(elements)));
		fields.putIfAbsent(textField, FieldTypes.MEMO);
	}

	@SuppressWarnings("unchecked")
	private void getExDates(List<Property> exProps, String dateField) {
		if (exProps.isEmpty()) {
			return;
		}

		List<String> result = new ArrayList<>();
		fields.putIfAbsent(dateField, FieldTypes.MEMO);

		exProps.forEach(prop -> {
			ExDate<Temporal> obj = (ExDate<Temporal>) prop;
			obj.getDates().forEach(temporal -> {
				getLocalDateOrTime(dateField, null, temporal, false);
				result.add(General.convertDate((LocalDate) map.get(dateField)));
			});
		});

		map.put(dateField, General.convertListToString(result));
	}

	@SuppressWarnings("unchecked")
	private void getRDates(Property rDateProp, String dateStart, String dateEnd) {
		List<String> result1 = new ArrayList<>();
		List<String> result2 = new ArrayList<>();

		RDate<?> rDate = (RDate<?>) rDateProp;
		String temporalType = "PERIOD";
		Optional<Parameter> parmOpt = rDateProp.getParameter(Parameter.VALUE);
		if (parmOpt.isPresent()) {
			temporalType = parmOpt.get().getValue();
		}

		if (temporalType.equals("PERIOD")) {
			Optional<?> periodsOpt = rDate.getPeriods();
			if (periodsOpt.isEmpty()) {
				return;
			}

			Set<net.fortuna.ical4j.model.Period<?>> periods = (Set<net.fortuna.ical4j.model.Period<?>>) periodsOpt
					.get();
			periods.forEach(period -> {
				result1.add(General.convertTimestamp((LocalDateTime) period.getStart()));
				result2.add(General.convertTimestamp((LocalDateTime) period.getEnd()));
			});
		} else {
			boolean isStart = true;
			boolean isDateTime = temporalType.equals("DATE-TIME");
			for (Object date : rDate.getDates()) {
				if (isStart) {
					result1.add(isDateTime ? General.convertTimestamp((LocalDateTime) date)
							: General.convertDate((LocalDate) date));
				} else {
					result2.add(isDateTime ? General.convertTimestamp((LocalDateTime) date)
							: General.convertDate((LocalDate) date));
				}
				isStart = !isStart;
			}
		}

		fields.putIfAbsent(dateStart, FieldTypes.MEMO);
		fields.putIfAbsent(dateEnd, FieldTypes.MEMO);

		map.put(dateStart, General.convertListToString(result1));
		map.put(dateEnd, General.convertListToString(result2));
	}

	private void getRules(Property propRule, String prefix) {
		RRule<?> rrule = (RRule<?>) propRule;
		Recur<?> recur = rrule.getRecur();

		map.put(prefix + "RecurFreq", General.capitalizeFirstLetter(recur.getFrequency().toString()));
		getLocalDateOrTime(prefix + "RecurEndBy", null, recur.getUntil(), false);

		// Get Weekday(s)
		List<WeekDay> days = recur.getDayList();
		StringBuilder buf = new StringBuilder();

		if (CollectionUtils.isNotEmpty(days)) {
			switch (days.size()) {
			case 1:
				WeekDay day = days.get(0);
				buf.append(weekDays.get(day));
				if (day.getOffset() > 0) {
					buf.insert(0, General.ordinal(day.getOffset()) + General.SPACE);
				}
				break;
			case 7:
				buf.append("Daily");
				break;
			case 5:
				String weekdays = "MO,TU,WE,TH,FR";
				if (days.stream().allMatch(wDay -> weekdays.contains(wDay.getDay().toString()))) {
					buf.append("Weekdays");
					break;
				}
			default:
				days.forEach(wDay -> buf.append(weekDays.get(wDay)).append(", "));
				buf.delete(buf.length() - 2, buf.length());
			}
		}

		if (recur.getInterval() > 0) {
			buf.append(buf.length() > 0 ? ", every " : "Every ").append(General.ordinal(recur.getInterval()));
		}

		if (recur.getFrequency() != null) {
			if (buf.length() > 0) {
				buf.append(General.SPACE).append(frequency.get(recur.getFrequency()));
			} else {
				buf.append(General.capitalizeFirstLetter(recur.getFrequency().toString()));
			}
		}

		if (recur.getCount() > 0) {
			map.put(prefix + "RecurEndAfter", recur.getCount() + " times");
			fields.putIfAbsent(prefix + "RecurEndAfter", FieldTypes.TEXT);
		}

		map.put(prefix + "RecurRepeat", buf.toString());
		fields.putIfAbsent(prefix + "RecurRepeat", FieldTypes.TEXT);
	}

	private void getText(Property prop, String textField, boolean isUppercase) {
		String value = prop.getValue().trim();
		if (StringUtils.isEmpty(value)) {
			return;
		}

		if (value.equals("X-EMAIL")) {
			value = "Email";
		} else if (textField.endsWith(REQUEST_STATUS)) {
			value = reqStatus.get(value);
		} else if (textField.endsWith(REPEAT)) {
			value += " times";
		} else if (textField.endsWith(STATUS)) {
			value = eventStatus.get(value);
		} else if (textField.endsWith(ORGANIZER) && value.contains(":")) {
			value = value.substring(value.indexOf(":") + 1);
		}

		map.put(textField, isUppercase ? General.capitalizeFirstLetter(value) : value);
		boolean isMemo = value.contains("\n");

		FieldTypes type = fields.getOrDefault(textField, FieldTypes.TEXT);
		if (isMemo && type.equals(FieldTypes.TEXT)) {
			fields.put(textField, FieldTypes.MEMO);
		} else {
			fields.putIfAbsent(textField, isMemo ? FieldTypes.MEMO : FieldTypes.TEXT);
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

	@SuppressWarnings("unchecked")
	private void getDateAndTime(Property dateProp, String dateField, String timeField, boolean isTimestamp) {
		getLocalDateOrTime(dateField, timeField, ((DateProperty<?>) dateProp).getDate(), isTimestamp);
	}

	private void getLocalDateOrTime(String dateField, String timeField, Object obj, boolean isTimestamp) {
		if (obj == null) {
			return;
		}

		if (obj instanceof LocalDate) {
			map.put(dateField, obj);
			fields.putIfAbsent(dateField, FieldTypes.DATE);
			return;
		}

		LocalDateTime localTime = null;
		if (obj instanceof LocalDateTime) {
			localTime = (LocalDateTime) obj;
		} else if (obj instanceof ZonedDateTime) {
			localTime = ((ZonedDateTime) obj).toLocalDateTime();
		} else if (obj instanceof Instant) {
			localTime = LocalDateTime.parse(obj.toString(),
					DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()));
		}

		if (localTime != null) {
			if (isTimestamp) {
				map.put(dateField, localTime);
				fields.putIfAbsent(dateField, FieldTypes.TIMESTAMP);
			} else {
				map.put(dateField, localTime.toLocalDate());
				fields.putIfAbsent(dateField, FieldTypes.DATE);
			}

			if (timeField != null && LocalTime.of(23, 59, 59).isAfter(localTime.toLocalTime())) {
				map.put(timeField, localTime.toLocalTime());
				fields.putIfAbsent(timeField, FieldTypes.TIME);
			}
		}
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		return dbRecords.get(currentRecord++);
	}

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		// Not used yet, we are only importing
		return 0;
	}

	@Override
	public List<Object> getDbFieldValues(String field) throws Exception {
		Set<Object> result = new HashSet<>();
		dbRecords.forEach(m -> result.add(m.getOrDefault(field, General.EMPTY_STRING)));
		return new ArrayList<>(result);
	}
}
