package dbengine.utils;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.Duration;
import java.time.LocalDateTime;

import application.utils.General;

public class PilotHeader {
	private RandomAccessFile pilotRaf;
	private FileChannel pilotChannel;

	private String pdaDatabase;
	private int appInfoId;
	private String dbType;
	private String dbCreator;
	private int totalRecords;
	private static final int DB_NAME_LENGTH = 32;

	public PilotHeader(RandomAccessFile r) throws Exception {
		pilotRaf = r;
		pilotChannel = pilotRaf.getChannel();
		if (pilotRaf.length() > 0) {
			readHeader();
		}
	}

	private void readHeader() throws Exception {
		pilotRaf.seek(0);
		byte[] byteArray = new byte[32];
		pilotRaf.read(byteArray);
		pdaDatabase = new String(byteArray);
		pdaDatabase = pdaDatabase.substring(0, pdaDatabase.indexOf('\0'));
		pdaDatabase = pdaDatabase.trim();
		pilotRaf.seek(52);
		appInfoId = pilotRaf.readInt();
		byteArray = new byte[4];
		pilotRaf.seek(60);
		pilotRaf.read(byteArray);
		dbType = new String(byteArray);
		pilotRaf.read(byteArray);
		dbCreator = new String(byteArray);
		pilotRaf.skipBytes(8);
		totalRecords = pilotRaf.readShort();
	}

	public void writeHeader() throws Exception {
		FileLock pilotLock = pilotChannel.lock();
		pilotRaf.seek(0);
		pilotRaf.write(General.getNullTerminatedString(pdaDatabase, DB_NAME_LENGTH, "")); // byte[32] name
		pilotRaf.writeShort(8); // short attribute (Normal DB)
		pilotRaf.writeShort(0); // short version
		pilotRaf.writeInt(getPalmDate()); // int creationDate (current date)
		pilotRaf.writeInt(getPalmDate()); // int modificationDate (current date)
		pilotRaf.writeInt(0); // int lastBackupDate
		pilotRaf.writeInt(0); // int modificationNumber
		pilotRaf.writeInt(appInfoId); // int appInfoID
		pilotRaf.writeInt(0); // int sortInfoID
		pilotRaf.writeBytes(dbType); // byte[4] type
		pilotRaf.writeBytes(dbCreator); // byte[4] creator
		pilotRaf.writeInt(0); // int uniqueIDSeed
		pilotRaf.writeInt(0); // int nextRecordListID
		pilotRaf.writeShort(totalRecords); // short numRecords
		pilotRaf.write(new byte[2 + totalRecords * 8]); // Record list
		pilotChannel.force(true);
		pilotLock.release();
	}

	public void updateHeader() throws Exception {
		FileLock pilotLock = pilotChannel.lock();
		pilotRaf.seek(0);
		pilotRaf.write(General.getNullTerminatedString(pdaDatabase, DB_NAME_LENGTH, "")); // byte[32] name
		pilotRaf.seek(28);
		pilotRaf.writeInt(getPalmDate()); // int modificationDate (current date)
		pilotRaf.seek(52);
		pilotRaf.writeInt(appInfoId); // appInfoID
		pilotRaf.seek(68);
		pilotRaf.writeInt(totalRecords); // uniqueIDSeed
		pilotRaf.seek(76);
		pilotRaf.writeShort(totalRecords); // short numRecords
		pilotChannel.force(true);
		pilotLock.release();
	}

	/**
	 * Converts current time to a Palm date (no. of seconds since 01.01.1904)
	 */
	private int getPalmDate() {
		LocalDateTime palmDate = LocalDateTime.of(1904, 1, 1, 0, 0, 0);
		return (int) Duration.between(palmDate, LocalDateTime.now()).getSeconds();
	}

	public int getAppInfoId() {
		return appInfoId;
	}

	public void setAppInfoId(int appInfoId) {
		this.appInfoId = appInfoId;
	}

	public String getDbCreator() {
		return dbCreator;
	}

	public void setDbCreator(String dbCreator) {
		this.dbCreator = dbCreator;
	}

	public String getDbType() {
		return dbType;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public String getPdaDatabase() {
		return pdaDatabase;
	}

	public void setPdaDatabase(String pdaDatabase) {
		this.pdaDatabase = pdaDatabase;
	}

	public int getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}
}
