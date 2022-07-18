package dbengine.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import ezvcard.Ezvcard;
import ezvcard.parameter.ExpertiseLevel;
import ezvcard.parameter.HobbyLevel;
import ezvcard.parameter.InterestLevel;
import ezvcard.property.Address;
import ezvcard.property.Agent;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.Birthplace;
import ezvcard.property.CalendarRequestUri;
import ezvcard.property.CalendarUri;
import ezvcard.property.Categories;
import ezvcard.property.Classification;
import ezvcard.property.Deathdate;
import ezvcard.property.Deathplace;
import ezvcard.property.Email;
import ezvcard.property.Expertise;
import ezvcard.property.FormattedName;
import ezvcard.property.FreeBusyUrl;
import ezvcard.property.Gender;
import ezvcard.property.Hobby;
import ezvcard.property.Impp;
import ezvcard.property.Interest;
import ezvcard.property.Kind;
import ezvcard.property.Mailer;
import ezvcard.property.Member;
import ezvcard.property.Nickname;
import ezvcard.property.Note;
import ezvcard.property.OrgDirectory;
import ezvcard.property.Organization;
import ezvcard.property.Profile;
import ezvcard.property.Role;
import ezvcard.property.SortString;
import ezvcard.property.Source;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Uid;
import ezvcard.property.Url;
import ezvcard.util.PartialDate;

public class VCard extends GeneralDB implements IConvert {
	private int currentRecord;
	private List<ezvcard.VCard> vcards;
	private Map<String, FieldTypes> fields;
	private Map<String, Object> map;

	protected List<Map<String, Object>> dbRecords = new ArrayList<>();

	public VCard(Profiles pref) {
		super(pref);
	}

	@Override
	public void closeFile() {
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		currentRecord = 0;
		vcards = Ezvcard.parse(new File(myDatabase)).all();
		totalRecords = vcards.size();
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		// Not used yet, we are only importing
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		return dbRecords.get(currentRecord++);
	}

	@Override
	public void readTableContents() throws Exception {
		dbFieldNames.clear();
		dbFieldTypes.clear();
		fields = new HashMap<>();

		for (ezvcard.VCard vcard : vcards) {
			map = new HashMap<>();

			setAddresses(vcard.getAddresses());
			setAgent(vcard.getAgent());
			setAnniversary(vcard.getAnniversary());
			setBirthday(vcard.getBirthday());
			setBirthplace(vcard.getBirthplace());
			setCalendarRequestsUris(vcard.getCalendarRequestUris());
			setCalendarUris(vcard.getCalendarUris());
			setCategories(vcard.getCategories());
			setClassification(vcard.getClassification());
			setDeathdate(vcard.getDeathdate());
			setDeathplace(vcard.getDeathplace());
			setEmails(vcard.getEmails());
			setExpertise(vcard.getExpertise());
			setFbUrls(vcard.getFbUrls());
			setFormattedName(vcard.getFormattedName());
			setGender(vcard.getGender());
			setHobbies(vcard.getHobbies());
			setImpps(vcard.getImpps());
			setInterests(vcard.getInterests());
			setKind(vcard.getKind());
			setMailer(vcard.getMailer());
			setMembers(vcard.getMembers());
			setNotes(vcard.getNotes());
			setNickname(vcard.getNickname());
			setOrganisation(vcard.getOrganization());
			setOrgDirectories(vcard.getOrgDirectories());
			setProfile(vcard.getProfile());
			setRoles(vcard.getRoles());
			setSortString(vcard.getSortString());
			setSources(vcard.getSources());
			setStructuredName(vcard.getStructuredName());
			setTelephoneNumbers(vcard.getTelephoneNumbers());
			setTitles(vcard.getTitles());
			setUid(vcard.getUid());
			setUrls(vcard.getUrls());

			dbRecords.add(map);
		}

		fields.keySet().forEach(field -> dbFieldNames.add(field));
		Collections.sort(dbFieldNames);
		dbFieldNames.forEach(field -> dbFieldTypes.add(fields.get(field)));
	}

