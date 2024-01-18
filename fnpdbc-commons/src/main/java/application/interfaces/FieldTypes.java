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

	public boolean isSort() {
		return isSort;
	}

	public static final int MIN_MEMO_FIELD_LEN = 120;
}
