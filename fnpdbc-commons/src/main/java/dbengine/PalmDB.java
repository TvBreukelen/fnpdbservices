package dbengine;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FNProgException;
import dbengine.utils.PilotHeader;

public abstract class PalmDB extends GeneralDB implements IConvert {
	/**
	 * Title: PilotFile Description: Generic PilotDB Export File Handler class
	 * Copyright: (c) 2003-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private int recordNo = 0;
	private int appInfoId = 0;
	private long offsetPos = 78;
	protected int appInfoDataLength = 0;
	private int currentRecord = 0;

	protected RandomAccessFile pdbRaf = null;
	protected ByteArrayOutputStream pdbBaos = new ByteArrayOutputStream();
	protected DataOutputStream pdbDas = new DataOutputStream(pdbBaos);

	private PilotHeader header;

	protected PalmDB(Profiles pref) {
		super(pref);
	}

	@Override
	public void openFile(boolean isInputFile) throws Exception {
		dbRecords.clear();
		File outFile = new File(myDatabase);
		pdbRaf = new RandomAccessFile(outFile, "rw");
		header = new PilotHeader(pdbRaf);
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

	protected void gotoRecord(int record) {
		recordNo = record;
		offsetPos = 78L + recordNo * 8;
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

	/**
	 * Sets the file pointer to the next record in the Pilot DB
	 *
	 * @return int[3] where [0] = current record offset, [1] = recordID and [2] =
	 *         next record offset
	 * @throws Exception
	 */
	protected int[] setPointer2NextRecord() throws Exception {
		recordNo++;
		if (recordNo > totalRecords) {
			throw FNProgException.getException("noMatchFieldsDBHeader", myDatabase, Integer.toString(recordNo),
					Integer.toString(totalRecords));
		}

		pdbRaf.seek(offsetPos);
		int[] recordID = new int[3];
		recordID[0] = pdbRaf.readInt();
		recordID[1] = pdbRaf.readInt();
		recordID[2] = pdbRaf.readInt();

		if (recordNo == totalRecords) {
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
	public Map<String, Object> readRecord() throws Exception {
		return dbRecords.get(currentRecord++);
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

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		// Not used
		return 0;
	}

}