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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import application.utils.General;

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

	private static final int DBASE_LEVEL_7 = 4;

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
	List<DBFField> fieldArray; /* each 32 or 48 bytes */
	byte terminator1; /* n+1 */
	/* DBF structure ends here */

	private boolean hasMemoFile = false;
	private String characterSet;

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
		int read = 32;

		if (isDB7()) {
			// Skip language drive name (32 bytes) and reserved (4 bytes)
			in.skipBytes(36);
			read += 36;
		}

		int fieldLen = isDB7() ? 48 : 32;

		fieldArray = new ArrayList<>();
		while (read <= headerLength - fieldLen) {
			DBFField field = DBFField.createField(in, isDB7());
			if (field != null) {
				fieldArray.add(field);
				read += fieldLen;
			} else {
				// field descriptor array terminator found
				read += 1;
				break;
			}
		}

		/* it might be required to leap to the start of records at times */
		int skip = headerLength - read;
		if (skip > 0) {
			in.skipBytes(skip);
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

		setCharacterSet();
	}

	public boolean isDB7() {
		return (signature & 0x7) == DBASE_LEVEL_7;
	}

	private void setCharacterSet() {
		// Check if the database uses a non ANSI characterset
		characterSet = "";
		Properties properties = General.getProperties("dBase");
		int test = Byte.toUnsignedInt(languageDriver);
		String result = properties.getProperty(Integer.toString(test), "");

		if (result.isEmpty()) {
			return;
		}

		String[] charset = result.split(",");
		String[] charSets = General.getCharacterSets();

		for (String cSet : charSets) {
			if (cSet.equalsIgnoreCase(charset[1])) {
				characterSet = cSet;
				break;
			}
		}
	}

	public String getCharacterSet() {
		return characterSet;
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
		if (isDB7()) {
			return (short) (48 + 48 * fieldArray.size() + 1);
		}
		return (short) (32 + 32 * fieldArray.size() + 1);
	}

	private short findRecordLength() {
		int recordLen = 0;
		for (DBFField element : fieldArray) {
			recordLen += element.getFieldLength();
		}
		return (short) (recordLen + 1);
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
			default:
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
		setCharacterSet();
	}
}
