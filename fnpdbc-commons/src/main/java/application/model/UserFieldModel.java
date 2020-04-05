package application.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import application.utils.BasisField;
import application.utils.GUIFactory;

public class UserFieldModel extends AbstractTableModel {
	private static final long serialVersionUID = -3745524495569802922L;

	private List<BasisField> tableData = new ArrayList<>();
	private String[] columnNames = GUIFactory.getArray("exportHeaders");

	public void setTableData(List<BasisField> tableData) {
		if (tableData != null) {
			this.tableData = tableData;
			fireTableDataChanged();
		}
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 2;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
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
		tableData.add(row, obj);
		fireTableDataChanged();
	}

	@Override
	public Object getValueAt(int row, int col) {
		BasisField field = tableData.get(row);
		switch(col) {
		case 0:
			return field.getFieldAlias();
		case 1:
			return field.getFieldType();
		case 2:
			return field.getFieldHeader();
		}
		return null;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		String s = value.toString().trim();
		if (s.isEmpty()) {
			return;
		}

		BasisField field = tableData.get(row);
		field.setFieldHeader(s);
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
		for(int i : rows) {
			result.add(tableData.get(i));
		}
		return result;
	}
}
