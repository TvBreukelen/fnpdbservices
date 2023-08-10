package dbconvert.dialog;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import application.interfaces.IConfigDb;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ICalendarConfig extends JPanel implements IConfigDb {
	private static final long serialVersionUID = -2372698523273959887L;

	private JRadioButton complLotus;
	private JRadioButton complOutlook;
	private JCheckBox relaxedParsing;
	private JCheckBox relaxedUnfolding;
	private JCheckBox relaxedValidation;

	private Profiles pdaSettings;

	public ICalendarConfig(Profiles pref) {
		pdaSettings = pref;
		buildDialog();
	}

	private void buildDialog() {
		setLayout(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();
		add(createCompatibilityPanel(), c.gridCell(0, 0, 2, 4));
		add(createParsingPanel(), c.gridCell(1, 0, 2, 4));
	}

	private Component createCompatibilityPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JRadioButton complStandard = GUIFactory.getJRadioButton("standardComp", null);
		complLotus = GUIFactory.getJRadioButton("standardNotes", null);
		complOutlook = GUIFactory.getJRadioButton("standardOutlook", null);

		panel.add(General.addVerticalButtons(GUIFactory.getTitle("compatibility"), complStandard, complLotus,
				complOutlook));

		complLotus.setSelected(pdaSettings.isNotesCompatible());
		complOutlook.setSelected(pdaSettings.isOutlookCompatible());
		complStandard.setSelected(pdaSettings.isStandardCompatible());

		return panel;
	}

	private Component createParsingPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		relaxedParsing = GUIFactory.getJCheckBox("relaxedParsing", pdaSettings.isRelaxedParsing());
		relaxedUnfolding = GUIFactory.getJCheckBox("relaxedUnfolding", pdaSettings.isRelaxedUnfolding());
		relaxedValidation = GUIFactory.getJCheckBox("relaxedValidation", pdaSettings.isRelaxedValidation());

		panel.add(General.addVerticalButtons(GUIFactory.getTitle("parsing"), relaxedParsing, relaxedUnfolding,
				relaxedValidation));
		return panel;
	}

	@Override
	public void setProperties() throws Exception {
		pdaSettings.setNotesCompatible(complLotus.isSelected());
		pdaSettings.setOutlookCompatible(complOutlook.isSelected());
		pdaSettings.setRelaxedParsing(relaxedParsing.isSelected());
		pdaSettings.setRelaxedUnfolding(relaxedUnfolding.isSelected());
		pdaSettings.setRelaxedValidation(relaxedValidation.isSelected());
	}
}
