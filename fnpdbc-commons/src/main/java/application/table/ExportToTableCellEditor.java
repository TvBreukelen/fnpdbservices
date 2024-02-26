package application.table;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import application.dialog.ChangeExportToDialog;
import application.model.ProjectModel;

public class ExportToTableCellEditor extends ActionTableCellEditor {
	private static final long serialVersionUID = 2182716753237787855L;

	public ExportToTableCellEditor() {
		super(new JCheckBox());
		clickCountToStart = 2;
	}

	@Override
	protected void editCell(JTable table, int row, int column) {
		if (table.getSelectedRow() > -1) {
			ProjectModel model = (ProjectModel) table.getModel();
			model.getProfileObject();
			ChangeExportToDialog dialog = new ChangeExportToDialog(model.getProfileObject(), this);
			dialog.setVisible(true);
		}
	}
}
