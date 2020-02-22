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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 * An IniFile is an abstract class which defines the public and protected
 * methods of an IniFile implementation. This class is designed to represent a
 * physical INI file (a.k.a. a configuration file) on a hard disk, which is a
 * type of file that stores properties of configuration data for applications.
 * </p>
 *
 * @author David Lewis
 * @version 1.1.0
 * @since 0.1.10
 */
public class IniFile implements Cloneable, Iterable<IniSection> {

	/**
	 * A Map of all the sections within this IniFile, with the IniSections
	 * indexed by their names.
	 *
	 * @since 0.1.27
	 */
	private Map<String, IniSection> sections;

	/**
	 * A List of the order that the sections will be outputted.
	 *
	 * @since 0.1.27
	 */
	private List<IniSection> sectionOrder;


	/**
	 * <p>
	 * Field which marks whether this instance of an IniFile is case sensitive
	 * or not.
	 * </p>
	 */
	private boolean caseSensitive;

	/**
	 * <p>
	 * Default constructor which creates a new instance of this abstract
	 * <code>IniFile</code> and sets the <code>IniFile</code> to have a default
	 * <code>IniValidator</code> and to be case insensitive.
	 * </p>
	 */
	public IniFile() {
		this(false);
	}

	/**
	 * <p>
	 * Constructor which creates a new instance of this abstract
	 * <code>IniFile</code> and sets the IniFile to have a default
	 *
	 * @param caseSensitive
	 *            Sets whether this instance of <code>IniFile</code> is case
	 *            sensitive or not.
	 */
	public IniFile(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		sections = new TreeMap<>();
		sectionOrder = new ArrayList<>();
	}

	/**
	 * <p>
	 * This method adds the given <code>IniSection</code> to the
	 * <code>IniFile</code> providing that there are no existing sections with
	 * the same name as the given <code>IniSection</code>.
	 * </p>
	 *
	 * <p>
	 * The given section can also not be added if the given section is not
	 * compatible with the policies of the <code>IniFile</code>. That is to say
	 * the <code>IniSection</code> has a different case-sensitivity, or an
	 * unequal <code>IniValidator</code> to the <code>IniFile</code>.
	 * </p>
	 *
	 * @param section
	 *            The section to add to this INI file.
	 * @return True if the section was added successfully, false if a section
	 *         with the same name already exists, or if the given section is
	 *         incompatible with the IniFile.
	 */
	public boolean addSection(IniSection section) {

		// **********************************************************************
		// cannot add a null section
		// **********************************************************************
		if (section == null) {
			return false;
		}

		// **********************************************************************
		// get name of section
		// **********************************************************************
		String sectionName = section.getName();

		// **********************************************************************
		// check that name of section is valid
		// **********************************************************************

		if (this.isCaseSensitive() != section.isCaseSensitive()) {
			return false;
		}

		// **********************************************************************
		// check if a section with the same name already exists
		// **********************************************************************
		if (this.hasSection(sectionName)) {
			return false;
		} else {
			// append section to end of IniFile
			return addSection(section, this.getNumberOfSections());
		}
	}

	/**
	 * <p>
	 * Adds a section to this INI file.
	 *
	 * If an existing section has the same name as the given section, then the
	 * new section is not added and the method returns false.
	 *
	 * If the given section is of a type that is not compatible with the class
	 * that implements this interface, then a compatible copy of the section is
	 * made which is then added to the class.
	 *
	 * @param section
	 *            The section to add to this INI file.
	 * @param index
	 *            The index where to add the section, where 0 is the index of
	 *            the first section. Any section that already exists at this
	 *            index will be moved to <code>index + 1</code>. If the value is
	 *            greater than the number of sections within this INI file, then
	 *            the section is appended to the end of the INI file.
	 * @return True if the section was added successfully, false if a section
	 *         with the same name already exists.
	 * @throws IndexOutOfBoundsException
	 *             if the value of <code>index</code> is less than 0.
	 */
	public boolean addSection(IniSection section, int index) {

		if (section == null) {
			return false;
		}

		String sectionName = section.getName();

		// **********************************************************************
		// check that section is compatible
		// **********************************************************************

		if (this.isCaseSensitive() != section.isCaseSensitive()) {
			return false;
		}

		// **********************************************************************
		// add section
		// **********************************************************************

		// add section only if it doesn't already exists
		if (hasSection(section)) {
			return false;
		} else {
			sections.put(sectionName, section);
			sectionOrder.add(index, section);
			return true;
		}
	}

