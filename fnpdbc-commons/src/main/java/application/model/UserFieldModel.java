package application.model;

import java.util.ArrayList;
import java.util.List;

import application.interfaces.ExportFile;
import application.utils.BasisField;
import application.utils.GUIFactory;
import application.utils.General;

public class UserFieldModel extends HiddenColumnModel {
	private static final long serialVersionUID = -3745524495569802922L;

	public static final int COL_IMPORT_FIELD = 0;
	public static final int COL_TYPE = 1;
	public static final int COL_EXPORT_FIELD = 2;
	public static final int COL_TEXT_EXPORT = 3;
	public static final int COL_NOT_NULL = 4;
	public static final int COL_PRIMARY_KEY = 5;
	public static final int COL_AUTO_INCREMENT = 6;
	public static final int COL_UNIQUE = 7;

	private List<BasisField> tableData = new ArrayList<>();
	private String[] columnNames = GUIFactory.getArray("exportHeaders");
	private ExportFile exportFile;
	private boolean hasTextExport;
	private boolean isPkSet = false;

	public UserFieldModel(ExportFile exp) {
		super(COL_UNIQUE + 1);
		setOutputFile(exp);
	}

	public void setOutputFile(ExportFile exp) {
		exportFile = exp;

		// Reset text fields
		tableData.forEach(e -> e.setOutputAsText(false));
		resetColumnVisibility();
	}

	public void setTableData(List<BasisField> tableData, boolean isTextExport) {
		if (tableData != null) {
			this.tableData = tableData;

			// PostgresSQL allows only one primary key column
			if (exportFile == ExportFile.POSTGRESQL) {
				List<BasisField> pkFields = tableData.stream().filter(BasisField::isPrimaryKey).toList();
				isPkSet = !pkFields.isEmpty();
				if (pkFields.size() > 1) {
					for (int i = 1; i < pkFields.size(); i++) {
						pkFields.get(i).setPrimaryKey(false);
					}
				}
			}

			hasTextExport = isTextExport;
			resetColumnVisibility();
		}
	}

	private void resetColumnVisibility() {
		boolean enableSql = exportFile.isSqlDatabase();

		columnsVisible[COL_TEXT_EXPORT] = hasTextExport;
		columnsVisible[COL_NOT_NULL] = enableSql;
		columnsVisible[COL_PRIMARY_KEY] = enableSql;
		columnsVisible[COL_AUTO_INCREMENT] = enableSql;
		columnsVisible[COL_UNIQUE] = enableSql;

		visibleColumns = 0;
		for (boolean element : columnsVisible) {
			if (element) {
				visibleColumns++;
			}
		}

		fireTableStructureChanged();
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[getNumber(col)];
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		switch (getNumber(col)) {
		case COL_TEXT_EXPORT:
			return getValueAt(row, col) != null;
		case COL_EXPORT_FIELD, COL_NOT_NULL, COL_PRIMARY_KEY, COL_UNIQUE:
			return true;
		case COL_AUTO_INCREMENT:
			return getValueAt(row, COL_TYPE).toString().equals("Number");
		default:
			return false;
		}
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return getNumber(col) > 2 ? Boolean.class : Object.class;
	}

	@Override
	public int getRowCount() {
		return tableData.size();
	}

	public void removeRecords(int[] rows) {
		List<BasisField> vDelete = new ArrayList<>();
		for (int i : rows) {
			vDelete.add(tableData.get(i));

		}
		tableData.removeAll(vDelete);
		fireTableDataChanged();
	}

	public void addRecord(BasisField field, int row) {
		BasisField obj = new BasisField(field);
		if (exportFile.isSqlDatabase()) {
			// No dot allowed in the field name
			obj.setFieldHeader(obj.getFieldHeader().replace(".", General.EMPTY_STRING));
		}
		tableData.add(row, obj);
		fireTableDataChanged();
	}

	@Override
	public Object getValueAt(int row, int col) {
		BasisField field = tableData.get(row);
		switch (getNumber(col)) {
		case COL_IMPORT_FIELD:
			return field.getFieldAlias();
		case COL_TYPE:
			String text = field.getFieldType().toString();
			return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
		case COL_EXPORT_FIELD:
			return field.getFieldHeader();
		case COL_TEXT_EXPORT:
			return field.getFieldType().isTextConvertable() ? field.isOutputAsText() : null;
		case COL_NOT_NULL:
			return field.isNotNullable();
		case COL_PRIMARY_KEY:
			return field.isPrimaryKey();
		case COL_AUTO_INCREMENT:
			return field.isAutoIncrement();
		case COL_UNIQUE:
			return field.isUnique();
		default:
			return "";
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		String s = value.toString().trim();
		if (s.isEmpty()) {
			return;
		}

		BasisField field = tableData.get(row);
		switch (getNumber(col)) {
		case COL_EXPORT_FIELD:
			field.setFieldHeader(exportFile.isSqlDatabase() ? s.replace(".", "") : s);
			break;
		case COL_TEXT_EXPORT:
			field.setOutputAsText((Boolean) value);
			break;
		case COL_PRIMARY_KEY:
			boolean isPK = (Boolean) value;
			if (isPkSet && exportFile == ExportFile.POSTGRESQL) {
				isPK = false;
			}

			isPkSet = !field.isPrimaryKey();
			field.setPrimaryKey(isPK);

			if (!isPK && field.isAutoIncrement()) {
				field.setAutoIncrement(false);
				fireTableDataChanged();
			}
			break;
		case COL_NOT_NULL:
			field.setNotNullable((Boolean) value);
			break;
		case COL_AUTO_INCREMENT:
			field.setAutoIncrement((Boolean) value && field.isPrimaryKey());
			break;
		case COL_UNIQUE:
			field.setUnique((Boolean) value);
			break;
		default:
			break;
		}

		fireTableCellUpdated(row, col);
	}

	public List<BasisField> getUserFields() {
		return tableData;
	}

	public void clear() {
		tableData.clear();
		fireTableDataChanged();
	}

	public boolean moveRowDown(int row) {
		if (row < getRowCount() - 1) {
			BasisField field = tableData.get(row);
			tableData.remove(field);
			tableData.add(++row, field);
			fireTableDataChanged();
			return true;
		}
		return false;
	}

	public boolean moveRowUp(int row) {
		if (row > 0) {
			BasisField field = tableData.get(row);
			tableData.remove(field);
			tableData.add(--row, field);
			fireTableDataChanged();
			return true;
		}
		return false;
	}

	public List<BasisField> getSelectedItems(int[] rows) {
		List<BasisField> result = new ArrayList<>();
		for (int i : rows) {
			result.add(tableData.get(i));
		}
		return result;
	}
}
