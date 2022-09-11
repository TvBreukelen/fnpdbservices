package application.utils;

import java.text.NumberFormat;

import application.interfaces.FieldTypes;

public class FieldDefinition extends BasisField {
	/**
	 * Title: FieldDefinition Description: Helper Class for field -names, -types and
	 * -headers Copyright: (c) 2004-2011
	 *
	 * @author Tom van Breukelen
	 * @version 8.0
	 */
	private static final long serialVersionUID = 1031194870833449030L;

	private String table = "";
	private int size = 1;
	private int decimalPoint = 0;
	private int sqlType = 0;
	private boolean isExport = true;
	private boolean isHideTable = false;
	private boolean isRoleField = false;
	private boolean isComposed = false;
	private int indexValue = 0;
	private String indexField = "";

	private FieldDefinition() {
		// Visible for cloning only
	}

	public FieldDefinition(String name, String header, FieldTypes type) {
		super(name, header, name, type);
	}

	public FieldDefinition(String name, FieldTypes type, boolean isExport) {
		super(name, name, name, type);
		setExport(isExport);
	}

	public FieldDefinition(BasisField field) {
		super(field);
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setSize(Object obj) {
		if (obj == null) {
			return;
		}

		String s = obj.toString();
		size = Math.max(size, s.length());

		if (getFieldType() != FieldTypes.BIG_DECIMAL && getFieldType() != FieldTypes.FLOAT) {
			return;
		}

		int index = s.lastIndexOf('.');
		if (++index != -1) {
			String s1 = s.substring(index);
			for (int i = s1.length() - 1; i > -1; i--) {
				if (s1.charAt(i) == '0') {
					index++;
				} else {
					break;
				}
			}

			setDecimalPoint(Math.max(decimalPoint, s.length() - index));
		}
	}

	public int getDecimalPoint() {
		return decimalPoint;
	}

	public NumberFormat getNumberFormat() {
		if (getFieldType().isNumeric()) {
			NumberFormat result = NumberFormat.getNumberInstance();
			result.setMaximumFractionDigits(decimalPoint);
			result.setMinimumFractionDigits(0);
			return result;
		}

		return null;
	}

	public void setDecimalPoint(int decimalPoint) {
		this.decimalPoint = decimalPoint;
	}

	public boolean isExport() {
		return isExport;
	}

	public void setExport(boolean isExport) {
		this.isExport = isExport;
	}

	public boolean isHideTable() {
		return isHideTable;
	}

	public void setHideTable(boolean isHideTable) {
		this.isHideTable = isHideTable;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
		if (table.equals(getFieldAlias())) {
			isHideTable = true;
		}
	}

	public int getIndexValue() {
		return indexValue;
	}

	public void setIndexValue(int indexValue) {
		this.indexValue = indexValue;
	}

	public String getIndexField() {
		return indexField;
	}

	public void setIndexField(String indexField) {
		this.indexField = indexField;
	}

	public boolean isRoleField() {
		return isRoleField;
	}

	public void setRoleField(boolean isRoleField) {
		this.isRoleField = isRoleField;
	}

	public boolean isContentsField() {
		return isComposed;
	}

	public void setContentsField(boolean isContentsField) {
		isComposed = isContentsField;
	}

	public FieldDefinition copy() {
		FieldDefinition result = new FieldDefinition();
		result.setDecimalPoint(decimalPoint);
		result.setExport(isExport);
		result.setFieldAlias(getFieldAlias());
		result.setFieldHeader(getFieldHeader());
		result.setFieldName(getFieldName());
		result.setFieldType(getFieldType());
		result.setContentsField(isComposed);
		result.setSize(size);
		result.setTable(table);
		result.setHideTable(isHideTable);
		result.setRoleField(isRoleField);
		result.setIndexField(indexField);
		result.setIndexValue(indexValue);
		result.setSQLType(sqlType);
		return result;
	}

	public int getSQLType() {
		return sqlType;
	}

	public void setSQLType(int sqlType) {
		this.sqlType = sqlType;
	}
}
