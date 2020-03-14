package application.utils.gui;

import java.awt.event.ActionListener;
import java.util.function.Predicate;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import application.interfaces.TvBSoftware;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.ini.IniFile;
import application.utils.ini.IniItem;
import application.utils.ini.IniSection;

public class InternetSitesMenu {
	private ActionListener listener;
	private JMenu menu;
	private TvBSoftware software;

	public InternetSitesMenu(ActionListener listener, TvBSoftware software) {
		menu = GUIFactory.getJMenu("menuSites");
		this.listener = listener;
		this.software = software;
	}

	private void createSiteMenuItem(IniSection section) {
		JMenu result = new JMenu(section.getName());
		for (IniItem iniItem : section.getItems()) {
			String name = iniItem.getName();
			String value = iniItem.getValue();
			JMenuItem item = new JMenuItem(name);
			item.setActionCommand(value);
			item.setToolTipText(value);
			item.addActionListener(listener);
			result.add(item);

			if (name.toLowerCase().startsWith("support")) {
				software.setSupport(value);
			} else if (name.toLowerCase().startsWith("download")) {
				software.setDownload(value);
			}
		}
		menu.add(result);
	}

	public JMenu getInternetSitesMenu() {
		boolean isDBConvert = software == TvBSoftware.DBCONVERT;
		try {
			IniFile ini = General.getIniFile("config/InternetSites.ini");
			Predicate<IniSection> filter = isDBConvert
					? f -> f.getName().startsWith("FNProg") || f.getName().startsWith("Fans")
					: f -> f.getName().equals("DBConvert");

			ini.getSections().stream().filter(filter.negate()).forEach(this::createSiteMenuItem);
		} catch (Exception e) {
			// Should not have happened
		}
		return menu;
	}
}
