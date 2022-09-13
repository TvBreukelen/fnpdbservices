package application.interfaces;

public enum FieldTypes {
	/**
	 * Title: FieldTypes Description: Enums for internal Database fieldtypes
	 *
	 * @author Tom van Breukelen
	 */

	BIG_DECIMAL('b'), BOOLEAN('B'), CURRENCY('C'), DATE('D'), DATE_TIME_OFFSET('O'), DURATION('t'), FLOAT('F'),
	FUSSY_DATE('d'), IMAGE('I'), LINKED('l'), LIST('L'), MEMO('M'), NUMBER('N'), TEXT(' '), THUMBNAIL('X'), TIME('T'),
	TIMESTAMP('S'), YEAR('Y'), UNKNOWN('U');

	private char typeID;

	FieldTypes(char type) {
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

	public boolean isTextConvertable(boolean isTimeExport) {
		switch (this) {
		case BOOLEAN:
		case DATE:
			return true;
		case DATE_TIME_OFFSET:
		case DURATION:
		case TIME:
		case TIMESTAMP:
			return isTimeExport;
		default:
			return false;
		}
	}

	public boolean isSetFieldSize() {
		switch (this) {
		case BIG_DECIMAL:
		case CURRENCY:
		case FLOAT:
		case NUMBER:
		case TEXT:
			return true;
		default:
			return false;
		}
	}

	public boolean isNumeric() {
		switch (this) {
		case BIG_DECIMAL:
		case FLOAT:
		case NUMBER:
		case CURRENCY:
			return true;
		default:
			return false;
		}
	}

	public static final int MIN_MEMO_FIELD_LEN = 120;
}
