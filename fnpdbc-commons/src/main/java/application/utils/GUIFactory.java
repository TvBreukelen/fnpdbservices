package application.utils;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.text.NumberFormatter;

import org.apache.commons.lang3.StringUtils;

import application.preferences.GeneralSettings;
import application.utils.gui.JHyperLink;

public final class GUIFactory {
	private static Properties pScreens;

	private GUIFactory() {
		refresh();
	}

	public static void refresh() {
		String language = GeneralSettings.getInstance().getLanguage();
		pScreens = General.getLanguages(language);

		switch (language.charAt(0)) {
		case 'D':
			Locale.setDefault(Locale.GERMAN);
			break;
		case 'N':
			Locale.setDefault(new Locale("nl"));
			break;
		default:
			Locale.setDefault(Locale.ENGLISH);
			break;
		}
	}

	public static JComboBox<String> getJComboBox(String resourceID) {
		JComboBox<String> result = new JComboBox<>(getArray(resourceID));
		result.setToolTipText(getToolTip(resourceID));
		return result;
	}

	public static JLabel getJLabel(String resourceID) {
		return new JLabel(getText(resourceID));
	}

	public static JLabel getJLabel(String resourceID, Icon icon) {
		JLabel result = getJLabel(getText(resourceID));
		result.setIcon(icon);
		return result;
	}

	public static JLabel getJLabel(String resourceID, Font font) {
		JLabel result = getJLabel(getText(resourceID));
		result.setFont(font);
		return result;
	}

	public static JTextField getJTextField(String resourceID, String text) {
		JTextField result;
		if (StringUtils.isEmpty(text)) {
			result = new JTextField(10);
		} else {
			result = new JTextField();
			result.setText(text);

		}
		result.setToolTipText(getToolTip(resourceID));
		return result;
	}

	public static JPasswordField getJPasswordField(String resourceID, String text) {
		JPasswordField result;
		if (StringUtils.isEmpty(text)) {
			result = new JPasswordField(10);
		} else {
			result = new JPasswordField();
			result.setText(text);

		}
		result.setToolTipText(getToolTip(resourceID));
		return result;
	}

	public static JFormattedTextField getIntegerTextField(String resourceID, int min, int max, String text) {
		NumberFormat intFormat = NumberFormat.getIntegerInstance();

		NumberFormatter numberFormatter = new NumberFormatter(intFormat);
		numberFormatter.setValueClass(Integer.class);
		numberFormatter.setAllowsInvalid(false);
		numberFormatter.setMinimum(min); // Optional
		numberFormatter.setMaximum(max);

		JFormattedTextField result = new JFormattedTextField(numberFormatter);
		if (StringUtils.isNotEmpty(text)) {
			result.setText(text);
		}

		result.setToolTipText(getToolTip(resourceID));
		return result;
	}

	public static JFormattedTextField getDoubleTextField(String resourceID, double min, String text) {
		NumberFormat intFormat = NumberFormat.getCurrencyInstance();

		NumberFormatter numberFormatter = new NumberFormatter(intFormat);
		numberFormatter.setValueClass(Double.class);
		numberFormatter.setAllowsInvalid(false);
		numberFormatter.setMinimum(min); // Optional

		JFormattedTextField result = new JFormattedTextField(numberFormatter);
		if (StringUtils.isNotEmpty(text)) {
			result.setText(text);
		}

		result.setToolTipText(getToolTip(resourceID));
		return result;
	}

	public static JButton getJButton(String resourceID, ActionListener action) {
		JButton result = new JButton(getText(resourceID));
		result.setToolTipText(getToolTip(resourceID));
		result.addActionListener(action);
		setMnemonic(result, resourceID);
		return result;
	}

	public static JButton getJButton(String resourceID, String actionCommand, ActionListener action) {
		JButton result = getJButton(resourceID, action);
		result.setActionCommand(actionCommand);
		return result;
	}

