
package application.dialog;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigGeneral extends BasicDialog {
	/**
	 * Title: ConfigGeneral Description: Generic Class to setup the General
	 * Configuration parameters Copyright: (c) 2008-2015
	 *
	 * @author Tom van Breukelen
	 * @version 8.0
	 */
	private static final long serialVersionUID = 7553290524617159304L;
	private JTextField fdChecked;
	private JTextField fdUnchecked;
	private JTextField dateExample;
	private JTextField durationExample;
	private JTextField timeExample;
	private JTextField defaultFileFolder;
	private JTextField defaultBackupFolder;
	private JTextField defaultImageFolder;
	private JTextField defaultPdaFolder;

	private JCheckBox noImagePath;

	private JComboBox<String> dateFormat;
	private JComboBox<String> dateDelimiter;
	private JComboBox<String> timeFormat;
	private JComboBox<String> durationFormat;

	private int versionDaysCheck = 30;

	transient ActionListener funcSelectDir;
	private Font bold = new Font("serif", Font.BOLD, 14);
	private XGridBagConstraints c = new XGridBagConstraints();

	private static final String[] DATE_DELIMITERS = { "", " ", "/", "-", ".", "," };

	public ConfigGeneral() {
		init();
	}

	@Override
	protected void init() {
		init(GUIFactory.getTitle("configGeneral"));
		setHelpFile("general");

		funcSelectDir = e -> {
			JTextField t = null;
			switch (e.getActionCommand().charAt(0)) {
			case '0':
				t = defaultFileFolder;
				break;
			case '1':
				t = defaultBackupFolder;
				break;
			case '2':
				t = defaultImageFolder;
				break;
			default:
				t = defaultPdaFolder;
			}
			General.getSelectedFolder(ConfigGeneral.this, t, GUIFactory.getText("funcSelectDir"));
		};

		buildDialog();
		showDateExample();
		showTimeExample();
		showDurationExample();
		pack();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel();
		result.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

		JTabbedPane pane = new JTabbedPane(SwingConstants.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
		pane.addTab(GUIFactory.getText("conversions"), addDateTimeTab());
		pane.addTab("Internet", addInternetTab());
		pane.addTab(GUIFactory.getText("folders"), addFoldersTab());

		result.add(pane);
		return result;
	}

	private Component addDateTimeTab() {
		JPanel result = new JPanel(new GridBagLayout());
		final String EXAMPLE = "example";

		dateFormat = new JComboBox<>(new String[] { "d M yy", "d M yyyy", "d MMM yy", "d MMM yyyy", "dd MM yy",
				"dd MM yyyy", "dd MMM yy", "dd MMM yyyy", "M d yy", "M d yyyy", "MM dd yy", "MM dd yyyy", "yy M d",
				"yy MM dd", "yy MMM dd", "yyyy M d", "yyyy MM dd", "yyyy MMM dd" });
		dateFormat.setSelectedItem(generalSettings.getDateFormat());
		dateFormat.setToolTipText(GUIFactory.getToolTip("dateFormat"));
		dateFormat.addActionListener(e -> showDateExample());

		dateDelimiter = GUIFactory.getJComboBox("dateDelimiter");
		dateDelimiter.setSelectedIndex(General.getID(generalSettings.getDateDelimiter(), DATE_DELIMITERS));
		dateDelimiter.addActionListener(e -> showDateExample());

		dateExample = GUIFactory.getJTextField(EXAMPLE, "");
		dateExample.setEditable(false);

		timeFormat = new JComboBox<>(new String[] { "h:mm", "hh:mm", "HH:mm", "h:mm:ss", "hh:mm:ss", "HH:mm:ss",
				"h:mm am/pm", "hh:mm am/pm", "h:mm:ss am/pm", "hh:mm:ss am/pm" });
		timeFormat.setSelectedItem(generalSettings.getTimeFormat());
		timeFormat.setToolTipText(GUIFactory.getToolTip("timeFormat"));
		timeFormat.addActionListener(e -> showTimeExample());

		timeExample = GUIFactory.getJTextField(EXAMPLE, "");
		timeExample.setEditable(false);

		durationFormat = new JComboBox<>(new String[] { "h:mm:ss", "mmm:ss" });
		durationFormat.setSelectedItem(generalSettings.getDurationFormat());
		durationFormat.setToolTipText(GUIFactory.getToolTip("durationFormat"));
		durationFormat.addActionListener(e -> showDurationExample());

		durationExample = GUIFactory.getJTextField(EXAMPLE, "");
		durationExample.setEditable(false);

		fdChecked = new JTextField(generalSettings.getCheckBoxChecked());
		fdChecked.setToolTipText(GUIFactory.getToolTip("selected"));
		fdUnchecked = new JTextField(generalSettings.getCheckBoxUnchecked());
		fdUnchecked.setToolTipText(GUIFactory.getToolTip("unselected"));

		defaultFileFolder = GUIFactory.getJTextField("defaultFileFolder", generalSettings.getDefaultFileFolder());
		defaultBackupFolder = GUIFactory.getJTextField("defaultBackupFolder", generalSettings.getDefaultBackupFolder());
		defaultImageFolder = GUIFactory.getJTextField("defaultImageFolder", generalSettings.getDefaultImageFolder());
		defaultPdaFolder = GUIFactory.getJTextField("defaultPdaFolder", generalSettings.getDefaultPdaFolder());
		noImagePath = GUIFactory.getJCheckBox("noImagePath", generalSettings.isNoImagePath());

		result.add(Box.createVerticalStrut(10), c.gridCell(0, 0, 0, 0));
		result.add(GUIFactory.getJLabel("dateFormat", bold), c.gridCell(0, 1, 0, 0));
		result.add(dateFormat, c.gridCell(0, 2, 0, 0));
		result.add(dateDelimiter, c.gridCell(1, 2, 0, 0));
		result.add(dateExample, c.gridCell(2, 2, 0, 0));
		result.add(Box.createVerticalStrut(10), c.gridCell(2, 3, 0, 0));

		result.add(GUIFactory.getJLabel("timeFormat", bold), c.gridCell(0, 4, 0, 0));
		result.add(GUIFactory.getJLabel("durationFormat", bold), c.gridCell(2, 4, 0, 0));
		result.add(timeFormat, c.gridCell(0, 5, 0, 0));
		result.add(timeExample, c.gridCell(1, 5, 0, 0));
		result.add(durationFormat, c.gridCell(2, 5, 0, 0));
		result.add(durationExample, c.gridCell(3, 5, 3, 0));

		Box box1 = Box.createHorizontalBox();
		box1.add(GUIFactory.getJLabel("true"));
		box1.add(Box.createHorizontalStrut(5));
		box1.add(fdChecked);

		Box box2 = Box.createHorizontalBox();
		box2.add(GUIFactory.getJLabel("false"));
		box2.add(Box.createHorizontalStrut(5));
		box2.add(fdUnchecked);

		result.add(Box.createVerticalStrut(10), c.gridCell(0, 6, 0, 0));
		result.add(GUIFactory.getJLabel("boolean", bold), c.gridCell(0, 7, 0, 0));
		result.add(box1, c.gridCell(0, 8, 0, 0));
		result.add(box2, c.gridCell(1, 8, 0, 0));
		result.add(Box.createVerticalStrut(20), c.gridCell(0, 9, 0, 0));

		result.setBorder(BorderFactory.createTitledBorder(GUIFactory.getText("conversions")));
		return result;
	}

	private Component addInternetTab() {
		versionDaysCheck = generalSettings.getVersionDaysCheck();

		ActionListener listener = e -> versionDaysCheck = Integer.valueOf(e.getActionCommand());
		JRadioButton bt1 = GUIFactory.getJRadioButton("oncePerDay", "1", listener);
		JRadioButton bt2 = GUIFactory.getJRadioButton("oncePerWeek", "7", listener);
		JRadioButton bt3 = GUIFactory.getJRadioButton("oncePerMonth", "30", listener);
		JRadioButton bt4 = GUIFactory.getJRadioButton("never", "0", listener);

		JPanel result = General.addVerticalButtons(GUIFactory.getText("checkNewVersion"), bt1, bt2, bt3, bt4);

		switch (versionDaysCheck) {
		case 0:
			bt4.setSelected(true);
			break;
		case 1:
			bt1.setSelected(true);
			break;
		case 7:
			bt2.setSelected(true);
			break;
		default:
			bt3.setSelected(true);
			break;
		}

		return result;
	}

	private Component addFoldersTab() {
		JPanel result = new JPanel(new GridBagLayout());
		final String BROWSE_FOLDER = "browseFolder";

		result.add(Box.createHorizontalStrut(10), c.gridCell(0, 0, 0, 0));
		result.add(GUIFactory.getJLabel("defaultFileFolder"), c.gridCell(0, 1, 0, 0));
		result.add(defaultFileFolder, c.gridCell(1, 1, 2, 0));
		result.add(GUIFactory.getJButton(BROWSE_FOLDER, "0", funcSelectDir), c.gridCell(2, 1, 0, 0));
		result.add(GUIFactory.getJLabel("defaultBackupFolder"), c.gridCell(0, 2, 0, 0));
		result.add(defaultBackupFolder, c.gridCell(1, 2, 2, 0));
		result.add(GUIFactory.getJButton(BROWSE_FOLDER, "1", funcSelectDir), c.gridCell(2, 2, 0, 0));
		result.add(GUIFactory.getJLabel("defaultImageFolder"), c.gridCell(0, 3, 0, 0));
		result.add(defaultImageFolder, c.gridCell(1, 3, 2, 0));
		result.add(GUIFactory.getJButton(BROWSE_FOLDER, "2", funcSelectDir), c.gridCell(2, 3, 0, 0));
		result.add(GUIFactory.getJLabel("defaultPdaFolder"), c.gridCell(0, 4, 0, 0));
		result.add(defaultPdaFolder, c.gridCell(1, 4, 2, 0));
		result.add(Box.createVerticalStrut(10), c.gridCell(0, 5, 0, 0));
		result.add(noImagePath, c.gridmultipleCell(0, 6, 3, 0, 0, 0));
		result.setBorder(BorderFactory.createTitledBorder(GUIFactory.getText("folders")));
		return result;
	}

	private void showDateExample() {
		String format = dateFormat.getSelectedItem().toString();
		String delimiter = DATE_DELIMITERS[dateDelimiter.getSelectedIndex()];
		DateTimeFormatter sd = DateTimeFormatter.ofPattern(format.replace(" ", delimiter), Locale.ENGLISH);
		dateExample.setText(LocalDate.now().format(sd));
	}

	private void showTimeExample() {
		String[] times = { "9:15", "09:15", "21:15", "9:15:22", "09:15:22", "21:15:22", "9:15 pm", "09:15 pm",
				"9:15:22 pm", "09:15:22 pm" };
		timeExample.setText(times[timeFormat.getSelectedIndex()]);
	}

	private void showDurationExample() {
		durationExample.setText(durationFormat.getSelectedIndex() == 0 ? "2:10:15" : "170:15");
	}

	@Override
	protected void save() throws Exception {
		generalSettings.setDateFormat(dateFormat.getSelectedItem().toString());
		generalSettings.setDateDelimiter(DATE_DELIMITERS[dateDelimiter.getSelectedIndex()]);
		generalSettings.setTimeFormat(timeFormat.getSelectedItem().toString());
		generalSettings.setDurationFormat(durationFormat.getSelectedItem().toString());
		generalSettings.setCheckBoxChecked(fdChecked.getText().trim());
		generalSettings.setCheckBoxUnchecked(fdUnchecked.getText().trim());
		generalSettings.setDefaultBackupFolder(defaultBackupFolder.getText().trim());
		generalSettings.setDefaultFileFolder(defaultFileFolder.getText().trim());
		generalSettings.setDefaultImageFolder(defaultImageFolder.getText().trim());
		generalSettings.setDefaultPdaFolder(defaultPdaFolder.getText().trim());
		generalSettings.setVersionDaysCheck(versionDaysCheck);
		generalSettings.setNoImagePath(noImagePath.isSelected());
	}

	@Override
	public void activateComponents() {
		showDateExample();
	}
}