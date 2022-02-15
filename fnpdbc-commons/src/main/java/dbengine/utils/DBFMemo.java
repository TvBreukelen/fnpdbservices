package dbengine.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import application.utils.FNProgException;
import application.utils.General;

public class DBFMemo extends DBFBase {
	private File memoFile;
	private RandomAccessFile memoRaf;
	private ByteArrayOutputStream memoBaos = new ByteArrayOutputStream();
	private DataOutputStream memoDas = new DataOutputStream(memoBaos);
	private int memoBlockSize = 512;
	private int nextMemoIndex = 1;

	public DBFMemo(File memoFile, DBFHeader header) {
		this.memoFile = memoFile;
		this.header = header;
	}

	public void openMemoFile() throws Exception {
		checkMemoFileFormat();
		memoRaf = new RandomAccessFile(memoFile, "rw");
		if (memoFile.exists() && memoRaf.length() > 0) {
			getMemoBlockSize();
			readNextMemoIndex();
		}
	}

	public void closeMemoFile() {
		if (memoRaf != null) {
			try {
				memoRaf.close();
			} catch (Exception e) {
				// Nothing to do
			}
			memoRaf = null;
		}
	}

	private void checkMemoFileFormat() throws Exception {
		String ext = memoFile.getName().toUpperCase();
		ext = ext.substring(ext.lastIndexOf('.') + 1);
		StringBuilder buf = new StringBuilder(memoFile.getAbsolutePath());
		buf.delete(buf.lastIndexOf("."), buf.length());
		buf.append(".dbf");

		switch (header.signature) {
		case DBFHeader.SIG_DBASE_III_WITH_MEMO:
		case DBFHeader.SIG_DBASE_IV_WITH_MEMO:
		case DBFHeader.SIG_DBASE_V_WITH_MEMO:
			if (!ext.equals("DBT")) {
				throw FNProgException.getException("foxproNotDbase", buf.toString());
			}
			break;
		case DBFHeader.SIG_FOXPRO_WITH_MEMO:
		case DBFHeader.SIG_VISUAL_FOXPRO_WITH_MEMO:
			if (!ext.equals("FPT")) {
				throw FNProgException.getException("dbaseNotFoxpro", buf.toString());
			}
			break;
		default:
			throw FNProgException.getException("unknownXbase", buf.toString());
		}
	}

	private void getMemoBlockSize() throws Exception {
		switch (header.signature) {
		case DBFHeader.SIG_DBASE_III_WITH_MEMO:
			// Memo Block size is fixed to 512 bytes
			break;
		case DBFHeader.SIG_DBASE_IV_WITH_MEMO:
		case DBFHeader.SIG_DBASE_V_WITH_MEMO:
			byte[] buf = new byte[4];
			memoRaf.seek(20);
			memoRaf.read(buf);
			memoBlockSize = General.intLittleEndian(buf);
			break;
		case DBFHeader.SIG_FOXPRO_WITH_MEMO:
		case DBFHeader.SIG_VISUAL_FOXPRO_WITH_MEMO:
			memoRaf.seek(6);
			int foxproBlockSize = memoRaf.readShort();

			if (foxproBlockSize == 0) {
				memoBlockSize = 1;
			} else {
				if (foxproBlockSize < 33) {
					memoBlockSize = foxproBlockSize * 512;
				} else {
					memoBlockSize = foxproBlockSize;
				}
			}
		default:
			break;
		}
	}

	public int getNextMemoIndex() {
		return nextMemoIndex;
	}

	public String readMemo(int number) throws Exception {
		if (number == 0) {
			return null;
		}

		memoRaf.seek((long) number * memoBlockSize);
		ByteArrayOutputStream mBaos = new ByteArrayOutputStream();
		byte[] buf = new byte[4];
		int len = 0;

		switch (header.signature) {
		case DBFHeader.SIG_DBASE_III_WITH_MEMO:
			while (true) {
				int b = memoRaf.read();
				if (b == 0x01a || b == -1) {
					break;
				}
				mBaos.write((byte) b);
			}
			return characterSetName.equals("") ? new String(mBaos.toByteArray())
					: new String(mBaos.toByteArray(), characterSetName);
		case DBFHeader.SIG_DBASE_IV_WITH_MEMO:
		case DBFHeader.SIG_DBASE_V_WITH_MEMO:
			memoRaf.read(buf); // ignore "FFh FFh 08h 00h"
			memoRaf.read(buf); // return memo size
			len = General.intLittleEndian(buf) - 8; // Subtract 8, because the length included the previous two fields
			break;
		case DBFHeader.SIG_FOXPRO_WITH_MEMO:
		case DBFHeader.SIG_VISUAL_FOXPRO_WITH_MEMO:
			if (memoRaf.readInt() != 1) {
				return null; // Memo doesn't contain texts
			}
			len = memoRaf.readInt();
		default:
			break;
		}

		buf = new byte[len];
		memoRaf.read(buf); // returns memo data
		return characterSetName.equals("") ? new String(buf).trim() : new String(buf, characterSetName).trim();
	}

