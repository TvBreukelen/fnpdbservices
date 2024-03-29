package application.table;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import application.utils.General;

public class BooleanRenderer extends JCheckBox implements TableCellRenderer {
	private static final long serialVersionUID = 1L;
	private JLabel label = new JLabel(General.EMPTY_STRING);
	private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	public BooleanRenderer() {
		setHorizontalAlignment(SwingConstants.CENTER);
		setVerticalAlignment(SwingConstants.TOP);
		setBorderPainted(true);
		setOpaque(true);
		label.setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		boolean isBoolean = value instanceof Boolean;
		Component comp = isBoolean ? this : label;

		if (isSelected) {
			comp.setForeground(table.getSelectionForeground());
			comp.setBackground(table.getSelectionBackground());
		} else {
			comp.setBackground(Color.BLACK);
			comp.setForeground(new Color(255, 0, 255));
		}
		if (isBoolean) {
			setSelected((Boolean) value);
		}

		if (hasFocus) {
			setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		} else {
			setBorder(noFocusBorder);
		}

		return comp;
	}
}