	private void setAddresses(List<Address> addresses) {
		if (CollectionUtils.isEmpty(addresses)) {
			return;
		}

		final String field = "Address";

		for (int i = 0; i < addresses.size(); i++) {
			Address address = addresses.get(i);
			setField(getFieldName(field, i, ".AltId"), address.getAltId());
			setField(getFieldName(field, i, ".Country"), address.getCountry());
			setField(getFieldName(field, i, ".ExtendedAddress"), address.getExtendedAddress());
			setField(getFieldName(field, i, ".ExtendedAddressFull"), address.getExtendedAddressFull());
			setField(getFieldName(field, i, ".Group"), address.getGroup());
			setField(getFieldName(field, i, ".Label"), address.getLabel());
			setField(getFieldName(field, i, ".Language"), address.getLanguage());
			setField(getFieldName(field, i, ".Locality"), address.getLocality());
			setField(getFieldName(field, i, ".PoBox"), address.getPoBox());
			setField(getFieldName(field, i, ".PostalCode"), address.getPostalCode());
			setField(getFieldName(field, i, ".Region"), address.getRegion());
			setField(getFieldName(field, i, ".StreetAddress"), address.getStreetAddress());
			setField(getFieldName(field, i, ".StreetAddressFull"), address.getStreetAddressFull());
		}
	}

	private void setAgent(Agent agent) {
		if (agent == null) {
			return;
		}

		setField("Agent.Group", agent.getGroup());
		setField("Agent.Url", agent.getUrl());
	}

	private void setAnniversary(Anniversary anniversary) {
		if (anniversary == null) {
			return;
		}

		setField("Anniversary.AltId", anniversary.getAltId());
		setField("Anniversary.Group", anniversary.getGroup());
		setField("Anniversary.Language", anniversary.getLanguage());
		setField("Anniversary.Text", anniversary.getText());

		Date date = anniversary.getDate();
		if (date != null) {
			setField("Anniversary.Date", anniversary.getDate());
		}

		PartialDate pDate = anniversary.getPartialDate();
		if (pDate != null) {
			setField("Anniversary.PartialDate", anniversary.getPartialDate());
		}
	}

	private void setBirthday(Birthday birthday) {
		if (birthday == null) {
			return;
		}

		setField("Birthday.AltId", birthday.getAltId());
		setField("Birthday.Group", birthday.getGroup());
		setField("Birthday.Language", birthday.getLanguage());
		setField("Birthday.Text", birthday.getText());
		setField("Birthday.Date", birthday.getDate());
	}

	private void setBirthplace(Birthplace birthplace) {
		if (birthplace == null) {
			return;
		}

		setField("Birthplace.AltId", birthplace.getAltId());
		setField("Birthplace.Group", birthplace.getGroup());
		setField("Birthplace.Language", birthplace.getLanguage());
		setField("Birthplace.Text", birthplace.getText());
		setField("Birthplace.Uri", birthplace.getUri());
	}

	private void setCalendarUris(List<CalendarUri> calendarUris) {
		if (CollectionUtils.isEmpty(calendarUris)) {
			return;
		}

		final String field = "CalendarUri";

		for (int i = 0; i < calendarUris.size(); i++) {
			CalendarUri uri = calendarUris.get(0);
			setField(getFieldName(field, i, ".AltId"), uri.getAltId());
			setField(getFieldName(field, i, ".Group"), uri.getGroup());
			setField(getFieldName(field, i, ".MediaType"), uri.getMediaType());
			setField(getFieldName(field, i, ".Type"), uri.getType());
			setField(getFieldName(field, i, ""), uri.getValue());
		}
	}

	private void setCalendarRequestsUris(List<CalendarRequestUri> calendarUris) {
		if (CollectionUtils.isEmpty(calendarUris)) {
			return;
		}

		final String field = "CalendarRequestUri";

		for (int i = 0; i < calendarUris.size(); i++) {
			CalendarRequestUri uri = calendarUris.get(0);
			setField(getFieldName(field, i, ".AltId"), uri.getAltId());
			setField(getFieldName(field, i, ".Group"), uri.getGroup());
			setField(getFieldName(field, i, ".MediaType"), uri.getMediaType());
			setField(getFieldName(field, i, ".Type"), uri.getType());
			setField(getFieldName(field, i, ""), uri.getValue());
		}
	}

