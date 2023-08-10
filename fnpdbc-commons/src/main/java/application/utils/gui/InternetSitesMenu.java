package application.utils.gui;

import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import application.interfaces.TvBSoftware;
import application.utils.GUIFactory;
import application.utils.General;

public class InternetSitesMenu {
	private ActionListener listener;
	private JMenu menu;
	private TvBSoftware software;

	public InternetSitesMenu(ActionListener listener, TvBSoftware software) {
		menu = GUIFactory.getJMenu("menuSites");
		this.listener = listener;
		this.software = software;
	}

	@SuppressWarnings("unchecked")
	public JMenu getInternetSitesMenu() {
		JMenu soft = new JMenu(software.getName());
		menu.add(soft);
		JMenuItem menuItem = new JMenuItem("Support Site");
		menuItem.setActionCommand(software.getSupport());
		menuItem.setToolTipText(software.getSupport());
		menuItem.addActionListener(listener);
		soft.add(menuItem);

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Map<String, Object> map;
		try {
			map = mapper.readValue(General.getInputStreamReader("config/InternetSites.yaml"), Map.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		List<Map<String, Object>> internetSites = (List<Map<String, Object>>) map.entrySet().iterator().next()
				.getValue();

		internetSites.forEach(entry -> {
			String name = entry.get("name").toString();
			if (!(name.startsWith("Fans") && software == TvBSoftware.DBCONVERT)) {
				JMenu result = new JMenu(name);
				menu.add(result);

				List<Map<String, Object>> webSites = (List<Map<String, Object>>) entry.get("website");
				webSites.forEach(site -> {
					site.entrySet().forEach(e -> {
						JMenuItem it = new JMenuItem(e.getKey());
						it.setActionCommand(e.getValue().toString());
						it.setToolTipText(it.getActionCommand());
						it.addActionListener(listener);
						result.add(it);
					});
				});
			}
		});
		return menu;
	}
}
