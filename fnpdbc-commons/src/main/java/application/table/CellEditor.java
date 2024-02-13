package application.table;

import java.awt.Component;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import application.interfaces.FieldTypes;
import application.utils.FieldDefinition;
import application.utils.General;

public class CellEditor extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 1L;
	private List<FieldDefinition> dbFields;
	private JTextField component = new JTextField();
	private JTextArea componentArea = new JTextArea();

	private FieldDefinition field;
	private boolean isTextAreaSet = false;

	public CellEditor(List<FieldDefinition> map) {
		dbFields = map;
		componentArea.setFont(component.getFont());
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex,
			int colIndex) {
		field = dbFields.get(table.convertColumnIndexToModel(colIndex));
		isTextAreaSet = value instanceof JTextArea;

		if (isTextAreaSet) {
			componentArea.setText(((JTextArea) value).getText());
			return componentArea;
		}

		component.setText(value.toString());
		return component;
	}

	@Override
	public boolean stopCellEditing() {
		if (!isTextAreaSet
				&& (field.getFieldType() == FieldTypes.DATE || field.getFieldType() == FieldTypes.FUSSY_DATE)) {
			component.setText(General.convertFussyDate2DB(component.getText()));
		}

		fireEditingStopped();
		return true;
	}

	@Override
	public Object getCellEditorValue() {
		return isTextAreaSet ? componentArea.getText() : component.getText();
	}
}
