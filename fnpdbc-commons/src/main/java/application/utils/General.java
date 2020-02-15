package application.utils;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.ini4j.Ini;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.painter.MattePainter;

import application.interfaces.ExportFile;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.table.BooleanRenderer;
import application.utils.gui.XGridBagConstraints;

public final class General {
	/**
	 * Title: General Description: General UtilitiesClass
	 * Copyright (c) 2003-2020
	 *
	 * @author Tom van Breukelen
	 * @version 8+
	 */
	private static GeneralSettings mySettings = GeneralSettings.getInstance();

	public static final DateTimeFormatter sdInternalDate = DateTimeFormatter.BASIC_ISO_DATE;
	public static final DateTimeFormatter sdInternalTime = DateTimeFormatter.ofPattern("HH:mm:ss");
	public static final DateTimeFormatter sdInternalTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String tempDir = System.getProperty("java.io.tmpdir", "");

	private static MattePainter painter;

	public static final boolean IS_MAC_OSX = System.getProperty("os.name").equals("Mac OS X");
	public static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Win");
	public static final boolean IS_X64 = System.getProperty("sun.arch.data.model", "32").equals("64");
	public static final boolean IS_GTK = UIManager.getLookAndFeel().getClass().getName().contains("GTK");

	public static boolean isQuietMode = false;

	private General() {
		// Hidden constructor
	}

	public static int getID(String pNameID, String[] pIds) {
		for (int i = 0; i < pIds.length; i++) {
			if (pIds[i].equalsIgnoreCase(pNameID)) {
				return i;
			}
		}
		return 0;
	}

	public static void setQuietMode() {
		isQuietMode = true;
	}

	/**
	 * Converts a "normal" string to a byte array containing a fix length null
	 * terminated string
	 *
	 * @see splitNullTerminatedString
	 */
	public static byte[] getNullTerminatedString(String field, int length, String encoding) {
		if (field == null || field.isEmpty()) {
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
		System.arraycopy(convertString2Bytes(field, encoding), 0, result, 0, FIELD_LENGTH);
		return result;
	}

	/**
	 * Convert a byte array containing null terminated Strings to a ArrayList of
	 * strings
	 *
	 * @see getNullTerminatedString
	 */
	public static List<String> splitNullTerminatedString(byte[] fields, int fieldLen) throws Exception {
		List<String> result = null;
		if (fields == null || fields.length == 0) {
			return result;
		}

		final int max = fields.length / fieldLen;
		ByteArrayInputStream bytes = new ByteArrayInputStream(fields);
		byte[] field = new byte[fieldLen];

		result = new ArrayList<>(max);
		String s = null;

		for (int i = 0; i < max; i++) {
			bytes.read(field);
			if (field[0] == 0) {
				break;
			}

			s = new String(field);
			int index = s.indexOf("\0");
			result.add(index != -1 ? s.substring(0, index) : s);
		}

		return result;
	}

	public static void setEnabled(Component component, boolean enable) {
		if (component == null) {
			return;
		}

		component.setEnabled(enable);
		if (enable) {
			component.setCursor(null);
		}

		// Check for instance of container.
		if (component instanceof Container) {
			// Continue setEnabled if there are more components.
			Component components[] = ((Container) component).getComponents();
			for (Component element : components) {
				if (element != null) {
					setEnabled(element, enable); // <- Here is the recursive
													// call.
				}
			}
		}
	}

	/**
	 * Sets the preferred width of the visible columns in a JTable. The columns will
	 * be just wide enough to show the column head and the widest cell in the
	 * column. Margin pixels are added to the left and right (resulting in an
	 * additional width of 4 pixels).
	 */
	public static void packColumns(JTable table) {
		final int MAXWIDTH = 300;
		DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
		int maxCols = colModel.getColumnCount();
		int maxRows = table.getRowCount();

		for (int vColIndex = 0; vColIndex < maxCols; vColIndex++) {
			TableColumn col = colModel.getColumn(vColIndex);
			int width = 0;

			// Get width of column header
			TableCellRenderer renderer = col.getHeaderRenderer();
			if (renderer == null) {
				renderer = table.getTableHeader().getDefaultRenderer();
			}

			Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
			width = comp.getPreferredSize().width;

			if (maxRows > 0) {
				renderer = table.getCellRenderer(0, vColIndex);
				if (renderer instanceof BooleanRenderer) {
					width = Math.max(width, 25);
				} else {
					// Get maximum width of column data
					for (int r = 0; r < table.getRowCount(); r++) {
						try {
							comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false,
									false, r, vColIndex);
							width = Math.max(width, comp.getPreferredSize().width);
							if (width > MAXWIDTH) {
								break;
							}
						} catch (Exception e) {
							// Row contains an invalid object
						}
					}
				}
			}

			// Add margin
			width += 4;

			if (width > MAXWIDTH) {
				width = MAXWIDTH;
			}

			// Set the width
			col.setPreferredWidth(width);
		}
	}

