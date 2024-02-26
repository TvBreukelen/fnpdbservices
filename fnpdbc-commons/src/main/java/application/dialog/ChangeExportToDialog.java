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
import dbengine.utils.DatabaseHelper;

public class ChangeExportToDialog extends BasicDialog {
	private static final long serialVersionUID = 7463448287153606221L;
	private JTextField exportTo;
	private JPasswordField password;

	private ExportFile exportFile;

	transient DatabaseHelper helper;
	transient Profiles project;

	public ChangeExportToDialog(ProfileObject project, ExportToTableCellEditor editor) {
		super();
		this.project = project.getProfiles();
		init();
	}

	private void init() {
		init(GUIFactory.getTitle("changeExportFileDialog"));
		setHelpFile("changeExportFile");
		helper = project.getToDatabase();
		exportFile = project.getExportFileEnum();

		buildDialog();
		pack();
		activateComponents();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		exportTo = new JTextField(helper.getDatabase());
		exportTo.getDocument().addDocumentListener(funcDocumentChange);

		Dimension dim = exportTo.getPreferredSize();
		dim.setSize(dim.getWidth() < 320 ? 320 : dim.getWidth(), dim.getHeight());
		exportTo.setPreferredSize(dim);

		JButton button = GUIFactory.getJButton("browseFile", e -> General.getSelectedFile(ChangeExportToDialog.this,
				exportTo, exportFile, General.EMPTY_STRING, false));

		password = new JPasswordField(General.decryptPassword(helper.getPassword()));
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

		helper.setDatabase(exportToFile);
		helper.setPassword(General.encryptPassword(password.getPassword()));
		String node = project.setDatabase(helper);
		project.setToDatabase(node);

		setVisible(false);
		dispose();
	}

	@Override
	public void activateComponents() {
		btSave.setEnabled(!exportTo.getText().trim().isEmpty());
	}
}
