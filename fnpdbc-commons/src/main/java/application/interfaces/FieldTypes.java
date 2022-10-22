package application.interfaces;

public enum FieldTypes {
	/**
	 * Title: FieldTypes Description: Enums for internal Database fieldtypes
	 *
	 * @author Tom van Breukelen
	 */

	BIG_DECIMAL('b'), BOOLEAN('B'), CURRENCY('C'), DATE('D'), DURATION('t'), FLOAT('F'), FUSSY_DATE('d'), IMAGE('I'),
	LINKED('l'), LIST('L'), MEMO('M'), NUMBER('N'), TEXT(' '), THUMBNAIL('X'), TIME('T'), TIMESTAMP('S'), YEAR('Y'),
	UNKNOWN('U');

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

	public boolean isTextConvertable(ExportFile file) {
		switch (this) {
		case BOOLEAN:
			return file.isBooleanExport();
		case DATE:
			return file.isDateExport();
		case TIMESTAMP:
			return file.isTimestampExport();
		case DURATION:
			return file.isDurationExport();
		case TIME:
			return file.isTimeExport();
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
