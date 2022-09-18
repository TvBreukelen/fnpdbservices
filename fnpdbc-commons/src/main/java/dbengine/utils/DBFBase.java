/*
 * $Id: DBFBase.java,v 1.3 2004/03/31 15:59:40 anil Exp $ Serves as the base class of DBFReader and DBFWriter.
 *
 * @author: anil@linuxense.com
 *
 * Support for choosing implemented character Sets as suggested by Nick Voznesensky <darkers@mail.ru>
 */
/**
 * Base class for DBFReader and DBFWriter.
 */
package dbengine.utils;

public abstract class DBFBase {
	protected static final int END_OF_DATA = 0x1A;
	protected DBFHeader header;
	protected DBFMemo memo;

	/**
	 * Returns the number of fields in the DBF.
	 */
	public int getFieldCount() {
		return header.fieldArray.size();
	}

	/**
	 * Returns the number of records in the DBF.
	 */
	public int getRecordCount() {
		return header.numberOfRecords;
	}

	public DBFField getField(int i) {
		return header.fieldArray.get(i);
	}
}
