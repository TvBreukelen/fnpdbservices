package dbengine;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import dbengine.utils.PilotHeader;

public abstract class PalmDB extends GeneralDB implements IConvert {
	/**
	 * Title: PilotFile Description: Generic PilotDB Export File Handler class
	 * Copyright: (c) 2003-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private int currentRecord = 0;
	private int appInfoId = 0;
	private long offsetPos = 78;
	protected int appInfoDataLength = 0;

	protected RandomAccessFile pdbRaf = null;
	protected ByteArrayOutputStream pdbBaos = new ByteArrayOutputStream();
	protected DataOutputStream pdbDas = new DataOutputStream(pdbBaos);
	protected static final String CODE_PAGE = "Cp1252";

	private PilotHeader header;

	protected PalmDB(Profiles pref) {
		super(pref);
	}

	@Override
	public void openFile(boolean isInputFile) throws Exception {
		File outFile = new File(myDatabase);

		if (isInputFile) {
			useAppend = false;
		} else {
			if (useAppend) {
				useAppend = outFile.exists();
			} else {
				outFile.delete();
			}
		}

		pdbRaf = new RandomAccessFile(outFile, "rw");
		header = new PilotHeader(pdbRaf);
	}

	@Override
	public String getPdaDatabase() {
		if (header != null) {
			return header.getPdaDatabase();
		}
		return "";
	}

	protected void createPalmDB(int totalRec) throws Exception {
		gotoRecord(0);
		appInfoId = 80 + totalRec * 8;

		header.setPdaDatabase(myPref.getPdaDatabaseName());
		header.setDbCreator(myExportFile.getCreator());
		header.setDbType(myExportFile.getDbType());
		header.setAppInfoId(appInfoId);
		header.setTotalRecords(totalRec);
		header.writeHeader();
	}

	@Override
	public void readTableContents() throws Exception {
		if (pdbRaf.length() == 0) {
			throw FNProgException.getException("noRecords", myDatabase);
		}

		String dbType = header.getDbType();
		String dbCreator = header.getDbCreator();
		appInfoId = header.getAppInfoId();
		totalRecords = header.getTotalRecords();

		if (isInputFile) {
			if (!dbType.equals(myImportFile.getDbType()) || !dbCreator.equals(myImportFile.getCreator())) {
				for (ExportFile exp : ExportFile.values()) {
					if (dbType.equals(exp.getDbType()) && dbCreator.equals(exp.getCreator())) {
						throw FNProgException.getException("differentDatabase", myDatabase, exp.getName(),
								myImportFile.getName());
					}
				}
				throw FNProgException.getException("invalidDatabaseID", myDatabase, myImportFile.getName(),
						myImportFile.getDbType() + myImportFile.getCreator(), dbType + dbCreator);
			}

			if (totalRecords <= 0) {
				throw FNProgException.getException("noRecords", myDatabase);
			}
		}

		int[] index = setPointer2NextRecord();
		appInfoDataLength = index[0] - appInfoId;
		gotoRecord(0);
		readAppInfo();
	}

	protected void appendPilotDB(int totalRec, List<FieldDefinition> newFields) throws Exception {
		readTableContents();
		compareNewFields(newFields);

		// Calculate new total records
		int oTotalRec = totalRecords;
		totalRecords += totalRec;
		int oAppInfoId = 80 + oTotalRec * 8;
		appInfoId = 80 + totalRecords * 8;
		gotoRecord(0);

		// Update header
		header.setAppInfoId(appInfoId);
		header.setPdaDatabase(myPref.getPdaDatabaseName());
		header.setTotalRecords(totalRecords);
		header.updateHeader();

		// get the number of bytes that we have to move
		int bytes2Move = (int) (pdbRaf.length() + 1 - oAppInfoId);
		int additionalSize = appInfoId - oAppInfoId;

		// Set old EOF to new EOF
		byte[] byteArray = new byte[additionalSize];
		pdbRaf.seek(pdbRaf.length());
		pdbRaf.write(byteArray);

		// Read old records at oAppInfoId and to move them to nAppInfoId
		byteArray = new byte[bytes2Move];
		pdbRaf.seek(oAppInfoId);
		pdbRaf.read(byteArray);
		pdbRaf.seek(appInfoId);
		pdbRaf.write(byteArray);

		// Recalculate record offsets in the Record List
		int offset;
		for (int i = 0; i < oTotalRec; i++) {
			pdbRaf.seek(offsetPos);
			offset = pdbRaf.readInt() + additionalSize;
			pdbRaf.seek(offsetPos);
			pdbRaf.writeInt(offset);
			offsetPos += 8;
		}
	}

	protected void gotoRecord(int record) {
		currentRecord = record;
		offsetPos = 78L + currentRecord * 8;
	}

	protected void skipBytes(int bytes) throws IOException {
		pdbRaf.skipBytes(bytes);
	}

	protected void readAppInfo(int offset, byte[] byteArray) throws IOException {
		long pointer = pdbRaf.getFilePointer();
		pdbRaf.seek((long) appInfoId + offset);
		pdbRaf.read(byteArray);
		pdbRaf.seek(pointer); // Set file pointer back to original state
	}

	protected void writeAppInfo(int offset, byte[] byteArray) throws IOException {
		long pointer = pdbRaf.getFilePointer();
		pdbRaf.seek((long) appInfoId + offset);
		pdbRaf.write(byteArray);
		pdbRaf.seek(pointer); // Set file pointer back to original state
	}

	protected void writeRecord(byte[] parm, int recordID) throws IOException {
		// Write record at end of file
		pdbRaf.seek(pdbRaf.length());
		int recordOffset = (int) pdbRaf.getFilePointer();
		pdbRaf.write(parm);

		// Write record offset position and recordID
		pdbRaf.seek(offsetPos);
		pdbRaf.writeInt(recordOffset);
		pdbRaf.writeInt(recordID);
		currentRecord++;
		offsetPos += 8;
	}

	/**
	 * Sets the file pointer to the next record in the Pilot DB
	 *
	 * @return int[3] where [0] = current record offset, [1] = recordID and [2] =
	 *         next record offset
	 * @throws Exception
	 */
	protected int[] setPointer2NextRecord() throws Exception {
		currentRecord++;
		if (currentRecord > totalRecords) {
			throw FNProgException.getException("noMatchFieldsDBHeader", myDatabase, Integer.toString(currentRecord),
					Integer.toString(totalRecords));
		}

		pdbRaf.seek(offsetPos);
		int[] recordID = new int[3];
		recordID[0] = pdbRaf.readInt();
		recordID[1] = pdbRaf.readInt();
		recordID[2] = pdbRaf.readInt();

		if (currentRecord == totalRecords) {
			recordID[2] = (int) pdbRaf.length();
		}

		pdbRaf.seek(recordID[0]);
		offsetPos += 8;
		return recordID;
	}

