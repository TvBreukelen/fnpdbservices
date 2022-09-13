package application.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import application.interfaces.FieldTypes;
import application.utils.FieldDefinition;
import application.utils.General;

public class ViewerModel extends AbstractTableModel {
	/**
	 * Title: ViewerModel
	 *
	 * @apiNote TableModel for calls Viewer
	 * @since 2004
	 * @author Tom van Breukelen
	 * @version 8.2
	 */
	private static final long serialVersionUID = -4971384644052737284L;
	private List<FieldDefinition> fieldList;
	private List<Map<String, Object>> tableData = new ArrayList<>();

	private boolean[] columnsVisible;
	private int totalColumns;
	private int visibleColumns;

	public ViewerModel(List<FieldDefinition> dbFields) {
		fieldList = dbFields;
		init();
	}

	private void init() {
		totalColumns = fieldList.size();
		columnsVisible = new boolean[totalColumns];
		visibleColumns = totalColumns;
		Arrays.fill(columnsVisible, true);
	}

	public List<Map<String, Object>> getDataListMap() {
		return tableData;
	}

	public void setDataListMap(List<Map<String, Object>> data) {
		tableData = data;
	}

	public void resetColumnVisibility() {
		boolean isColumnChange = false;

		for (int i = 0; i < totalColumns; i++) {
			FieldDefinition field = fieldList.get(i);
			boolean show = field.isExport();
			if (columnsVisible[i] != show) {
				columnsVisible[i] = show;
				visibleColumns += show ? 1 : -1;
				isColumnChange = true;
			}
		}

		if (isColumnChange) {
			fireTableStructureChanged();
		}
	}

	public int getLastColumn() {
		return totalColumns - 1;
	}

	/**
	 * This function converts a column number in the table to the right number of
	 * the data.
	 */
	protected int getNumber(int col) {
		int n = col; // right number to return
		int i = 0;
		do {
			if (!columnsVisible[i]) {
				n++;
			}
			i++;
		} while (i < n);
		// If we are on an invisible column,
		// we have to go one step further
		while (!columnsVisible[n]) {
			n++;
		}
		return n;
	}

	@Override
	public int getColumnCount() {
		return visibleColumns;
	}

	@Override
	public String getColumnName(int col) {
		return fieldList.get(getNumber(col)).getFieldHeader();
	}

	@Override
	public int getRowCount() {
		return tableData.size();
	}

	@Override
	public Class<? extends Object> getColumnClass(int col) {
		return getValueAt(0, col).getClass();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}

	@Override
	public Object getValueAt(int row, int col) {
		FieldDefinition field = fieldList.get(getNumber(col));
		Object obj = tableData.get(row).get(field.getFieldAlias());

		if (obj == null || obj.equals("")) {
			return "";
		}

		switch (field.getFieldType()) {
		case BOOLEAN:
		case IMAGE:
			return obj;
		case BIG_DECIMAL:
		case FLOAT:
		case NUMBER:
			return obj instanceof Number ? field.getNumberFormat().format(obj) : obj.toString();
		case MEMO:
			String s = obj.toString();
			if (s.length() > FieldTypes.MIN_MEMO_FIELD_LEN) {
				return createMemoField(s);
			}
			return s;
		default:
			return General.convertObject(obj, field.getFieldType());
		}
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		Map<String, Object> map = tableData.get(row);
		FieldDefinition field = fieldList.get(getNumber(col));
		map.put(field.getFieldAlias(), aValue);
	}

	private JTextArea createMemoField(String memo) {
		return new JTextArea(General.setDialogText(memo, FieldTypes.MIN_MEMO_FIELD_LEN));
	}
}
