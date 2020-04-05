package application.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import application.interfaces.FieldTypes;
import application.utils.BasisField;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;

public class SelectionFieldModel extends AbstractTableModel {
	private static final long serialVersionUID = -5511820630161468736L;
	private List<FieldDefinition> tableData = new ArrayList<>();
	private String[] columnNames = GUIFactory.getArray("exportHeaders");

	public void setTableData(List<FieldDefinition> tableData) {
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

	@Override
	public Object getValueAt(int row, int col) {
		FieldDefinition field = tableData.get(row);
		switch(col) {
		case 0:
			return field.getFieldAlias();
		case 1:
			return field.getFieldType();
		case 2:
			return field.getFieldType() == FieldTypes.MEMO ? "" : field.getSize();
		}
		return null;
	}
	public void clear() {
		tableData.clear();
		fireTableDataChanged();
	}

	public List<BasisField> getSelectedItems(int[] rows) {
		List<BasisField> result = new ArrayList<>();
		for(int i : rows) {
			result.add(new BasisField(tableData.get(i)));
		}
		return result;
	}
}
