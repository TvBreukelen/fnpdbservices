/*
 * Utils Class for contining utility functions.
 *
 * This file is part of JavaDBF packege.
 *
 * author: anil@linuxense.com license: LGPL (http://www.gnu.org/copyleft/lesser.html)
 *
 * $Id: Utils.java,v 1.7 2004/03/31 16:00:34 anil Exp $
 */
package dbengine.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

import application.utils.General;

/**
 * Miscellaneous functions required by the JavaDBF package.
 */
public final class Utils {

	public static final int ALIGN_LEFT = 10;
	public static final int ALIGN_RIGHT = 12;

	private Utils() {
		// Hide constructor
	}

	public static byte[] trimLeftSpaces(byte[] arr) {
		StringBuilder sb = new StringBuilder(arr.length);
		for (byte element : arr) {
			if (element != ' ') {
				sb.append((char) element);
			}
		}
		return sb.toString().getBytes();
	}

	public static byte[] textPadding(String text, String characterSetName, int length) {
		return textPadding(text, characterSetName, length, Utils.ALIGN_LEFT);
	}

	public static byte[] textPadding(String text, String characterSetName, int length, int alignment) {
		return textPadding(text, characterSetName, length, alignment, (byte) ' ');
	}

	public static byte[] textPadding(String text, String characterSetName, int length, int alignment,
			byte paddingByte) {

		if (text.length() >= length) {
			return General.convertString2Bytes(text.substring(0, length), characterSetName);
		}

		byte[] byteArray = new byte[length];
		Arrays.fill(byteArray, paddingByte);

		if (alignment == ALIGN_RIGHT) {
			int tOffset = length - text.length();
			System.arraycopy(General.convertString2Bytes(text, characterSetName), 0, byteArray, tOffset,
					text.length());
		} else {
			System.arraycopy(General.convertString2Bytes(text, characterSetName), 0, byteArray, 0, text.length());
		}
		return byteArray;
	}

	public static byte[] numberFormating(Number num, DBFField field) {
		return textPadding(getDecimalFormat(field.getFieldLength(), field.getDecimalCount()).format(num), "",
				field.getFieldLength(), ALIGN_RIGHT);
	}

	public static DecimalFormat getDecimalFormat(int fieldLength, int sizeDecimalPart) {
		int sizeWholePart = fieldLength - (sizeDecimalPart > 0 ? sizeDecimalPart + 1 : 0);
		StringBuilder format = new StringBuilder(fieldLength);
		DecimalFormatSymbols dSym = new DecimalFormatSymbols();
		dSym.setDecimalSeparator('.');

		for (int i = 0; i < sizeWholePart; i++) {
			format.append("#");
		}
		format.setCharAt(format.length() - 1, '0');

		if (sizeDecimalPart > 0) {
			format.append(".");
			for (int i = 0; i < sizeDecimalPart; i++) {
				format.append("0");
			}
		}

		return new DecimalFormat(format.toString(), dSym);
	}

	public static boolean contains(byte[] arr, byte value) {
		for (byte element : arr) {
			if (element == value) {
				return true;
			}
		}
		return false;
	}
}