	private void setCategories(Categories categories) {
		if (categories == null) {
			return;
		}

		setField("Categories.AltId", categories.getAltId());
		setField("Categories.Group", categories.getGroup());
		setField("Categories.Type", categories.getType());
		setField("Categories", categories.getValues());
	}

	private void setClassification(Classification classification) {
		if (classification == null) {
			return;
		}

		setField("Classification.Group", classification.getGroup());
		setField("Classification", classification.getValue());
	}

	private void setDeathdate(Deathdate deathdate) {
		if (deathdate == null) {
			return;
		}

		setField("Deathdate.AltId", deathdate.getAltId());
		setField("Deathdate.Group", deathdate.getGroup());
		setField("Deathdate.Language", deathdate.getLanguage());
		setField("Deathdate.Text", deathdate.getText());
		setField("Deathdate.Date", deathdate.getDate());
	}

	private void setDeathplace(Deathplace deathplace) {
		if (deathplace == null) {
			return;
		}

		setField("Deathplace.AltId", deathplace.getAltId());
		setField("Deathplace.Group", deathplace.getGroup());
		setField("Deathplace.Language", deathplace.getLanguage());
		setField("Deathplace.Text", deathplace.getText());
		setField("Deathplace.Uri", deathplace.getUri());
	}

	private void setEmails(List<Email> emails) {
		if (CollectionUtils.isEmpty(emails)) {
			return;
		}

		final String field = "Email";

		for (int i = 0; i < emails.size(); i++) {
			Email email = emails.get(i);
			setField(getFieldName(field, i, ".AltId"), email.getAltId());
			setField(getFieldName(field, i, ".Group"), email.getGroup());
			setField(getFieldName(field, i, ".Types"), email.getTypes());
			setField(getFieldName(field, i, ""), email.getValue());
		}
	}

	private void setExpertise(List<Expertise> expertise) {
		if (CollectionUtils.isEmpty(expertise)) {
			return;
		}

		final String field = "Expertise";

		for (int i = 0; i < expertise.size(); i++) {
			Expertise expert = expertise.get(i);
			setField(getFieldName(field, i, ".AltId"), expert.getAltId());
			setField(getFieldName(field, i, ".Group"), expert.getGroup());
			setField(getFieldName(field, i, ".Language"), expert.getLanguage());
			setField(getFieldName(field, i, ".Type"), expert.getType());
			setField(getFieldName(field, i, ""), expert.getValue());

			Optional<ExpertiseLevel> level = Optional.ofNullable(expert.getLevel());
			if (level.isPresent()) {
				setField(getFieldName(field, i, ".Level"), level.get().getValue());
			}
		}
	}

	private void setFbUrls(List<FreeBusyUrl> fbUrls) {
		if (CollectionUtils.isEmpty(fbUrls)) {
			return;
		}

		final String field = "FreeBusyUrl";

		for (int i = 0; i < fbUrls.size(); i++) {
			FreeBusyUrl url = fbUrls.get(i);
			setField(getFieldName(field, i, ".AltId"), url.getAltId());
			setField(getFieldName(field, i, ".Group"), url.getGroup());
			setField(getFieldName(field, i, ".MediaType"), url.getMediaType());
			setField(getFieldName(field, i, ".Type"), url.getType());
			setField(getFieldName(field, i, ""), url.getValue());
		}
	}

	private void setFormattedName(FormattedName name) {
		if (name == null) {
			return;
		}
		setField("FormattedName.AltId", name.getAltId());
		setField("FormattedName.Group", name.getGroup());
		setField("FormattedName.Language", name.getLanguage());
		setField("FormattedName.Type", name.getType());
		setField("FormattedName", name.getValue());
	}

	private void setGender(Gender gender) {
		if (gender == null) {
			return;
		}

		setField("Gender", gender.getGender());
		setField("Gender.Group", gender.getGroup());
		setField("Gender.Text", gender.getText());
	}