	private void readNextMemoIndex() throws IOException {
		byte[] buf = new byte[4];
		memoRaf.seek(0);
		memoRaf.readFully(buf);
		nextMemoIndex = General.intLittleEndian(buf);
	}

	public void writeMemoHeader() throws IOException {
		memoRaf.seek(0);
		memoRaf.write(new byte[memoBlockSize < 512 ? 512 : memoBlockSize]);

		memoRaf.seek(0);
		switch (header.signature) {
		case DBFHeader.SIG_DBASE_III_WITH_MEMO:
		case DBFHeader.SIG_DBASE_IV_WITH_MEMO:
		case DBFHeader.SIG_DBASE_V_WITH_MEMO:
			memoRaf.writeInt(Integer.reverseBytes(nextMemoIndex));
			memoRaf.seek(16);
			memoRaf.writeByte(header.signature == DBFHeader.SIG_DBASE_III_WITH_MEMO ? 3 : 4);
			memoRaf.seek(20);
			memoRaf.writeInt(Integer.reverseBytes(memoBlockSize));

			if (header.signature == DBFHeader.SIG_DBASE_III_WITH_MEMO) {
				// Not really necessary
				break;
			}

			String dbFile = memoFile.getName().toUpperCase();
			dbFile = dbFile.substring(0, dbFile.lastIndexOf('.'));
			memoRaf.seek(8);
			memoRaf.writeBytes(dbFile);
			break;
		case DBFHeader.SIG_FOXPRO_WITH_MEMO:
		case DBFHeader.SIG_VISUAL_FOXPRO_WITH_MEMO:
			memoBlockSize = 64;
			nextMemoIndex = 8;
			memoRaf.writeInt(nextMemoIndex);
			memoRaf.seek(6);
			memoRaf.writeShort(64);
		default:
			break;
		}
	}

	public void writeMemo(String memo) throws IOException {
		memoRaf.seek(0);
		byte[] buf = new byte[4];

		switch (header.signature) {
		case DBFHeader.SIG_FOXPRO_WITH_MEMO:
		case DBFHeader.SIG_VISUAL_FOXPRO_WITH_MEMO:
			nextMemoIndex = memoRaf.readInt();
			memoDas.writeInt(1);
			memoDas.writeInt(memo.length());
		default:
			memoRaf.readFully(buf);
			nextMemoIndex = General.intLittleEndian(buf);

			if (header.signature == DBFHeader.SIG_DBASE_IV_WITH_MEMO
					|| header.signature == DBFHeader.SIG_DBASE_V_WITH_MEMO) {
				buf = new byte[4];
				buf[0] = (byte) 0xFF;
				buf[1] = (byte) 0xFF;
				buf[2] = (byte) 0x08;
				memoDas.write(buf);
				memoDas.writeInt(Integer.reverseBytes(8 + memo.length()));
			}
		}

		memoDas.write(General.convertString2Bytes(memo, characterSetName));
		memoDas.write(END_OF_DATA);
		memoDas.write(END_OF_DATA);

		// Fill the memosize block
		while (memoBaos.size() % memoBlockSize != 0) {
			memoDas.write(0);
		}

		// Update header
		memoRaf.seek(0);
		int offset = nextMemoIndex + memoBaos.size() / memoBlockSize;
		memoRaf.writeInt(Integer.reverseBytes(offset));

		// Write memo
		memoRaf.seek((long) nextMemoIndex * memoBlockSize);
		memoRaf.write(memoBaos.toByteArray());
		memoBaos.reset();
	}
}
