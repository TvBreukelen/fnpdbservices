package application.table;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import application.interfaces.FieldTypes;
import application.utils.General;

public class CellRenderer extends DefaultListCellRenderer {
	private static final long serialVersionUID = -2833486177205557493L;
	private FieldTypes dbField;

	public CellRenderer(FieldTypes fieldType) {
		dbField = fieldType;
		setOpaque(true);
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		if (value != null && !value.equals("")) {
			switch (dbField) {
			case DATE:
				value = General.convertDate(value.toString());
				break;
			case FUSSY_DATE:
				value = General.convertFussyDate(value.toString());
				break;
			case DURATION:
				value = General.convertDuration((Number) value);
				break;
			case TIME:
				value = General.convertTime(value.toString());
				break;
			case TIMESTAMP:
				value = General.convertTimestamp(value.toString());
				break;
			default:
				value = value.toString();
			}
		}
		return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	}
}
