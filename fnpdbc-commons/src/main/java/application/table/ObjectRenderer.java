package application.table;

import java.awt.Component;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import application.utils.FieldDefinition;

public class ObjectRenderer extends DefaultTableCellRenderer {
	/**
	 * Title: ObjectRenderer Description: TableCellRenderer for Objects and
	 * Components used by class Viewer
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 1L;
	private List<FieldDefinition> dbFields;

	public ObjectRenderer(List<FieldDefinition> map) {
		setVerticalAlignment(SwingConstants.TOP);
		dbFields = map;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (value instanceof Component) {
			int rowHeight = Math.max(getPreferredHeight((Component) value), 25);
			table.setRowHeight(row, rowHeight);
			return (Component) value;
		}

		setHorizontalAlignment(value instanceof String ? SwingConstants.LEFT : SwingConstants.RIGHT);
		if (table.getRowHeight(row) < 25) {
			table.setRowHeight(row, 25);
		}

		if (value instanceof Double) {
			FieldDefinition field = dbFields.get(column);
			setText(field.getDecimalFormat().format(value));
		} else {
			setText(value.toString());
		}
		return this;
	}

	public int getPreferredHeight(Component value) {
		return value.getPreferredSize().height + 5;
	}
}
