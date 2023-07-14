package application.dialog;

import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import application.interfaces.IConfigDb;
import application.preferences.Profiles;
import application.utils.GUIFactory;

public class XBaseCharsets extends JPanel implements IConfigDb {
	private static final long serialVersionUID = -3737375965488553609L;
	private Profiles pdaSettings;
	private List<Integer> langList = Arrays.asList(3, 203, 202, 31, 201, 19, 7);
	private JComboBox<String> jc = new JComboBox<>(new String[] { "Windows Latin-1", "Modern Greek", "Turkish",
			"Central and Eastern European", "Cyrillic languages (like Russian)", "Japanese", "Arabic" });

	public XBaseCharsets(Profiles pref) {
		pdaSettings = pref;
		buildDialog();
	}

	private void buildDialog() {
		setLayout(new FlowLayout(FlowLayout.LEFT));
		setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("languageDriver")));
		jc.setSelectedIndex(langList.indexOf(pdaSettings.getLanguageDriver()));
		add(jc);
	}

	@Override
	public void setProperties() throws Exception {
		pdaSettings.setLanguageDriver(langList.get(jc.getSelectedIndex()));
	}

}
