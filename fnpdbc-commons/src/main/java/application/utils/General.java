package application.utils;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import application.FileType;
import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.utils.gui.XGridBagConstraints;

public final class General {
	/**
	 * Title: General Description: General UtilitiesClass Copyright (c) 2003-2020
	 *
	 * @author Tom van Breukelen
	 * @version 8+
	 */
	private static GeneralSettings mySettings = GeneralSettings.getInstance();

	public static final DateTimeFormatter sdInternalDate = DateTimeFormatter.BASIC_ISO_DATE;
	public static final DateTimeFormatter sdInternalTime = DateTimeFormatter.ofPattern("HH:mm:ss");
	public static final DateTimeFormatter sdInternalTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static final String EMPTY_STRING = "";
	public static final String SPACE = " ";
	public static final String TEXT_DELIMITER = "\"";

	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir", EMPTY_STRING);

	public static final boolean IS_MAC_OSX = System.getProperty("os.name").startsWith("Mac");
	public static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Win");
	public static final boolean IS_X64 = System.getProperty("sun.arch.data.model", "32").equals("64");

	private static final Map<String, ImageIcon> ICON_MAP = new HashMap<>();

	public static boolean isQuietMode = false;

	public static JPanel addVerticalButtons(String title, AbstractButton... buttons) {
		final int max = buttons.length;
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();
		ButtonGroup bGroup = new ButtonGroup();

		for (int i = 0; i < max; i++) {
			if (buttons[i] instanceof JRadioButton) {
				bGroup.add(buttons[i]);
			}
			result.add(buttons[i], c.gridCell(1, i, 1, 0));
		}

		result.add(Box.createVerticalGlue(), c.gridCell(0, max, 0, 1));
		if (title != null) {
			result.setBorder(BorderFactory.createTitledBorder(title));
		}
		return result;
	}

