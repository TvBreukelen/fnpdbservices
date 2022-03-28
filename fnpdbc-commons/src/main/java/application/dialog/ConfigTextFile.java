package application.dialog;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import application.interfaces.ExportFile;
import application.interfaces.IConfigDb;
import application.interfaces.IConfigSoft;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigTextFile extends JPanel implements IConfigDb {
	/**
	 * Title: DbTextFile Description: TextFile Configuration parms Copyright: (c)
	 * 2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 8006516257526651336L;
	private JRadioButton standardCsv;

	private JCheckBox inclHeaders;
	private JCheckBox inclLinebreaks;
	private JComboBox<String> separator;
	private JComboBox<String> delimiter;
	private JComboBox<String> splitFile;

	private JPanel otherPanel;
	private boolean isExport;

	private Profiles pdaSettings;
	private IConfigSoft dialog;

	private static final String STANDARD_CSV = "standardCsv";
	private static final String OTHER_CSV = "otherCsv";

	public ConfigTextFile(Profiles pref, boolean isExport) {
		pdaSettings = pref;
		this.isExport = isExport;

		buildDialog();
		activateComponents();
	}

	// For imports only
	public ConfigTextFile(IConfigSoft configSoft, Profiles prefText) {
		this(prefText, false);
		dialog = configSoft;
	}

	@Override
	public void setProperties() {
		int index = separator.getSelectedIndex();
		String separatorChar = ",\t;|".substring(index, index + 1);

		index = delimiter.getSelectedIndex();
		String delimiterChar = "\"'/$%".substring(index, index + 1);

		if (isExport) {
			pdaSettings.setUseHeader(inclHeaders.isSelected());
			pdaSettings.setUseLinebreak(inclLinebreaks.isSelected());
			pdaSettings.setMaxFileSize(splitFile.getSelectedIndex());
			pdaSettings.setTextFileFormat(standardCsv.isSelected() ? STANDARD_CSV : OTHER_CSV);
			pdaSettings.setFieldSeparator(separatorChar);
			pdaSettings.setTextDelimiter(delimiterChar);
		} else {
			pdaSettings.setImportTextFileFormat(standardCsv.isSelected() ? STANDARD_CSV : OTHER_CSV);
			pdaSettings.setImportFieldSeparator(separatorChar);
			pdaSettings.setImportTextDelimiter(delimiterChar);
		}
	}

	private void buildDialog() {
		setLayout(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		add(createLeftPanel(), c.gridCell(0, 0, 2, 4));
		otherPanel = createRightPanel();
		add(otherPanel, c.gridCell(1, 0, 2, 4));

		if (!isExport) {
			JButton button = GUIFactory.getJButton("apply", e -> {
				setProperties();
				dialog.verifyDatabase();
			});

			JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			panel.add(button);
			add(panel, c.gridCell(1, 1, 1, 1));
		}
	}

	private JPanel createLeftPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		ActionListener listener = e -> activateComponents();

		standardCsv = GUIFactory.getJRadioButton(STANDARD_CSV, ExportFile.HANDBASE.getName(), listener);
		JRadioButton otherCsv = GUIFactory.getJRadioButton(OTHER_CSV, ExportFile.TEXTFILE.getName(), listener);
		panel.add(General.addVerticalButtons(GUIFactory.getTitle("exportFormat"), standardCsv, otherCsv));

		String csvType = isExport ? pdaSettings.getTextFileFormat() : pdaSettings.getImportTextFileFormat();

		if (csvType.equals(OTHER_CSV)) {
			otherCsv.setSelected(true);
		} else {
			standardCsv.setSelected(true);
		}

		if (isExport) {
			splitFile = new JComboBox<>(
					new String[] { GUIFactory.getText("noLimit"), "512kB", "1024kB", "2048kB", "4096kB" });
			splitFile.setToolTipText(GUIFactory.getToolTip("splitFile"));
			splitFile.setSelectedIndex(pdaSettings.getMaxFileSize());

			Box box = Box.createHorizontalBox();
			box.add(GUIFactory.getJLabel("maxFileSize"));
			box.add(Box.createHorizontalStrut(10));
			box.add(splitFile);
			box.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("largeFileSplit")));
			panel.add(box);
		}
		return panel;
	}

	private JPanel createRightPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		separator = new JComboBox<>(new String[] { ",", GUIFactory.getText("tab"), ";", "|" });
		separator.setSelectedIndex(
				",\t;|".indexOf(isExport ? pdaSettings.getFieldSeparator() : pdaSettings.getImportFieldSeparator()));
		separator.setToolTipText(GUIFactory.getToolTip("separator"));

		delimiter = new JComboBox<>(new String[] { "\"", "'", "/", "$", "%" });
		delimiter.setSelectedItem(isExport ? pdaSettings.getTextDelimiter() : pdaSettings.getImportTextDelimiter());
		delimiter.setToolTipText(GUIFactory.getToolTip("delimiter"));

		result.add(GUIFactory.getJLabel("separator"), c.gridCell(0, 1, 0, 0));
		result.add(separator, c.gridCell(1, 1, 0, 0));
		result.add(GUIFactory.getJLabel("delimiter"), c.gridCell(0, 2, 0, 0));
		result.add(delimiter, c.gridCell(1, 2, 0, 0));

		if (isExport) {
			inclHeaders = GUIFactory.getJCheckBox("inclHeaders", pdaSettings.isUseHeader());
			inclLinebreaks = GUIFactory.getJCheckBox("inclLinebreaks", pdaSettings.isUseLinebreak());
			result.add(inclHeaders, c.gridCell(0, 3, 0, 0));
			result.add(inclLinebreaks, c.gridCell(0, 4, 0, 0));
		}

		result.add(Box.createVerticalGlue(), c.gridCell(0, 5, 0, 2));
		result.setBorder(BorderFactory.createTitledBorder("Other"));
		return result;
	}

	@Override
	public void activateComponents() {
		General.setEnabled(otherPanel, !standardCsv.isSelected());

		if (standardCsv.isSelected()) {
			separator.setSelectedIndex(0);
			delimiter.setSelectedIndex(0);
			if (isExport) {
				inclHeaders.setSelected(true);
				inclLinebreaks.setSelected(false);
			}
		}
	}
}