	private void setHobbies(List<Hobby> hobbies) {
		if (CollectionUtils.isEmpty(hobbies)) {
			return;
		}

		final String field = "Hobby";

		for (int i = 0; i < hobbies.size(); i++) {
			Hobby hobby = hobbies.get(i);
			setField(getFieldName(field, i, ".AltId"), hobby.getAltId());
			setField(getFieldName(field, i, ".Group"), hobby.getGroup());
			setField(getFieldName(field, i, ".Language"), hobby.getLanguage());

			Optional<HobbyLevel> level = Optional.ofNullable(hobby.getLevel());
			if (level.isPresent()) {
				setField(getFieldName(field, i, ".Level"), level.get().getValue());
			}

			setField(getFieldName(field, i, ".Type"), hobby.getType());
			setField(getFieldName(field, i, ""), hobby.getValue());
		}
	}

	private void setImpps(List<Impp> impps) {
		if (CollectionUtils.isEmpty(impps)) {
			return;
		}

		final String field = "Impp";

		for (int i = 0; i < impps.size(); i++) {
			Impp impp = impps.get(i);
			setField(getFieldName(field, i, ".AltId"), impp.getAltId());
			setField(getFieldName(field, i, ".Group"), impp.getGroup());
			setField(getFieldName(field, i, ".Handle"), impp.getHandle());
			setField(getFieldName(field, i, ".MediaType"), impp.getMediaType());
			setField(getFieldName(field, i, ".Protocol"), impp.getProtocol());
			setField(getFieldName(field, i, ".Types"), impp.getTypes());

			if (impp.getUri() != null) {
				setField(getFieldName(field, i, ".Uri.Authority"), impp.getUri().getAuthority());
				setField(getFieldName(field, i, ".Uri.Fragment"), impp.getUri().getFragment());
				setField(getFieldName(field, i, ".Uri.Host"), impp.getUri().getHost());
				setField(getFieldName(field, i, ".Uri.Path"), impp.getUri().getPath());
				setField(getFieldName(field, i, ".Uri.Port"), impp.getUri().getPort());
				setField(getFieldName(field, i, ".Uri.Query"), impp.getUri().getQuery());
				setField(getFieldName(field, i, ".Uri.RawAuthority"), impp.getUri().getRawAuthority());
				setField(getFieldName(field, i, ".Uri.RawFragment"), impp.getUri().getRawFragment());
				setField(getFieldName(field, i, ".Uri.RawPath"), impp.getUri().getRawPath());
				setField(getFieldName(field, i, ".Uri.RawQuery"), impp.getUri().getRawQuery());
				setField(getFieldName(field, i, ".Uri.RawSchemeSpecificPart"),
						impp.getUri().getRawSchemeSpecificPart());
				setField(getFieldName(field, i, ".Uri.RawUserInfo"), impp.getUri().getRawUserInfo());
				setField(getFieldName(field, i, ".Uri.Scheme"), impp.getUri().getScheme());
				setField(getFieldName(field, i, ".Uri.SchemeSpecificPart"), impp.getUri().getSchemeSpecificPart());
				setField(getFieldName(field, i, ".Uri.UserInfo"), impp.getUri().getUserInfo());
			}
		}
	}

	private void setInterests(List<Interest> interests) {
		if (CollectionUtils.isEmpty(interests)) {
			return;
		}

		final String field = "Interest";

		for (int i = 0; i < interests.size(); i++) {
			Interest interest = interests.get(i);
			setField(getFieldName(field, i, ".AltId"), interest.getAltId());
			setField(getFieldName(field, i, ".Group"), interest.getGroup());
			setField(getFieldName(field, i, ".Language"), interest.getLanguage());

			Optional<InterestLevel> level = Optional.ofNullable(interest.getLevel());
			if (level.isPresent()) {
				setField(getFieldName(field, i, ".Level"), level.get().getValue());
			}

			setField(getFieldName(field, i, ".Type"), interest.getType());
			setField(getFieldName(field, i, ""), interest.getValue());
		}
	}

	private void setKind(Kind kind) {
		if (kind == null) {
			return;
		}

		setField("Kind.Group", kind.getGroup());
		setField("Kind", kind.getValue());
	}

