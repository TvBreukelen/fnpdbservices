package application.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import application.interfaces.ExportFile;
import application.model.ProfileObject;
import application.preferences.Profiles;
import application.table.ExportToTableCellEditor;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ChangeExportToDialog extends BasicDialog {
	private static final long serialVersionUID = 7463448287153606221L;
	private JTextField exportTo;
	private JPasswordField password;

	private String exportFilename;
	private ExportFile exportFile;

	private Profiles project;
	private ExportToTableCellEditor editor;

	public ChangeExportToDialog(ProfileObject project, ExportToTableCellEditor editor) {
		super();
		this.project = project.getProfiles();
		this.editor = editor;
		init();
	}

	private void init() {
		init(GUIFactory.getTitle("changeExportFileDialog"));
		setHelpFile("changeExportFile");
		exportFilename = project.getExportFile();
		exportFile = project.getExportFileEnum();

		buildDialog();
		pack();
		activateComponents();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		exportTo = new JTextField(exportFilename);
		exportTo.getDocument().addDocumentListener(funcDocumentChange);

		Dimension dim = exportTo.getPreferredSize();
		dim.setSize(dim.getWidth() < 320 ? 320 : dim.getWidth(), dim.getHeight());
		exportTo.setPreferredSize(dim);

		JButton button = GUIFactory.getJButton("browseFile",
				e -> General.getSelectedFile(ChangeExportToDialog.this, exportTo, exportFile, "", false));

		password = new JPasswordField(project.getExportPassword());
		JLabel lPass = GUIFactory.getJLabel("password");

		result.add(GUIFactory.getJLabel("fileName"), c.gridCell(0, 1, 0, 0));
		result.add(exportTo, c.gridmultipleCell(1, 1, 0, 0, 3, 1));
		result.add(button, c.gridCell(4, 1, 0, 0));
		result.add(lPass, c.gridCell(0, 2, 0, 0));
		result.add(password, c.gridCell(1, 2, 0, 1));

		lPass.setVisible(exportFile.isPasswordSupported());
		password.setVisible(exportFile.isPasswordSupported());
		result.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		return result;
	}

	@Override
	protected void save() throws Exception {
		String exportToFile = exportTo.getText().trim();
		if (!General.isFileExtensionOk(exportToFile, exportFile)) {
			exportToFile = General.getBaseName(exportToFile, exportFile);
		}
		editor.setSavedValues(exportToFile, password.getPassword());
		setVisible(false);
		dispose();
	}

	@Override
	public void activateComponents() {
		btSave.setEnabled(!exportTo.getText().trim().isEmpty());
	}
}
