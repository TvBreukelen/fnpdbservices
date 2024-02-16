package application.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;

import application.interfaces.FieldTypes;
import application.utils.FieldDefinition;
import application.utils.General;

public class ViewerModel extends HiddenColumnModel {
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
	transient List<Map<String, Object>> tableData = new ArrayList<>();

	public ViewerModel(List<FieldDefinition> dbFields) {
		super(dbFields.size());
		fieldList = dbFields;
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

		if (obj == null || obj.equals(General.EMPTY_STRING)) {
			return General.EMPTY_STRING;
		}

		switch (field.getFieldType()) {
		case BOOLEAN:
			return obj;
		case IMAGE:
			if (obj instanceof ImageIcon icon) {
				return General.createScaledIcon(icon, 320, 320);
			}
			return obj;
		case BIG_DECIMAL, FLOAT, NUMBER:
			return obj instanceof Number ? field.getNumberFormat().format(obj) : obj.toString();
		case MEMO:
			String s = obj.toString();
			if (s.contains("\n") || s.length() > FieldTypes.MIN_MEMO_FIELD_LEN) {
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
		JTextArea result = new JTextArea(General.setDialogText(memo, FieldTypes.MIN_MEMO_FIELD_LEN));
		result.setBorder(BorderFactory.createEmptyBorder());
		return result;
	}
}
