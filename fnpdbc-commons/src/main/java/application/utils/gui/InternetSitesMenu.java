package application.utils.gui;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import application.interfaces.TvBSoftware;
import application.utils.GUIFactory;
import application.utils.General;

public class InternetSitesMenu {
	private ActionListener _listener;
	private JMenu _menu;
	private TvBSoftware _software;

	public InternetSitesMenu(ActionListener listener, TvBSoftware software) {
		_menu = GUIFactory.getJMenu("menuSites");
		_listener = listener;
		_software = software;
	}

	private void createSiteMenuItem(Section section) {
		JMenu result = new JMenu(section.getName());
		for (String name : section.keySet()) {
			JMenuItem item = new JMenuItem(name);
			String value = section.get(name);
			item.setActionCommand(value);
			item.setToolTipText(value);
			item.addActionListener(_listener);
			result.add(item);

			if (name.toLowerCase().startsWith("support")) {
				_software.setSupport(value);
			} else if (name.toLowerCase().startsWith("download")) {
				_software.setDownload(value);
			}
		}
		_menu.add(result);
	}

	public JMenu getInternetSitesMenu() {
		boolean isDBConvert = _software == TvBSoftware.DBCONVERT;
		try {
			Ini ini = General.getIniFile("config/InternetSites.ini");

			for (Section section : ini.values()) {
				if (isDBConvert) {
					if (section.getName().startsWith("FNProg") || section.getName().startsWith("Fans")) {
						continue;
					}
				} else if (section.getName().equals("DBConvert")) {
					continue;
				}
				createSiteMenuItem(section);
			}
		} catch (Exception e) {
			// Should not have happened
		}
		return _menu;
	}
}
