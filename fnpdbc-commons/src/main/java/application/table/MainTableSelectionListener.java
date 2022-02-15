package application.table;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import application.dialog.ProgramDialog;
import application.model.ProfileObject;
import application.model.ProjectModel;

public class MainTableSelectionListener implements ListSelectionListener {
	private JTable table;
	private ProgramDialog dialog;

	public MainTableSelectionListener(JTable table, ProgramDialog dialog) {
		this.table = table;
		this.dialog = dialog;
	}

	@Override
	public void valueChanged(ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) {
			return;
		}

		switch (table.getSelectedRowCount()) {
		case 0:
			break;
		case 1:
			int modelIndex = table.convertRowIndexToModel(table.getSelectedRow());
			ProfileObject obj = ((ProjectModel) table.getModel()).getProfileObject(modelIndex);
			obj.alignProfiles();
			obj.alignTable();
			dialog.activateComponents();

			if (evt.getSource() instanceof ButtonTableCellEditor) {
				dialog.clickEdit();
			}

			break;
		default:
		}
	}
}