	/**
	 * Adds a section to this INI file.
	 *
	 * If an existing section has the same name as the given section, then the
	 * method returns false.
	 *
	 * The index of the section, if it added successfully, is unknown and
	 * depends on the implementation of this interface. Programmers should not
	 * make any assumptions on where the new section is added.
	 *
	 * @param sectionName
	 *
	 * @return A reference to the IniSection created, or null if, the section
	 *         could not be created.
	 *
	 */
	public IniSection addSection(String sectionName) {

		// cannot add a null section
		if (sectionName == null) {
			return null;
		}

		// check if a section with the same name already exists
		if (this.hasSection(sectionName)) {
			return null;
		}

		// append section to end of IniFile
		return addSection(sectionName, this.getNumberOfSections());
	}

	/**
	 * Adds a new section to this INI file which has the given name.
	 *
	 * If an existing section has the same name as the given section, then the
	 * method returns false.
	 *
	 * @param sectionName
	 *            The name of the new section to add.
	 * @param index
	 *            The index where to add the section, where 0 is the index of
	 *            the first section. Any section that already exists at this
	 *            index will be moved to <code>index + 1</code>. If the value is
	 *            greater than the number of sections within this INI file, then
	 *            the section is appended to the end of the INI file.
	 * @return A reference to the IniSection created, or null if, the section
	 *         could not be created.
	 * @throws IndexOutOfBoundsException
	 *             if the value of <code>index</code> is less than 0.
	 */
	public IniSection addSection(String sectionName, int index) {

		// cannot add a null section
		if (sectionName == null) {
			return null;
		}

		// check if a section with the same name already exists
		if (this.hasSection(sectionName)) {
			return null;
		}

		IniSection section = createSection(sectionName);
		addSection(section, index);
		return section;
	}

	/**
	 * </p>
	 * This method creates and returns a new instance of an
	 * <code>IniSection</code> with the same <code>IniValidator</code> and case
	 * sensitivity as this object.
	 * </p>
	 *
	 * @param name
	 *            The name of the <code>IniSection</code> to create.
	 * @return A new instance of an <code>IniSection</code> with the same
	 *         <code>IniValidator</code> and case sensitivity as this object.
	 */
	protected IniSection createSection(String name) {
		return new IniSection(name, isCaseSensitive());
	}

