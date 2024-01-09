package application.table;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import application.dialog.ChangeExportToDialog;
import application.model.ProfileObject;
import application.model.ProjectModel;
import application.utils.General;

public class ExportToTableCellEditor extends ActionTableCellEditor {
	private static final long serialVersionUID = 2182716753237787855L;
	private String exportFilename = General.EMPTY_STRING;
	private String exportUser = General.EMPTY_STRING;
	private char[] filePassword;
	private boolean isSaved = false;

	public ExportToTableCellEditor() {
		super(new JCheckBox());
		clickCountToStart = 2;
	}

	@Override
	protected void editCell(JTable table, int row, int column) {
		if (table.getSelectedRow() > -1) {
			ProjectModel model = (ProjectModel) table.getModel();
			ProfileObject profile = model.getProfileObject();
			ChangeExportToDialog dialog = new ChangeExportToDialog(model.getProfileObject(), this);
			dialog.setVisible(true);

			if (isSaved) {
				profile.getProfiles().updateTofile(exportFilename, exportUser, filePassword);
			}
		}
	}

	public void setSavedValues(String filename, char[] cs) {
		exportFilename = filename;
		filePassword = cs;
		isSaved = true;
	}
}
