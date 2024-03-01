package application.table;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import application.dialog.ChangeExportToDialog;
import application.interfaces.IExportProcess;
import application.model.ProjectModel;

public class ExportToTableCellEditor extends ActionTableCellEditor {
	private static final long serialVersionUID = 2182716753237787855L;
	transient IExportProcess expProcess;

	public ExportToTableCellEditor(IExportProcess process) {
		super(new JCheckBox());
		clickCountToStart = 2;
		expProcess = process;
	}

	@Override
	protected void editCell(JTable table, int row, int column) {
		if (table.getSelectedRow() > -1) {
			ProjectModel model = (ProjectModel) table.getModel();
			model.getProfileObject();
			ChangeExportToDialog dialog = new ChangeExportToDialog(model.getProfileObject(), expProcess);
			dialog.setVisible(true);
		}
	}
}
