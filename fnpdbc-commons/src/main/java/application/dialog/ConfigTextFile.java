package application.dialog;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;

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
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigTextFile extends JPanel implements IConfigDb {
	/**
	 * Title: DbTextFile
	 *
	 * @description: TextFile Configuration parms Copyright: (c) 2005
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 8006516257526651336L;
	private JRadioButton buddyCsv;
	private JRadioButton otherCsv;

	private JCheckBox inclHeaders;
	private JCheckBox inclLinebreaks;
	private JComboBox<String> separator;
	private JComboBox<String> delimiter;
	private JComboBox<String> splitFile;

	private JPanel otherPanel;
	private boolean isExport;

	transient Profiles pdaSettings;
	transient ConfigDialog dialog;
	private BuddyExport buddy;

	public static final String STANDARD_CSV = "standardCsv";
	private static final String BUDDY_CSV = "buddyCsv";
	private static final String OTHER_CSV = "otherCsv";

	public enum BuddyExport {
		NONE("None"), BOOK_BUDDY("BookBuddy"), MOVIE_BUDDY("MovieBuddy"), MUSIC_BUDDY("MusixBuddy");

		private String name;

		BuddyExport(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public ConfigTextFile(Profiles pref, boolean isExport, BuddyExport buddy) {
		pdaSettings = pref;
		this.isExport = isExport;
		this.buddy = buddy;

		buildDialog();
		activateComponents();
	}

	// For imports only
	public ConfigTextFile(ConfigDialog configSoft, Profiles prefText) {
		this(prefText, false, BuddyExport.NONE);
		dialog = configSoft;
	}

	@Override
	public void setProperties() {
		int index = separator.getSelectedIndex();
		String separatorChar = ",\t;|".substring(index, index + 1);

		index = delimiter.getSelectedIndex();
		String delimiterChar = "\"'/$%".substring(index, index + 1);

		String csvType = STANDARD_CSV;
		if (otherCsv.isSelected()) {
			csvType = OTHER_CSV;
		} else if (buddyCsv.isSelected()) {
			csvType = BUDDY_CSV;
		}

		if (isExport) {
			pdaSettings.setUseHeader(inclHeaders.isSelected());
			pdaSettings.setUseLinebreak(buddyCsv.isSelected() || inclLinebreaks.isSelected());
			pdaSettings.setMaxFileSize(splitFile.getSelectedIndex());
			pdaSettings.setTextFileFormat(csvType);
			pdaSettings.setFieldSeparator(separatorChar);
			pdaSettings.setTextDelimiter(delimiterChar);
		} else {
			pdaSettings.setImportTextfields(separatorChar, delimiterChar, csvType);
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

		JRadioButton standardCsv = GUIFactory.getJRadioButton(STANDARD_CSV, ExportFile.HANDBASE.getName(),
				e -> activateComponents());

		otherCsv = GUIFactory.getJRadioButton(OTHER_CSV, ExportFile.TEXTFILE.getName(), e -> activateComponents());
		buddyCsv = GUIFactory.getJRadioButton(buddy.getName(), buddy.getName(), e -> activateComponents());

		if (buddy == BuddyExport.NONE) {
			panel.add(General.addVerticalButtons(GUIFactory.getTitle("exportFormat"), standardCsv, otherCsv));
		} else {
			panel.add(General.addVerticalButtons(GUIFactory.getTitle("exportFormat"), standardCsv, buddyCsv, otherCsv));
		}

		String csvType = isExport ? pdaSettings.getTextFileFormat() : pdaSettings.getImportTextFileFormat();

		switch (csvType) {
		case OTHER_CSV:
			otherCsv.setSelected(true);
			break;
		case BUDDY_CSV:
			buddyCsv.setSelected(true);
			break;
		default:
			standardCsv.setSelected(true);
			break;
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

		delimiter = new JComboBox<>(new String[] { General.TEXT_DELIMITER, "'", "/", "$", "%" });
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
		General.setEnabled(otherPanel, otherCsv.isSelected());

		if (!otherCsv.isSelected()) {
			separator.setSelectedIndex(0);
			delimiter.setSelectedIndex(0);
			if (isExport) {
				inclHeaders.setSelected(true);
				inclLinebreaks.setSelected(false);
			}
		}
	}
}