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
 * The <code>FormatException</code> class is an exception which is thrown by the
 * class <code>IniFileReader</code> whenever it reads an INI file from the hard
 * disk that is not in the correct format of an INI file.
 *
 * @author David Lewis
 * @version 0.1.22
 * @since 0.1.18
 */
public class FormatException extends RuntimeException {

	/**
	 * The serial version of this class.
	 *
	 * @since 0.1.20
	 */
	private static final long serialVersionUID = 8671879510488663502L;

	/**
	 * <p>
	 * Default constructor, creates a new instance of
	 * <code>FormatException</code> and sets the message of the exception to the
	 * vague message "<em>A format exception occurred.</em>".
	 * </p>
	 *
	 * @since 0.1.18
	 */
	public FormatException() {
		this("A format exception occurred.");
	}

	/**
	 * <p>
	 * Constructor, creates a new instance of <code>FormatException</code> and
	 * sets the message to the given String.
	 *
	 * @param message
	 *            The message of this exception.
	 * @since 0.1.18
	 */
	public FormatException(String message) {
		super(message);
	}
}
