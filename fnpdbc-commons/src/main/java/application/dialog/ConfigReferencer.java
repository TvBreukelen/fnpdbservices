package application.dialog;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import application.interfaces.IConfigDb;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;

public class ConfigReferencer extends JPanel implements IConfigDb {
	/**
	 * Title: DbReferencer Description: P. Referencer Configuration parms Copyright:
	 * (c) 2004-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = -2203382109505063497L;
	private JCheckBox paragraphHeader;
	private JCheckBox titleHeader;
	private Profiles pdaSettings;

	public ConfigReferencer(Profiles pref) {
		pdaSettings = pref;
		buildDialog();
	}

	@Override
	public void setProperties() {
		pdaSettings.setUseParagraphHeader(paragraphHeader.isSelected());
		pdaSettings.setUseTitleHeader(titleHeader.isSelected());
	}

	private void buildDialog() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		paragraphHeader = GUIFactory.getJCheckBox("paragraphHeader", pdaSettings.isUseParagraphHeader());
		titleHeader = GUIFactory.getJCheckBox("titleHeader", pdaSettings.isUseTitleHeader());
		add(General.addVerticalButtons(GUIFactory.getTitle("export"), paragraphHeader, titleHeader));
	}
}