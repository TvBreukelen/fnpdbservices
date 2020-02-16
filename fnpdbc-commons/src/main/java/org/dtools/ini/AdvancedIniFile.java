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

package org.dtools.ini;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 * This implementation of the <code>IniFile</code> interface offers faster
 * performance compared with the <code>BasicIniFile</code> implementation, but
 * at the expense of greater memory usage.
 * </p>
 *
 * @author David Lewis
 * @version 1.1.0
 * @since 0.2.00
 */
public class AdvancedIniFile extends IniFile {

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
	 * Default constructor which creates a new instance of
	 * <code>BasicIniFile</code> and sets the <code>IniFile</code> to have a
	 * default <code>IniValidator</code> and to be case insensitive.
	 * </p>
	 *
	 * @since 0.1.15
	 */
	public AdvancedIniFile() {
		this(false);
	}

	/**
	 * <p>
	 * Default constructor which creates a new instance of
	 * <code>BasicIniFile</code> and sets the <code>IniFile</code> to have a
	 * default <code>IniValidator</code>.
	 * </p>
	 *
	 * @param caseSensitive
	 *            Sets whether this instance of <code>IniFile</code> is case
	 *            sensitive or not.
	 *
	 * @since 0.1.15
	 */
	public AdvancedIniFile(boolean caseSensitive) {
		super(caseSensitive);
		sections = new TreeMap<>();
		sectionOrder = new ArrayList<>();
	}

	@Override
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

	@Override
	protected IniSection createSection(String name) {
		return new AdvancedIniSection(name, isCaseSensitive());
	}

	@Override
	public IniSection getSection(int index) {
		return sectionOrder.get(index);
	}

	@Override
	public Collection<IniSection> getSections() {
		return new ArrayList<>(sectionOrder);
	}

	@Override
	public int indexOf(IniSection section) {
		return sectionOrder.indexOf(section);
	}

	@Override
	public Object clone() {
		return new AdvancedIniFile();
	}

	@Override
	public Iterator<IniSection> iterator() {
		return sectionOrder.iterator();
	}
}
