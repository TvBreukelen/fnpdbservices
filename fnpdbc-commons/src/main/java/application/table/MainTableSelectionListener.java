package application.table;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import application.dialog.ProgramDialog;
import application.model.ProfileObject;
import application.model.ProjectModel;

public class MainTableSelectionListener implements ListSelectionListener {
	private JTable _table;
	private ProgramDialog _dialog;

	public MainTableSelectionListener(JTable table, ProgramDialog dialog) {
		_table = table;
		_dialog = dialog;
	}

	@Override
	public void valueChanged(ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) {
			return;
		}

		switch (_table.getSelectedRowCount()) {
		case 0:
			break;
		case 1:
			int modelIndex = _table.convertRowIndexToModel(_table.getSelectedRow());
			ProfileObject obj = ((ProjectModel) _table.getModel()).getProfileObject(modelIndex);
			obj.alignProfiles();
			obj.alignTable();
			_dialog.activateComponents();
			break;
		default:
		}
	}
}
