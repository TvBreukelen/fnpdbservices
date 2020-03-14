package application.model;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import application.interfaces.FieldTypes;
import application.utils.FieldDefinition;
import application.utils.General;

public class ViewerModel extends AbstractTableModel {
	/**
	 * Title: ViewerModel Description: TableModel for class Viewer Copyright: (c)
	 * 2004-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8.2
	 */
	private static final long serialVersionUID = -4971384644052737284L;
	private List<FieldDefinition> fieldList;
	private List<Map<String, Object>> tableData = new Vector<>();

	private Font font;
	private boolean[] columnsVisible;
	private int totalColumns;
	private int visibleColumns;

	public ViewerModel(List<FieldDefinition> dbFields) {
		fieldList = dbFields;
		init();
	}

	private void init() {
		totalColumns = fieldList.size();
		font = new JLabel().getFont();
		columnsVisible = new boolean[totalColumns];
		visibleColumns = totalColumns;
		Arrays.fill(columnsVisible, true);
	}

	public List<Map<String, Object>> getDataVector() {
		return tableData;
	}

	public void setDataVector(List<Map<String, Object>> data) {
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
		int realCol = getNumber(col);
		switch (fieldList.get(realCol).getFieldType()) {
		case IMAGE:
		case LIST:
			return false;
		default:
			return true;
		}
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
		case FLOAT:
		case NUMBER:
			if (obj instanceof Number) {
				return obj;
			}
			break;
		case DATE:
			return General.convertDate(obj.toString());
		case FUSSY_DATE:
			return General.convertFussyDate(obj.toString());
		case MEMO:
			return createMemoField(obj.toString());
		case TIME:
			return General.convertTime(obj.toString());
		case DURATION:
			return General.convertDuration((Number) obj);
		case TIMESTAMP:
			return General.convertTimestamp(obj.toString());
		default:
			break;
		}

		String s = obj.toString();
		if (s.length() > FieldTypes.MIN_MEMO_FIELD_LEN || s.indexOf('\n') > -1) {
			return createMemoField(s);
		}
		return s;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		Map<String, Object> map = tableData.get(row);
		FieldDefinition field = fieldList.get(getNumber(col));
		map.put(field.getFieldAlias(), aValue);
	}

	private JTextArea createMemoField(String memo) {
		JTextArea result = new JTextArea(General.setDialogText(memo, FieldTypes.MIN_MEMO_FIELD_LEN));
		result.setFont(font);
		return result;
	}
}
