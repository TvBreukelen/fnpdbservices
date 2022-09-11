package application.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class ObjectRenderer extends DefaultTableCellRenderer {
	/**
	 * Title: ObjectRenderer Description: TableCellRenderer for Objects and
	 * Components used by class Viewer
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 1L;

	public ObjectRenderer() {
		setVerticalAlignment(SwingConstants.TOP);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (value instanceof Component) {
			int rowHeight = Math.max(getPreferredHeight((Component) value), 25);
			table.setRowHeight(row, rowHeight);
			return (Component) value;
		}

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	public int getPreferredHeight(Component value) {
		return value.getPreferredSize().height + 5;
	}
}
