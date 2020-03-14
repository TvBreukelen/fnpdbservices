package application.table;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

import application.utils.GUIFactory;

public abstract class ActionTableCellEditor extends DefaultCellEditor {
	private static final long serialVersionUID = -2942498603023207149L;
	protected JButton button;
	private JTable table;
	private int row;
	private int column;

	public ActionTableCellEditor(JCheckBox checkBox) {
		super(checkBox);
		button = new JButton();
		button.setOpaque(true);
		button.addActionListener(e -> fireEditingStopped());
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (isSelected) {
			button.setForeground(table.getSelectionForeground());
			button.setBackground(table.getSelectionBackground());
		} else {
			button.setForeground(table.getForeground());
			button.setBackground(table.getBackground());
		}

		button.setText(GUIFactory.getText("edit"));
		this.table = table;
		this.row = row;
		this.column = column;
		return button;
	}

	@Override
	public Object getCellEditorValue() {
		return table.getValueAt(row, column);
	}

	@Override
	protected void fireEditingStopped() {
		editCell(table, row, column);
		super.fireEditingStopped();
	}

	protected abstract void editCell(JTable table, int row, int column);
}