	public static byte[] convertString2Bytes(String s, String encoding) {
		if (encoding.isEmpty()) {
			return s.getBytes();
		}

		try {
			return s.getBytes(encoding);
		} catch (Exception e) {
			// Encoding is not supported
			return s.getBytes();
		}
	}

	public static String convertBytes2String(byte[] b, String encoding) {
		if (encoding.isEmpty()) {
			return new String(b);
		}

		try {
			return new String(b, encoding);
		} catch (Exception e) {
			return new String(b);
		}
	}

	public static long getChecksum(byte[] bytes) {
		Checksum checksumEngine = new CRC32();
		checksumEngine.update(bytes, 0, bytes.length);
		return checksumEngine.getValue();
	}

	public static String encryptPassword(char[] password) {
		if (password == null || password.length == 0) {
			return "";
		}

		String encryped = xor(password);
		return Base64.getMimeEncoder().encodeToString(encryped.getBytes());
	}

	public static String decryptPassword(String password) {
		if (password == null || password.isEmpty()) {
			return "";
		}

		String encrypted = new String(Base64.getMimeDecoder().decode(password));
		return xor(encrypted.toCharArray());
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
		} catch (Throwable e) {
			isError = true;
			errors.append("\n" + e.getMessage());
		}

		if (isError) {
			throw FNProgException.getException("programExecError", pCmd[0], errors.toString());
		}
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

	public static Map<String, String> getLookAndFeels() {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put("Java", UIManager.getCrossPlatformLookAndFeelClassName());
		map.put("System", UIManager.getSystemLookAndFeelClassName());

		for (UIManager.LookAndFeelInfo result : UIManager.getInstalledLookAndFeels()) {
			if (!map.containsValue(result.getClassName())) {
				map.put(result.getName(), result.getClassName());
			}
		}

		return map;
	}

	public static Properties getProperties(String nodeName) {
		return getPropertyFile(nodeName, false);
	}

	public static Properties getLanguages(String nodeName) {
		return getPropertyFile(nodeName, true);
	}