	public static JRadioButton getJRadioButton(String resourceID, ActionListener action) {
		JRadioButton result = new JRadioButton(getText(resourceID));
		result.setToolTipText(getToolTip(resourceID));
		result.addActionListener(action);
		setMnemonic(result, resourceID);
		return result;
	}

	public static JRadioButton getJRadioButton(String resourceID, String actionCommand, ActionListener action) {
		JRadioButton result = getJRadioButton(resourceID, action);
		result.setActionCommand(actionCommand);
		return result;
	}

	public static JCheckBox getJCheckBox(String resourceID, boolean value) {
		JCheckBox result = new JCheckBox(getText(resourceID));
		result.setToolTipText(getToolTip(resourceID));
		setMnemonic(result, resourceID);
		result.setSelected(value);
		return result;
	}

	public static JCheckBox getJCheckBox(String resourceID, boolean value, ActionListener action) {
		JCheckBox result = getJCheckBox(resourceID, value);
		result.addActionListener(action);
		return result;
	}

	public static JHyperLink getJHyperLink(String resourceID, ActionListener action) {
		JHyperLink result = new JHyperLink();
		result.setText(getText(resourceID));
		result.setToolTipText(getToolTip(resourceID));
		result.addActionListener(action);
		return result;
	}

	public static JHyperLink getJHyperLink(String resourceID, ActionListener action, ImageIcon icon) {
		JHyperLink result = getJHyperLink(resourceID, action);
		result.setIcon(icon);
		return result;
	}

	public static JMenu getJMenu(String resourceID) {
		JMenu result = new JMenu(getText(resourceID));
		result.setToolTipText(getToolTip(resourceID));
		setMnemonic(result, resourceID);
		return result;
	}

	public static JMenuItem getJMenuItem(String resourceID, ActionListener action) {
		JMenuItem result = new JMenuItem(getText(resourceID));
		result.setToolTipText(getToolTip(resourceID));
		result.addActionListener(action);
		setMnemonic(result, resourceID);
		return result;
	}

	private static void setMnemonic(AbstractButton button, String resourceID) {
		String mnemonic = pScreens.getProperty(resourceID + ".mnemonic", "");
		if (mnemonic.length() == 1) {
			button.setMnemonic(mnemonic.charAt(0));
		}
	}

	public static String getToolTip(String resourceID) {
		if (resourceID.isEmpty()) {
			return resourceID;
		}

		String result = pScreens.getProperty(resourceID + ".tooltip", resourceID);
		if (result.equals(resourceID)) {
			result = pScreens.getProperty(resourceID + ".text", resourceID);
			if (result.equals(resourceID)) {
				result = pScreens.getProperty(resourceID + ".title", resourceID);
			}
		}
		return result;
	}

	public static String getText(String resourceID) {
		return pScreens.getProperty(resourceID + ".text", resourceID);
	}

	public static String getTitle(String resourceID) {
		return pScreens.getProperty(resourceID + ".title", resourceID);
	}

	public static String getTexts(String resourceID) {
		StringBuilder buf = new StringBuilder(2000);
		for (int i = 0; i < 25; i++) {
			String s = pScreens.getProperty(resourceID + ".text" + i, "");
			if (s.isEmpty()) {
				break;
			}
			buf.append(s);
			buf.append(" ");
		}
		return buf.toString().trim();
	}

	public static String[] getArray(String resourceID) {
		String s = pScreens.getProperty(resourceID + ".array", resourceID);
		return s.split(", ");
	}

	public static String getMessage(String resourceID, String... parms) {
		StringBuilder result = new StringBuilder(pScreens.getProperty(resourceID + ".mesg", resourceID));
		int parmLength = parms.length;

		for (int i = 0; i < parmLength; i++) {
			String s = "%" + i;
			int index = result.indexOf(s);
			if (index != -1) {
				result.delete(index, index + s.length());
				result.insert(index, parms[i]);
			}
		}

		return result.toString();
	}
}