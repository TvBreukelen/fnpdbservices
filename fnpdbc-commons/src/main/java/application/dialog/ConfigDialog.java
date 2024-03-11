package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import application.dialog.ConfigTextFile.BuddyExport;
import application.interfaces.ExportFile;
import application.interfaces.IDatabaseFactory;
import application.interfaces.IExportProcess;
import application.model.FilterData;
import application.model.SortData;
import application.preferences.Databases;
import application.preferences.Profiles;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;
import dbengine.utils.DatabaseHelper;

public abstract class ConfigDialog extends BasicDialog {
	private static final long serialVersionUID = -7620633992759071770L;

	protected JTextField profile = new JTextField();
	protected JButton btFilter;
	protected JButton btSortOrder;
	protected JTabbedPane tabPane;
	protected ScFieldSelect fieldSelect;
	protected ScConfigDb configDb;

	protected DatabaseHelper dbVerified;
	protected Profiles profiles;
	protected Databases dbSettings;
	protected ExportFile myExportFile;
	protected IDatabaseFactory factory;

	protected static final String FUNC_NEW = "funcNew";

	protected Map<String, FilterData> filterDataMap = new HashMap<>();
	protected Map<String, SortData> sortDataMap = new HashMap<>();

	protected boolean isNewProfile = false;
	protected boolean isFileValid = false;
	protected String profileID = General.EMPTY_STRING;

	protected String myView;

	protected void init(IDatabaseFactory dbFactory, Profiles profiles) {
		super.init(isNewProfile ? GUIFactory.getTitle(FUNC_NEW)
				: profiles.getProfileID() + General.SPACE + GUIFactory.getText("configuration"), 6);

		this.profiles = profiles;
		this.factory = dbFactory;

		dbSettings = profiles.getDbSettings();
		dbVerified = profiles.getFromDatabase();

		btSortOrder = GUIFactory.createToolBarButton(GUIFactory.getTitle("sortOrder"), "Sort.png", e -> {
			ConfigSort sort = new ConfigSort(dbFactory, sortDataMap.computeIfAbsent(myView, s -> new SortData()));
			sort.setVisible(true);
		});

		btFilter = GUIFactory.createToolBarButton(GUIFactory.getTitle("filter"), "Filter.png", e -> {
			ConfigFilter filter = new ConfigFilter(dbFactory,
					filterDataMap.computeIfAbsent(myView, f -> new FilterData()));
			filter.setVisible(true);
		});

		DatabaseHelper toDb = profiles.getToDatabase();
		fieldSelect = new ScFieldSelect(dbFactory, toDb.getDatabaseType());
		configDb = new ScConfigDb(this, fieldSelect, profiles);
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

	public BuddyExport getBuddyExport() {
		return BuddyExport.NONE;
	}

	@Override
	public void activateComponents() {
		isFileValid = factory.isConnected();
		boolean isValidProfile = true;

		profileID = profile.getText().trim();
		DatabaseHelper helper = configDb == null ? new DatabaseHelper(General.EMPTY_STRING, ExportFile.TEXTFILE)
				: configDb.getDatabaseHelper();
		myExportFile = helper.getDatabaseType();

		if (isFileValid && isNewProfile) {
			isValidProfile = !profileID.isEmpty();
			if (isValidProfile) {
				isValidProfile = !profiles.profileExists(myExportFile.getName(), profileID);
			}
		}

		if (isValidProfile) {
			isValidProfile = !helper.getDatabase().isBlank();
		}

		btSave.setEnabled(isFileValid && isValidProfile);
		btFilter.setEnabled(isFileValid);
		btSortOrder.setEnabled(isFileValid);
	}

	protected void checkDuplicatelFieldNames() throws FNProgException {
		// Check for duplicate field names
		if (myExportFile.isSqlDatabase()) {
			Set<String> fields = new HashSet<>();
			for (BasisField f : fieldSelect.getFieldList()) {
				if (fields.contains(f.getFieldHeader())) {
					throw FNProgException.getException("duplicateField", f.getFieldHeader());
				}
				fields.add(f.getFieldHeader());
			}
		}
	}

	protected abstract IExportProcess getExportProcess();

	protected void verifyDatabase() {
		// Only used by DBConvert
	}
}
