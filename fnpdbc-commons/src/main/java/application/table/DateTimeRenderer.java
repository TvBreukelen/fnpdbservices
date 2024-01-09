package application.table;

import java.awt.Component;
import java.time.LocalDateTime;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import application.utils.General;

public class DateTimeRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 7572689437526038733L;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		String dateTime = value != null ? ((LocalDateTime) value).format(General.getSimpleTimestampFormat())
				: General.EMPTY_STRING;
		return super.getTableCellRendererComponent(table, dateTime, isSelected, hasFocus, row, column);
	}
}
