package application.interfaces;

public enum FieldTypes {
	/**
	 * Title: FieldTypes Description: Enums for internal Database fieldtypes
	 *
	 * @author Tom van Breukelen
	 */

	BOOLEAN('B'), CURRENCY('C'), DATE('D'), DURATION('t'), FLOAT('F'), FUSSY_DATE('d'), IMAGE('I'), LINKED('l'),
	LIST('L'), MEMO('M'), NUMBER('N'), TEXT(' '), THUMBNAIL('X'), TIME('T'), TIMESTAMP('S'), UNKNOWN('U');

	private char typeID;

	private FieldTypes(char type) {
		typeID = type;
	}

	public static FieldTypes getField(char id) {
		for (FieldTypes field : values()) {
			if (field.typeID == id) {
				return field;
			}
		}
		return FieldTypes.TEXT;
	}

	public boolean isTextConvertable() {
		switch (this) {
		case BOOLEAN:
		case DATE:
		case DURATION:
		case TIME:
		case TIMESTAMP:
			return true;
		default:
			return false;
		}
	}

	public static final int MIN_MEMO_FIELD_LEN = 120;
}
