package application.utils.gui;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import application.interfaces.TvBSoftware;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.ini.IniFile;

public class InternetSitesMenu {
	private ActionListener listener;
	private JMenu menu;
	private TvBSoftware software;

	public InternetSitesMenu(ActionListener listener, TvBSoftware software) {
		menu = GUIFactory.getJMenu("menuSites");
		this.listener = listener;
		this.software = software;
	}

	public JMenu getInternetSitesMenu() {
		JMenu soft = new JMenu(software.getName());
		menu.add(soft);
		JMenuItem menuItem = new JMenuItem("Support Site");
		menuItem.setActionCommand(software.getSupport());
		menuItem.setToolTipText(software.getSupport());
		menuItem.addActionListener(listener);
		soft.add(menuItem);

		IniFile in = General.getIniFile("config/InternetSites.ini");

		in.getSections().forEach(ini -> {
			if (!(ini.getName().startsWith("Fans") && software == TvBSoftware.DBCONVERT)) {
				JMenu result = new JMenu(ini.getName());
				menu.add(result);
				ini.getItems().forEach(item -> {
					String name = item.getName();
					String value = item.getValue();
					JMenuItem it = new JMenuItem(name);
					it.setActionCommand(value);
					it.setToolTipText(value);
					it.addActionListener(listener);
					result.add(it);
				});
			}
		});
		return menu;
	}
}