	/**
	 * <p>
	 * This predicate returns true if this <code>IniFile</code> is equal to the
	 * given object. For <code>other</code> to be equal to this one it must:
	 * </p>
	 *
	 * <ul>
	 * <li>be an instance of <code>IniFile</code>.</li>
	 * <li>have the same case-sensitivity as this <code>IniFile</code>.
	 * <li>have an equal <code>IniValidator</code> as this
	 * <code>IniFile</code>'s <code>IniValidator</code>.</li>
	 * <li>have the same number of <code>IniSection</code>s as this
	 * <code>IniFile</code>.</li>
	 * <li>have equal <code>IniSection</code>s as this
	 * <code>IniFile</code>.</li>
	 * <li>have the same order of <code>IniSection</code>s as this
	 * <code>IniFile</code>.</li>
	 * </ul>
	 *
	 * @param other
	 *            The other Object to test for equality.
	 * @return True if equal, false if not equal.
	 * @since 0.1.15
	 */
	@Override
	public boolean equals(Object other) {

		// **********************************************************************
		// Step 1 - Check to see if other object is an instance of IniFil
		// **********************************************************************
		if (!(other instanceof IniFile)) {
			return false;
		}

		IniFile otherIni = (IniFile) other;

		// **********************************************************************
		// Step 2 - check that the two IniFiles have the same case sensitive
		// **********************************************************************
		if (this.isCaseSensitive() != otherIni.isCaseSensitive()) {
			return false;
		}

		// **********************************************************************
		// Step 3 - Check to see if other IniFile has same number of sections
		// **********************************************************************
		if (otherIni.getNumberOfSections() != this.getNumberOfSections()) {
			return false;
		}

		// **********************************************************************
		// Step 4 - Check that all section in this IniFile are present and
		// equal in the otherIni object.
		// **********************************************************************
		for (IniSection section : this.getSections()) {

			String sectionName = section.getName();

			// check that the other IniFile has a section of the same name
			if (!otherIni.hasSection(sectionName)) {
				return false;
			}
			// check that other section is equal to this section
			IniSection otherSection = otherIni.getSection(sectionName);

			if (!section.equals(otherSection)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the total number of all the items, in every section, that this
	 * IniFile has.
	 *
	 * @return the total number of items in the IniFile
	 */
	public int getNumberOfItems() {

		int total = 0;

		for (IniSection section : this.getSections()) {
			total += section.getNumberOfItems();
		}

		return total;
	}

	/**
	 * Returns the number of sections this IniFile object has.
	 *
	 * @return the number of sections
	 */
	public int getNumberOfSections() {
		return getSections().size();
	}

	/**
	 * Get the section which is at the given index.
	 *
	 * @param index
	 *            the index of the section to retrieve.
	 * @return The section that is at the given index.
	 * @throws IndexOutOfBoundsException
	 *             if the given value is less than 0 or greater or equal to the
	 *             number of sections in this INI file (i.e.
	 *             <code>&gt; getNumberOfSections()-1</code>.
	 */
	public IniSection getSection(int index) {
		return sectionOrder.get(index);
	}

	/**
	 * Returns the section that is called <code>name</code>, or null if no such
	 * section exists.
	 *
	 * @param name
	 *            the name of the section to return
	 *
	 * @return The IniSection with the given name, or null if no section with
	 *         that name exists.
	 */
	public IniSection getSection(String name) {
		for (IniSection section : getSections()) {

			if (section.getName().equals(name)) {
				return section;
			}
		}

		return null;
	}

	/**
	 * Gets a collection of the names of all the sections in this IniFile.
	 *
	 * @return a collection of all the names.
	 */
	public Collection<String> getSectionNames() {

		// create a new collection to store section names
		Collection<String> sectionNames = null;
		sectionNames = new ArrayList<>(getNumberOfSections());

		for (IniSection section : sectionOrder) {
			sectionNames.add(section.getName());
		}

		return sectionNames;
	}

	/**
	 * Gets a collection of all the sections within this INI file.
	 *
	 * @return A collection of all the Sections.
	 */
	public Collection<IniSection> getSections() {
		return new ArrayList<>(sectionOrder);
	}

	@Override
	public int hashCode() {

		int total = 0;

		for (IniSection section : sectionOrder) {
			total = (total + section.hashCode()) % Integer.MAX_VALUE;
		}

		return total;
	}

	/**
	 *
	 * Predicate that returns true if this IniFile has a given section. More
	 * specifically this method returns true if the class contains a reference
	 * to the given section, and not if this class has a similar section with
	 * the same name, items and values.
	 *
	 * @param section
	 *            The section to test.
	 *
	 * @return True if this IniFile has the section, false otherwise
	 */
	public boolean hasSection(IniSection section) {
		return sectionOrder.contains(section);
	}

	/**
	 * Predicate that returns true if this IniFile has a section with the given
	 * name.
	 *
	 * @param name
	 *            The name of the section to test
	 * @return True if this IniFile has the section, false otherwise
	 */
	public boolean hasSection(String name) {
		return sections.containsKey(name);
	}

	/**
	 * Get the index of the given section, where 0 is the index is the first
	 * section. If the given section is not in this INI file, then -1 is
	 * returned.
	 *
	 * @param section
	 *            The section whose index will be retured
	 * @return The index of the section, or -1 is no such section exists.
	 */
	public int indexOf(IniSection section) {
		return sectionOrder.indexOf(section);
	}

	/**
	 * Get the index of the section whose name is given, where 0 is the index is
	 * the first section. If the given section name doesn't exists, then -1 is
	 * returned.
	 *
	 * @param sectionName
	 *            The name of the section whose index will be retured
	 * @return The index of the section, or -1 is no such section exists.
	 */
	public int indexOf(String sectionName) {
		return indexOf(getSection(sectionName));
	}

	/**
	 * <p>
	 * Predicate that returns true if this object is case sensitive, or false if
	 * it is case insensitive.
	 * </p>
	 *
	 * @return boolean
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Predicate that returns true if this IniFile has no sections.
	 *
	 * @return True if the object has no sections, false if the object has at
	 *         least one section.
	 */
	public boolean isEmpty() {
		return getNumberOfSections() == 0;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		int noSections = getNumberOfSections();
		int noItems = getNumberOfItems();

		sb.append("IniFile: (Sections: ");
		sb.append(noSections + ", Items: " + noItems + ")");

		for (IniSection section : getSections()) {
			sb.append("\n");
			sb.append(section.toString());
		}

		return sb.toString();
	}

	@Override
	public Object clone() {
		return new IniFile();
	}

	@Override
	public Iterator<IniSection> iterator() {
		return sectionOrder.iterator();
	}
}
