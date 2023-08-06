package application.preferences;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

import application.utils.General;

/**
 * @author van_breukelen
 *
 */
public final class GeneralSettings {
	private final String userHome = System.getProperty("user.home", "");

	private String checkBoxChecked;
	private String checkBoxUnchecked;
	private String dateDelimiter;
	private String dateFormat;
	private String dbcVersion;
	private String defaultBackupFolder;
	private String defaultFileFolder;
	private String defaultImageFolder;
	private String defaultPdaFolder;
	private String durationFormat;
	private String fnpVersion;
	private String handbaseConversionProgram;
	private String language;
	private String lookAndFeel;
	private String timeFormat;
	private int versionDaysCheck;
	private String checkVersionDate;

	private boolean isIncrementalExport;
	private boolean isNewExport;
	private boolean isNoFilterExport;
	private boolean isNoImagePath;

	private int width;
	private int height;
	private int helpWidth;
	private int helpHeight;

	private static final GeneralSettings gInstance = new GeneralSettings(getCallerApp());
	private Preferences myPref;

	private GeneralSettings(String software) {
		myPref = Preferences.userRoot().node(software);
		myPref = myPref.node("general_settings");

		checkBoxChecked = myPref.get("checkbox.checked", "Yes");
		checkBoxUnchecked = myPref.get("checkbox.unchecked", "");
		dateDelimiter = myPref.get("date.delimiter", "/");
		dateFormat = myPref.get("date.format", "dd MM yyyy");
		dbcVersion = myPref.get("dbconvert.version", "");
		defaultBackupFolder = myPref.get("default.backup.folder", userHome);
		defaultFileFolder = myPref.get("default.file.folder", userHome);
		defaultImageFolder = myPref.get("default.image.folder", userHome);
		defaultPdaFolder = myPref.get("default.pda.folder", userHome);
		durationFormat = myPref.get("duration.format", "H:mm:ss");
		fnpVersion = myPref.get("fnprog2pda.version", "");
		handbaseConversionProgram = myPref.get("handbase.conversion.program", "");
		language = myPref.get("language", "English");
		lookAndFeel = myPref.get("gui.lookandfeel", "Nimbus");
		timeFormat = myPref.get("time.format", "hh:mm");

		height = myPref.getInt("frame.height", 500);
		width = myPref.getInt("frame.width", 850);
		helpHeight = myPref.getInt("help.height", 500);
		helpWidth = myPref.getInt("help.width", 700);

		isIncrementalExport = myPref.getBoolean("incremental.export", false);
		isNewExport = myPref.getBoolean("new.export", false);
		isNoFilterExport = myPref.getBoolean("filter.export", false);
		isNoImagePath = myPref.getBoolean("noImagePath.check", false);

		versionDaysCheck = myPref.getInt("version.days.check", 30);
		checkVersionDate = myPref.get("check.version.date", "");
	}

	public static GeneralSettings getInstance() {
		return gInstance;
	}

	public String getKeyValue(String key) {
		return myPref.get(key, "");
	}

	public void removeKey(String key) {
		myPref.remove(key);
	}

	public String getCheckBoxChecked() {
		return checkBoxChecked;
	}

	public void setCheckBoxChecked(String checkBoxChecked) {
		PrefUtils.writePref(myPref, "checkbox.checked", checkBoxChecked, this.checkBoxChecked, "Yes");
		this.checkBoxChecked = checkBoxChecked;
	}

	public String getCheckBoxUnchecked() {
		return checkBoxUnchecked;
	}

	public void setCheckBoxUnchecked(String checkBoxUnchecked) {
		PrefUtils.writePref(myPref, "checkbox.unchecked", checkBoxUnchecked, this.checkBoxUnchecked, "");
		this.checkBoxUnchecked = checkBoxUnchecked;
	}

	public String getDateDelimiter() {
		return dateDelimiter;
	}

	public void setDateDelimiter(String dateDelimiter) {
		PrefUtils.writePref(myPref, "date.delimiter", dateDelimiter, this.dateDelimiter, "/");
		this.dateDelimiter = dateDelimiter;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		PrefUtils.writePref(myPref, "date.format", dateFormat, this.dateFormat, "dd MM yyyy");
		this.dateFormat = dateFormat;
	}

	public String getDefaultBackupFolder() {
		return defaultBackupFolder;
	}

	public void setDefaultBackupFolder(String defaultBackupFolder) {
		PrefUtils.writePref(myPref, "default.backup.folder", defaultBackupFolder, this.defaultBackupFolder, userHome);
		this.defaultBackupFolder = defaultBackupFolder;
	}

	public String getDefaultFileFolder() {
		return defaultFileFolder;
	}

	public void setDefaultFileFolder(String defaultFileFolder) {
		PrefUtils.writePref(myPref, "default.file.folder", defaultFileFolder, this.defaultFileFolder, userHome);
		this.defaultFileFolder = defaultFileFolder;
	}

	public String getDefaultImageFolder() {
		return defaultImageFolder;
	}

	public void setDefaultImageFolder(String defaultImageFolder) {
		PrefUtils.writePref(myPref, "default.image.folder", defaultImageFolder, this.defaultImageFolder, userHome);
		this.defaultImageFolder = defaultImageFolder;
	}

	public String getDefaultPdaFolder() {
		return defaultPdaFolder;
	}

	public void setDefaultPdaFolder(String defaultPdaFolder) {
		PrefUtils.writePref(myPref, "default.pda.folder", defaultPdaFolder, this.defaultPdaFolder, "");
		this.defaultPdaFolder = defaultPdaFolder;
	}

	public String getDurationFormat() {
		return durationFormat;
	}

