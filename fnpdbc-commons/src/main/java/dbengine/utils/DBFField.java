/*
 * DBFField Class represents a "field" (or column) definition of a DBF data structure.
 *
 * This file is part of JavaDBF packege.
 *
 * author: anil@linuxense.com license: LGPL (http://www.gnu.org/copyleft/lesser.html)
 *
 * $Id: DBFField.java,v 1.7 2004/03/31 10:50:11 anil Exp $
 */

package dbengine.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * DBFField represents a field specification in an dbf file.
 *
 * DBFField objects are either created and added to a DBFWriter object or
 * obtained from DBFReader object through getField( int) query.
 *
 */
public class DBFField {

	public static final byte FIELD_TYPE_C = (byte) 'C';
	public static final byte FIELD_TYPE_L = (byte) 'L';
	public static final byte FIELD_TYPE_I = (byte) 'I';
	public static final byte FIELD_TYPE_N = (byte) 'N';
	public static final byte FIELD_TYPE_F = (byte) 'F';
	public static final byte FIELD_TYPE_D = (byte) 'D';
	public static final byte FIELD_TYPE_M = (byte) 'M';
	public static final byte FIELD_TYPE_Y = (byte) 'Y'; // Foxpro currency format

	/* Field struct variables start here */
	private byte[] fieldName = new byte[11]; /* 0-10 */
	private byte dataType; /* 11 */
	private int reserv1; /* 12-15 */
	private int fieldLength; /* 16 */
	private byte decimalCount; /* 17 */
	private short reserv2; /* 18-19 */
	private byte workAreaId; /* 20 */
	private short reserv3; /* 21-22 */
	private byte setFieldsFlag; /* 23 */
	private byte[] reserv4 = new byte[7]; /* 24-30 */
	private byte indexFieldFlag; /* 31 */
	/* Field struct variables end here */

	/* other class variables */
	private int nameNullIndex = 0;

	/**
	 * Creates a DBFField object from the data read from the given DataInputStream.
	 *
	 * The data in the DataInputStream object is supposed to be organised correctly
	 * and the stream "pointer" is supposed to be positioned properly.
	 *
	 * @param in DataInputStream
	 * @return Returns the created DBFField object.
	 * @throws IOException If any stream reading problems occurs.
	 */
	protected static DBFField createField(DataInput in) throws IOException {
		DBFField field = new DBFField();
		byte tByte = in.readByte(); /* 0 */

		if (tByte == 10 || tByte == 13) {
			return null;
		}

		in.readFully(field.fieldName, 1, 10); /* 1-10 */
		field.fieldName[0] = tByte;

		for (int i = 0; i < field.fieldName.length; i++) {
			if (field.fieldName[i] == (byte) 0) {
				field.nameNullIndex = i;
				break;
			}
		}

		field.dataType = in.readByte(); /* 11 */
		field.reserv1 = Integer.reverseBytes(in.readInt()); /* 12-15 */
		field.fieldLength = in.readUnsignedByte(); /* 16 */
		field.decimalCount = in.readByte(); /* 17 */
		field.reserv2 = Short.reverseBytes(in.readShort()); /* 18-19 */
		field.workAreaId = in.readByte(); /* 20 */
		field.reserv3 = Short.reverseBytes(in.readShort()); /* 21-22 */
		field.setFieldsFlag = in.readByte(); /* 23 */
		in.readFully(field.reserv4); /* 24-30 */
		field.indexFieldFlag = in.readByte(); /* 31 */
		return field;
	}

	/**
	 * Writes the content of DBFField object into the stream as per DBF format
	 * specifications.
	 *
	 * @param os OutputStream
	 * @throws IOException if any stream related issues occur.
	 */
	protected void write(DataOutput out) throws IOException {
		// Field Name
		out.write(fieldName); /* 0-10 */
		out.write(new byte[11 - fieldName.length]);

		// data type
		out.writeByte(dataType); /* 11 */
		out.writeInt(reserv1); /* 12-15 */
		out.writeByte(fieldLength); /* 16 */
		out.writeByte(decimalCount); /* 17 */
		out.writeShort(reserv2); /* 18-19 */
		out.writeByte(workAreaId); /* 20 */
		out.writeShort(reserv3); /* 21-22 */
		out.writeByte(setFieldsFlag); /* 23 */
		out.write(reserv4); /* 24-30 */
		out.writeByte(indexFieldFlag); /* 31 */
	}

	/**
	 * Returns the name of the field.
	 *
	 * @return Name of the field as String.
	 */
	public String getName() {
		return new String(fieldName, 0, nameNullIndex);
	}

	/**
	 * Returns the data type of the field.
	 *
	 * @return Data type as byte.
	 */
	public byte getDataType() {
		return dataType;
	}

	/**
	 * Returns field length.
	 *
	 * @return field length as int.
	 */
	public int getFieldLength() {
		return fieldLength;
	}

	/**
	 * Returns the decimal part. This is applicable only if the field type if of
	 * numeric in nature.
	 *
	 * If the field is specified to hold integral values the value returned by this
	 * method will be zero.
	 *
	 * @return decimal field size as int.
	 */
	public int getDecimalCount() {
		return decimalCount;
	}

	/**
	 * Sets the name of the field.
	 *
	 * @param name of the field as String.
	 * @since 0.3.3.1
	 */
	public void setName(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Field name cannot be null");
		}

		if (value.length() == 0 || value.length() > 10) {
			throw new IllegalArgumentException("Field name should be of length 0-10");
		}

		fieldName = value.getBytes();
		nameNullIndex = fieldName.length;
	}

	/**
	 * Sets the data type of the field.
	 *
	 * @param type of the field. One of the following:<br>
	 *             C, L, I, N, F, D, M, Y
	 */
	public void setDataType(byte value) {
		switch (value) {
		case FIELD_TYPE_D:
			fieldLength = 8;
			break;
		case FIELD_TYPE_L:
			fieldLength = 1;
			break;
		case FIELD_TYPE_M:
			fieldLength = 10;
			break;
		case FIELD_TYPE_C:
		case FIELD_TYPE_I:
		case FIELD_TYPE_N:
		case FIELD_TYPE_F:
			break;
		case FIELD_TYPE_Y:
			fieldLength = 8;
			break;
		default:
			throw new IllegalArgumentException("Unknown data type");
		}
		dataType = value;
	}

	/**
	 * Length of the field. This method should be called before calling
	 * setDecimalCount().
	 *
	 * @param Length of the field as int.
	 */
	public void setFieldLength(int value) {
		if (dataType == FIELD_TYPE_D || dataType == FIELD_TYPE_L || dataType == FIELD_TYPE_Y) {
			// Proper values are set at SetDataType time
			return;
		}

		if (value <= 0) {
			throw new IllegalArgumentException("Field length should be a positive number");
		}

		fieldLength = value;
	}

	/**
	 * Sets the decimal place size of the field. Before calling this method the size
	 * of the field should be set by calling setFieldLength().
	 *
	 * @param Size of the decimal field.
	 */
	public void setDecimalCount(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("Decimal length should be a positive number");
		}

		if (value > fieldLength) {
			throw new IllegalArgumentException("Decimal length should be less than field length");
		}

		decimalCount = (byte) value;
	}
}
