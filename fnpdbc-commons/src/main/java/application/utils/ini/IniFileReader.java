/*
 * Java INI Package
 * Copyright (C) 2008 David Lewis
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public Licence as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public Licence for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package application.utils.ini;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * <p>
 * This class provides users an easy way to read INI files from the hard disk
 * and parse them to an <code>IniFile</code> object.
 * <p>
 *
 * @author David Lewis
 * @version 1.1.0
 * @since 0.1.14
 *
 *        16.02.2020 Tom van Breukelen. Use constructor with InputStream,
 *        removed Validation
 */
public class IniFileReader {
	/**
	 * <p>
	 * This method returns the name of the item in the given string. For an item
	 * name to be returned the given string must be a valid item (determined by
	 * <code>boolean <strong>isItem</strong>(String)</code>). If the given string is
	 * not an item then an exception is thrown.
	 * </p>
	 *
	 * @param line The line to get the item name from.
	 * @return The name of the item from the given string.
	 * @throws RuntimeException If the given string is not an item.
	 * @see #isItem(String)
	 * @since 0.1.16
	 */
	static String getItemName(String line) {
		if (!isItem(line)) {
			throw new RuntimeException("getItemName(String) is unable to "
					+ "return the name of the item as the given string (\"" + line + "\") is not an item.");
		}

		// get the index of the first occurrence of the equals sign
		int pos = line.indexOf('=');
		return pos == -1 ? "" : line.substring(0, pos).trim();
	}

	/**
	 * <p>
	 * This method returns the value of the item in the given string. For an item
	 * value to be returned the given string must be a valid item (determined by
	 * <code>boolean <strong>isItem</strong>(String)</code>). If the given string is
	 * not an item then an exception is thrown.
	 * </p>
	 *
	 * @param line The line to get the item value from.
	 * @return The value of the item from the given string.
	 * @throws RuntimeException If the given string is not an item.
	 * @see #isItem(String)
	 * @since 0.1.16
	 */
	static String getItemValue(String line) {
		if (!isItem(line)) {
			throw new RuntimeException("getItemValue(String) is unable to "
					+ "return the value of the item as the given string (\"" + line + "\" is not an item.");
		}

		// **********************************************************************
		// get the index of the first occurrence of the equals sign
		// **********************************************************************
		int posEquals = line.indexOf('=');
		return posEquals == -1 ? line : line.substring(posEquals + 1).trim();
	}

	/**
	 * <p>
	 * This method returns the name of the section in the given string. For a
	 * section name to be returned the given string must be a valid section
	 * (determined by <code>boolean <strong>isSection</strong>(String)</code>). If
	 * the given string is not an item then an exception is thrown.
	 * </p>
	 *
	 * @param line The line to get the item name from.
	 * @return The name of the item from the given string.
	 * @throws RuntimeException if the given string is not an item.
	 * @see #isSection(String)
	 * @since 0.1.16
	 */
	static String getSectionName(String line) {

		if (!isSection(line)) {
			throw new RuntimeException("getSectionName(String) is unable to "
					+ "return the name of the section as the given string (\"" + line + "\" is not a section.");
		}

		int firstPos = line.indexOf('[');
		int lastPos = line.indexOf(']');

		return line.substring(firstPos + 1, lastPos).trim();
	}

	/**
	 * <p>
	 * Predicate that returns true if the given string contains an item. For a
	 * string to contain an item, the string should be in the following format:
	 * </p>
	 * <p>
	 * <code><em>item_name</em> <strong>"="</strong> [<em>item_value</em>]
	 * [<em>end_line_comment</em>]</code>
	 * </p>
	 * <p>
	 * where:
	 * <p>
	 * <ul>
	 * <li><em>item_name</em> - the name of the item.
	 * <li><em>item_value</em> - (optional) the value of the item.
	 * <li><em>end_line_comment</em> - (optional) a comment
	 * </ul>
	 * <p>
	 * However this method only requires that the string has at least a name
	 * followed by an equals sign. Also the name of the section does not have to be
	 * a valid name, as defined by the <code>IniFile</code>'s
	 * <code>IniValidator</code>, as that is the responsibility of the object
	 * <code>IniValidator</code>.
	 * </p>
	 *
	 * @param line The string to test.
	 * @return True if the string is a valid item, false otherwise.
	 * @since 0.1.14
	 */
	static boolean isItem(String line) {

		line = removeComments(line);
		if (line.isEmpty()) {
			return false;
		}

		int pos = line.indexOf('=');
		return pos != -1 ? !line.substring(0, pos).trim().isEmpty() : false;
	}

