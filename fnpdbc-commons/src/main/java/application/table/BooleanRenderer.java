package application.table;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

public class BooleanRenderer extends JCheckBox implements TableCellRenderer {
	private static final long serialVersionUID = 1L;

	public BooleanRenderer(Color background) {
		setVerticalAlignment(SwingConstants.TOP);
		setBackground(background);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (value instanceof Boolean) {
			setSelected((Boolean) value);
		}
		return this;
	}

}
