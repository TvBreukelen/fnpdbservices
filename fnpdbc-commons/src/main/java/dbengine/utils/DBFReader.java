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

		/* it might be required to leap to the start of records at times */
		int t_dataStartIndex = header.headerLength - (32 + 32 * header.fieldArray.length) - 1;
		if (t_dataStartIndex > 0) {
			dataInputStream.skip(t_dataStartIndex);
		}
	}

	public void closeDBFFile() {
		memo.closeMemoFile();
		try {
			dataInputStream.close();
		} catch (Exception e) {
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
	public DBFField getField(int index) throws Exception {
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
		Object recordObjects[] = new Object[getFieldCount()];
		try {
			boolean isDeleted = false;
			do {
				if (isDeleted) {
					dataInputStream.skip(header.recordLength - 1);
				}

				int t_byte = dataInputStream.readByte();
				if (t_byte == END_OF_DATA) {
					return null;
				}

				isDeleted = t_byte == '*';
			} while (isDeleted);

			for (int i = 0; i < getFieldCount(); i++) {
				switch (header.fieldArray[i].getDataType()) {
				case DBFField.FIELD_TYPE_C:
					byte b_array[] = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(b_array);
					recordObjects[i] = characterSetName.equals("") ? new String(b_array).trim()
							: new String(b_array, characterSetName).trim();
					break;
				case DBFField.FIELD_TYPE_D:
					byte t_byte_year[] = new byte[4];
					dataInputStream.read(t_byte_year);
					byte t_byte_month[] = new byte[2];
					dataInputStream.read(t_byte_month);
					byte t_byte_day[] = new byte[2];
					dataInputStream.read(t_byte_day);
					try {
						recordObjects[i] = LocalDate.of(Integer.parseInt(new String(t_byte_year)),
								Integer.parseInt(new String(t_byte_month)), Integer.parseInt(new String(t_byte_day)));
					} catch (NumberFormatException e) {
						/* this field may be empty or may have improper value set */
						recordObjects[i] = null;
					}
					break;
				case DBFField.FIELD_TYPE_I:
					byte[] t_integer = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(t_integer);
					recordObjects[i] = General.intLittleEndian(t_integer);
					break;
				case DBFField.FIELD_TYPE_F:
				case DBFField.FIELD_TYPE_N:
					try {
						byte t_numeric[] = new byte[header.fieldArray[i].getFieldLength()];
						dataInputStream.read(t_numeric);
						t_numeric = Utils.trimLeftSpaces(t_numeric);
						if (t_numeric.length > 0 && !Utils.contains(t_numeric, (byte) '?')) {
							recordObjects[i] = new Double(new String(t_numeric).replace(',', '.'));
						} else {
							recordObjects[i] = null;
						}
					} catch (NumberFormatException e) {
						throw new Exception("Failed to parse Number: " + e.getMessage());
					}
					break;
				case DBFField.FIELD_TYPE_L:
					byte t_logical = dataInputStream.readByte();
					if (t_logical == 'Y' || t_logical == 't' || t_logical == 'T' || t_logical == 't') {
						recordObjects[i] = Boolean.TRUE;
					} else {
						recordObjects[i] = Boolean.FALSE;
					}
					break;
				case DBFField.FIELD_TYPE_M:
					String s = "";
					int index = 0;
					byte t_memo[] = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(t_memo);

					if (header.hasMemoFile()) {
						if (t_memo.length == 4) {
							// Foxpro format
							index = General.intLittleEndian(t_memo);
						} else {
							s = new String(t_memo).trim();
							if (s.length() > 0) {
								index = Integer.parseInt(s);
							}
						}
						recordObjects[i] = index == 0 ? null : memo.readMemo(index);
					}
					break;
				case DBFField.FIELD_TYPE_Y:
					byte[] t_currency = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(t_currency);
					recordObjects[i] = new Double(General.longLittleEndian(t_currency) / 10000.0);
					break;
				default:
					byte t_unknown[] = new byte[header.fieldArray[i].getFieldLength()];
					dataInputStream.read(t_unknown);
					recordObjects[i] = null;
				}
			}
		} catch (EOFException e) {
			return null;
		}
		return recordObjects;
	}
}
