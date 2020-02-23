package application.table;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

public class ButtonTableCellEditor extends ActionTableCellEditor {
	private static final long serialVersionUID = 4572899617279598428L;

	public ButtonTableCellEditor() {
		super(new JCheckBox());
	}

	public JButton getButton() {
		return super.button;
	}
	
	@Override
	protected void editCell(JTable table, int row, int column) {
	}

}
