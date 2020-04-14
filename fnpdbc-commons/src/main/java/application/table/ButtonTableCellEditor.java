package application.table;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;

public class ButtonTableCellEditor extends ActionTableCellEditor {
	private static final long serialVersionUID = 4572899617279598428L;
	private MainTableSelectionListener listener;
	private ListSelectionEvent evt;
	
	public ButtonTableCellEditor(MainTableSelectionListener listener) {
		super(new JCheckBox());
		this.listener = listener;
		this.clickCountToStart = 1;

		evt = new ListSelectionEvent(this, 0, 0, false);
	}

	@Override
	protected void editCell(JTable table, int row, int column) {
		// No editing required
		listener.valueChanged(evt);
	}
}