	protected void readLn(byte[] buffer) throws IOException {
		pdbRaf.read(buffer);
	}

	@Override
	public void closeFile() {
		try {
			pdbRaf.close();
		} catch (Exception e) {
			// Nothing that can be done about this
		}
		pdbRaf = null;
	}

	protected double getDouble(byte[] b) {
		long l = 0L;
		l += getUByte(b[0]);
		for (int i = 1; i < 8; i++) {
			l <<= 8;
			l += getUByte(b[i]);
		}
		return Double.longBitsToDouble(l);
	}

	protected byte[] getDouble(double d) {
		long l = Double.doubleToRawLongBits(d);
		byte[] result = new byte[8];

		result[0] = (byte) (int) ((l & 0xff00000000000000L) >> 56);
		result[1] = (byte) (int) ((l & 0xff000000000000L) >> 48);
		result[2] = (byte) (int) ((l & 0xff0000000000L) >> 40);
		result[3] = (byte) (int) ((l & 0xff00000000L) >> 32);
		result[4] = (byte) (int) ((l & 0xff000000L) >> 24);
		result[5] = (byte) (int) ((l & 0xff0000L) >> 16);
		result[6] = (byte) (int) ((l & 65280L) >> 8);
		result[7] = (byte) (int) (l & 255L);
		return result;
	}

	protected int getInt(byte[] b) {
		int i = 0;
		i += getUByte(b[0]);
		i <<= 8;
		i += getUByte(b[1]);
		i <<= 8;
		i += getUByte(b[2]);
		i <<= 8;
		i += getUByte(b[3]);
		return i;
	}

	protected byte[] getInt(int i) {
		byte[] result = new byte[4];
		result[0] = (byte) ((i & 0xff000000) >> 24);
		result[1] = (byte) ((i & 0xff0000) >> 16);
		result[2] = (byte) ((i & 0xff00) >> 8);
		result[3] = (byte) (i & 0xff);
		return result;
	}

	protected short getShort(byte[] b) {
		short s = 0;
		s += getUByte(b[0]);
		s <<= 8;
		s += getUByte(b[1]);
		return s;
	}

	protected short getUByte(byte b) {
		return b >= 0 ? b : (short) (256 + b);
	}

	protected abstract void readAppInfo() throws Exception;
}