	private void setMailer(Mailer mailer) {
		if (mailer == null) {
			return;
		}

		setField("Mailer.Group", mailer.getGroup());
		setField("Mailer", mailer.getValue());
	}

	private void setMembers(List<Member> members) {
		if (CollectionUtils.isEmpty(members)) {
			return;
		}

		final String field = "Member";

		for (int i = 0; i < members.size(); i++) {
			Member member = members.get(i);
			setField(getFieldName(field, i, ".AltId"), member.getAltId());
			setField(getFieldName(field, i, ".Group"), member.getGroup());
			setField(getFieldName(field, i, ".MediaType"), member.getMediaType());
			setField(getFieldName(field, i, ".Uri"), member.getUri());
			setField(getFieldName(field, i, ""), member.getValue());
		}
	}

	private void setNickname(Nickname nickname) {
		if (nickname == null) {
			return;
		}

		setField("Nickname.AltId", nickname.getAltId());
		setField("Nickname.Group", nickname.getGroup());
		setField("Nickname.Language", nickname.getLanguage());
		setField("Nickname.Type", nickname.getType());
		setField("Nickname", nickname.getValues());
	}

	private void setNotes(List<Note> notes) {
		if (CollectionUtils.isEmpty(notes)) {
			return;
		}

		final String field = "Note";

		for (int i = 0; i < notes.size(); i++) {
			Note note = notes.get(i);
			setField(getFieldName(field, i, ".AltId"), note.getAltId());
			setField(getFieldName(field, i, ".Group"), note.getGroup());
			setField(getFieldName(field, i, ".Language"), note.getLanguage());
			setField(getFieldName(field, i, ".Type"), note.getType());
			setField(getFieldName(field, i, ""), note.getValue());

		}

	}

	private void setOrganisation(Organization organization) {
		if (organization == null) {
			return;
		}

		setField("Organization.AltId", organization.getAltId());
		setField("Organization.Group", organization.getGroup());
		setField("Organization.Language", organization.getLanguage());
		setField("Organization.Type", organization.getType());
		setField("Organization", organization.getValues());
	}

	private void setOrgDirectories(List<OrgDirectory> orgDirectories) {
		if (CollectionUtils.isEmpty(orgDirectories)) {
			return;
		}

		final String field = "OrgDirectories";

		for (int i = 0; i < orgDirectories.size(); i++) {
			OrgDirectory dir = orgDirectories.get(i);
			setField(getFieldName(field, i, ".AltId"), dir.getAltId());
			setField(getFieldName(field, i, ".Group"), dir.getGroup());
			setField(getFieldName(field, i, ".Language"), dir.getLanguage());
			setField(getFieldName(field, i, ".Type"), dir.getType());
			setField(getFieldName(field, i, ""), dir.getValue());
		}
	}

	private void setProfile(Profile profile) {
		if (profile == null) {
			return;
		}

		setField("Profile.Group", profile.getGroup());
		setField("Profile", profile.getValue());
	}

	private void setRoles(List<Role> roles) {
		if (CollectionUtils.isEmpty(roles)) {
			return;
		}

		final String field = "Role";

		for (int i = 0; i < roles.size(); i++) {
			Role role = roles.get(i);
			setField(getFieldName(field, i, ".AltId"), role.getAltId());
			setField(getFieldName(field, i, ".Group"), role.getGroup());
			setField(getFieldName(field, i, ".Language"), role.getLanguage());
			setField(getFieldName(field, i, ".Type"), role.getType());
			setField(getFieldName(field, i, ""), role.getValue());
		}
	}

	private void setSortString(SortString sortString) {
		if (sortString == null) {
			return;
		}

		setField("SortString.Group", sortString.getGroup());
		setField("SortString", sortString.getValue());
	}

	private void setSources(List<Source> sources) {
		if (CollectionUtils.isEmpty(sources)) {
			return;
		}

		final String field = "Source";

		for (int i = 0; i < sources.size(); i++) {
			Source source = sources.get(i);
			setField(getFieldName(field, i, ".AltId"), source.getAltId());
			setField(getFieldName(field, i, ".Group"), source.getGroup());
			setField(getFieldName(field, i, ""), source.getValue());
		}
	}

