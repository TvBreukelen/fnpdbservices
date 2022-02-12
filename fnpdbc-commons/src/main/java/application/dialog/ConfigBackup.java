package application.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXHeader;

import application.interfaces.ExportFile;
import application.preferences.Project;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigBackup extends BasicDialog {
	private static final long serialVersionUID = -6269743140266203149L;

	private JTextField fdBackup = new JTextField();
	private boolean isBackup;
	private boolean isRestored = false;

	public ConfigBackup(boolean isBackup) {
		this.isBackup = isBackup;
		init();
	}

	private void init() {
		init(GUIFactory.getText(isBackup ? "funcBackup" : "funcRestore"));
		btSave.setEnabled(false);
		btHelp.setVisible(false);
		buildDialog();
		pack();
	}

	@Override
	protected Component createCenterPanel() {
		JButton btOpen = GUIFactory.getJButton("browseFile", e -> {
			General.getSelectedFile(ConfigBackup.this, fdBackup, ExportFile.XML,
					generalSettings.getDefaultBackupFolder(), !isBackup);
			btSave.setEnabled(fdBackup.getText().length() > 0);
		});

		fdBackup.setEditable(false);
		fdBackup.setPreferredSize(new Dimension(300, 25));

		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		JXHeader label = new JXHeader(null, GUIFactory.getTexts(isBackup ? "backupText" : "restoreText"));
		label.setPreferredSize(new Dimension(400, 50));

		result.add(Box.createVerticalStrut(5), c.gridCell(0, 0, 0, 0));
		result.add(label, c.gridmultipleCell(0, 1, 0, 0, 0, 2));
		result.add(Box.createVerticalStrut(15), c.gridCell(0, 2, 0, 0));
		result.add(GUIFactory.getJLabel("fileName"), c.gridCell(0, 3, 0, 0));
		result.add(fdBackup, c.gridCell(1, 3, 0, 2));
		result.add(btOpen, c.gridCell(2, 3, 0, 0));
		result.add(Box.createVerticalStrut(15), c.gridCell(0, 4, 0, 0));
		return result;
	}

	@Override
	protected void save() {
		if (isBackup) {
			Project.backupApplication(fdBackup.getText());
			General.showMessage(this, GUIFactory.getText("backupCompleted"), GUIFactory.getText("funcBackup"), false);
		} else {
			Project.restoreApplication(fdBackup.getText());
			isRestored = true;
			General.showMessage(this, GUIFactory.getText("restoreCompleted"), GUIFactory.getText("funcRestore"), false);
		}
	}

	protected boolean isRestored() {
		return isRestored;
	}
}
