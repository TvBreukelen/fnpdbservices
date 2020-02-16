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

/**
 * <p>
 * This interface states the methods for getting, setting and removing comments
 * to and from an object.
 * </p>
 *
 * <p>
 * This interface supports three types of comments:
 * </p>
 * <ul>
 * <li><strong>pre comments</strong> - comments that appear on the line before
 * the object</li>
 * <li><strong>end line comments</strong> - comments that appear on the same
 * line, but after the object</li>
 * <li><strong>post comments</strong> - comments that appear on the line after
 * the object</li>
 * </ul>
 *
 * <p>
 * A class that implements this interface is allowed to support or omit any of
 * the comment types, and can omit a type by not implementing the set methods
 * and returning null for the get methods of a particular comment type.
 * </p>
 *
 * @author David Lewis
 * @version 0.1.16
 * @since 0.1.10
 */
public interface Commentable {

	/**
	 * This single character defines the symbol used as the beginning of a
	 * comment in an ini file. Comments begin with this symbol and continue
	 * until the end of the line.
	 */
	public static final char COMMENT_SYMBOL = ';';

	/**
	 * <p>
	 * Returns the comment that comes before the object. If not such comment
	 * exists an empty string is returned.
	 * </p>
	 *
	 * @return the comment or "" if no comment exists.
	 * @since 0.1.10
	 */
	public String getPreComment();

	/**
	 * <p>
	 * Returns the comment that comes after the object on the same line. If not
	 * such comment exists an empty string is returned.
	 * </p>
	 *
	 * @return the comment or "" if no comment exists.
	 * @since 0.1.10
	 */
	public String getEndLineComment();

	/**
	 * <p>
	 * Returns the comment that comes after the object. If not such comment
	 * exists an empty string is returned.
	 * </p>
	 *
	 * @return the comment or "" if no comment exists.
	 * @since 0.1.10
	 */
	public String getPostComment();

	/**
	 * <p>
	 * Set the comment that comes before the object.
	 * </p>
	 *
	 * @param comment
	 *            the text comment, if this value is null, then the comment is
	 *            set to an empty string.
	 * @since 0.1.10
	 */
	public void setPreComment(String comment);

	/**
	 * <p>
	 * Set the comment that comes after the object but on the same line.
	 * </p>
	 *
	 * @param comment
	 *            The text comment, if this value is null, then the comment is
	 *            set to an empty string.
	 * @throws IllegalArgumentException
	 *             if the given string contains a new line character (i.e.
	 *             "\n").
	 * @since 0.1.10
	 */
	public void setEndLineComment(String comment);

	/**
	 * Set the comment that comes after the object.
	 *
	 * @param comment
	 *            the text comment, if this value is null, then the comment is
	 *            set to an empty string.
	 * @since 0.1.10
	 */
	public void setPostComment(String comment);

	/**
	 * Removes a pre-comment from the object. This is the equivalent to:<BR>
	 * <code>setPreComment("");</code>
	 *
	 * @since 0.1.12
	 */
	public void removePreComment();

	/**
	 * Removes an end line comment from the object. This is the equivalent
	 * to:<BR>
	 * <code>setEndLineComment("");</code>
	 *
	 * @since 0.1.12
	 */
	public void removeEndLineComment();

	/**
	 * Removes a post-comment from the object. This is the equivalent to:<BR>
	 * <code>setPostComment("");</code>
	 *
	 * @since 0.1.12
	 */
	public void removePostComment();
}
