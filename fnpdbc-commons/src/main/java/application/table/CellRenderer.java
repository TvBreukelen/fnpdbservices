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

		return super.getListCellRendererComponent(list, General.convertObject(value, dbField), index, isSelected,
				cellHasFocus);
	}
}
