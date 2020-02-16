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
 * This implementation of the <code>IniSection</code> interface offers faster
 * performance compared with the <code>BasicIniSection</code> implementation,
 * but at the expense of greater memory usage.
 * </p>
 *
 * @author David Lewis
 * @version 1.1.0
 * @since 0.2.00
 */
public class AdvancedIniSection extends IniSection {

	/**
	 * A Map of all the IniItems within this section, index by the item's name.
	 *
	 * @since 0.1.27
	 */
	private Map<String, IniItem> items;

	/**
	 * A list of the items in the order that they will be outputted.
	 *
	 * @since 0.1.27
	 */
	private List<IniItem> itemLists;

	/**
	 * <p>
	 * Default constructor which creates a new instance of this
	 * <code>IniSection</code> and sets the <code>IniSection</code> to have a
	 * default <code>IniValidator</code> and to be case insensitive.
	 * </p>
	 *
	 * @param name
	 *            The name of this <code>IniSection</code>.
	 */
	public AdvancedIniSection(String name) {
		this(name, false);
	}

	/**
	 * <p>
	 * Default constructor which creates a new instance of this
	 * <code>IniSection</code> and sets the <code>IniSection</code> to have a
	 * default <code>IniValidator</code> and to be case insensitive.
	 * </p>
	 *
	 * @param name
	 *            The name of this <code>IniSection</code>.
	 * @param caseSensitive
	 *            Sets whether this <code>IniSection</code> is case sensitive.
	 */
	public AdvancedIniSection(String name, boolean caseSensitive) {
		super(name, caseSensitive);

		items = new TreeMap<>();
		itemLists = new ArrayList<>();
	}

	@Override
	public boolean addItem(IniItem item) {
		return addItem(item, getNumberOfItems());
	}

	@Override
	public boolean addItem(IniItem item, int index) {

		if (item == null) {
			return false;
		}

		String itemName = item.getName();

		// **********************************************************************

		// **********************************************************************
		if (this.hasItem(itemName)) {
			return false;
		} else {
			itemLists.add(index, item);
			items.put(itemName, item);

			return true;
		}
	}

	@Override
	public IniItem addItem(String name) {
		return addItem(name, getNumberOfItems());
	}

	@Override
	public IniItem addItem(String itemName, int index) {

		// **********************************************************************
		// check if we can add the item
		// **********************************************************************

		// cannot add null items
		if (itemName == null) {
			return null;
		}

		// cannot add the item if another one with the same name already exists
		if (hasItem(itemName)) {
			return null;
		} else {
			IniItem newItem = createItem(itemName);
			addItem(newItem, index);
			return newItem;
		}
	}

	@Override
	protected IniItem createItem(String name) {
		return new IniItem(name, isCaseSensitive());
	}

	@Override
	public IniItem getItem(int index) {
		return itemLists.get(index);
	}

	@Override
	public IniItem getItem(String name) {
		return items.get(name);
	}

	@Override
	public Collection<String> getItemNames() {
		return items.keySet();
	}

	@Override
	public Collection<IniItem> getItems() {
		return new ArrayList<>(items.values());
	}

	@Override
	public int getNumberOfItems() {
		return items.size();
	}

	@Override
	public boolean hasItem(IniItem item) {
		return items.containsValue(item);
	}

	@Override
	public boolean hasItem(String name) {
		return items.containsKey(name);
	}

	@Override
	public int indexOf(IniItem item) {
		return itemLists.indexOf(item);
	}

	@Override
	public int indexOf(String name) {
		return indexOf(getItem(name));
	}

	@Override
	public boolean isEmpty() {
		return itemLists.isEmpty();
	}

	@Override
	public Iterator<IniItem> iterator() {
		return itemLists.iterator();
	}
}
