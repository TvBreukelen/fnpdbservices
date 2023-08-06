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
import net.fortuna.ical4j.model.component.VJournal;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
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

	private static Map<WeekDay, String> weekDays = new HashMap<>();
	private static EnumMap<Frequency, String> frequency = new EnumMap<>(Frequency.class);
	private static Map<String, String> status = new HashMap<>();
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

		status.put(Status.VALUE_CANCELLED, "Cancelled");
		status.put(Status.VALUE_COMPLETED, "Completed");
		status.put(Status.VALUE_CONFIRMED, "Confirmed");
		status.put(Status.VALUE_DRAFT, "Draft");
		status.put(Status.VALUE_FINAL, "Final");
		status.put(Status.VALUE_IN_PROCESS, "In Process");
		status.put(Status.VALUE_NEEDS_ACTION, "Needs Action");
		status.put(Status.VALUE_TENTATIVE, "Tentative");

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
			String event;

			switch (comp.getName()) {
			case Component.VALARM:
				event = "Alarm";
				processAlarm((VAlarm) comp, null);
				break;
			case Component.VAVAILABILITY:
				event = "Availability";
				break;
			case Component.VEVENT:
				event = "Event";
				processEvent((VEvent) comp);
				break;
			case Component.VFREEBUSY:
				event = "Free/Busy";
				break;
			case Component.VJOURNAL:
				event = "Journal";
				processJournal((VJournal) comp);
				break;
			case Component.VTIMEZONE:
				totalRecords--;
				continue;
			case Component.VTODO:
				event = "Task";
				processToDo((VToDo) comp);
				break;
			default:
				event = "Event";
				break;
			}

			map.put("Event", event);
			fields.putIfAbsent("Event", FieldTypes.TEXT);

			dbRecords.add(map);
		}

		fields.keySet().forEach(field -> dbFieldNames.add(field));
		Collections.sort(dbFieldNames);
		dbFieldNames.forEach(field -> dbFieldTypes.add(fields.get(field)));
	}

	private void processEvent(VEvent event) {
		getList(event.getAttachments(), "Attachments");
		getAttendees(event.getAttendees(), "Attendees");
		processList(event.getCategories(), "Categories");
		getText(event.getClassification(), "Classification", true);
		getList(event.getComments(), "Comments");
		getText(event.getContact(), "Contact", false);
		getDateAndTime(event.getCreated(), "Created", null, true);
		getDateAndTime(event.getDateTimeCompleted(), "Completed", null, false);
		getDateAndTime(event.getDateTimeDue(), "DueDate", null, false);
		getDateAndTime(event.getDateTimeEnd(), "DateEnd", "TimeEnd", false);
		getDateAndTime(event.getDateTimeStamp(), "Timestamp", null, true);
		getDateAndTime(event.getDateTimeStart(), "DateStart", "TimeStart", false);
		getText(event.getDescription(), "Description", false);
		getDuration(event.getDuration(), "Duration");
		event.getFreeBusyTime();
		getText(event.getGeographicPos(), "Geo", false);
		getDateAndTime(event.getLastModified(), "LastModified", null, true);
		getText(event.getLocation(), "Location", false);
		getText(event.getOrganizer(), "Organizer", false);
		event.getParticipants().forEach(participant -> processParticipant(participant, "Participant."));
		getText(event.getPercentComplete(), "PercentComplete", false);
		getText(event.getPriority(), "Priority", false);
		getDateAndTime(event.getRecurrenceId(), "RecurrenceId", null, true);
		event.getResources();
		getText(event.getStatus(), "Status", false);
		getText(event.getSummary(), "Summary", false);
		event.getTimeTransparency();
		getText(event.getUid(), "Uid", false);
		getText(event.getUrl(), "Url", false);

		getText(event.getProperty(Property.REQUEST_STATUS), "RequestStatus", false);
		getExDates(event.getProperties(Property.EXDATE), "ExDate");
		getRules(event.getProperty(Property.RRULE));
		event.getAlarms().forEach(alarm -> processAlarm(alarm, "Alarm"));
	}

	private void processJournal(VJournal journal) {
		getList(journal.getAttachments(), "Attachments");
		getAttendees(journal.getAttendees(), "Attendees");
		processList(journal.getCategories(), "Categories");
		getText(journal.getClassification(), "Classification", true);
		getList(journal.getComments(), "Comments");
		getText(journal.getContact(), "Contact", false);
		getDateAndTime(journal.getCreated(), "Created", null, true);
		getDateAndTime(journal.getDateTimeCompleted(), "Completed", null, false);
		getDateAndTime(journal.getDateTimeDue(), "DueDate", null, false);
		getDateAndTime(journal.getDateTimeEnd(), "DateEnd", "TimeEnd", false);
		getDateAndTime(journal.getDateTimeStamp(), "Timestamp", null, true);
		getDateAndTime(journal.getDateTimeStart(), "DateStart", "TimeStart", false);
		getText(journal.getDescription(), "Description", false);
		getDuration(journal.getDuration(), "Duration");
		journal.getFreeBusyTime();
		getText(journal.getGeographicPos(), "Geo", false);
		getDateAndTime(journal.getLastModified(), "LastModified", null, true);
		getText(journal.getLocation(), "Location", false);
		getText(journal.getOrganizer(), "Organizer", false);
		journal.getParticipants().forEach(participant -> processParticipant(participant, "Participant."));
		getText(journal.getPercentComplete(), "PercentComplete", false);
		getText(journal.getPriority(), "Priority", false);
		getText(journal.getRecurrenceId(), "RecurrenceId", false);
		journal.getResources();
		getText(journal.getStatus(), "Status", false);
		getText(journal.getSummary(), "Summary", false);
		journal.getTimeTransparency();
		getText(journal.getUid(), "Uid", false);
		getText(journal.getUrl(), "Url", false);

		getText(journal.getProperty(Property.REQUEST_STATUS), "RequestStatus", false);
		getExDates(journal.getProperties(Property.EXDATE), "ExDate");
		getRules(journal.getProperty(Property.RRULE));
	}

	private void processToDo(VToDo todo) {
		getList(todo.getAttachments(), "Attachments");
		getAttendees(todo.getAttendees(), "Attendees");
		processList(todo.getCategories(), "Categories");
		getText(todo.getClassification(), "Classification", true);
		getList(todo.getComments(), "Comments");
		getText(todo.getContact(), "Contact", false);
		getDateAndTime(todo.getCreated(), "Created", null, true);
		getDateAndTime(todo.getDateTimeCompleted(), "Completed", null, false);
		getDateAndTime(todo.getDateTimeDue(), "DueDate", null, false);
		getDateAndTime(todo.getDateTimeEnd(), "DateEnd", "TimeEnd", false);
		getDateAndTime(todo.getDateTimeStamp(), "Timestamp", null, true);
		getDateAndTime(todo.getDateTimeStart(), "DateStart", "TimeStart", false);
		getText(todo.getDescription(), "Description", false);
		getDuration(todo.getDuration(), "Duration");
		todo.getFreeBusyTime();
		getText(todo.getGeographicPos(), "Geo", false);
		getDateAndTime(todo.getLastModified(), "LastModified", null, true);
		getText(todo.getLocation(), "Location", false);
		getText(todo.getOrganizer(), "Organizer", false);
		todo.getParticipants().forEach(participant -> processParticipant(participant, "Participant."));
		getText(todo.getPercentComplete(), "PercentComplete", false);
		getText(todo.getPriority(), "Priority", false);
		getText(todo.getRecurrenceId(), "RecurrenceId", false);
		todo.getResources();
		getText(todo.getStatus(), "Status", false);
		getText(todo.getSummary(), "Summary", false);
		todo.getTimeTransparency();
		getText(todo.getUid(), "Uid", false);
		getText(todo.getUrl(), "Url", false);

		todo.getAlarms().forEach(alarm -> processAlarm(alarm, "Alarm"));
	}

	private void processAlarm(VAlarm alarm, String prefix) {
		getList(alarm.getAttachments(), prefix + "Attachments");
		processList(alarm.getCategories(), prefix + "Categories");
		getText(alarm.getClassification(), prefix + "Classification", true);
		getList(alarm.getComments(), prefix + "Comments");
		getText(alarm.getDescription(), prefix + "Description", false);
		getText(alarm.getGeographicPos(), prefix + "Geo", false);
		getText(alarm.getLocation(), prefix + "Location", false);
		getText(alarm.getPercentComplete(), prefix + "PercentComplete", false);
		getText(alarm.getPriority(), prefix + "Priority", false);
		getText(alarm.getStatus(), prefix + "Priority", true);
		getText(alarm.getSummary(), prefix + "Summary", false);

		getText(alarm.getProperty(Property.ACTION), prefix + "Action", true);
		getText(alarm.getProperty(Property.REPEAT), prefix + "Repeat", true);
		getDuration(alarm.getProperty(Property.DURATION), prefix + "Duration");

		processTrigger(alarm, prefix);
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

		LocalDate date = (LocalDate) map.get(add ? "DateStart" : "DateEnd");
		LocalTime time = (LocalTime) map.get(add ? "TimeStart" : "TimeEnd");
		if (date != null && time != null) {
			LocalDateTime ldt = time.atDate(date).plus(duration);
			map.put(durationField, ldt);
			fields.putIfAbsent(durationField, FieldTypes.TIMESTAMP);
		}
	}

	private void processParticipant(Participant participant, String prefix) {
		System.out.println(participant);
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

		if (textField.endsWith("RequestStatus")) {
			value = reqStatus.get(value);
		} else if (textField.endsWith("Repeat")) {
			value += " times";
		} else if (textField.endsWith("Status")) {
			value = status.get(value);
		} else if (textField.endsWith("Organizer") && value.contains(":")) {
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
