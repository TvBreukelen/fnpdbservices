package application.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXHeader;

import application.dialog.ProgramDialog.Action;
import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigClone extends BasicDialog {
	private JTextField projectName;
	private JTextField exportToFile;
	private JComboBox<String> cbExportFile;

	private Profiles _project;
	private ProgramDialog _dialog;
	private static final long serialVersionUID = -6576646959544052585L;

	public ConfigClone(Profiles project, ProgramDialog dialog) {
		_project = project;
		_dialog = dialog;
		init();
	}

	@Override
	protected void init() {
		init(GUIFactory.getTitle("funcClone") + ": " + _project.getProfileID());
		setHelpFile("cloneprofile");

		projectName = new JTextField(_project.getProfileID());
		projectName.getDocument().addDocumentListener(funcDocumentChange);
		exportToFile = new JTextField(_project.getExportFile());
		exportToFile.getDocument().addDocumentListener(funcDocumentChange);

		cbExportFile = new JComboBox<>(ExportFile.getExportFilenames(false));
		cbExportFile.setSelectedItem(_project.getProjectID());
		cbExportFile.addActionListener(arg0 -> activateComponents());

		buildDialog();
		activateComponents();
		pack();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		JXHeader label = new JXHeader(null, GUIFactory.getTexts("cloneText"));
		label.setPreferredSize(new Dimension(400, 50));

		JButton btOpen = GUIFactory.getJButton("browseFile", e -> {
			String projectCopyTo = cbExportFile.getSelectedItem().toString();
			ExportFile exp = ExportFile.getExportFile(projectCopyTo);
			General.getSelectedFile(ConfigClone.this, exportToFile, exp, "", false);
			activateComponents();
		});

		result.add(Box.createVerticalStrut(5), c.gridCell(0, 0, 0, 0));
		result.add(label, c.gridmultipleCell(0, 1, 0, 0, 0, 2));
		result.add(Box.createVerticalStrut(10), c.gridCell(0, 4, 0, 0));
		result.add(cbExportFile, c.gridCell(0, 5, 0, 0));
		result.add(GUIFactory.getJLabel("fileName"), c.gridCell(1, 5, 0, 0));
		result.add(exportToFile, c.gridCell(2, 5, 2, 0));
		result.add(btOpen, c.gridCell(3, 5, 0, 0));
		result.add(GUIFactory.getJLabel("profile"), c.gridCell(1, 6, 0, 0));
		result.add(projectName, c.gridCell(2, 6, 0, 0));
		result.add(Box.createVerticalGlue(), c.gridCell(0, 7, 0, 0));
		result.add(Box.createVerticalStrut(10), c.gridCell(0, 8, 0, 0));
		return result;
	}

	@Override
	protected void save() throws FNProgException {
		String profileID = projectName.getText().trim();
		String projectCopyTo = cbExportFile.getSelectedItem().toString();
		String copyToFile = exportToFile.getText().trim();

		ExportFile exp = ExportFile.getExportFile(projectCopyTo);
		if (copyToFile.isEmpty()) {
			copyToFile = General.getDefaultPDADatabase(exp);
		} else if (!General.isFileExtensionOk(copyToFile, exp)) {
			copyToFile = copyToFile + "." + exp.getFileExtention()[0];
		}

		_project.cloneCurrentProfile(projectCopyTo, profileID);
		_project.setExportFile(copyToFile);

		_dialog.updateProfile(Action.Clone);
	}

	@Override
	public void activateComponents() {
		boolean isValid = !exportToFile.getText().isEmpty();
		if (isValid) {
			String profileID = projectName.getText().trim();
			isValid = !profileID.isEmpty();
			if (isValid) {
				isValid = !_project.profileExists(cbExportFile.getSelectedItem().toString(), profileID);
			}
		}

		btSave.setEnabled(isValid);
	}
}
