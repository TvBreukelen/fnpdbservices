/*
 * $Id: DBFBase.java,v 1.3 2004/03/31 15:59:40 anil Exp $ Serves as the base class of DBFReader adn DBFWriter.
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
	protected String characterSetName = "8859_1";
	protected static final int END_OF_DATA = 0x1A;
	protected DBFHeader header;
	protected DBFMemo memo;

	/*
	 * If the library is used in a non-latin environment use this method to set
	 * corresponding character set. More information:
	 * http://www.iana.org/assignments/character-sets Also see the documentation of
	 * the class java.nio.charset.Charset
	 */
	public String getCharactersetName() {
		return characterSetName;
	}

	public void setCharactersetName(String characterSet) {
		characterSetName = characterSet;
		if (!(this instanceof DBFMemo) && memo != null) {
			memo.setCharactersetName(characterSet);
		}
	}

	public int getFieldCount() throws Exception {
		return header.fieldArray.length;
	}
	
	public DBFField getFields(int i) {
		return header.fieldArray[i];
	}
	
}
