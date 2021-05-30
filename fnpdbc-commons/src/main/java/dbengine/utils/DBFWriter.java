/*
 * DBFWriter Class for defining a DBF structure and addin data to that structure and finally writing it to an OutputStream.
 *
 * This file is part of JavaDBF packege.
 *
 * author: anil@linuxense.com license: LGPL (http://www.gnu.org/copyleft/lesser.html)
 *
 * $Id: DBFWriter.java,v 1.9 2004/03/31 10:57:16 anil Exp $
 */
package dbengine.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.time.LocalDate;

import application.utils.FNProgException;
import application.utils.General;

/**
 * An object of this class can create a DBF file.
 *
 * Create an object, <br>
 * then define fields by creating DBFField objects and<br>
 * add them to the DBFWriter object<br>
 * add records using the addRecord() method and then<br>
 * call write() method.
 */
public class DBFWriter extends DBFBase {
	private int recordCount = 0;
	RandomAccessFile raf = null; /* Open and append records to an existing DBF */

	/**
	 * Creates a DBFWriter which can append to records to an existing DBF file.
	 *
	 * @param dbfFile. The file passed in should be a valid DBF file.
	 * @exception Throws DBFException if the passed in file does exist but not a
	 *                   valid DBF file, or if an IO error occurs.
	 */
	public DBFWriter(File dbfFile, File dbtFile) throws Exception {
		raf = new RandomAccessFile(dbfFile, "rw");

		/*
		 * before proceeding check whether the passed in File object is an
		 * empty/non-existent file or not.
		 */
		if (!dbfFile.exists() || dbfFile.length() == 0) {
			header = new DBFHeader();
			memo = new DBFMemo(dbtFile, header);
			return;
		}

		header = new DBFHeader();
		header.read(raf);

		if (header.signature == DBFHeader.SIG_VISUAL_FOXPRO_WITH_MEMO) {
			throw FNProgException.getException("visualFoxproError");
		}

		// Open memo file, if memo fields are defined
		if (header.hasMemoFile()) {
			memo = new DBFMemo(dbtFile, header);
			memo.openMemoFile();
		}

		/* position file pointer at the end of the raf */
		raf.seek(raf.length() - 1 /* to ignore the END_OF_DATA byte at EoF */);

		recordCount = header.numberOfRecords;
	}

	/**
	 * Sets fields for new DBF files only!
	 */
	public void setFields(DBFField[] fields, byte signature) throws Exception {
		boolean hasMemoFields = false;
		if (header.fieldArray != null) {
			throw new Exception("FieldTypes has already been set");
		}

		if (fields == null || fields.length == 0) {
			throw new Exception("Should have at least one field");
		}

		for (int i = 0; i < fields.length; i++) {
			if (fields[i] == null) {
				throw new Exception("Field " + (i + 1) + " is null");
			}

			if (!hasMemoFields && fields[i].getDataType() == DBFField.FIELD_TYPE_M) {
				hasMemoFields = true;
			}
		}

		header.fieldArray = fields;
		header.setSignature(hasMemoFields, signature);

		if (hasMemoFields) {
			memo.openMemoFile();
		}

		/*
		 * this is a new/non-existent file. So write header before proceeding and if
		 * there are memo fields to process then write the memo file header
		 */
		header.write(raf);
		if (hasMemoFields) {
			memo.writeMemoHeader();
		}
	}

