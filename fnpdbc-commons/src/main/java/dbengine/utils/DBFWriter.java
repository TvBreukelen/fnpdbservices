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
import java.util.List;
import java.util.Map;

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
	public void setFields(List<DBFField> fields, byte signature) throws Exception {
		boolean hasMemoFields = false;
		if (header.fieldArray != null) {
			throw new Exception("FieldTypes has already been set");
		}

		if (fields == null || fields.isEmpty()) {
			throw new Exception("Should have at least one field");
		}

		for (DBFField field : fields) {
			if (field == null) {
				throw new Exception("Found a null Field");
			}

			if (!hasMemoFields && field.getDataType() == DBFField.FIELD_TYPE_M) {
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
	public void addRecord(Map<String, Object> values) throws Exception {
		if (values == null || values.isEmpty()) {
			throw new Exception("Empty record cannot be added as row");
		}

		raf.write((byte) ' ');
		for (DBFField field : header.fieldArray) { /* iterate throught fields */
			Object dbValue = values.get(field.getName());

			switch (field.getDataType()) {
			case DBFField.FIELD_TYPE_C:
				if (dbValue != null) {
					raf.write(Utils.textPadding(dbValue.toString(), characterSetName, field.getFieldLength()));
				} else {
					raf.write(Utils.textPadding("", characterSetName, field.getFieldLength()));
				}
				break;
			case DBFField.FIELD_TYPE_D:
				raf.writeBytes(dbValue != null ? General.convertDate((LocalDate) dbValue, General.sdInternalDate)
						: "        ");
				break;
			case DBFField.FIELD_TYPE_F:
			case DBFField.FIELD_TYPE_N:
				if (dbValue != null) {
					raf.write(Utils.numberFormating((Number) dbValue, field));
				} else {
					raf.write(Utils.textPadding(" ", "", field.getFieldLength(), Utils.ALIGN_RIGHT));
				}
				break;
			case DBFField.FIELD_TYPE_L:
				raf.write(Boolean.TRUE.equals(dbValue) ? (byte) 'T' : (byte) 'F');
				break;
			case DBFField.FIELD_TYPE_M:
				if (dbValue != null && !dbValue.toString().isEmpty()) {
					memo.writeMemo(dbValue.toString());
					if (header.signature == DBFHeader.SIG_FOXPRO_WITH_MEMO) {
						raf.writeInt(Integer.reverseBytes(memo.getNextMemoIndex()));
					} else {
						raf.write(General.getNullTerminatedString(Long.toString(memo.getNextMemoIndex()), 10,
								characterSetName));
					}
				} else {
					raf.writeBytes("          ".substring(0, field.getFieldLength()));
				}
				break;
			case DBFField.FIELD_TYPE_Y:
				if (dbValue != null) {
					raf.writeLong(Long.reverseBytes((long) ((Double) dbValue * 10000.0)));
				} else {
					raf.writeBytes("          ".substring(0, field.getFieldLength()));
				}
				break;
			default:
				throw new Exception("Unknown field type " + field.getDataType());
			}
		}
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
}