	/**
	 * Converts a database date to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertDate(LocalDate pDate) {
		return convertDate(pDate, getSimpleDateFormat());
	}

	public static String convertDate(LocalDate pDate, DateTimeFormatter format) {
		if (pDate == null) {
			return EMPTY_STRING;
		}

		return format.format(pDate);
	}

	/**
	 * Converts 'readable' date created by convertDate back to a database date
	 * format
	 *
	 * @param pDate : the date in the database table to be converted
	 */
	public static LocalDate convertDate2DB(String pDate, DateTimeFormatter format) {
		if (StringUtils.isEmpty(pDate)) {
			return null;
		}

		try {
			return LocalDate.parse(pDate, format);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Convert a duration stored in the database table to a 'readable' format
	 */
	public static String convertDuration(Duration duration) {
		if (duration == null) {
			return EMPTY_STRING;
		}

		return DurationFormatUtils.formatDuration(duration.toMillis(), mySettings.getDurationFormat());
	}

	/**
	 * Convert a duration stored in the database table to a 'readable' format
	 */
	public static String convertDuration(Integer duration) {
		if (duration == null) {
			return EMPTY_STRING;
		}

		return DurationFormatUtils.formatDuration(Duration.ofSeconds(duration).toMillis(),
				mySettings.getDurationFormat());
	}

	/**
	 * Method to convert a 'readable' duration created by convertDuration back to a
	 * database date format
	 */
	public static Duration convertDuration2DB(String pTime) {
		if (StringUtils.isEmpty(pTime)) {
			return null;
		}

		if (pTime.equals("0")) {
			return Duration.ofSeconds(0);
		}

		pTime = pTime.trim().toUpperCase();
		int hrs = 0;
		int min = 0;
		int sec = 0;

		String[] displayTime = pTime.split(":");
		int index = displayTime.length - 1;

		sec = Integer.parseInt(displayTime[index--]);
		min = Integer.parseInt(displayTime[index--]);
		if (index == 0) {
			hrs = Integer.parseInt(displayTime[0]);
		}

		return Duration.ofSeconds(hrs * 3600L + min * 60 + sec);
	}

	/**
	 * Converts a date with a missing year, month or day to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertFussyDate(String pDate) {
		return convertFussyDate(pDate, mySettings.getDateFormat());
	}

	public static String convertFussyDate(String pDate, String dateFormat) {
		boolean isNoDay = true;
		boolean isNoMonth = true;

		switch (pDate.length()) {
		case 4:
			break;
		case 6:
			if (pDate.endsWith("00")) {
				pDate = pDate.substring(0, 4);
				break;
			}
			isNoMonth = false;
			break;
		case 8:
			if (pDate.endsWith("00")) {
				if (pDate.endsWith("0000")) {
					pDate = pDate.substring(0, 4);
					break;
				}
				pDate = pDate.substring(0, 6);
				isNoMonth = false;
				break;
			}
			isNoMonth = false;
			isNoDay = false;
			break;
		default:
			return pDate;
		}

		if (isNoDay) {
			dateFormat = dateFormat.replace("d", EMPTY_STRING);
		}

		if (isNoMonth) {
			dateFormat = dateFormat.replace("M", EMPTY_STRING);
		}

		dateFormat = dateFormat.replace("  ", SPACE).trim();
		dateFormat = dateFormat.replace(SPACE, mySettings.getDateDelimiter());
		String fDate = pDate + "0101".substring(0, 8 - pDate.length());

		LocalDate date = convertDate2DB(fDate, General.sdInternalDate);
		if (date == null) {
			return pDate;
		}

		DateTimeFormatter sd = DateTimeFormatter.ofPattern(dateFormat);
		return sd.format(date);
	}

	/**
	 * Converts a 'readable' fussy date back to a database date format
	 */
	public static String convertFussyDate2DB(String pDate) {
		if (pDate == null || pDate.trim().length() == 0) {
			return EMPTY_STRING;
		}

		pDate = pDate.trim();
		String[] displayDate = pDate.split(mySettings.getDateDelimiter());
		String[] dBDate = new String[3];

		int[] dateOrder = new int[3];
		String dateFormat = mySettings.getDateFormat().replace(" ", EMPTY_STRING);
		dateFormat = dateFormat.replace("MM", "M");
		dateFormat = dateFormat.replace("yyyy", "y");
		dateFormat = dateFormat.replace("yy", "y");
		dateFormat = dateFormat.replace("dd", "d");

		dateOrder[0] = dateFormat.indexOf('d');
		dateOrder[1] = dateFormat.indexOf('M');
		dateOrder[2] = dateFormat.indexOf('y');

		final int index = displayDate.length;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < index; i++) {
			dBDate[dateOrder[i]] = displayDate[i];
		}

		for (int i = index; i < 3; i++) {
			dBDate[dateOrder[i]] = EMPTY_STRING;
		}

		result.append(dBDate[2] + dBDate[1] + dBDate[0]);
		return result.toString();
	}

	/**
	 * Converts an image in memory to a bitmap, jpeg or png file
	 */
	public static boolean convertImage(ImageIcon icon, ExportFile exp, Profiles pdaSetting, String fileName,
			boolean isScaling) throws Exception {

		if (icon == null || !exp.isImageExport()) {
			return false;
		}

		String type = fileName.substring(fileName.lastIndexOf('.') + 1);
		int scaledHeight = isScaling ? pdaSetting.getImageHeight() : 0;
		int scaledWidth = pdaSetting.getImageWidth();

		ImageIcon scaledImage = createScaledIcon(icon, scaledHeight, scaledWidth);

		File file = new File(fileName);
		file.createNewFile();

		try {
			ImageIO.write((BufferedImage) scaledImage.getImage(), type, file);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	public static ImageIcon createScaledIcon(ImageIcon icon, int scaledHeight, int scaledWidth) {
		if (scaledHeight == 0 || scaledWidth == 0) {
			// No scaling possible
			return icon;
		}

		Image image = icon.getImage();
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);

		if (imageWidth == -1 || imageHeight == -1) {
			// the image was not loaded.
			return icon;
		}

		if (imageWidth < scaledWidth && imageHeight < scaledHeight) {
			// We don't want to "blow up" the image
			return icon;
		}

		double thumbRatio = (double) scaledWidth / (double) scaledHeight;
		double imageRatio = (double) imageWidth / (double) imageHeight;

		if (thumbRatio < imageRatio) {
			scaledHeight = (int) (scaledWidth / imageRatio);
		} else {
			scaledWidth = (int) (scaledHeight * imageRatio);
		}

		BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = scaledImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
		graphics2D.dispose();

		return new ImageIcon(scaledImage);
	}

	public static List<String> convertStringToList(String dbValue, String separator) {
		return new ArrayList<>(Arrays.asList(dbValue.split(separator)));
	}

	public static List<String> convertStringToList(String dbValue) {
		return convertStringToList(dbValue, "\n");
	}

	public static String convertListToString(List<?> dbValue, final String separator) {
		if (CollectionUtils.isEmpty(dbValue)) {
			return EMPTY_STRING;
		}

		StringBuilder buf = new StringBuilder();
		dbValue.forEach(o -> buf.append(removeBrackets(o.toString())).append(separator));
		buf.delete(buf.lastIndexOf(separator), buf.length());
		return buf.toString().trim();
	}

	public static String convertListToString(List<?> dbValue) {
		return convertListToString(dbValue, "\n");
	}

	private static String removeBrackets(String text) {
		if (StringUtils.isEmpty(text) || text.length() < 2) {
			return text;
		}

		String begin = text.substring(0, 1);
		String end = text.substring(text.length() - 1);

		if ("([{".indexOf(begin) != -1 && ")]}".indexOf(end) != -1) {
			return text.substring(1, text.length() - 2);
		}

		return text;
	}

	public static String convertDoubleQuotes(String text) {
		if (!text.contains(TEXT_DELIMITER)) {
			return text;
		}

		StringBuilder result = new StringBuilder();
		char[] chars = text.toCharArray();
		for (char c : chars) {
			if (c == '"') {
				result.append("\\");
			}
			result.append(c);
		}

		return result.toString();
	}

	/**
	 * Converts a database date to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertTime(LocalTime pTime, DateTimeFormatter format) {
		if (pTime == null) {
			return EMPTY_STRING;
		}

		return format.format(pTime);
	}

	public static LocalTime convertTime2DB(String pTime, DateTimeFormatter format) {
		if (StringUtils.isEmpty(pTime)) {
			return null;
		}
		try {
			return LocalTime.parse(pTime, format);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts a database time stamp to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertTimestamp(LocalDateTime pDate) {
		return convertTimestamp(pDate, getSimpleTimestampFormat());
	}

	/**
	 * Converts a database time stamp to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertTimestamp(LocalDateTime pDate, DateTimeFormatter format) {
		if (pDate == null) {
			return EMPTY_STRING;
		}

		return format.format(pDate);
	}

	/**
	 * Converts 'readable' time stamp created by convertTimetamp back to a database
	 * date format
	 */
	public static LocalDateTime convertTimestamp2DB(String pDate, DateTimeFormatter format) {
		if (StringUtils.isEmpty(pDate)) {
			return null;
		}

		try {
			return LocalDateTime.parse(pDate, format);
		} catch (Exception e) {
			return null;
		}
	}

	public static String addLeadingZeroes(Number pIndex, int pLength) {
		final String format = "%0" + pLength + "d";
		return String.format(format, pIndex.intValue());
	}

	public static String convertTrack(Number pIndex, int pLength) {
		return addLeadingZeroes(pIndex, pLength) + ".";
	}

	public static String convertObject(Object obj, FieldTypes dbField) {
		if (obj == null || obj.equals(EMPTY_STRING)) {
			return EMPTY_STRING;
		}

		switch (dbField) {
		case DATE:
			return convertDate((LocalDate) obj);
		case FUSSY_DATE:
			return convertFussyDate(obj.toString());
		case DURATION:
			return convertDuration((Duration) obj);
		case TIME:
			return convertTime((LocalTime) obj, General.getSimpleTimeFormat());
		case TIMESTAMP:
			return convertTimestamp((LocalDateTime) obj, General.getSimpleTimestampFormat());
		case YEAR:
			int year = ((LocalDate) obj).getYear();
			return year == 0 ? EMPTY_STRING : Integer.toString(year);
		default:
			return obj.toString();
		}
	}

	public static LocalDate convertDateToLocalDate(Date dateToConvert) {
		return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public static String readFile(String path) throws IOException {
		return Files.readString(Paths.get(path));
	}

	public static byte[] convertStringToByteArray(String s1, String characterSet) {
		if (StringUtils.isEmpty(characterSet)) {
			return s1.getBytes(StandardCharsets.ISO_8859_1);
		}

		try {
			return s1.getBytes(characterSet);
		} catch (UnsupportedEncodingException e) {
			return s1.getBytes(StandardCharsets.ISO_8859_1);
		}
	}

	public static String convertByteArrayToString(byte[] b1, String characterSet) {
		if (StringUtils.isEmpty(characterSet)) {
			return new String(b1, StandardCharsets.ISO_8859_1);
		}

		try {
			return new String(b1, characterSet);
		} catch (UnsupportedEncodingException e) {
			return new String(b1, StandardCharsets.ISO_8859_1);
		}
	}

	public static boolean copyFile(String fromFile, String toFile) throws Exception {
		Path path = Paths.get(fromFile);
		if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			Path toPath = Paths.get(toFile);
			Files.copy(path, toPath, REPLACE_EXISTING);
			return true;
		}
		return false;
	}

	public static ImageIcon createImageIcon(String image) {
		ImageIcon result = ICON_MAP.get(image);
		if (result != null) {
			return result;
		}

		try {
			URL url = General.class.getResource("/images/" + image);
			result = url == null ? new ImageIcon("/images/" + image) : new ImageIcon(url);
			ICON_MAP.put(image, result);

		} catch (Exception e) {
			// Nothing to do
		}
		return result;
	}

	public static String decryptPassword(String password) {
		if (StringUtils.isEmpty(password)) {
			return EMPTY_STRING;
		}

		String encrypted = new String(Base64.getMimeDecoder().decode(password));
		return xor(encrypted.toCharArray());
	}

	public static String encryptPassword(char[] password) {
		if (password == null || password.length == 0) {
			return EMPTY_STRING;
		}

		String encryped = xor(password);
		return Base64.getMimeEncoder().encodeToString(encryped.getBytes());
	}

	public static void errorMessage(Component parent, Throwable e, String title, String mesg) {
		if (isQuietMode || e instanceof FNProgException) {
			showMessage(parent, e.getMessage(), title, true);
			return;
		}

		ErrorInfo info = new ErrorInfo(title, mesg == null ? GUIFactory.getText("internalProgramError") : mesg, null,
				title, e, Level.SEVERE, null);
		JXErrorPane.showDialog(parent, info);
	}

	public static int compareVersions(String version1, String version2) {
		String[] version1Splits = version1.split("\\.");
		String[] version2Splits = version2.split("\\.");
		int maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length);

		for (int i = 0; i < maxLengthOfVersionSplits; i++) {
			Integer v1 = i < version1Splits.length ? Integer.parseInt(version1Splits[i]) : 0;
			Integer v2 = i < version2Splits.length ? Integer.parseInt(version2Splits[i]) : 0;
			int compare = v1.compareTo(v2);
			if (compare != 0) {
				return compare;
			}
		}
		return 0;
	}

	/**
	 * Executes another program within the current one
	 *
	 * @param pCmd array containing the program file and (optional) command line
	 *             parameters
	 * @throws an exception when the program returned any output message
	 */
	public static void executeProgram(String[] pCmd) throws FNProgException {
		boolean isError = false;
		String s = null;
		StringBuilder errors = new StringBuilder();

		try {
			Process p = Runtime.getRuntime().exec(pCmd);

			// Try to catch the output of the running program
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null) {
				isError = true;
				errors.append("\n" + s);
			}
			p.waitFor();
		} catch (Exception e) {
			isError = true;
			errors.append("\n" + e.getMessage());
		}

		if (isError) {
			throw FNProgException.getException("programExecError", pCmd[0], errors.toString());
		}
	}

