package application.dialog;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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

	private JFormattedTextField checkVersionDays;
	private JCheckBox noVersionCheck;
	private JCheckBox noImagePath;

	private JComboBox<String> dateFormat;
	private JComboBox<String> dateDelimiter;
	private JComboBox<String> timeFormat;
	private JComboBox<String> durationFormat;

	transient ActionListener funcSelectDir;

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
		Box result = Box.createVerticalBox();
		JPanel p1 = new JPanel(new GridBagLayout());
		JPanel p2 = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		dateFormat = new JComboBox<>(new String[] { "d M yy", "d M yyyy", "d MMM yy", "d MMM yyyy", "dd MM yy",
				"dd MM yyyy", "dd MMM yy", "dd MMM yyyy", "M d yy", "M d yyyy", "MM dd yy", "MM dd yyyy", "yy M d",
				"yy MM dd", "yy MMM dd", "yyyy M d", "yyyy MM dd", "yyyy MMM dd" });
		dateFormat.setSelectedItem(generalSettings.getDateFormat());
		dateFormat.setToolTipText(GUIFactory.getToolTip("dateFormat"));
		dateFormat.addActionListener(e -> showDateExample());

		dateDelimiter = GUIFactory.getJComboBox("dateDelimiter");
		dateDelimiter.setSelectedIndex(General.getID(generalSettings.getDateDelimiter(), DATE_DELIMITERS));
		dateDelimiter.addActionListener(e -> showDateExample());

		dateExample = GUIFactory.getJTextField("example", "");
		dateExample.setEditable(false);

		timeFormat = new JComboBox<>(new String[] { "h:mm", "hh:mm", "HH:mm", "h:mm:ss", "hh:mm:ss", "HH:mm:ss",
				"h:mm am/pm", "hh:mm am/pm", "h:mm:ss am/pm", "hh:mm:ss am/pm" });
		timeFormat.setSelectedItem(generalSettings.getTimeFormat());
		timeFormat.setToolTipText(GUIFactory.getToolTip("timeFormat"));
		timeFormat.addActionListener(e -> showTimeExample());

		timeExample = GUIFactory.getJTextField("example", "");
		timeExample.setEditable(false);

		durationFormat = new JComboBox<>(new String[] { "h:mm:ss", "mmm:ss" });
		durationFormat.setSelectedItem(generalSettings.getDurationFormat());
		durationFormat.setToolTipText(GUIFactory.getToolTip("durationFormat"));
		durationFormat.addActionListener(e -> showDurationExample());

		durationExample = GUIFactory.getJTextField("example", "");
		durationExample.setEditable(false);

		fdChecked = new JTextField(generalSettings.getCheckBoxChecked());
		fdChecked.setToolTipText(GUIFactory.getToolTip("selected"));

		fdUnchecked = new JTextField(generalSettings.getCheckBoxUnchecked());
		fdUnchecked.setToolTipText(GUIFactory.getToolTip("unselected"));

		JLabel label1 = GUIFactory.getJLabel("dateTime");
		label1.setFont(new Font("serif", Font.BOLD, 14));

		JLabel label2 = GUIFactory.getJLabel("checkBox");
		label2.setFont(new Font("serif", Font.BOLD, 14));

		JLabel label3 = GUIFactory.getJLabel("selected");
		label3.setIcon(General.createImageIcon("CheckboxSelected.gif"));

		JLabel label4 = GUIFactory.getJLabel("unselected");
		label4.setIcon(General.createImageIcon("CheckboxUnselected.gif"));

		JLabel label5 = GUIFactory.getJLabel("checkNewVersion");
		label5.setFont(new Font("serif", Font.BOLD, 14));

		JLabel label6 = GUIFactory.getJLabel("folders");
		label6.setFont(new Font("serif", Font.BOLD, 14));

		defaultFileFolder = GUIFactory.getJTextField("defaultFileFolder", generalSettings.getDefaultFileFolder());
		defaultBackupFolder = GUIFactory.getJTextField("defaultBackupFolder", generalSettings.getDefaultBackupFolder());
		defaultImageFolder = GUIFactory.getJTextField("defaultImageFolder", generalSettings.getDefaultImageFolder());
		defaultPdaFolder = GUIFactory.getJTextField("defaultPdaFolder", generalSettings.getDefaultPdaFolder());
		checkVersionDays = GUIFactory.getIntegerTextField("checkNewVersion", 1, 365,
				Integer.toString(generalSettings.getVersionDaysCheck()));

		noVersionCheck = GUIFactory.getJCheckBox("never", !generalSettings.isNoVersionCheck(),
				e -> checkVersionDays.setEnabled(!noVersionCheck.isSelected()));
		noVersionCheck.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		noVersionCheck.doClick();

		noImagePath = GUIFactory.getJCheckBox("noImagePath", generalSettings.isNoImagePath());

		JButton bt1 = GUIFactory.getJButton("browseFolder", funcSelectDir);
		bt1.setActionCommand("0");

		JButton bt2 = GUIFactory.getJButton("browseFolder", funcSelectDir);
		bt2.setActionCommand("1");

		JButton bt3 = GUIFactory.getJButton("browseFolder", funcSelectDir);
		bt3.setActionCommand("2");

		p1.add(Box.createHorizontalStrut(10), c.gridCell(0, 0, 0, 0));
		p1.add(label1, c.gridCell(1, 0, 0, 0));
		p1.add(GUIFactory.getJLabel("dateFormat"), c.gridCell(1, 1, 0, 0));

		Box box = Box.createHorizontalBox();
		box.add(dateFormat);
		box.add(dateDelimiter);

		p1.add(box, c.gridCell(1, 2, 0, 0));

		Box box0 = Box.createHorizontalBox();
		box0.add(dateExample);

		Box box1 = Box.createHorizontalBox();
		box1.add(timeFormat);
		box1.add(timeExample);

		Box box2 = Box.createHorizontalBox();
		box2.add(durationFormat);
		box2.add(durationExample);

		Box box3 = Box.createHorizontalBox();
		box3.add(noVersionCheck);
		box3.add(Box.createHorizontalStrut(10));
		box3.add(GUIFactory.getJLabel("every"));
		box3.add(checkVersionDays);
		box3.add(GUIFactory.getJLabel("days"));

		p1.add(box0, c.gridCell(2, 2, 0, 0));
		p1.add(Box.createVerticalStrut(10), c.gridCell(2, 3, 0, 0));
		p1.add(GUIFactory.getJLabel("timeFormat"), c.gridCell(1, 4, 0, 0));
		p1.add(GUIFactory.getJLabel("durationFormat"), c.gridCell(2, 4, 0, 0));

		p1.add(box1, c.gridCell(1, 5, 0, 0));
		p1.add(box2, c.gridCell(2, 5, 0, 0));
		p1.add(Box.createVerticalStrut(10), c.gridCell(1, 6, 0, 0));

		p1.add(label2, c.gridCell(1, 7, 0, 0));
		p1.add(label3, c.gridCell(1, 8, 0, 0));
		p1.add(label4, c.gridCell(2, 8, 0, 0));
		p1.add(fdChecked, c.gridCell(1, 9, 0, 0));
		p1.add(fdUnchecked, c.gridCell(2, 9, 0, 0));

		p1.add(Box.createVerticalStrut(10), c.gridCell(1, 10, 0, 0));
		p1.add(label5, c.gridCell(1, 11, 0, 0));
		p1.add(box3, c.gridCell(1, 12, 0, 0));
		p1.add(Box.createVerticalStrut(10), c.gridCell(1, 13, 0, 0));
		p1.add(noImagePath, c.gridmultipleCell(1, 14, 0, 0, 2, 0));
		p1.add(Box.createVerticalStrut(10), c.gridCell(1, 15, 0, 0));

		p2.add(Box.createHorizontalStrut(10), c.gridCell(0, 0, 0, 0));
		p2.add(label6, c.gridCell(1, 1, 0, 0));

		p2.add(GUIFactory.getJLabel("defaultFileFolder"), c.gridCell(1, 2, 0, 0));
		p2.add(defaultFileFolder, c.gridCell(2, 2, 2, 0));
		p2.add(bt1, c.gridCell(3, 2, 0, 0));
		p2.add(GUIFactory.getJLabel("defaultBackupFolder"), c.gridCell(1, 3, 0, 0));
		p2.add(defaultBackupFolder, c.gridCell(2, 3, 2, 0));
		p2.add(bt2, c.gridCell(3, 3, 0, 0));
		p2.add(GUIFactory.getJLabel("defaultImageFolder"), c.gridCell(1, 4, 0, 0));
		p2.add(defaultImageFolder, c.gridCell(2, 4, 2, 0));
		p2.add(bt3, c.gridCell(3, 4, 0, 0));
		p2.add(GUIFactory.getJLabel("defaultPdaFolder"), c.gridCell(1, 5, 0, 0));
		p2.add(defaultPdaFolder, c.gridCell(2, 5, 2, 0));

		result.add(Box.createVerticalStrut(10));
		result.add(p1);
		result.add(Box.createVerticalStrut(10));
		result.add(p2);
		result.add(Box.createVerticalStrut(10));
		result.setBorder(BorderFactory.createEtchedBorder());
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
		generalSettings.setVersionDaysCheck(Integer.parseInt(checkVersionDays.getText()));
		generalSettings.setNoVersionCheck(noVersionCheck.isSelected());
		generalSettings.setNoImagePath(noImagePath.isSelected());
	}

	@Override
	public void activateComponents() {
		showDateExample();
	}
}