package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import application.interfaces.IDatabaseFactory;
import application.model.FilterData;
import application.model.SortData;
import application.preferences.Databases;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import dbengine.utils.DatabaseHelper;

public abstract class ConfigDialog extends BasicDialog {
	private static final long serialVersionUID = -7620633992759071770L;

	protected JButton btFilter;
	protected JButton btSortOrder;
	protected JTabbedPane tabPane;
	protected ScFieldSelect fieldSelect;

	protected DatabaseHelper dbVerified;
	protected DatabaseHelper dbExport;
	protected Profiles profiles;
	protected Databases dbSettings;

	protected static final String FUNC_NEW = "funcNew";

	protected Map<String, FilterData> filterDataMap = new HashMap<>();
	protected Map<String, SortData> sortDataMap = new HashMap<>();

	protected boolean isNewProfile = false;
	protected String myView;

	protected void init(IDatabaseFactory dbFactory, Profiles profiles) {
		btSortOrder = GUIFactory.createToolBarButton(GUIFactory.getTitle("sortOrder"), "Sort.png", e -> {
			ConfigSort sort = new ConfigSort(dbFactory, sortDataMap.computeIfAbsent(myView, s -> new SortData()));
			sort.setVisible(true);
		});

		btFilter = GUIFactory.createToolBarButton(GUIFactory.getTitle("filter"), "Filter.png", e -> {
			ConfigFilter filter = new ConfigFilter(dbFactory,
					filterDataMap.computeIfAbsent(myView, f -> new FilterData()));
			filter.setVisible(true);
		});

		fieldSelect = new ScFieldSelect(dbFactory);
		this.profiles = profiles;

		dbSettings = profiles.getDbSettings();
		dbVerified = profiles.getFromDatabase();
		dbExport = profiles.getToDatabase();

		super.init(isNewProfile ? GUIFactory.getTitle(FUNC_NEW)
				: profiles.getProfileID() + General.SPACE + GUIFactory.getText("configuration"), 6);
	}

	@Override
	protected Component createCenterPanel() {
		tabPane = new JTabbedPane();
		tabPane.add(GUIFactory.getText("exportFiles"), createSelectionPanel());
		tabPane.add(GUIFactory.getText("exportFields"), createFieldPanel());
		tabPane.setEnabledAt(1, false);
		return tabPane;
	}

	protected abstract Component createSelectionPanel();

	protected JPanel createFieldPanel() {
		JPanel result = new JPanel(new BorderLayout());
		result.add(fieldSelect.createFieldPanel(), BorderLayout.CENTER);
		return result;
	}
}
