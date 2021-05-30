package application.table;

import java.awt.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
				value = General.convertDate((LocalDate) value, General.getSimpleDateFormat());
				break;
			case FUSSY_DATE:
				value = General.convertFussyDate(value.toString());
				break;
			case DURATION:
				value = General.convertDuration((Duration) value);
				break;
			case TIME:
				value = General.convertTime((LocalTime) value, General.getSimpleDateFormat());
				break;
			case TIMESTAMP:
				value = General.convertTimestamp((LocalDateTime) value, General.getSimpleTimeFormat());
				break;
			default:
				value = value.toString();
			}
		}
		return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	}
}
