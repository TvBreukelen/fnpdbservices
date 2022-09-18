/*
 * DBFReader Class for reading the records assuming that the given InputStream comtains DBF data.
 *
 * This file is part of JavaDBF packege.
 *
 * Author: anil@linuxense.com License: LGPL (http://www.gnu.org/copyleft/lesser.html)
 *
 * $Id: DBFReader.java,v 1.8 2004/03/31 10:54:03 anil Exp $
 */

package dbengine.utils;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.JulianFields;
import java.util.HashMap;
import java.util.Map;

import application.utils.General;

/**
 * DBFReader class can creates objects to represent DBF data. This Class is used
 * to read data from a DBF file. Meta data and records can be queried against
 * this document.
 */
public class DBFReader extends DBFBase {
	private DataInputStream dataInputStream;

	/**
	 * Initializes a DBFReader object.
	 *
	 * When this constructor returns the object will have completed reading the
	 * header (meta date) and header information can be queried there on. And it
	 * will be ready to return the first row.
	 *
	 * @param InputStream where the data is read from.
	 */
	public DBFReader(InputStream in, File dbtFile) throws Exception {
		dataInputStream = new DataInputStream(in);
		header = new DBFHeader();
		header.read(dataInputStream);
		memo = new DBFMemo(dbtFile, header);

		if (header.hasMemoFile()) {
			memo.openMemoFile();
		}
	}

	public void closeDBFFile() {
		memo.closeMemoFile();
		try {
			dataInputStream.close();
		} catch (Exception e) {
			// Nothing to do here
		}
		memo = null;
		header = null;
		dataInputStream = null;
	}

	/**
	 * Reads the returns the next row in the DBF stream.
	 *
	 * @returns The next row as an Object array. Types of the elements these arrays
	 *          follow the convention mentioned in the class description.
	 */
	public Map<String, Object> nextRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();

		try {
			boolean isDeleted = false;
			do {
				if (isDeleted) {
					dataInputStream.skip((long) header.recordLength - 1);
				}

				int tByte = dataInputStream.readByte();
				if (tByte == END_OF_DATA) {
					return result;
				}

				isDeleted = tByte == '*';
			} while (isDeleted);

			for (DBFField field : header.fieldArray) {
				switch (field.getDataType()) {
				case DBFField.FIELD_TYPE_C:
					byte[] bArray = new byte[field.getFieldLength()];
					dataInputStream.read(bArray);
					result.put(field.getName(), General.convertByteArrayToString(bArray, header.getCharacterSet()));
					break;
				case DBFField.FIELD_TYPE_D:
					byte[] bYear = new byte[4];
					byte[] bMonth = new byte[2];
					byte[] bDay = new byte[2];
					dataInputStream.read(bYear);
					dataInputStream.read(bMonth);
					dataInputStream.read(bDay);
					try {
						result.put(field.getName(), LocalDate.of(Integer.parseInt(new String(bYear)),
								Integer.parseInt(new String(bMonth)), Integer.parseInt(new String(bDay))));
					} catch (NumberFormatException e) {
						/* this field may be empty or may have improper value set */
						result.put(field.getName(), "");
					}
					break;
				case DBFField.FIELD_TYPE_I:
					byte[] bInteger = new byte[field.getFieldLength()];
					dataInputStream.read(bInteger);
					result.put(field.getName(), General.intLittleEndian(bInteger));
					break;
				case DBFField.FIELD_TYPE_F:
				case DBFField.FIELD_TYPE_N:
					try {
						byte[] bNumeric = new byte[field.getFieldLength()];
						dataInputStream.read(bNumeric);
						bNumeric = Utils.trimLeftSpaces(bNumeric, null);
						if (bNumeric.length > 0 && !Utils.contains(bNumeric, (byte) '?')) {
							result.put(field.getName(), Double.valueOf(new String(bNumeric).replace(',', '.')));
						}
					} catch (NumberFormatException e) {
						result.put(field.getName(), "");
					}
					break;
				case DBFField.FIELD_TYPE_L:
					byte bLogical = dataInputStream.readByte();
					result.put(field.getName(),
							bLogical == 'Y' || bLogical == 't' || bLogical == 'T' || bLogical == 'y' ? Boolean.TRUE
									: Boolean.FALSE);
					break;
				case DBFField.FIELD_TYPE_M:
					String s = "";
					int index = 0;
					byte[] bMemo = new byte[field.getFieldLength()];
					dataInputStream.read(bMemo);

					if (header.hasMemoFile()) {
						if (bMemo.length == 4) {
							// Foxpro format
							index = General.intLittleEndian(bMemo);
						} else {
							s = new String(bMemo).trim();
							if (s.length() > 0) {
								index = Integer.parseInt(s);
							}
						}
						result.put(field.getName(), index == 0 ? null : memo.readMemo(index));
					}
					break;
				case DBFField.FIELD_TYPE_T:
					byte[] bDatetime = new byte[field.getFieldLength()];
					dataInputStream.read(bDatetime);
					if (bDatetime.length > 7) {
						byte[] bDate = { bDatetime[0], bDatetime[1], bDatetime[2], bDatetime[3] };
						byte[] bTime = { bDatetime[4], bDatetime[5], bDatetime[6], bDatetime[7] };
						long date = General.intLittleEndian(bDate);
						long time = General.intLittleEndian(bTime);
						if (date != time) {
							LocalDate ld = LocalDate.MAX.with(JulianFields.JULIAN_DAY, date);
							LocalTime lt = LocalTime.ofNanoOfDay(time * 1000000);
							LocalDateTime dt = LocalDateTime.of(ld, lt);
							result.put(field.getName(), dt);
						}
					}
					break;
				case DBFField.FIELD_TYPE_Y:
					byte[] bCurrency = new byte[field.getFieldLength()];
					dataInputStream.read(bCurrency);
					result.put(field.getName(), General.longLittleEndian(bCurrency) / 10000.0);
					break;
				default:
					byte[] bUnknown = new byte[field.getFieldLength()];
					dataInputStream.read(bUnknown);
				}
			}
		} catch (EOFException e) {
			return new HashMap<>();
		}
		return result;
	}
}
