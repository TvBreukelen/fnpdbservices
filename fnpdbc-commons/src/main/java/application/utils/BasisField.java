package application.utils;

import java.io.Serializable;

import application.interfaces.FieldTypes;

public class BasisField implements Serializable {
	private static final long serialVersionUID = 4369830344673297986L;

	private String fieldAlias;
	private String fieldName;
	private String fieldHeader;
	private boolean outputAsText;
	private boolean isNotNullable;
	private boolean isPrimaryKey;
	private boolean isAutoIncrement;
	private boolean isUnique;

	private FieldTypes fieldType;

	public BasisField() {
	}

	public BasisField(BasisField field) {
		set(field);
	}

	public BasisField(String alias, String name, String header, FieldTypes type) {
		fieldAlias = alias;
		fieldName = name;
		fieldHeader = header;
		fieldType = type;
	}

	public String getFieldAlias() {
		return fieldAlias;
	}

	public void setFieldAlias(String alias) {
		fieldAlias = alias;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String name) {
		fieldName = name;
	}

	public String getFieldHeader() {
		return fieldHeader;
	}

	public void setFieldHeader(String header) {
		fieldHeader = header;
	}

	public FieldTypes getFieldType() {
		return fieldType;
	}

	public void setFieldType(FieldTypes type) {
		fieldType = type;
	}

	public boolean isOutputAsText() {
		return outputAsText;
	}

	public void setOutputAsText(boolean outputAsText) {
		this.outputAsText = outputAsText;
	}

	public boolean isNotNullable() {
		return isNotNullable;
	}

	public void setNotNullable(boolean isNotNullable) {
		this.isNotNullable = isNotNullable;
	}

	public boolean isAutoIncrement() {
		return isAutoIncrement;
	}

	public void setAutoIncrement(boolean isAutoIncrement) {
		this.isAutoIncrement = isAutoIncrement;
	}

	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}

	public void setPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}

	public boolean isUnique() {
		return isUnique;
	}

	public void setUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}

	public void set(BasisField field) {
		setFieldAlias(field.getFieldAlias());
		setFieldName(field.getFieldName());
		setFieldHeader(field.getFieldHeader());
		setFieldType(field.getFieldType());
		setOutputAsText(field.isOutputAsText());
		setNotNullable(field.isNotNullable());
		setAutoIncrement(field.isAutoIncrement());
		setPrimaryKey(field.isPrimaryKey());
		setUnique(field.isUnique());
	}

	@Override
	public String toString() {
		return getFieldAlias();
	}
}