	private void setStructuredName(StructuredName structuredName) {
		if (structuredName == null) {
			return;
		}

		setField("StructuredName.AdditionalName", structuredName.getAdditionalNames());
		setField("StructuredName.AltId", structuredName.getAltId());
		setField("StructuredName.Family", structuredName.getFamily());
		setField("StructuredName.Given", structuredName.getGiven());
		setField("StructuredName.Group", structuredName.getGroup());
		setField("StructuredName.Language", structuredName.getLanguage());
		setField("StructuredName.Prefixes", structuredName.getPrefixes());
		setField("StructuredName.SortAs", structuredName.getSortAs());
		setField("StructuredName.Suffixes", structuredName.getSuffixes());
	}

	private void setTelephoneNumbers(List<Telephone> telephoneNumbers) {
		if (CollectionUtils.isEmpty(telephoneNumbers)) {
			return;
		}

		final String field = "Telephone";

		for (int i = 0; i < telephoneNumbers.size(); i++) {
			Telephone telephone = telephoneNumbers.get(i);
			setField(getFieldName(field, i, ".AltId"), telephone.getAltId());
			setField(getFieldName(field, i, ".Group"), telephone.getGroup());
			setField(getFieldName(field, i, ".Text"), telephone.getText());
			setField(getFieldName(field, i, ".Types"), telephone.getTypes());

			if (telephone.getUri() != null) {
				setField(getFieldName(field, i, ".Extension"), telephone.getUri().getExtension());
				setField(getFieldName(field, i, ".IsdbSubaddress"), telephone.getUri().getIsdnSubaddress());
				setField(getFieldName(field, i, ".Number"), telephone.getUri().getNumber());
				setField(getFieldName(field, i, ".Context"), telephone.getUri().getPhoneContext());
			}
		}
	}

	private void setTitles(List<Title> titles) {
		if (CollectionUtils.isEmpty(titles)) {
			return;
		}

		final String field = "Title";

		for (int i = 0; i < titles.size(); i++) {
			Title title = titles.get(i);
			setField(getFieldName(field, i, ".AltId"), title.getAltId());
			setField(getFieldName(field, i, ".Group"), title.getGroup());
			setField(getFieldName(field, i, ".Type"), title.getType());
			setField(getFieldName(field, i, ""), title.getValue());
		}
	}

	private void setUid(Uid uid) {
		if (uid == null) {
			return;
		}

		setField("Uid.Group", uid.getGroup());
		setField("Uid", uid.getValue());
	}

	private void setUrls(List<Url> urls) {
		if (CollectionUtils.isEmpty(urls)) {
			return;
		}

		final String field = "Url";

		for (int i = 0; i < urls.size(); i++) {
			Url url = urls.get(i);
			setField(getFieldName(field, i, ".AltId"), url.getAltId());
			setField(getFieldName(field, i, ".Group"), url.getGroup());
			setField(getFieldName(field, i, ".MediaType"), url.getMediaType());
			setField(getFieldName(field, i, ".Type"), url.getType());
			setField(getFieldName(field, i, ""), url.getValue());
		}
	}

	private void setField(String field, Object value) {
		if (value == null || value.equals("")) {
			return;
		}

		if (value instanceof List<?>) {
			map.put(field, General.convertListToString((List<?>) value));
			fields.putIfAbsent(field, FieldTypes.MEMO);
		} else if (value instanceof Date) {
			map.put(field, General.convertDateToLocalDate((Date) value));
			fields.putIfAbsent(field, FieldTypes.DATE);
		} else if (value instanceof Integer) {
			map.put(field, value);
			fields.putIfAbsent(field, FieldTypes.NUMBER);
		} else {
			map.put(field, value.toString());
			fields.putIfAbsent(field, FieldTypes.TEXT);
		}
	}

	private String getFieldName(String field, int count, String suffix) {
		return field + (count > 0 ? "[" + count + "]" + suffix : suffix);
	}
}