	public void setDurationFormat(String durationFormat) {
		PrefUtils.writePref(myPref, "duration.format", durationFormat, this.durationFormat, "H:mm:ss");
		this.durationFormat = durationFormat;
	}

	public String getHandbaseConversionProgram() {
		return handbaseConversionProgram;
	}

	public void setHandbaseConversionProgram(String handbaseConversionProgram) {
		PrefUtils.writePref(myPref, "handbase.conversion.program", handbaseConversionProgram,
				this.handbaseConversionProgram, "");
		this.handbaseConversionProgram = handbaseConversionProgram;
	}

	public boolean isNoFilterExport() {
		return isNoFilterExport;
	}

	public void setNoFilterExport(boolean isNoFilterExport) {
		PrefUtils.writePref(myPref, "filter.export", isNoFilterExport, this.isNoFilterExport, false);
		this.isNoFilterExport = isNoFilterExport;
	}

	public boolean isIncrementalExport() {
		return isIncrementalExport;
	}

	public void setIncrementalExport(boolean isIncrementalExport) {
		PrefUtils.writePref(myPref, "incremental.export", isIncrementalExport, this.isIncrementalExport, false);
		this.isIncrementalExport = isIncrementalExport;
	}

	public boolean isNewExport() {
		return isNewExport;
	}

	public void setNewExport(boolean isNewExport) {
		PrefUtils.writePref(myPref, "new.export", isNewExport, this.isNewExport, false);
		this.isNewExport = isNewExport;
	}

	public boolean isNoVersionCheck() {
		return versionDaysCheck == 0;
	}

	public boolean isNoImagePath() {
		return isNoImagePath;
	}

	public void setNoImagePath(boolean isNoImagePath) {
		PrefUtils.writePref(myPref, "noImagePath.check", isNoImagePath, this.isNoImagePath, false);
		this.isNoImagePath = isNoImagePath;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		PrefUtils.writePref(myPref, "language", language, this.language, "English");
		this.language = language;
	}

	public String getLookAndFeel() {
		return lookAndFeel;
	}

	public void setLookAndFeel(String lookAndFeel) {
		PrefUtils.writePref(myPref, "gui.lookandfeel", lookAndFeel, this.lookAndFeel, "Nimbus");
		this.lookAndFeel = lookAndFeel;
	}

	public String getTimeFormat() {
		return timeFormat;
	}

	public void setTimeFormat(String timeFormat) {
		PrefUtils.writePref(myPref, "time.format", timeFormat, this.timeFormat, "hh:mm");
		this.timeFormat = timeFormat;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		PrefUtils.writePref(myPref, "frame.width", width, this.width, 850);
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		PrefUtils.writePref(myPref, "frame.height", height, this.height, 500);
		this.height = height;
	}

	public int getHelpWidth() {
		return helpWidth;
	}

	public void setHelpWidth(int helpWidth) {
		PrefUtils.writePref(myPref, "help.width", helpWidth, this.helpWidth, 700);
		this.helpWidth = helpWidth;
	}

	public int getHelpHeight() {
		return helpHeight;
	}

	public void setHelpHeight(int helpHeight) {
		PrefUtils.writePref(myPref, "help.height", helpHeight, this.helpHeight, 500);
		this.helpHeight = helpHeight;
	}

	public int getVersionDaysCheck() {
		return versionDaysCheck;
	}

	public void setVersionDaysCheck(int versionDaysCheck) {
		PrefUtils.writePref(myPref, "version.days.check", versionDaysCheck, this.versionDaysCheck, 10);
		this.versionDaysCheck = versionDaysCheck;
	}

	public LocalDate getCheckVersionDate() {
		LocalDate result;
		try {
			result = LocalDate.parse(checkVersionDate, DateTimeFormatter.ISO_DATE);
		} catch (Exception e) {
			result = LocalDate.now();
		}

		return result;
	}

	public void setCheckVersionDate() {
		if (versionDaysCheck > 0) {
			LocalDate futureCheckVersionDate = LocalDate.now().plusDays(versionDaysCheck);
			setCheckVersionDate(futureCheckVersionDate.format(DateTimeFormatter.ISO_DATE));
		}
	}

	public void setCheckVersionDate(String checkVersionDate) {
		PrefUtils.writePref(myPref, "check.version.date", checkVersionDate, this.checkVersionDate, "");
		this.checkVersionDate = checkVersionDate;
	}

	public long getGeneralCRC32Checksum() {
		StringBuilder buf = new StringBuilder(50);
		buf.append(checkBoxChecked);
		buf.append(checkBoxUnchecked);
		buf.append(dateDelimiter);
		buf.append(dateFormat);
		buf.append(durationFormat);
		buf.append(isIncrementalExport);
		buf.append(isNoFilterExport);
		buf.append(isNoImagePath);
		buf.append(timeFormat);
		buf.append(width);
		buf.append(height);
		buf.append(helpWidth);
		buf.append(helpHeight);
		buf.append(versionDaysCheck);
		return General.getChecksum(buf.toString().getBytes());
	}

	public String getDbcVersion() {
		return dbcVersion;
	}

	public void setDbcVersion(String version) {
		PrefUtils.writePref(myPref, "dbconvert.version", version, dbcVersion, "");
		fnpVersion = version;
	}

	public String getFnpVersion() {
		return fnpVersion;
	}

	public void setFnpVersion(String version) {
		PrefUtils.writePref(myPref, "fnprog2pda.version", version, fnpVersion, "");
		fnpVersion = version;
	}

	public static String getCallerApp() {
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		StackTraceElement ste = stElements[stElements.length - 1];
		String className = ste.getClassName();
		return className.substring(0, className.indexOf("."));
	}
}
