package application.table;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class ETable extends JTable {
	private static final long serialVersionUID = 7128520868065757774L;

	private Color alternateColor = new Color(237, 243, 245);
	private Color disabledcolor = new Color(217, 217, 217);
	private Color backgroundColor = getSelectionBackground();

	private boolean isNotDisabled;

	public ETable(AbstractTableModel model, boolean isNotDisable) {
		super(model);
		setShowGrid(true);
		setGridColor(Color.darkGray);

		isNotDisabled = isNotDisable;
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);

		if (c.getBackground() == null || !c.getBackground().equals(backgroundColor)) {
			Color secondRow = row % 2 == 0 ? alternateColor : super.getBackground();
			if (isNotDisabled) {
				c.setBackground(secondRow);
				return c;
			}

			c.setBackground(getModel().isCellEditable(convertRowIndexToModel(row), convertColumnIndexToModel(column))
					? secondRow
					: disabledcolor);
		}
		return c;
	}
}