	public static Ini getIniFile(String file) {
		Ini result;
		try (BufferedReader reader = new BufferedReader(getInputStreamReader(file))) {
			result = new Ini(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
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

	private static Reader getInputStreamReader(String file) {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(file);
		return new InputStreamReader(is, StandardCharsets.UTF_8);
	}

	public static List<String> getCharacterSets() {
		SortedMap<String, Charset> charSets = Charset.availableCharsets();
		List<String> result = new ArrayList<>(100);
		result.add(" ");
		charSets.keySet().forEach(result::add);
		return result;
	}

	public static ImageIcon createImageIcon(String image) {
		try {
			URL url = image.getClass().getResource("/images/" + image);
			if (url == null) {
				return new ImageIcon("images/" + image);
			}
			return new ImageIcon(url);
		} catch (Exception e) {
			//
		}
		return null;
	}

	public static JButton createToolBarButton(String toolTip, String iconFile, ActionListener action) {
		JButton result = new JButton();
		result.addActionListener(action);
		result.setToolTipText(toolTip);
		result.setIcon(createImageIcon(iconFile));
		result.setMargin(new Insets(2, 2, 2, 2));
		return result;
	}

	/**
	 * Converts an image in memory to a bitmap or jpeg file
	 */
	public static boolean convertImage(BufferedImage image, ExportFile exp, Profiles pdaSetting, String fileName,
			boolean isScaling) throws Exception {
		if (image == null || !exp.isImageExport()) {
			return false;
		}

		String type = fileName.substring(fileName.lastIndexOf(".") + 1);
		int scaledWidth = pdaSetting.getImageWidth();
		int scaledHeight = pdaSetting.getImageHeight();

		if (!isScaling && scaledHeight == 0) {
			isScaling = false;
		}

		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);

		if (imageWidth == -1 || imageHeight == -1) {
			// the image was not loaded.
			return false;
		}

		if (isScaling && imageWidth < scaledWidth && imageHeight < scaledHeight) {
			// We'll not blow up the image
			isScaling = false;
		}

		BufferedImage scaledImage = image;
		if (isScaling) {
			double thumbRatio = (double) scaledWidth / (double) scaledHeight;
			double imageRatio = (double) imageWidth / (double) imageHeight;

			if (thumbRatio < imageRatio) {
				scaledHeight = (int) (scaledWidth / imageRatio);
			} else {
				scaledWidth = (int) (scaledHeight * imageRatio);
			}

			scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = scaledImage.createGraphics();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
			graphics2D.dispose();
		}

		File file = new File(fileName);
		file.createNewFile();

		try {
			ImageIO.write(scaledImage, type, file);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the DateFormat used to convert a date in a 'readable' format
	 */
	public static String getDateFormat() {
		return mySettings.getDateFormat().replaceAll(" ", mySettings.getDateDelimiter());
	}

	/**
	 * Returns the SimpleDateFormat used to convert a date in a 'readable' format
	 */
	public static DateTimeFormatter getSimpleDateFormat() {
		return DateTimeFormatter.ofPattern(getDateFormat());
	}

	/**
	 * Returns the SimpleDateFormat used to convert a time stamp in a 'readable'
	 * format
	 */
	public static DateTimeFormatter getSimpleTimestampFormat() {
		return DateTimeFormatter.ofPattern(getDateFormat() + " HH:mm:ss");
	}

	/**
	 * Returns the TimeFormat used to convert a time in a 'readable' format
	 */
	public static String getTimeFormat() {
		return mySettings.getTimeFormat();
	}

	/**
	 * Returns the SimpleDateFormat used to convert a time in a 'readable' format
	 */
	public static DateTimeFormatter getSimpleTimeFormat() {
		return DateTimeFormatter.ofPattern(getTimeFormat().replaceAll("am/pm", "aa"));
	}

	public static Date convertLocalDateToDate(LocalDate date) {
		return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts a database date to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertDate(String pDate) {
		LocalDate date = convertDB2Date(pDate);
		if (date == null) {
			return "";
		}

		return getSimpleDateFormat().format(date);
	}

	/**
	 * Converts a database time stamp to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertTimestamp(String pDate) {
		LocalDateTime date = convertDB2Timestamp(pDate);
		if (date == null) {
			return "";
		}

		return getSimpleTimestampFormat().format(date);
	}

	/**
	 * Converts a date with a missing year, month or day to a 'readable' format
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static String convertFussyDate(String pDate) {
		if (pDate == null || pDate.isEmpty()) {
			return "";
		}

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

		LocalDate date = convertDB2Date(pDate);
		if (date == null) {
			return pDate;
		}

		String dateFormat = mySettings.getDateFormat();
		if (isNoDay) {
			dateFormat = dateFormat.replaceAll("d", "");
		}

		if (isNoMonth) {
			dateFormat = dateFormat.replaceAll("M", "");
		}

		dateFormat = dateFormat.replaceAll("  ", " ").trim();
		DateTimeFormatter sd = DateTimeFormatter.ofPattern(dateFormat.replaceAll(" ", mySettings.getDateDelimiter()));
		return sd.format(date);
	}

	/**
	 * Converts 'readable' date created by convertDate back to a database date
	 * format
	 *
	 * @param pDate : the date in the database table to be converted
	 */
	public static String convertDate2DB(String pDate) {
		if (pDate == null || pDate.isEmpty()) {
			return "";
		}

		try {
			LocalDate date = LocalDate.parse(pDate, getSimpleDateFormat());
			return convertDate2DB(date);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Converts 'readable' time stamp created by convertTimetamp back to a database
	 * date format
	 */
	public static String convertTimestamp2DB(String pDate) {
		if (pDate == null || pDate.isEmpty()) {
			return "";
		}

		try {
			LocalDateTime date = LocalDateTime.parse(pDate, getSimpleTimestampFormat());
			return convertTimestamp2DB(date);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Converts a 'readable' fussy date back to a database date format
	 */
	public static String convertFussyDate2DB(String pDate) {
		if (pDate == null || pDate.trim().length() == 0) {
			return "";
		}

		pDate = pDate.trim();
		String[] displayDate = pDate.split(mySettings.getDateDelimiter());
		String[] dBDate = new String[3];

		int[] dateOrder = new int[3];
		String dateFormat = mySettings.getDateFormat().replaceAll(" ", "");
		dateFormat = dateFormat.replaceAll("MM", "M");
		dateFormat = dateFormat.replaceAll("yyyy", "y");
		dateFormat = dateFormat.replaceAll("yy", "y");
		dateFormat = dateFormat.replaceAll("dd", "d");

		dateOrder[0] = dateFormat.indexOf("d");
		dateOrder[1] = dateFormat.indexOf("M");
		dateOrder[2] = dateFormat.indexOf("y");

		final int index = displayDate.length;
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < index; i++) {
			dBDate[dateOrder[i]] = displayDate[i];
		}

		for (int i = index; i < 3; i++) {
			dBDate[dateOrder[i]] = "";
		}

		result.append(dBDate[2] + dBDate[1] + dBDate[0]);
		return result.toString();
	}

	public static LocalDateTime convertDB2Timestamp(String pDate) {
		if (pDate == null || pDate.isEmpty() || pDate.length() != 19) {
			return null;
		}

		try {
			return LocalDateTime.parse(pDate, sdInternalTimestamp);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts a internal database date to a JAVA date
	 *
	 * @param pDate the date in the database table to be converted
	 */
	public static LocalDate convertDB2Date(String pDate) {
		if (pDate == null || pDate.isEmpty()) {
			return null;
		}

		String format;
		switch (pDate.length()) {
		case 4:
			format = "yyyy";
			break;
		case 6:
			format = "yyyyMM";
			break;
		case 8:
			format = "yyyyMMdd";
			break;
		default:
			return null;
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		try {
			return LocalDate.parse(pDate, formatter);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts a Java date back to a database date format
	 */
	public static String convertDate2DB(Date pDate) {
		if (pDate == null) {
			return "";
		}
		return sdInternalDate.format(new Timestamp(pDate.getTime()).toLocalDateTime());
	}

	/**
	 * Converts a Java LocalDateTime back to a database date format
	 */
	public static String convertDate2DB(LocalDate pDate) {
		if (pDate == null) {
			return "";
		}
		return sdInternalDate.format(pDate);
	}

	/**
	 * Converts a Java time back to the database time format
	 */
	public static String convertTime2DB(LocalTime pTime) {
		if (pTime == null) {
			return "";
		}

		int hrs = pTime.getHour() * 3600;
		int min = pTime.getMinute() * 60;
		int sec = pTime.getSecond();

		return Integer.toString(hrs + min + sec);
	}

	public static String convertTimestamp2DB(Date pDate) {
		if (pDate == null) {
			return "";
		}

		return sdInternalTimestamp.format(new Timestamp(pDate.getTime()).toLocalDateTime());
	}

	/**
	 * Converts a Java time stamp back to a database time stamp format
	 */
	public static String convertTimestamp2DB(LocalDateTime pDate) {
		if (pDate == null) {
			return "";
		}

		return sdInternalTimestamp.format(pDate);
	}

	/**
	 * Converts a time stored in the database format to a 'readable' format
	 */
	public static String convertTime(String pTime) {
		if (pTime == null || pTime.isEmpty()) {
			return "";
		}

		int time = Integer.parseInt(pTime);
		int hrs = time / 3600;
		int totalMin = hrs * 3600;
		int min = (time - totalMin) / 60;
		int sec = time - (totalMin + min * 60);

		LocalTime lc = LocalTime.of(hrs, min, sec);
		return getSimpleTimeFormat().format(lc);
	}

	/**
	 * Convert a duration stored in the database table to a 'readable' format
	 */
	public static String convertDuration(Number number) {
		if (number == null) {
			return "";
		}

		int time = number.intValue();
		if (time == 0) {
			return "0:00";
		}

		int min = 0;
		int hrs = 0;
		int sec = 0;
		int totalMin = 0;

		StringBuilder result = new StringBuilder();

		if (mySettings.getDurationFormat().equals("h:mm:ss")) {
			hrs = time / 3600;
			totalMin = hrs * 3600;
			result.append(hrs + ":");
		}

		min = (time - totalMin) / 60;
		sec = time - (totalMin + min * 60);

		if (min < 10) {
			result.append("0");
		}
		result.append(min);
		result.append(":");

		if (sec < 10) {
			result.append("0");
		}
		result.append(sec);
		return result.toString();
	}

	/**
	 * Method to convert a 'readable' time created by convertTime back to a database
	 * date format
	 */
	public static String convertTime2DB(String pTime) {
		if (pTime == null || pTime.isEmpty()) {
			return "";
		}

		if (pTime.equals("0")) {
			return "0";
		}

		pTime = pTime.trim().toUpperCase();
		int hrs = 0;
		int min = 0;
		int sec = 0;

		int total = 0;
		String[] displayTime = pTime.split(":");
		int index = displayTime.length - 1;

		boolean AMTime = displayTime[index].endsWith("AM");
		boolean PMTime = displayTime[index].endsWith("PM");

		if (AMTime || PMTime) {
			displayTime[index] = displayTime[index].substring(0, displayTime[index].length() - 2).trim();
		}

		min = Integer.parseInt(displayTime[1]);
		hrs = Integer.parseInt(displayTime[0]);

		if (index == 2) {
			sec = Integer.parseInt(displayTime[2]);
		}

		if (PMTime) {
			hrs += 12;
			if (hrs == 24) {
				hrs = 0;
			}
		}

		total = hrs * 3600 + min * 60 + sec;
		return String.valueOf(total);
	}

	/**
	 * Method to convert a 'readable' duration created by convertDuration back to a
	 * database date format
	 */
	public static String convertDuration2DB(String pTime) {
		if (pTime == null || pTime.isEmpty()) {
			return "";
		}

		if (pTime.equals("0")) {
			return "0";
		}

		pTime = pTime.trim().toUpperCase();
		int hrs = 0;
		int min = 0;
		int sec = 0;

		int total = 0;
		String[] displayTime = pTime.split(":");
		int index = displayTime.length - 1;

		sec = Integer.parseInt(displayTime[index--]);
		min = Integer.parseInt(displayTime[index--]);
		if (index == 0) {
			hrs = Integer.parseInt(displayTime[0]);
		}

		total = hrs * 3600 + min * 60 + sec;
		return String.valueOf(total);
	}

	public static String convertTrack(Number pIndex, int pLength) {
		StringBuilder result = new StringBuilder(String.valueOf(pIndex));
		int i = result.length();
		while (i++ < pLength) {
			result.insert(0, "0");
		}
		result.append(".");
		return result.toString();
	}

	public static boolean isNumerical(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}

		return str.matches("-?\\d+(\\.\\d+)?");
	}

	public static int intLittleEndian(byte[] buf) {
		return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	public static long longLittleEndian(byte[] buf) {
		return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getLong();
	}

	public static JPanel addVerticalButtons(String title, ActionListener action, AbstractButton... buttons) {
		final int max = buttons.length;
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();
		ButtonGroup bGroup = new ButtonGroup();

		for (int i = 0; i < max; i++) {
			if (buttons[i] instanceof JRadioButton) {
				bGroup.add(buttons[i]);
			}

			if (action != null) {
				buttons[i].addActionListener(action);
				buttons[i].setActionCommand(buttons[i].getText());
			}

			result.add(buttons[i], c.gridCell(1, i, 1, 0));
		}

		result.add(Box.createVerticalGlue(), c.gridCell(0, max, 0, 1));
		if (title != null) {
			result.setBorder(BorderFactory.createTitledBorder(title));
		}
		return result;
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

	public static void errorMessage(Component parent, Throwable e, String title, String mesg) {
		if (isQuietMode || e instanceof FNProgException) {
			showMessage(parent, e.getMessage(), title, true);
			return;
		}

		ErrorInfo info = new ErrorInfo(title, mesg == null ? GUIFactory.getText("internalProgramError") : mesg, null,
				title, e, Level.SEVERE, null);
		JXErrorPane.showDialog(parent, info);
	}

	public static void showMessage(Component parent, String mesg, String title, boolean isError) {
		if (isQuietMode) {
			System.out.println(title + ":" + mesg == null ? GUIFactory.getText("internalProgramError") : mesg);
			return;
		}
		JOptionPane.showMessageDialog(parent, setDialogText(mesg), title,
				isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
	}

	public static boolean showConfirmMessage(Component parent, String mesg, String title) {
		return JOptionPane.showConfirmDialog(parent, setDialogText(mesg), title,
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

	public static String setDialogText(String text) {
		return setDialogText(text, 120);
	}

	public static String setDialogText(String text, int maxLen) {
		if (text == null) {
			return "";
		}

		StringBuilder mesg = new StringBuilder(text);
		String s = null;
		if (text.length() > maxLen) {
			mesg = new StringBuilder();
			int start = 0;
			int end = text.indexOf(" ", maxLen);
			int index = 0;

			while (end != -1) {
				s = text.substring(start, ++end);
				index = s.indexOf("\n");
				if (index == -1) {
					mesg.append(s + "\n");
				} else {
					mesg.append(s.substring(0, ++index));
					end -= s.length() - index;
				}

				start = end;
				end = text.indexOf(" ", end + maxLen);
			}
			mesg.append(text.substring(start, text.length()));
		}
		return mesg.toString().trim();
	}

	public static boolean existFile(String filename) {
		File f = new File(filename);
		return f.exists();
	}

	public static void getSelectedFile(JDialog dialog, JTextField component, ExportFile file, String dir,
			boolean isMustExist) {
		getSelectedFile(dialog, component, dir, file.getFileType(), isMustExist, file.getFileExtention());
		if (!isMustExist) {
			String filename = component.getText();
			if (!filename.isEmpty() && !isFileExtensionOk(filename, file)) {
				component.setText(filename + "." + file.getFileExtention()[0]);
			}
		}
	}

	public static void getSelectedFile(JDialog dialog, JTextField component, String dir, String fileType,
			boolean isMustExist, String... fileExt) {
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

	public static boolean isFileExtensionOk(String dbFile, ExportFile exp) {
		boolean isExtOK = false;

		int index = dbFile.lastIndexOf(".");
		String ext = "csv";
		if (index != -1) {
			ext = dbFile.substring(dbFile.lastIndexOf(".") + 1).toLowerCase();
		}

		for (String element : exp.getFileExtention()) {
			if (element.equals("*") || ext.equals(element)) {
				isExtOK = true;
				break;
			}
		}
		return isExtOK;
	}

	public static boolean writeObjectToDisk(Object myObject) {
		try {
			// Write to disk with FileOutputStream
			FileOutputStream f_out = new FileOutputStream(tempDir + File.separator + "myobject.data");

			// Write object with ObjectOutputStream
			ObjectOutputStream obj_out = new ObjectOutputStream(f_out);

			// Write object out to disk
			obj_out.writeObject(myObject);
			obj_out.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static Object readObjectFromDisk() {
		Object result = null;
		try {
			// Read from disk using FileInputStream
			FileInputStream f_in = new FileInputStream(tempDir + File.separator + "myobject.data");

			// Read object using ObjectInputStream
			ObjectInputStream obj_in = new ObjectInputStream(f_in);

			// Read an object
			result = obj_in.readObject();
			obj_in.close();
		} catch (Exception e) {
			return null;
		}
		return result;
	}

	public static String getDefaultPDADatabase(ExportFile db) {
		return mySettings.getDefaultFileFolder() + File.separator + db.getName() + "." + db.getFileExtention()[0];
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

	/** this painter draws a gradient fill */
	public static MattePainter getPainter() {
		if (painter != null) {
			return painter;
		}

		int width = 100;
		int height = 100;
		Color color1 = Color.white;
		Color color2 = Color.lightGray;

		LinearGradientPaint gradientPaint = new LinearGradientPaint(0.0f, 0.0f, width, height,
				new float[] { 0.0f, 1.0f }, new Color[] { color1, color2 });

		painter = new MattePainter(gradientPaint);
		return painter;
	}
}