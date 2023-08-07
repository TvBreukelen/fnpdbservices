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
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.Participant;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.component.VJournal;
import net.fortuna.ical4j.model.component.VResource;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.FreeBusy;
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
	private static final String ATTACHMENTS = "Attachments";
	private static final String ATTENDEES = "Attendees";
	private static final String CATEGORIES = "Categories";
	private static final String CLASSIFICATION = "Classification";
	private static final String COMMENTS = "Comments";
	private static final String COMPLETED = "Completed";
	private static final String CONTACT = "Contact";
	private static final String CREATED = "Created";
	private static final String DATE_END = "DateEnd";
	private static final String DATE_START = "DateStart";
	private static final String DESCRIPTION = "Description";
	private static final String DUE_DATE = "DueDate";
	private static final String DURATION = "Duration";
	private static final String EVENT = "Event";
	private static final String FREE_BUSY_END = "FreeBusyEnd";
	private static final String FREE_BUSY_START = "FreeBusyStart";
	private static final String FREE_BUSY_TYPE = "FreeBusyType";
	private static final String GEO = "Geo";
	private static final String LAST_MODIFIED = "LastModified";
	private static final String LOCATION = "Location";
	private static final String ORGANIZER = "Organizer";
	private static final String PARTICIPANT = "Participant";
	private static final String PERCENT_COMPLETE = "PercentComplete";
	private static final String PRIORITY = "Priority";
	private static final String RECURRENCE_ID = "RecurrenceId";
	private static final String REPEAT = "Repeat";
	private static final String REQUEST_STATUS = "RequestStatus";
	private static final String STATUS = "Status";
	private static final String SUMMARY = "Summary";
	private static final String TIME_END = "TimeEnd";
	private static final String TIME_START = "TimeStart";
	private static final String TIMESTAMP = "Timestamp";
	private static final String TRANSP = "Transp";
	private static final String UID = "Uid";
	private static final String URL = "Url";

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
		System.setProperty("ical4j.unfolding.relaxed", "true");
		System.setProperty("ical4j.compatibility.notes", "true");
	}

	@Override
	public void closeFile() {
		// Auto close
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		currentRecord = 0;
		dbRecords.clear();

		try (FileInputStream fin = new FileInputStream(myDatabase)) {
			CalendarBuilder builder = new CalendarBuilder();
			Calendar cal = builder.build(fin);
			iCals = cal.getComponents();
		}

		totalRecords = iCals.size();
		if (totalRecords == 0) {
			throw FNProgException.getException("noFields", myDatabase);
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
			String eventType;

			switch (comp.getName()) {
			case Component.VALARM:
				eventType = ALARM;
				processAlarm((VAlarm) comp, null);
				break;
			case Component.VAVAILABILITY:
				eventType = "Availability";
				break;
			case Component.VEVENT:
				eventType = EVENT;
				processEvent((VEvent) comp);
				break;
			case Component.VFREEBUSY:
				eventType = "FreeBusy";
				processFreeBusy((VFreeBusy) comp, "");
				break;
			case Component.VJOURNAL:
				eventType = "Journal";
				processJournal((VJournal) comp);
				break;
			case Component.VTIMEZONE:
				totalRecords--;
				continue;
			case Component.VTODO:
				eventType = "Task";
				processToDo((VToDo) comp);
				break;
			default:
				eventType = EVENT;
				break;
			}

			map.put(EVENT, eventType);
			fields.putIfAbsent(EVENT, FieldTypes.TEXT);

			dbRecords.add(map);
		}

		fields.keySet().forEach(field -> dbFieldNames.add(field));
		Collections.sort(dbFieldNames);
		dbFieldNames.forEach(field -> dbFieldTypes.add(fields.get(field)));
	}

	private void processEvent(VEvent event) {
		getList(event.getAttachments(), ATTACHMENTS);
		getAttendees(event.getAttendees(), ATTENDEES);
		processList(event.getCategories(), CATEGORIES);
		getText(event.getClassification(), CLASSIFICATION, true);
		getList(event.getComments(), COMMENTS);
		getText(event.getContact(), CONTACT, false);
		getDateAndTime(event.getCreated(), CREATED, null, true);
		getDateAndTime(event.getDateTimeCompleted(), COMPLETED, null, false);
		getDateAndTime(event.getDateTimeDue(), DUE_DATE, null, false);
		getDateAndTime(event.getDateTimeEnd(), DATE_END, TIME_END, false);
		getDateAndTime(event.getDateTimeStamp(), TIMESTAMP, null, true);
		getDateAndTime(event.getDateTimeStart(), DATE_START, TIME_START, false);
		getText(event.getDescription(), DESCRIPTION, false);
		getDuration(event.getDuration(), DURATION);
		getFreeBusy(event.getFreeBusyTime(), FREE_BUSY_START, FREE_BUSY_END, FREE_BUSY_TYPE);
		getText(event.getGeographicPos(), GEO, false);
		getDateAndTime(event.getLastModified(), LAST_MODIFIED, null, true);
		getText(event.getLocation(), LOCATION, false);
		getText(event.getOrganizer(), ORGANIZER, false);
		event.getParticipants().forEach(participant -> processParticipant(participant, PARTICIPANT));
		getText(event.getPercentComplete(), PERCENT_COMPLETE, false);
		getText(event.getPriority(), PRIORITY, false);
		getDateAndTime(event.getRecurrenceId(), RECURRENCE_ID, null, true);
		processResources(event.getResources());
		getText(event.getStatus(), STATUS, false);
		getText(event.getSummary(), SUMMARY, false);
		getText(event.getTimeTransparency(), TRANSP, true);
		getText(event.getUid(), UID, false);
		getText(event.getUrl(), URL, false);

		getText(event.getProperty(Property.REQUEST_STATUS), REQUEST_STATUS, false);
		getExDates(event.getProperties(Property.EXDATE), "ExDate");
		getRules(event.getProperty(Property.RRULE));
		event.getAlarms().forEach(alarm -> processAlarm(alarm, ALARM));
	}

	private void processFreeBusy(VFreeBusy freeBusy, String prefix) {
		getAttendees(freeBusy.getProperties(Property.ATTENDEE), ATTENDEES);
		getText(freeBusy.getProperty(Property.COMMENT), prefix + COMMENTS, false);
		getText(freeBusy.getProperty(Property.CONTACT), prefix + CONTACT, false);
		getDateAndTime(freeBusy.getProperty(Property.DTEND), prefix + DATE_END, prefix + TIME_END, false);
		getDateAndTime(freeBusy.getProperty(Property.DTSTAMP), prefix + TIMESTAMP, null, true);
		getDateAndTime(freeBusy.getProperty(Property.DTSTART), prefix + DATE_START, prefix + TIME_START, false);
		getDuration(freeBusy.getProperty(Property.DURATION), prefix + DURATION);
		getFreeBusy(freeBusy.getProperty(Property.FREEBUSY), prefix + FREE_BUSY_START, prefix + FREE_BUSY_END,
				prefix + FREE_BUSY_TYPE);
		getText(freeBusy.getProperty(Property.REQUEST_STATUS), prefix + REQUEST_STATUS, false);
		getText(freeBusy.getProperty(Property.ORGANIZER), prefix + ORGANIZER, false);
		getText(freeBusy.getProperty(Property.UID), prefix + UID, false);
		getText(freeBusy.getProperty(Property.URL), prefix + URL, false);
	}

	private void processJournal(VJournal journal) {
		getList(journal.getAttachments(), ATTACHMENTS);
		getAttendees(journal.getAttendees(), ATTENDEES);
		processList(journal.getCategories(), CATEGORIES);
		getText(journal.getClassification(), CLASSIFICATION, true);
		getList(journal.getComments(), COMMENTS);
		getText(journal.getContact(), CONTACT, false);
		getDateAndTime(journal.getCreated(), CREATED, null, true);
		getDateAndTime(journal.getDateTimeCompleted(), COMPLETED, null, false);
		getDateAndTime(journal.getDateTimeDue(), DUE_DATE, null, false);
		getDateAndTime(journal.getDateTimeEnd(), DATE_END, TIME_END, false);
		getDateAndTime(journal.getDateTimeStamp(), TIMESTAMP, null, true);
		getDateAndTime(journal.getDateTimeStart(), DATE_START, TIME_START, false);
		getText(journal.getDescription(), DESCRIPTION, false);
		getDuration(journal.getDuration(), DURATION);
		getFreeBusy(journal.getFreeBusyTime(), FREE_BUSY_START, FREE_BUSY_END, FREE_BUSY_TYPE);
		getText(journal.getGeographicPos(), GEO, false);
		getDateAndTime(journal.getLastModified(), LAST_MODIFIED, null, true);
		getText(journal.getLocation(), LOCATION, false);
		getText(journal.getOrganizer(), ORGANIZER, false);
		journal.getParticipants().forEach(participant -> processParticipant(participant, PARTICIPANT));
		getText(journal.getPercentComplete(), PERCENT_COMPLETE, false);
		getText(journal.getPriority(), PRIORITY, false);
		getText(journal.getRecurrenceId(), RECURRENCE_ID, false);
		processResources(journal.getResources());
		getText(journal.getStatus(), STATUS, false);
		getText(journal.getSummary(), SUMMARY, false);
		getText(journal.getTimeTransparency(), TRANSP, true);
		getText(journal.getUid(), UID, false);
		getText(journal.getUrl(), URL, false);

		getText(journal.getProperty(Property.REQUEST_STATUS), REQUEST_STATUS, false);
		getExDates(journal.getProperties(Property.EXDATE), "ExDate");
		getRules(journal.getProperty(Property.RRULE));
	}

	private void processResources(List<VResource> resources) {
		if (resources.isEmpty()) {
			return;
		}

		StringBuilder result = new StringBuilder();
		resources.forEach(resource -> result.append(resource.getValue()).append("\n"));
		map.put("Resources", result.toString().trim());
	}

	private void processToDo(VToDo todo) {
		getList(todo.getAttachments(), ATTACHMENTS);
		getAttendees(todo.getAttendees(), ATTENDEES);
		processList(todo.getCategories(), CATEGORIES);
		getText(todo.getClassification(), CLASSIFICATION, true);
		getList(todo.getComments(), COMMENTS);
		getText(todo.getContact(), CONTACT, false);
		getDateAndTime(todo.getCreated(), CREATED, null, true);
		getDateAndTime(todo.getDateTimeCompleted(), COMPLETED, null, false);
		getDateAndTime(todo.getDateTimeDue(), DUE_DATE, null, false);
		getDateAndTime(todo.getDateTimeEnd(), DATE_END, TIME_END, false);
		getDateAndTime(todo.getDateTimeStamp(), TIMESTAMP, null, true);
		getDateAndTime(todo.getDateTimeStart(), DATE_START, TIME_START, false);
		getText(todo.getDescription(), DESCRIPTION, false);
		getDuration(todo.getDuration(), DURATION);
		getFreeBusy(todo.getFreeBusyTime(), FREE_BUSY_START, FREE_BUSY_END, FREE_BUSY_TYPE);
		getText(todo.getGeographicPos(), GEO, false);
		getDateAndTime(todo.getLastModified(), LAST_MODIFIED, null, true);
		getText(todo.getLocation(), LOCATION, false);
		getText(todo.getOrganizer(), ORGANIZER, false);
		todo.getParticipants().forEach(participant -> processParticipant(participant, PARTICIPANT));
		getText(todo.getPercentComplete(), PERCENT_COMPLETE, false);
		getText(todo.getPriority(), PRIORITY, false);
		getText(todo.getRecurrenceId(), RECURRENCE_ID, false);
		processResources(todo.getResources());
		getText(todo.getStatus(), STATUS, false);
		getText(todo.getSummary(), SUMMARY, false);
		getText(todo.getTimeTransparency(), TRANSP, true);
		getText(todo.getUid(), UID, false);
		getText(todo.getUrl(), URL, false);

		todo.getAlarms().forEach(alarm -> processAlarm(alarm, ALARM));
	}

	private void processAlarm(VAlarm alarm, String prefix) {
		getList(alarm.getAttachments(), prefix + ATTACHMENTS);
		processList(alarm.getCategories(), prefix + CATEGORIES);
		getText(alarm.getClassification(), prefix + CLASSIFICATION, true);
		getList(alarm.getComments(), prefix + COMMENTS);
		getText(alarm.getDescription(), prefix + DESCRIPTION, false);
		getText(alarm.getGeographicPos(), prefix + GEO, false);
		getText(alarm.getLocation(), prefix + LOCATION, false);
		getText(alarm.getPercentComplete(), prefix + PERCENT_COMPLETE, false);
		getText(alarm.getPriority(), prefix + PRIORITY, false);
		getText(alarm.getStatus(), prefix + STATUS, true);
		getText(alarm.getSummary(), prefix + SUMMARY, false);

		getText(alarm.getProperty(Property.ACTION), prefix + "Action", true);
		getText(alarm.getProperty(Property.REPEAT), prefix + REPEAT, true);
		getDuration(alarm.getProperty(Property.DURATION), prefix + DURATION);

		processTrigger(alarm, prefix);
	}

	private void processParticipant(Participant participant, String prefix) {
		getText(participant.getCalendarAddress(), prefix + "Address", false);
		getDateAndTime(participant.getCreated(), prefix + CREATED, null, true);
		getDateAndTime(participant.getDateStamp(), prefix + TIMESTAMP, null, true);
		getText(participant.getDescription(), prefix + DESCRIPTION, false);
		getDateAndTime(participant.getLastModified(), prefix + LAST_MODIFIED, null, true);
		getText(participant.getParticipantType(), prefix + "Type", false);
		getText(participant.getPriority(), prefix + PRIORITY, false);
		getText(participant.getStatus(), prefix + STATUS, false);
		getText(participant.getSummary(), prefix + SUMMARY, false);
		getText(participant.getUid(), prefix + UID, false);
		getText(participant.getUrl(), prefix + URL, false);

		getText(participant.getProperty(Property.GEO), prefix + GEO, true);
	}

	private void processTrigger(VAlarm alarm, String prefix) {
		getDateAndTime(alarm.getProperty(Property.TRIGGER), prefix + "Trigger", null, true);
		if (prefix.isEmpty()) {
			return;
		}

		Optional<Property> triggerOpt = alarm.getProperty(Property.TRIGGER);
		if (triggerOpt.isEmpty()) {
			return;
		}

		Trigger trigger = (Trigger) triggerOpt.get();
		if (trigger.getDuration() == null) {
			return;
		}

		calculateDuration(prefix + "TriggerStart", trigger.getDuration());
	}

	private void getDuration(Optional<Duration> duration, String durationField) {
		if (duration.isEmpty()) {
			return;
		}
		calculateDuration(durationField, duration.get().getDuration());
	}

	private void calculateDuration(String durationField, TemporalAmount duration) {
		boolean add = duration.get(duration instanceof Period ? ChronoUnit.DAYS : ChronoUnit.SECONDS) >= 0;

		LocalDate date = (LocalDate) map.get(add ? DATE_START : DATE_END);
		LocalTime time = (LocalTime) map.get(add ? TIME_START : TIME_END);
		if (date != null && time != null) {
			LocalDateTime ldt = time.atDate(date).plus(duration);
			map.put(durationField, ldt);
			fields.putIfAbsent(durationField, FieldTypes.TIMESTAMP);
		}
	}

	private void getFreeBusy(Optional<FreeBusy> freeBusyOpt, String fbStartField, String fbEndField,
			String fbTypeField) {

		if (freeBusyOpt.isEmpty()) {
			return;
		}

		StringBuilder resultStart = new StringBuilder();
		StringBuilder resultEnd = new StringBuilder();
		FreeBusy freeBusy = freeBusyOpt.get();
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

	private void processList(Optional<? extends Property> list, String textField) {
		if (list.isEmpty()) {
			return;
		}

		String[] elements = list.get().getValue().split(",");
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

	private void getRules(Optional<Property> optRule) {
		if (optRule.isEmpty()) {
			return;
		}

		RRule<?> rrule = (RRule<?>) optRule.get();
		Recur<?> recur = rrule.getRecur();

		map.put("RecurFreq", General.capitalizeFirstLetter(recur.getFrequency().toString()));
		getLocalDateOrTime("RecurEndBy", null, recur.getUntil(), false);

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
			map.put("RecurEndAfter", recur.getCount() + " times");
			fields.putIfAbsent("RecurEndAfter", FieldTypes.TEXT);
		}

		map.put("RecurRepeat", buf.toString());
		fields.putIfAbsent("RecurRepeat", FieldTypes.TEXT);
	}

	private void getText(Optional<? extends Property> prop, String textField, boolean isUppercase) {
		if (prop.isEmpty()) {
			return;
		}

		String value = prop.get().getValue().trim();
		if (StringUtils.isEmpty(value)) {
			return;
		}

		if (textField.endsWith(REQUEST_STATUS)) {
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
	private void getDateAndTime(Optional<? extends Property> optDate, String dateField, String timeField,
			boolean isTimestamp) {

		if (optDate.isEmpty()) {
			return;
		}

		DateProperty<Temporal> dateProp = (DateProperty<Temporal>) optDate.get();
		getLocalDateOrTime(dateField, timeField, dateProp.getDate(), isTimestamp);
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
	public void processData(Map<String, Object> dbRecord) throws Exception {
		// Not used yet, we are only importing
	}

	@Override
	public List<Object> getDbFieldValues(String field) throws Exception {
		Set<Object> result = new HashSet<>();
		dbRecords.forEach(m -> result.add(m.getOrDefault(field, "")));
		return new ArrayList<>(result);
	}
}
