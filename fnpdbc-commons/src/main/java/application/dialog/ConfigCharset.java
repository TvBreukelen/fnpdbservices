package application.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import application.interfaces.IEncoding;
import application.preferences.GeneralSettings;
import application.utils.GUIFactory;
import application.utils.General;

public class ConfigCharset extends BasicDialog {
	/**
	 * Title: ConfigCharset Description: Class for Encoding configuration Copyright:
	 * (c) 2004-2006
	 *
	 * @author Tom van Breukelen
	 * @version 5.6
	 */
	private static final long serialVersionUID = -9092039546465074319L;
	private JTextPane description;
	private Properties charset;
	private JList<String> charSets;
	private IEncoding encodingPref;

	public ConfigCharset() {
		encodingPref = GeneralSettings.getInstance();
		init();
	}

	public ConfigCharset(Component dialog, IEncoding pref) {
		super(dialog);
		encodingPref = pref;
		init();
	}

	@Override
	protected void init() {
		init(GUIFactory.getTitle("configCharset"));
		setHelpFile("encoding");
		loadCharsetProperties();
		buildDialog();
		pack();

		String encoding = encodingPref.getEncoding();
		charSets.setSelectedValue(encoding.isEmpty() ? " " : encoding, true);
	}

	@Override
	protected Component createCenterPanel() {
		Box result = Box.createHorizontalBox();

		charSets = new JList<>(new Vector<>(General.getCharacterSets()));

		charSets.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				String key = charSets.getSelectedValue();
				StringBuilder aliases = new StringBuilder();
				Charset set;

				if (key.equals(" ")) {
					key = "Default";
					set = Charset.defaultCharset();
				} else {
					set = Charset.forName(key);
				}

				for (Object o : set.aliases()) {
					aliases.append(o);
					aliases.append(", ");
				}

				int len = aliases.length();
				aliases.delete(len - 2, len);
				description.setText(charset.getProperty(key, "Same as: " + aliases.toString()));
			}
		});
		JScrollPane sc1 = new JScrollPane(charSets);

		description = new JTextPane();
		description.setEditable(false);
		JScrollPane sc2 = new JScrollPane(description);

		Box box1 = Box.createVerticalBox();
		box1.add(sc1);
		box1.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("configCharset")));

		Box box2 = Box.createVerticalBox();
		box2.add(sc2);
		box2.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("description")));
		box2.setPreferredSize(new Dimension(200, 100));

		result.add(box1);
		result.add(Box.createHorizontalStrut(5));
		result.add(box2);
		return result;
	}

	@Override
	protected void save() throws Exception {
		encodingPref.setEncoding(charSets.getSelectedValue().trim());
	}

	private void loadCharsetProperties() {
		try {
			charset = General.getProperties("Encoding");
		} catch (Exception e) {
			General.showMessage(this, GUIFactory.getMessage("loadingError", e.getMessage()),
					GUIFactory.getTitle("loadingError"), true);
		}
	}
}