	/**
	 * Add a record.
	 */
	public void addRecord(Object[] values) throws Exception {
		if (values == null) {
			throw new Exception("Null cannot be added as row");
		}

		if (values.length != header.fieldArray.length) {
			throw new Exception("Invalid record. Invalid number of fields in row");
		}

		for (int i = 0; i < header.fieldArray.length; i++) {
			if (values[i] == null) {
				continue;
			}

			if (values[i].equals("")) {
				values[i] = null;
				continue;
			}

			switch (header.fieldArray[i].getDataType()) {
			case DBFField.FIELD_TYPE_M:
			case DBFField.FIELD_TYPE_C:
				if (!(values[i] instanceof String)) {
					throw new Exception("Invalid value '" + values[i].toString() + "' for text field "
							+ header.fieldArray[i].getName());
				}
				break;
			case DBFField.FIELD_TYPE_L:
				if (!(values[i] instanceof Boolean)) {
					throw new Exception("Invalid value '" + values[i].toString() + "' for boolean field "
							+ header.fieldArray[i].getName());
				}
				break;
			case DBFField.FIELD_TYPE_D:
				if (!(values[i] instanceof LocalDate)) {
					throw new Exception("Invalid value '" + values[i].toString() + "' for date field "
							+ header.fieldArray[i].getName());
				}
				break;
			case DBFField.FIELD_TYPE_F:
			case DBFField.FIELD_TYPE_N:
			case DBFField.FIELD_TYPE_Y:
				if (!(values[i] instanceof Number)) {
					throw new Exception("Invalid value '" + values[i].toString() + "' for numeric field "
							+ header.fieldArray[i].getName());
				}
			default:
				break;
			}
		}

		writeRecord(values);
		recordCount++;
	}

	public void closeDBFFile() throws Exception {
		// Update the header for record count and the END_OF_DATA mark and close the
		// memo file
		header.numberOfRecords = recordCount;
		raf.seek(0);
		header.write(raf);
		raf.seek(raf.length());
		raf.writeByte(END_OF_DATA);
		raf.close();
		memo.closeMemoFile();
	}

	private void writeRecord(Object[] objectArray) throws Exception {
		raf.write((byte) ' ');
		for (int j = 0; j < getFieldCount(); j++) { /* iterate throught fields */
			switch (header.fieldArray[j].getDataType()) {
			case DBFField.FIELD_TYPE_C:
				if (objectArray[j] != null) {
					String str_value = objectArray[j].toString();
					raf.write(Utils.textPadding(str_value, characterSetName, header.fieldArray[j].getFieldLength()));
				} else {
					raf.write(Utils.textPadding("", characterSetName, header.fieldArray[j].getFieldLength()));
				}
				break;
			case DBFField.FIELD_TYPE_D:
				raf.writeBytes(
						objectArray[j] != null ? General.convertDate((LocalDate) objectArray[j], General.sdInternalDate)
								: "        ");
				break;
			case DBFField.FIELD_TYPE_F:
			case DBFField.FIELD_TYPE_N:
				if (objectArray[j] != null) {
					raf.write(Utils.numberFormating((Number) objectArray[j], header.fieldArray[j]));
				} else {
					raf.write(Utils.textPadding(" ", "", header.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
				}
				break;
			case DBFField.FIELD_TYPE_L:
				if (objectArray[j] != null) {
					if ((Boolean) objectArray[j]) {
						raf.write((byte) 'T');
					} else {
						raf.write((byte) 'F');
					}
				} else {
					raf.write((byte) '?');
				}
				break;
			case DBFField.FIELD_TYPE_M:
				if (objectArray[j] != null && objectArray[j].toString().length() != 0) {
					memo.writeMemo(objectArray[j].toString());
					if (header.signature == DBFHeader.SIG_FOXPRO_WITH_MEMO) {
						raf.writeInt(Integer.reverseBytes(memo.getNextMemoIndex()));
					} else {
						raf.write(General.getNullTerminatedString(Long.toString(memo.getNextMemoIndex()), 10,
								characterSetName));
					}
				} else {
					raf.writeBytes("          ".substring(0, header.fieldArray[j].getFieldLength()));
				}
				break;
			case DBFField.FIELD_TYPE_Y:
				if (objectArray[j] != null) {
					raf.writeLong(Long.reverseBytes((long) ((Double) objectArray[j] * 10000.0)));
				} else {
					raf.writeBytes("          ".substring(0, header.fieldArray[j].getFieldLength()));
				}
				break;
			default:
				throw new Exception("Unknown field type " + header.fieldArray[j].getDataType());
			}
		}
	}
}
