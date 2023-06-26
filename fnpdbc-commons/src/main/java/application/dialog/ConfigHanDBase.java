package application.dialog;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import application.interfaces.IConfigDb;
import application.interfaces.IConfigSoft;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigHanDBase extends JPanel implements IConfigDb {
	/**
	 * Title: ConfigHanDBase Description: HanDBase Configuration parms Copyright:
	 * (c) 2004-2012
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 8026494606938399070L;
	private JCheckBox autoInstallAllUsers;
	private JRadioButton fieldOrder;
	private JRadioButton physicalOrder;

	private JTextField autoInstallUser;

	private JLabel userLabel;
	transient IConfigSoft dialog;
	private boolean isImportEnabled;

	transient Profiles pdaSettings;
	transient GeneralSettings generalSettings = GeneralSettings.getInstance();

	public ConfigHanDBase(IConfigSoft dialog, Profiles pref) {
		this.dialog = dialog;
		pdaSettings = pref;
		buildDialog();
		activateComponents();
	}

	public void setImportEnabled(boolean enable) {
		isImportEnabled = enable;
		activateComponents();
	}

	@Override
	public void setProperties() {
		pdaSettings.setImportOption(fieldOrder.isSelected() ? 0 : 1);
		pdaSettings.setAutoInstUser(autoInstallAllUsers.isSelected() ? "ALL USERS" : autoInstallUser.getText().trim());
	}

	@Override
	public void activateComponents() {
		boolean isEnabled = !autoInstallAllUsers.isSelected();

		autoInstallUser.setEnabled(isEnabled);
		autoInstallUser.setEditable(isEnabled);
		userLabel.setEnabled(isEnabled);
		fieldOrder.setEnabled(isImportEnabled);
		physicalOrder.setEnabled(isImportEnabled);
	}

	private void buildDialog() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel(new GridLayout(1, 2));
		JPanel p2 = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		fieldOrder = GUIFactory.getJRadioButton("fieldOrder", null);
		physicalOrder = GUIFactory.getJRadioButton("physicalOrder", null);

		if (pdaSettings.getImportOption() == 0) {
			fieldOrder.setSelected(true);
		} else {
			physicalOrder.setSelected(true);
		}

		p1.add(General.addVerticalButtons(GUIFactory.getTitle("handbaseImport"), fieldOrder, physicalOrder),
				c.gridCell(0, 0, 0, 0));

		autoInstallAllUsers = GUIFactory.getJCheckBox("autoInstallAllUsers",
				pdaSettings.getAutoInstUser().equals("ALL USERS"), e -> activateComponents());

		autoInstallUser = GUIFactory.getJTextField("autoInstallUser",
				autoInstallAllUsers.isSelected() ? "" : pdaSettings.getAutoInstUser());
		autoInstallUser.setPreferredSize(new Dimension(70, 20));

		userLabel = GUIFactory.getJLabel("user");
		p2.add(autoInstallAllUsers, c.gridCell(0, 0, 0, 0));
		p2.add(userLabel, c.gridCell(0, 1, 0, 0));
		p2.add(autoInstallUser, c.gridCell(1, 1, 0, 0));
		p2.setBorder(BorderFactory.createTitledBorder("AutoInstall"));
		p1.add(p2);
		add(p1);
	}
}