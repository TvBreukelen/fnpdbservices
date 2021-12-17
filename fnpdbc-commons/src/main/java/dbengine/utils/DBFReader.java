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
	 * Returns the number of records in the DBF.
	 */
	public int getRecordCount() {
		return header.numberOfRecords;
	}

	/**
	 * Returns the asked Field. In case of an invalid index, it returns a
	 * ArrayIndexOutofboundsException.
	 *
	 * @param index. Index of the field. Index of the first field is zero.
	 */
	public DBFField getField(int index) {
		return header.fieldArray[index];
	}

	public DBFHeader getDBFHeader() {
		return header;
	}

	/**
	 * Reads the returns the next row in the DBF stream.
	 *
	 * @returns The next row as an Object array. Types of the elements these arrays
	 *          follow the convention mentioned in the class description.
	 */
	public Object[] nextRecord() throws Exception {
		Object[] recordObjects = new Object[getFieldCount()];
		try {
			boolean isDeleted = false;
			do {
				if (isDeleted) {
					dataInputStream.skip((long) header.recordLength - 1);
				}

				int tByte = dataInputStream.readByte();
				if (tByte == END_OF_DATA) {
					return new Object[0];
				}

				isDeleted = tByte == '*';
			} while (isDeleted);

			for (int i = 0; i < getFieldCount(); i++) {
				switch (header.fieldArray[i].getDataType()) {
				case DBFField.FIELD_TYPE_C:
					byte[] bArray = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(bArray);
					recordObjects[i] = characterSetName.equals("") ? new String(bArray).trim()
							: new String(bArray, characterSetName).trim();
					break;
				case DBFField.FIELD_TYPE_D:
					byte[] bYear = new byte[4];
					byte[] bMonth = new byte[2];
					byte[] bDay = new byte[2];
					dataInputStream.read(bYear);
					dataInputStream.read(bMonth);
					dataInputStream.read(bDay);
					try {
						recordObjects[i] = LocalDate.of(Integer.parseInt(new String(bYear)),
								Integer.parseInt(new String(bMonth)), Integer.parseInt(new String(bDay)));
					} catch (NumberFormatException e) {
						/* this field may be empty or may have improper value set */
						recordObjects[i] = null;
					}
					break;
				case DBFField.FIELD_TYPE_I:
					byte[] bInteger = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(bInteger);
					recordObjects[i] = General.intLittleEndian(bInteger);
					break;
				case DBFField.FIELD_TYPE_F:
				case DBFField.FIELD_TYPE_N:
					try {
						byte[] bNumeric = new byte[header.fieldArray[i].getFieldLength()];
						dataInputStream.read(bNumeric);
						bNumeric = Utils.trimLeftSpaces(bNumeric);
						if (bNumeric.length > 0 && !Utils.contains(bNumeric, (byte) '?')) {
							recordObjects[i] = Double.valueOf(new String(bNumeric).replace(',', '.'));
						} else {
							recordObjects[i] = null;
						}
					} catch (NumberFormatException e) {
						throw new Exception("Failed to parse Number: " + e.getMessage());
					}
					break;
				case DBFField.FIELD_TYPE_L:
					byte bLogical = dataInputStream.readByte();
					if (bLogical == 'Y' || bLogical == 't' || bLogical == 'T' || bLogical == 'y') {
						recordObjects[i] = Boolean.TRUE;
					} else {
						recordObjects[i] = Boolean.FALSE;
					}
					break;
				case DBFField.FIELD_TYPE_M:
					String s = "";
					int index = 0;
					byte[] bMemo = new byte[header.fieldArray[i].getFieldLength()];
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
						recordObjects[i] = index == 0 ? null : memo.readMemo(index);
					}
					break;
				case DBFField.FIELD_TYPE_T:
					// case DBFField.FIELD_TYPE_TS:
					byte[] bDatetime = new byte[header.fieldArray[i].getFieldLength()];
					recordObjects[i] = null;
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
							recordObjects[i] = dt;
						}
					}
					break;
				case DBFField.FIELD_TYPE_Y:
					byte[] bCurrency = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(bCurrency);
					recordObjects[i] = General.longLittleEndian(bCurrency) / 10000.0;
					break;
				default:
					byte[] bUnknown = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(bUnknown);
					recordObjects[i] = null;
				}
			}
		} catch (EOFException e) {
			return new Object[0];
		}
		return recordObjects;
	}
}
