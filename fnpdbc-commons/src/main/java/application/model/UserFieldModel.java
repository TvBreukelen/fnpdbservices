package application.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import application.utils.BasisField;
import application.utils.GUIFactory;

public class UserFieldModel extends AbstractTableModel {
	private static final long serialVersionUID = -3745524495569802922L;

	private Vector<BasisField> _tableData = new Vector<>();
	private String[] columnNames = GUIFactory.getArray("exportHeaders");

	public void setTableData(Vector<BasisField> tableData) {
		if (tableData != null) {
			_tableData = tableData;
			fireTableDataChanged();
		}
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 1;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return _tableData.size();
	}

	public void removeRecords(int[] rows) {
		List<BasisField> vDelete = new ArrayList<>();
		for (int i : rows) {
			vDelete.add(_tableData.get(i));

		}
		_tableData.removeAll(vDelete);
		fireTableDataChanged();
	}

	public void addRecord(BasisField field, int row) {
		BasisField obj = new BasisField(field);
		_tableData.add(row, obj);
		fireTableDataChanged();
	}

	@Override
	public Object getValueAt(int row, int col) {
		BasisField field = _tableData.get(row);
		switch (col) {
		case 0:
			return field.getFieldAlias();
		case 1:
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

		BasisField field = _tableData.get(row);
		field.setFieldHeader(s);
		fireTableCellUpdated(row, col);
	}

	public Vector<BasisField> getUserFields() {
		return _tableData;
	}

	public void clear() {
		_tableData.clear();
		fireTableDataChanged();
	}

	public boolean moveRowDown(int row) {
		if (row < getRowCount() - 1) {
			BasisField field = _tableData.get(row);
			_tableData.remove(field);
			_tableData.insertElementAt(field, ++row);
			fireTableDataChanged();
			return true;
		}
		return false;
	}

	public boolean moveRowUp(int row) {
		if (row > 0) {
			BasisField field = _tableData.get(row);
			_tableData.remove(field);
			_tableData.insertElementAt(field, --row);
			fireTableDataChanged();
			return true;
		}
		return false;
	}
}
