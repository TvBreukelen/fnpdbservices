package application.table;

import java.awt.Component;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import application.utils.General;

public class FilenameRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 314536074776986014L;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		String fileName = value != null ? value.toString() : General.EMPTY_STRING;
		String shortName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
		Component result = super.getTableCellRendererComponent(table, shortName, isSelected, hasFocus, row, column);

		if (result instanceof JComponent) {
			((JComponent) result).setToolTipText(fileName);
		}

		// Allow superclass to return rendering component.
		return result;
	}
}
