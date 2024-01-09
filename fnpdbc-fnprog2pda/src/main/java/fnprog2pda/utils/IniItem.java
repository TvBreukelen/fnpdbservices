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

package fnprog2pda.utils;

import application.utils.General;

/**
 * <p>
 * The IniItem class represents the simplest element of an INI file, an item,
 * which has only two important properties, a name and a value.
 * </p>
 *
 * <p>
 * Any primitive type can be saved as a value of this item, and any objects
 * saved as a value are converted to a String via the object's
 * <code>toString()</code>, however all values returned by an item are returned
 * as a </code>String</code>.
 * </p>
 *
 * @author David Lewis
 * @version 1.1.0
 * @since 0.1.10
 */
public class IniItem {

	/**
	 * The name of this item
	 */
	private String name;

	/**
	 * The value of this item
	 */
	private String value;

	/**
	 * Predicate that is true if this <code>IniItem</code> is case sensitive, or
	 * false if the <code>IniItem</code> is case insensitive.
	 */
	private boolean caseSensitive;

	/**
	 * <p>
	 * Default constructor which creates a new instance of this <code>IniItem</code>
	 * and sets the <code>IniItem</code> to have a default <code>IniValidator</code>
	 * and to be case insensitive.
	 * </p>
	 *
	 * @param name The name of this <code>IniItem</code>.
	 */
	public IniItem(String name) {
		this(name, false);
	}

	/**
	 * <p>
	 * Default constructor which creates a new instance of this <code>IniItem</code>
	 * and sets the <code>IniItem</code> to have a default
	 * <code>IniValidator</code>.
	 * </p>
	 *
	 * @param name          The name of this <code>IniItem</code>.
	 * @param caseSensitive Sets whether this <code>IniItem</code> is case
	 *                      sensitive.
	 */
	public IniItem(String name, boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		this.name = name;
		setValue(General.EMPTY_STRING);
	}

	/**
	 * This method clears the value of this item.
	 *
	 * @return Returns true is the item's value was cleared successfully, false
	 *         otherwise.
	 */
	public boolean clear() {
		setValue(null);

		return true;
	}

	/**
	 * <p>
	 * This predicate returns true if this <code>IniItem</code> is equal to the
	 * given object. For <code>otherObject</code> to be equal to this one it must:
	 * </p>
	 *
	 * <ul>
	 * <li>be an instance of <code>IniItem</code>.</li>
	 * <li>have the same case-sensitivity as this <code>IniItem</code>.
	 * <li>have an equal <code>IniValidator</code> as this <code>IniItem</code>'s
	 * <code>IniValidator</code>.</li>
	 * <li>have an equal name as this <code>IniItem</code>.*</li>
	 * <li>have an equal value as this <code>IniItem</code>.*</li>
	 * <li>have an equal pre-comment as this <code>IniItem</code>.*</li>
	 * <li>have an equal post-comment as this <code>IniItem</code>.*</li>
	 * <li>have an equal end-line-comment as this <code>IniItem</code>.*</li>
	 * </ul>
	 *
	 * <p>
	 * * If the two <code>IniItem</code>s are <strong>case-sensitive</strong>, then
	 * these are compared using the <code>equals(Object)</code> method. If the two
	 * <code>IniItem</code>s are <strong>case-insensitive</strong>, then these are
	 * compared using the <code>String.equalsIgnoreCase(String)</code> method.
	 * </p>
	 *
	 * @param otherObject The other Object to test for equality.
	 * @return True if equal, false if not equal.
	 * @since 0.1.15
	 */
	@Override
	public boolean equals(Object otherObject) {

		IniItem otherItem;

		/***********************************************************************
		 * check to see if the other object is an instance of the same class
		 **********************************************************************/
		if (!(otherObject instanceof IniItem)) {
			return false;
		} else {
			otherItem = (IniItem) otherObject;
		}

		/***********************************************************************
		 * check to see if the two IniItems have the same case sensitive setting
		 **********************************************************************/
		/***********************************************************************
		 * check to see if the two IniItems have the same name
		 **********************************************************************/
		if (isCaseSensitive() != otherItem.isCaseSensitive() || !testStrings(getName(), otherItem.getName())) {
			return false;
		}

		/***********************************************************************
		 * otherwise compare the values of the two items
		 **********************************************************************/
		String thisValue = getValue();
		String otherValue = otherItem.getValue();

		if (thisValue == null) {
			return otherItem.getValue() == null;
		} else {
			return testStrings(thisValue, otherValue);
		}
	}

	/**
	 * Returns the name of this item, note that the item cannot change its name and
	 * one would need to create a new item to do so.
	 *
	 * @return the name of this item
	 */
	public String getName() {
		return name;
	}

	/**
	 * This returns the String which this items stores. If the item has no value,
	 * then null is returned.
	 *
	 * @return <strong>String</strong>, the value of this item.
	 */
	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {

		int nameHashCode = getName().hashCode();
		int valueHashCode = getValue().hashCode();

		return (nameHashCode + valueHashCode) % Integer.MAX_VALUE;
	}

	/**
	 * Predicate that returns true if this item has a value, or false if it is empty
	 *
	 * @return true or false
	 */
	public boolean hasValue() {
		return !value.equals(General.EMPTY_STRING);
	}

	/**
	 * <p>
	 * Predicate that returns true if this <code>IniItem</code> is case
	 * <strong>sensitive</strong>, or false if this <code>IniItem</code> is case
	 * <strong>insensitive</strong>.
	 * </p>
	 *
	 * @return boolean
	 * @since 0.1.16
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Set the value as a String value
	 *
	 * @param value the value to set
	 */
	public void setValue(String value) {
		if (value == null) {
			this.value = General.EMPTY_STRING;
		} else {
			this.value = value;
		}
	}

	/**
	 * <p>
	 * Predicate that test's whether the two given strings are equal. This method is
	 * used rather than
	 * <code><em>String</em>.<strong>equals</strong>(<em>Object</em>)</code> method
	 * as this method takes into account if this <code>IniItem</code> is case
	 * sensitive or not.
	 * </p>
	 * <p>
	 * This method, like the equals method, is commutative, and therefore it does
	 * not matter which string is the first and which string is the second as the
	 * same result will be returned.
	 * </p>
	 *
	 * @param string1 The first string to test
	 * @param string2 The second string to test.
	 * @return True if the two strings are equal, false if they are not
	 */
	private boolean testStrings(String string1, String string2) {

		// if this IniItem is case sensitive, then compare the two string with
		// case sensitivity
		if (caseSensitive) {
			return string1.equals(string2);
		} else {
			// IniItem is case insensitive, therefore, compare strings whilst
			// ignoring case sensitivity.
			return string1.equalsIgnoreCase(string2);
		}
	}

	@Override
	public String toString() {
		return "IniItem \"" + getName() + "\": (Value: \"" + getValue() + "\")";
	}
}