	public static boolean existFile(String filename) {
		File f = new File(filename);
		return f.exists();
	}

	public static String getBaseName(String dbFile, ExportFile exp) {
		int index = dbFile.lastIndexOf('.');
		String extension = exp.getFileExtention().get(0);

		if (index == -1) {
			return dbFile + extension;
		}

		return dbFile.substring(0, index) + extension;
	}

	public static long getChecksum(byte[] bytes) {
		Checksum checksumEngine = new CRC32();
		checksumEngine.update(bytes, 0, bytes.length);
		return checksumEngine.getValue();
	}

	public static String getDefaultPDADatabase(ExportFile db) {
		return mySettings.getDefaultFileFolder() + File.separator + db.getName() + db.getFileExtention().get(0);
	}

	public static int getID(String pNameID, String[] pIds) {
		for (int i = 0; i < pIds.length; i++) {
			if (pIds[i].equalsIgnoreCase(pNameID)) {
				return i;
			}
		}
		return 0;
	}

	public static Reader getInputStreamReader(String file) {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(file);
		return new InputStreamReader(is, StandardCharsets.UTF_8);
	}

	public static Properties getLanguages(String nodeName) {
		return getPropertyFile(nodeName, true);
	}

	public static Map<String, String> getLookAndFeels() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("System", UIManager.getSystemLookAndFeelClassName());

