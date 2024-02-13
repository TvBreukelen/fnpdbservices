package application.interfaces;

public enum FieldTypes {
	/**
	 * Title: FieldTypes Description: Enums for internal Database fieldtypes
	 *
	 * @author Tom van Breukelen
	 */

	BIG_DECIMAL('b', true), BOOLEAN('B', true), CURRENCY('C', true), DATE('D', true), DURATION('t', true),
	FLOAT('F', true), FUSSY_DATE('d', true), IMAGE('I', false), LINKED('l', false), LIST('L', false), MEMO('M', false),
	NUMBER('N', true), TEXT(' ', true), THUMBNAIL('X', false), TIME('T', true), TIMESTAMP('S', true), YEAR('Y', true),
	UNKNOWN('U', false);

	private char typeID;
	private boolean isSort;

	FieldTypes(char typeID, boolean isSort) {
		this.typeID = typeID;
		this.isSort = isSort;
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
		return this == BOOLEAN || this == DATE || this == TIMESTAMP || this == DURATION || this == TIME;
	}

	public boolean isNumeric() {
		return this == BIG_DECIMAL || this == CURRENCY || this == FLOAT || this == NUMBER;
	}

	public boolean isSetFieldSize() {
		return this == TEXT || isNumeric();
	}

	public boolean isSort() {
		return isSort;
	}

	public static final int MIN_MEMO_FIELD_LEN = 120;
}
