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

	private String table = General.EMPTY_STRING;
	private int sqlType = 0;
	private boolean isExport = true;
	private boolean isHideTable = false;
	private boolean isRoleField = false;
	private boolean isComposed = false;
	private int indexValue = 0;
	private String indexField = General.EMPTY_STRING;

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

	public void setSize(Object obj) {
		if (obj == null) {
			return;
		}

		String s = obj.toString();
		setSize(Math.max(getSize(), s.length()));

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

			setDecimalPoint(Math.max(getDecimalPoint(), s.length() - index));
		}
	}

	public NumberFormat getNumberFormat() {
		if (getFieldType().isNumeric()) {
			NumberFormat result = NumberFormat.getNumberInstance();
			result.setMaximumFractionDigits(getDecimalPoint());
			result.setMinimumFractionDigits(0);
			return result;
		}

		return null;
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
		result.setFieldAlias(getFieldAlias());
		result.setFieldName(getFieldName());
		result.setFieldHeader(getFieldHeader());
		result.setFieldType(getFieldType());
		result.setOutputAsText(isOutputAsText());
		result.setNotNullable(isNotNullable());
		result.setAutoIncrement(isAutoIncrement());
		result.setPrimaryKey(isPrimaryKey());
		result.setDecimalPoint(getDecimalPoint());
		result.setSize(getSize());
		result.setExport(isExport);
		result.setContentsField(isComposed);
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