	/**
	 * <p>
	 * Predicate that returns true if the given string contains a section. For a
	 * string to contain a section, the string should be in the following format:
	 * </p>
	 * <p>
	 * <code><strong>"["</strong><em>sectionm_name</em><strong>"]"</strong>
	 * [<em>end_line_comment</em>]</code>
	 * </p>
	 * <p>
	 * where:
	 * <p>
	 * <ul>
	 * <li><em>sectionm_name</em> - the name of the section, which
	 * <strong>MUST</strong> be surrounded by square brackets (i.e. [ and ] ).
	 * <li><em>end_line_comment</em> - (optional) a comment
	 * </ul>
	 * <p>
	 * However this method only requires that the string has at least a name
	 * followed by an equals sign. Also the name of the section does not have to be
	 * a valid name, as defined by the <code>IniFile</code>'s
	 * <code>IniValidator</code>, as that is the responsibility of the object
	 * <code>IniValidator</code>.
	 * <p>
	 *
	 * @param line The string to test.
	 * @return True if the string is a valid section, false otherwise.
	 * @since 0.1.14
	 */
	static boolean isSection(String line) {

		line = removeComments(line);

		if (line.isEmpty()) {
			return false;
		}
		char firstChar = line.charAt(0);
		char lastChar = line.charAt(line.length() - 1);
		return firstChar == '[' && lastChar == ']';
	}

	/**
	 * <p>
	 * This method removes any comments (and comment symbols) from the given string
	 * and returns the remaining string. This allows other methods to test a string
	 * without concerning themselves about any comments within the string (e.g.
	 * {@link #isItem(String)} and {@link #isSection(String)} ).
	 * </p>
	 *
	 * @param line The string that will have comments removed from it.
	 * @return same as the imput string minus any comments.
	 * @since 0.1.14
	 */
	static String removeComments(String line) {
		int pos = line.indexOf(";");
		return pos != -1 ? line.substring(0, pos).trim() : line.trim();
	}

	/**
	 * A reference to the file which will be read in as an INI file.
	 *
	 * @since 0.1.14
	 */
	private BufferedReader reader;

	/**
	 * A reference to the <code>IniFile</code> object which will contian the parsed
	 * data from the INI file.
	 *
	 * @since 0.1.14
	 */
	private IniFile ini;

	/**
	 * Default constructor, creates a new <code>IniFileReader</code> object which
	 * will read from the given reader and populate the given data from the file
	 * into the given <code>IniFile</code> object.
	 * </p>
	 *
	 * @param ini  The IniFile which will be populated.
	 * @param file The file that will be read as an INI file.
	 * @since 0.1.14
	 */
	public IniFileReader(IniFile ini, BufferedReader reader) {
		this.ini = ini;
		this.reader = reader;
	}

	/**
	 * <p>
	 * This method begins the reading process of the file. The method opens the
	 * input file, which is set in the constructor, reads each line from the file,
	 * and parse that file to the <code>IniFile</code> object, which is also set in
	 * the constructor.
	 * </p>
	 *
	 * <p>
	 * If the method encounters a line of text from the string which it is unable to
	 * parse to the <code>IniFile</code>, then this method throws a
	 * <code>FormatException</code> exception.
	 * </p>
	 *
	 * @throws FormatException If an error is encountered whilst reading the input
	 *                         file.
	 * @throws IOException     If an exception occurs whilst reading the file.
	 * @since 0.1.14
	 */
	public void read() throws IOException {
		String line;
		IniSection currentSection = null;

		// **********************************************************************
		// process each line of the text file
		// **********************************************************************
		while ((line = reader.readLine()) != null) {

			// ******************************************************************
			// Trim any excess space from the beginning and end of the line
			// ******************************************************************
			line = line.trim();

			// ******************************************************************
			// If the line is empty, go to the next line
			// ******************************************************************
			if (line.isEmpty()) {
				continue;
			}

			// ******************************************************************
			// if the line is a section, then process it
			// ******************************************************************
			else if (isSection(line)) {

				// get the name of the section from the line
				String sectionName = getSectionName(line);

				// if section already exists, then get section
				if (ini.hasSection(sectionName)) {
					currentSection = ini.getSection(sectionName);
				} else {
					// section doesn't already exists
					// create a new instance of a section
					currentSection = ini.addSection(sectionName);
				}
			}

			// ******************************************************************
			// If the line is an item, then process the item
			// ******************************************************************
			else if (isItem(line)) {

				// **************************************************************
				// Check that a section has already been read
				// **************************************************************
				if (currentSection == null) {
					currentSection = ini.addSection("");
				}

				// **************************************************************
				// get name, value and end line comments of the item
				// **************************************************************
				String itemName = getItemName(line);
				String itemValue = getItemValue(line);

				IniItem item;

				// if the current section already has an item with same name
				if (currentSection.hasItem(itemName)) {
					item = currentSection.getItem(itemName);
				} else {
					// create a new instance of item
					item = currentSection.addItem(itemName);
				}

				// **************************************************************
				// add value
				// **************************************************************
				item.setValue(itemValue);
			}
		}

		reader.close();
	}
}
