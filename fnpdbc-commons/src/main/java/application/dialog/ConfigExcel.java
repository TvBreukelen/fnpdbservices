package application.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import application.interfaces.IConfigDb;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigExcel extends JPanel implements IConfigDb {
	/**
	 * Title: DbExcel Description: MS-Excel Configuration parms Copyright (c)
	 * 2004-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 9209643909894109017L;
	private ActionListener funcHeader = null;
	private JComboBox<String> font = null;
	private JComboBox<String> fontSize = null;
	private JCheckBox[] cHeader = new JCheckBox[3];
	private JCheckBox lockColumn = null;
	private Profiles pdaSettings;

	public ConfigExcel(Profiles pref) {
		pdaSettings = pref;
		funcHeader = e -> activateComponents();

		buildDialog();
		activateComponents();
	}

	@Override
	public void setProperties() {
		pdaSettings.setLock1stColumn(lockColumn.isSelected());
		pdaSettings.setFont(font.getSelectedItem().toString());
		pdaSettings.setFontSize(Integer.parseInt(fontSize.getSelectedItem().toString()));
		pdaSettings.setUseHeader(cHeader[0].isSelected());
		pdaSettings.setLockHeader(cHeader[1].isSelected());
		pdaSettings.setBoldHeader(cHeader[2].isSelected());
	}

	@Override
	public void activateComponents() {
		cHeader[1].setEnabled(cHeader[0].isSelected());
		cHeader[2].setEnabled(cHeader[0].isSelected());
	}

	private void buildDialog() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		lockColumn = GUIFactory.getJCheckBox("lockColumn", pdaSettings.isLock1stColumn());

		font = new JComboBox<>(new String[] { "Arial", "Courier", "Tahoma", "Times" });
		font.setSelectedItem(pdaSettings.getFont());

		fontSize = new JComboBox<>(new String[] { "8", "10", "12", "14", "16" });
		fontSize.setSelectedItem(String.valueOf(pdaSettings.getFontSize()));

		JPanel p = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		p.add(lockColumn, c.gridCell(0, 0, 0, 0, 0, 1, GridBagConstraints.LINE_END));
		p.add(GUIFactory.getJLabel("font"), c.gridCell(0, 1, 0, 0));
		p.add(font, c.gridCell(1, 1, 0, 0));
		p.add(fontSize, c.gridCell(2, 1, 0, 0));
		p.add(Box.createVerticalBox(), c.gridCell(0, 2, 0, 30)); // To move the previous lines up
		p.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("generalSettings")));
		add(p);

		cHeader[0] = GUIFactory.getJCheckBox("includeHeaders", pdaSettings.isUseHeader(), funcHeader);
		cHeader[1] = GUIFactory.getJCheckBox("freezeHeaders", pdaSettings.isLockHeader());
		cHeader[2] = GUIFactory.getJCheckBox("fontBold", pdaSettings.isBoldHeader());

		add(General.addVerticalButtons(GUIFactory.getTitle("headerSettings"), null, cHeader[0], cHeader[1],
				cHeader[2]));
	}
}