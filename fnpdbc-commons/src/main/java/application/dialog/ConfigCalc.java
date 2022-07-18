package application.dialog;

import java.awt.FlowLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import application.interfaces.IConfigDb;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;

public class ConfigCalc extends JPanel implements IConfigDb {
	/**
	 * Configuration dialog for Calc
	 *
	 * @author Tom van Breukelen
	 * @version 9
	 * @since 2021
	 */

	private static final long serialVersionUID = 6451018966275061118L;

	private JCheckBox[] cHeader = new JCheckBox[2];
	private Profiles pdaSettings;

	public ConfigCalc(Profiles pref) {
		pdaSettings = pref;
		buildDialog();
		activateComponents();
	}

	@Override
	public void setProperties() {
		pdaSettings.setUseHeader(cHeader[0].isSelected());
		pdaSettings.setBoldHeader(cHeader[1].isSelected());
	}

	@Override
	public void activateComponents() {
		cHeader[1].setEnabled(cHeader[0].isSelected());
	}

	private void buildDialog() {
		setLayout(new FlowLayout(FlowLayout.LEFT));

		cHeader[0] = GUIFactory.getJCheckBox("includeHeaders", pdaSettings.isUseHeader(), e -> activateComponents());
		cHeader[1] = GUIFactory.getJCheckBox("fontBold", pdaSettings.isBoldHeader());

		add(General.addVerticalButtons(GUIFactory.getTitle("headerSettings"), cHeader[0], cHeader[1]));
	}
}
