/*
 * DBFHeader Class for reading the metadata assuming that the given InputStream carries DBF data.
 *
 * This file is part of JavaDBF packege.
 *
 * Author: anil@linuxense.com License: LGPL (http://www.gnu.org/copyleft/lesser.html)
 *
 * $Id$
 */

package dbengine.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Vector;

public class DBFHeader {

	public static final byte SIG_DBASE_III = (byte) 0x03;
	public static final byte SIG_DBASE_IV = (byte) 0x04;
	public static final byte SIG_DBASE_V = (byte) 0x05;
	public static final byte SIG_FOXPRO = (byte) 0xFb;
	public static final byte SIG_VISUAL_FOXPRO = (byte) 0x30;
	public static final byte SIG_DBASE_III_WITH_MEMO = (byte) 0x83;
	public static final byte SIG_DBASE_IV_WITH_MEMO = (byte) 0x7b;
	public static final byte SIG_DBASE_V_WITH_MEMO = (byte) 0x8b;
	public static final byte SIG_FOXPRO_WITH_MEMO = (byte) 0xF5;
	public static final byte SIG_VISUAL_FOXPRO_WITH_MEMO = (byte) 0x31;

	/* DBF structure start here */
	byte signature; /* 0 */
	byte year; /* 1 */
	byte month; /* 2 */
	byte day; /* 3 */
	int numberOfRecords; /* 4-7 */
	short headerLength; /* 8-9 */
	short recordLength; /* 10-11 */
	short reserv1; /* 12-13 */
	byte incompleteTransaction; /* 14 */
	byte encryptionFlag; /* 15 */
	int freeRecordThread; /* 16-19 */
	int reserv2; /* 20-23 */
	int reserv3; /* 24-27 */
	byte mdxFlag; /* 28 */
	byte languageDriver; /* 29 */
	short reserv4; /* 30-31 */
	DBFField[] fieldArray; /* each 32 bytes */
	byte terminator1; /* n+1 */
	/* DBF structure ends here */

	private boolean hasMemoFile = false;

	public DBFHeader() {
		signature = SIG_DBASE_III;
		terminator1 = 0x0D;
	}

	public void read(DataInput in) throws IOException {
		signature = in.readByte(); /* 0 */
		year = in.readByte(); /* 1 */
		month = in.readByte(); /* 2 */
		day = in.readByte(); /* 3 */
		numberOfRecords = Integer.reverseBytes(in.readInt()); /* 4-7 */

		headerLength = Short.reverseBytes(in.readShort()); /* 8-9 */
		recordLength = Short.reverseBytes(in.readShort()); /* 10-11 */

		reserv1 = Short.reverseBytes(in.readShort()); /* 12-13 */
		incompleteTransaction = in.readByte(); /* 14 */
		encryptionFlag = in.readByte(); /* 15 */
		freeRecordThread = Integer.reverseBytes(in.readInt()); /* 16-19 */
		reserv2 = in.readInt(); /* 20-23 */
		reserv3 = in.readInt(); /* 24-27 */
		mdxFlag = in.readByte(); /* 28 */
		languageDriver = in.readByte(); /* 29 */
		reserv4 = Short.reverseBytes(in.readShort()); /* 30-31 */

		Vector<DBFField> v_fields = new Vector<>();

		DBFField field = DBFField.createField(in); /* 32 each */
		while (field != null) {
			v_fields.addElement(field);
			field = DBFField.createField(in);
		}

		fieldArray = new DBFField[v_fields.size()];
		for (int i = 0; i < fieldArray.length; i++) {
			fieldArray[i] = v_fields.elementAt(i);
		}

		switch (signature) {
		case SIG_DBASE_III_WITH_MEMO:
		case SIG_DBASE_IV_WITH_MEMO:
		case SIG_DBASE_V_WITH_MEMO:
		case SIG_FOXPRO_WITH_MEMO:
		case SIG_VISUAL_FOXPRO_WITH_MEMO:
			hasMemoFile = true;
			break;
		default:
			hasMemoFile = false;
		}
	}

	public void write(DataOutput dataOutput) throws IOException {
		LocalDate now = LocalDate.now();

		dataOutput.writeByte(signature); /* 0 */
		year = (byte) (now.getYear() - 1900);
		month = (byte) now.getMonthValue();
		day = (byte) now.getDayOfMonth();

		dataOutput.writeByte(year); /* 1 */
		dataOutput.writeByte(month); /* 2 */
		dataOutput.writeByte(day); /* 3 */

		numberOfRecords = Integer.reverseBytes(numberOfRecords);
		dataOutput.writeInt(numberOfRecords); /* 4-7 */

		headerLength = findHeaderLength();
		dataOutput.writeShort(Short.reverseBytes(headerLength)); /* 8-9 */

		recordLength = findRecordLength();
		dataOutput.writeShort(Short.reverseBytes(recordLength)); /* 10-11 */

		dataOutput.writeShort(Short.reverseBytes(reserv1)); /* 12-13 */
		dataOutput.writeByte(incompleteTransaction); /* 14 */
		dataOutput.writeByte(encryptionFlag); /* 15 */
		dataOutput.writeInt(Integer.reverseBytes(freeRecordThread)); /* 16-19 */
		dataOutput.writeInt(Integer.reverseBytes(reserv2)); /* 20-23 */
		dataOutput.writeInt(Integer.reverseBytes(reserv3)); /* 24-27 */

		dataOutput.writeByte(mdxFlag); /* 28 */
		dataOutput.writeByte(languageDriver); /* 29 */
		dataOutput.writeShort(Short.reverseBytes(reserv4)); /* 30-31 */

		for (DBFField element : fieldArray) {
			element.write(dataOutput);
		}

		dataOutput.writeByte(terminator1); /* n+1 */
	}

	private short findHeaderLength() {
		return (short) (32 + 32 * fieldArray.length + 1);
	}

	private short findRecordLength() {
		int recordLength = 0;
		for (DBFField element : fieldArray) {
			recordLength += element.getFieldLength();
		}
		return (short) (recordLength + 1);
	}

	public byte getSignature() {
		return signature;
	}

	public void setSignature(boolean hasMemoFile, byte sign) {
		if (hasMemoFile) {
			switch (sign) {
			case SIG_DBASE_III:
				signature = SIG_DBASE_III_WITH_MEMO;
				break;
			case SIG_DBASE_IV:
				signature = SIG_DBASE_IV_WITH_MEMO;
				break;
			case SIG_DBASE_V:
				signature = SIG_DBASE_V_WITH_MEMO;
				break;
			case SIG_FOXPRO:
				signature = SIG_FOXPRO_WITH_MEMO;
				break;
			}
		}
	}

	public boolean hasMemoFile() {
		return hasMemoFile;
	}

	public byte getLanguageDriver() {
		return languageDriver;
	}

	public void setLanguageDriver(byte driver) {
		languageDriver = driver;
	}
}