		for (UIManager.LookAndFeelInfo result : UIManager.getInstalledLookAndFeels()) {
			map.putIfAbsent(result.getName(), result.getClassName());
		}

		return map;
	}

	public static String ordinal(int i) {
		int mod100 = i % 100;
		int mod10 = i % 10;
		if (mod10 == 1 && mod100 != 11) {
			return i + "st";
		} else if (mod10 == 2 && mod100 != 12) {
			return i + "nd";
		} else if (mod10 == 3 && mod100 != 13) {
			return i + "rd";
		} else {
			return i + "th";
		}
	}

	/**
	 * Converts a "normal" string to a byte array containing a fix length null
	 * terminated string
	 *
	 * @see splitNullTerminatedString
	 */
	public static byte[] getNullTerminatedString(String field, String characterSet, int length) {
		if (StringUtils.isEmpty(field)) {
			return new byte[length];
		}

		if (length == 0) {
			length = field.length() + 1;
		}

		final int MAX_FIELD_LEN = length - 1; // Last char must be a null
		if (field.length() > MAX_FIELD_LEN) {
			field = field.substring(0, MAX_FIELD_LEN);
		}

		final int FIELD_LENGTH = field.length();
		byte[] result = new byte[length];
		System.arraycopy(convertStringToByteArray(field, characterSet), 0, result, 0, FIELD_LENGTH);
		return result;
	}

	public static Properties getProperties(String nodeName) {
		return getPropertyFile(nodeName, false);
	}

	private static Properties getPropertyFile(String nodeName, boolean isLanguage) {
		String config = isLanguage ? ".lang" : ".config";
		String file = (isLanguage ? "languages/" : "config/") + nodeName + config;
		Properties result = new Properties();

		try (BufferedReader reader = new BufferedReader(getInputStreamReader(file))) {
			result.load(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public static void getSelectedFile(JDialog dialog, JTextField component, ExportFile file, String dir,
			boolean isMustExist) {
		getSelectedFile(dialog, component, dir, file.getFileType(), isMustExist, file.getFileExtention());
		if (!isMustExist) {
			String filename = component.getText();
			if (!filename.isEmpty() && !isFileExtensionOk(filename, file)) {
				component.setText(filename + file.getFileExtention().get(0));
			}
		}
	}

	public static void getSelectedFile(JDialog dialog, JTextField component, FileType type, boolean isMustExist) {
		getSelectedFile(dialog, component, EMPTY_STRING, type.getType(), isMustExist, type.getExtention());
	}

	public static void getSelectedFile(JDialog dialog, JTextField component, String dir, String fileType,
			boolean isMustExist, List<String> fileExt) {
		GenericFileFilter filter = new GenericFileFilter(fileExt, fileType);
		String location = component.getText();

		if (location.isEmpty()) {
			location = dir.isEmpty() ? mySettings.getDefaultFileFolder() : dir;
		} else {
			int index = location.lastIndexOf(File.separator);
			location = index > -1 ? location.substring(0, index) : location;
		}

		if (IS_WINDOWS) {
			JFileChooser fc = new JFileChooser(location);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.addChoosableFileFilter(filter);
			fc.setAcceptAllFileFilterUsed(false);

			if (fc.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
				if (isMustExist) {
					if (existFile(fc.getSelectedFile().getAbsolutePath())) {
						component.setText(fc.getSelectedFile().getAbsolutePath());
					}
				} else {
					component.setText(fc.getSelectedFile().getAbsolutePath());
				}
			}
			return;
		}

		FileDialog fd = new FileDialog(dialog, fileType, isMustExist ? FileDialog.LOAD : FileDialog.SAVE);

		fd.setDirectory(location);
		fd.setFilenameFilter(filter);
		fd.setLocationRelativeTo(dialog);
		fd.setVisible(true);

		if (fd.getFile() != null) {
			if (isMustExist) {
				if (existFile(fd.getDirectory() + fd.getFile())) {
					component.setText(fd.getDirectory() + fd.getFile());
				}
			} else {
				component.setText(fd.getDirectory() + fd.getFile());
			}
		}
	}

	public static void getSelectedFolder(JDialog dialog, JTextField component, String title) {
		String location = component.getText();
		if (location.isEmpty()) {
			location = mySettings.getDefaultFileFolder();
		}

		if (IS_MAC_OSX) {
			FileDialog fd = new FileDialog(dialog, title, FileDialog.LOAD);
			fd.setDirectory(location);
			fd.setLocationRelativeTo(dialog);
			fd.setVisible(true);

			if (fd.getFile() != null) {
				component.setText(fd.getDirectory() + fd.getFile());
			}
			return;
		}

		JFileChooser fc = new JFileChooser(location);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (fc.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
			component.setText(fc.getSelectedFile().getAbsolutePath());
		}
	}

	/**
	 * Returns the SimpleDateFormat used to convert a date in a 'readable' format
	 */
	public static DateTimeFormatter getSimpleDateFormat() {
		return DateTimeFormatter.ofPattern(getDateFormat());
	}

	/**
	 * Returns the SimpleDateFormat used to convert a time in a 'readable' format
	 */
	public static DateTimeFormatter getSimpleTimeFormat() {
		return DateTimeFormatter.ofPattern(getTimeFormat());
	}

	/**
	 * Returns the SimpleDateFormat used to convert a time stamp in a 'readable'
	 * format
	 */
	public static DateTimeFormatter getSimpleTimestampFormat() {
		return DateTimeFormatter.ofPattern(getTimestampFormat());
	}

	public static String getSoftwareTypeVersion(String software, String softwareVersion) {
		StringBuilder result = new StringBuilder(software);
		if (!softwareVersion.isEmpty()) {
			result.append(" (vs ");
			result.append(softwareVersion);
			result.append(")");
		}
		return result.toString();
	}

	/**
	 * Returns the Date Format used to convert a date in a 'readable' format
	 */
	public static String getDateFormat() {
		return mySettings.getDateFormat().replace(SPACE, mySettings.getDateDelimiter());
	}

	/**
	 * Returns the Time Format used to convert a time in a 'readable' format
	 */
	public static String getTimeFormat() {
		return mySettings.getTimeFormat().replace("am/pm", "aa");
	}

	/**
	 * Returns the Timestamp Format used to convert a timestamp in a 'readable'
	 * format
	 */
	public static String getTimestampFormat() {
		return getDateFormat() + SPACE + getTimeFormat();
	}

	public static void gotoWebsite(String webpage) throws FNProgException {
		Desktop desktop = null;
		if (Desktop.isDesktopSupported()) {
			desktop = Desktop.getDesktop();
			if (!desktop.isSupported(Desktop.Action.BROWSE)) {
				throw FNProgException.getException("websiteError", webpage,
						"Unable to find an Internet browser on this computer");
			}
		} else {
			throw FNProgException.getException("websiteError", webpage,
					"This function is not supported on this platform");
		}

		try {
			URI uri = new URI(webpage);
			desktop.browse(uri);
		} catch (Exception e) {
			throw FNProgException.getException("websiteError", webpage, e.getMessage());
		}
	}

	public static boolean isFileExtensionOk(String dbFile, ExportFile exp) {
		if (exp != ExportFile.FIREBIRD && exp.isConnectHost()) {
			return !dbFile.contains(".");
		}

		int index = dbFile.lastIndexOf('.');
		String ext = "csv";
		if (index != -1) {
			ext = dbFile.substring(dbFile.lastIndexOf('.')).toLowerCase();
		}

		for (String element : exp.getFileExtention()) {
			if (element.equals(".*") || ext.equals(element)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isNumerical(String str) {
		if (StringUtils.isEmpty(str)) {
			return false;
		}

		return str.matches("-?\\d+(\\.\\d+)?");
	}

	/**
	 * Sets the preferred width of the visible columns and cells in a JTable.
	 */
	public static void packTable(JTable table) {
		DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
		int maxCols = colModel.getColumnCount();
		table.getRowCount();

		int[] colWidth = new int[maxCols]; // Max column widths

		// Get width of column headers
		for (int column = 0; column < maxCols; column++) {
			TableColumn col = colModel.getColumn(column);

			TableCellRenderer renderer = col.getHeaderRenderer();
			if (renderer == null) {
				renderer = table.getTableHeader().getDefaultRenderer();
			}

			Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
			colWidth[column] = comp.getPreferredSize().width;
		}

		// Set row height
		for (int row = 0; row < table.getRowCount(); row++) {
			int rowHeight = table.getRowHeight();

			for (int column = 0; column < maxCols; column++) {
				Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
				rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
				colWidth[column] = Math.max(colWidth[column], comp.getPreferredSize().width);
			}

			table.setRowHeight(row, rowHeight);
		}

		// Set column width
		for (int column = 0; column < maxCols; column++) {
			TableColumn col = colModel.getColumn(column);
			col.setPreferredWidth(Math.min(500, colWidth[column]));
		}
	}

	public static Object readObjectFromDisk() {
		Object result = null;
		// Read from disk using FileInputStream
		try (FileInputStream fIn = new FileInputStream(TEMP_DIR + File.separator + "myobject.data");
				ObjectInputStream oIn = new ObjectInputStream(fIn)) {
			result = oIn.readObject();
		} catch (Exception e) {
			return null;
		}
		return result;
	}

	private static Object setDialogText(String text) {
		String mesg = setDialogText(text, 120);

		if (mesg.length() > 254) {
			// create a JTextArea
			JTextArea textArea = new JTextArea(20, 25);
			textArea.setText(mesg);
			textArea.setEditable(false);

			// wrap a scrollpane around it
			JScrollPane scrollPane = new JScrollPane(textArea);
			textArea.setCaretPosition(0);
			return scrollPane;
		}

		return mesg;
	}

	public static String setDialogText(String text, int maxLen) {
		if (text == null) {
			return EMPTY_STRING;
		}

		String lineFeed = text.startsWith("<html>") ? "<br>" : "\n";

		StringBuilder mesg = new StringBuilder(text);
		String s = null;
		if (text.length() > maxLen) {
			mesg = new StringBuilder();
			int start = 0;
			int end = text.indexOf(' ', maxLen);
			int index = 0;

			while (end != -1) {
				s = text.substring(start, ++end);
				index = s.indexOf(lineFeed);
				if (index == -1) {
					mesg.append(s).append(lineFeed);
				} else {
					mesg.append(s.substring(0, ++index));
					end -= s.length() - index;
				}

				start = end;
				end = text.indexOf(' ', end + maxLen);
			}
			mesg.append(text.substring(start, text.length()));
		}
		return mesg.toString().trim();
	}

	public static void setEnabled(Component component, boolean enable) {
		if (component == null || !component.isVisible()) {
			return;
		}

		component.setEnabled(enable);
		component.setCursor(Cursor.getPredefinedCursor(enable ? Cursor.DEFAULT_CURSOR : Cursor.WAIT_CURSOR));

		// Check for instance of container.
		if (component instanceof Container container) {
			// Continue setEnabled if there are more components.
			Component[] components = container.getComponents();
			for (Component element : components) {
				if (element != null) {
					setEnabled(element, enable); // <- Here is the recursive
													// call.
				}
			}
		}
	}

	public static void setQuietMode() {
		isQuietMode = true;
	}

	public static boolean showConfirmMessage(Component parent, String mesg, String title) {
		return JOptionPane.showConfirmDialog(parent, setDialogText(mesg), title,
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

	public static void showMessage(Component parent, String mesg, String title, boolean isError) {
		if (isQuietMode) {
			if (mesg == null) {
				mesg = GUIFactory.getText("internalProgramError");
			}
			System.out.println(title + ": " + mesg);
			return;
		}
		JOptionPane.showMessageDialog(parent, setDialogText(mesg), title,
				isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
	}

	public static boolean writeObjectToDisk(Object myObject) {
		// Write to disk with FileOutputStream
		try (FileOutputStream fOut = new FileOutputStream(TEMP_DIR + File.separator + "myobject.data");
				ObjectOutputStream oOut = new ObjectOutputStream(fOut)) {
			oOut.writeObject(myObject);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static String xor(char[] password) {
		String keyPhrase = "PasswordProtected";
		char[] key = keyPhrase.toCharArray();
		int textLength = password.length;
		int keyLength = key.length;

		// encryption
		char[] result = new char[textLength];
		for (int i = 0; i < textLength; i++) {
			result[i] = (char) (password[i] ^ key[i % keyLength]);
		}
		return new String(result);
	}

	private General() {
		// Hidden constructor
	}
}