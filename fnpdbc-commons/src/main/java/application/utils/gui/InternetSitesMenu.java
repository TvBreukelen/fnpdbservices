package application.utils.gui;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import application.interfaces.TvBSoftware;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.ini.IniFile;
import application.utils.ini.IniItem;
import application.utils.ini.IniSection;

public class InternetSitesMenu {
	private ActionListener _listener;
	private JMenu _menu;
	private TvBSoftware _software;

	public InternetSitesMenu(ActionListener listener, TvBSoftware software) {
		_menu = GUIFactory.getJMenu("menuSites");
		_listener = listener;
		_software = software;
	}

	private void createSiteMenuItem(IniSection section) {
		JMenu result = new JMenu(section.getName());
		for (IniItem iniItem : section.getItems()) {
			String name = iniItem.getName();
			String value = iniItem.getValue();
			JMenuItem item = new JMenuItem(name);
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
			IniFile ini = General.getIniFile("config/InternetSites.ini");

			for (IniSection section : ini.getSections()) {
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
