package dbconvert.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import application.dialog.BasicDialog;
import application.dialog.ConfigFilter;
import application.dialog.ConfigSort;
import application.dialog.ConfigTextFile;
import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.ScConfigDb;
import application.dialog.ScFieldSelect;
import application.interfaces.ExportFile;
import application.interfaces.IConfigSoft;
import application.interfaces.TvBSoftware;
import application.model.FilterData;
import application.model.ProfileObject;
import application.model.ProjectModel;
import application.model.SortData;
import application.preferences.Databases;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbconvert.preferences.PrefDBConvert;
import dbconvert.software.XConverter;
import dbengine.utils.DatabaseHelper;

public class ConfigSoft extends BasicDialog implements IConfigSoft {
	/**
	 * Title: ConfigXConverter Description: XConverter Configuration program
	 * Copyright: (c) 2004-2020
	 *
	 * @author Tom van Breukelen
	 * @version 5+
	 */
	private static final long serialVersionUID = -4831514718413166627L;
	private JTabbedPane tabPane;

	private JTextField profile;
	private JTextField fdDatabase = new JTextField();

	private JButton btFilter;
	private JButton btSortOrder;
	private JButton btRelationships;

	private JComboBox<String> bDatabase;
	private JComboBox<String> bTablesWorksheets;
	private JComboBox<String> cbDatabases;
	private JLabel lTablesWorkSheets;

	transient ActionListener funcSelectTableOrSheet;
	transient ActionListener funcSelectImportFile;

	private ExportFile myExportFile;
	private ExportFile myImportFile;

	transient ScFieldSelect fieldSelect;
	transient ScConfigDb configDb;

	private ConfigTextFile textImport;
	private boolean isNewProfile = false;
	private String myView;

	private static final String FUNC_NEW = "funcNew";
	private static final String TABLE = "table";
	private static final String WORKSHEET = "worksheet";

	transient XConverter dbFactory;
	transient PrefDBConvert pdaSettings = PrefDBConvert.getInstance();
	transient Databases dbSettings = pdaSettings.getDbSettings();
	transient Map<String, FilterData> filterDataMap = new HashMap<>();
	transient Map<String, SortData> sortDataMap = new HashMap<>();

	private ProgramDialog dialog;
	private ProjectModel model;
	private DatabaseHelper dbVerified = new DatabaseHelper("", ExportFile.ACCESS);

	public ConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNew) {
		this.dialog = dialog;
		this.model = model;
		isNewProfile = isNew;
		setMinimumSize(new Dimension(500, 350));
		init();
	}

	private void init() {
		init(isNewProfile ? GUIFactory.getTitle(FUNC_NEW)
				: pdaSettings.getProfileID() + " " + GUIFactory.getText("configuration"));

		btRelationships = GUIFactory.createToolBarButton(GUIFactory.getTitle("relationships"), "table_relationship.png",
				e -> {
					ConfigSort sort = new ConfigSort(dbFactory, sortDataMap.get(myView));
					sort.setVisible(true);
				});

		btSortOrder = GUIFactory.createToolBarButton(GUIFactory.getTitle("sortOrder"), "Sort.png", e -> {
			ConfigSort sort = new ConfigSort(dbFactory, sortDataMap.get(myView));
			sort.setVisible(true);
		});

		btFilter = GUIFactory.createToolBarButton(GUIFactory.getTitle("filter"), "Filter.png", e -> {
			ConfigFilter filter = new ConfigFilter(dbFactory, filterDataMap.get(myView));
			filter.setVisible(true);
		});

		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
		myImportFile = isNewProfile ? ExportFile.ACCESS : dbSettings.getDatabaseType();
		dbFactory = new XConverter();

		funcSelectTableOrSheet = e -> tableOrWorksheetChanged();
		funcSelectImportFile = e -> importFileNameChanged(false);

		profile = GUIFactory.getJTextField(FUNC_NEW, isNewProfile ? "" : pdaSettings.getProfileID());
		profile.getDocument().addDocumentListener(funcDocumentChange);
		profile.setPreferredSize(new Dimension(100, 25));

		fieldSelect = new ScFieldSelect(dbFactory);
		configDb = new ScConfigDb(ConfigSoft.this, fieldSelect, myExportFile, pdaSettings);

		buildDialog();
		verifyDatabase();
		pack();
	}

	@Override
	protected void close() {
		dbFactory.close();
		super.close();
	}

	@Override
	protected Component createToolBar() {
		Box result = Box.createHorizontalBox();
		result.add(Box.createHorizontalStrut(5));
		result.add(btSave);
		result.add(addToToolbar());
		result.add(Box.createRigidArea(new Dimension(5, 42)));
		result.add(btRelationships);
		result.add(Box.createHorizontalStrut(2));
		result.add(btSortOrder);
		result.add(Box.createHorizontalStrut(2));
		result.add(btFilter);
		result.add(Box.createHorizontalGlue());
		result.add(btHelp);
		result.add(Box.createHorizontalStrut(2));
		result.add(btExit);
		result.add(Box.createHorizontalStrut(5));
		result.setBorder(BorderFactory.createRaisedBevelBorder());
		return result;
	}

	@Override
	protected Component createCenterPanel() {
		tabPane = new JTabbedPane();
		tabPane.add(GUIFactory.getText("exportFiles"), createSelectionPanel());
		tabPane.add(GUIFactory.getText("exportFields"), createFieldPanel());
		tabPane.setEnabledAt(1, false);
		return tabPane;
	}

	private JPanel createSelectionPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(Box.createVerticalStrut(15));

		textImport = new ConfigTextFile(this, pdaSettings);

		bDatabase = new JComboBox<>(ExportFile.getExportFilenames(true));
		bDatabase.setSelectedItem(myImportFile.getName());
		bDatabase.addActionListener(e -> importFileTypeChanged());
		bDatabase.setPreferredSize(configDb.getComboBoxSize());
		cbDatabases = new JComboBox<>();

		JButton btBrowse = GUIFactory.getJButton("browseFile", e -> {
			if (myImportFile.isConnectHost()) {
				HostConfig config = new HostConfig(dbFactory.getDbInHelper());
				config.setVisible(true);
				if (config.isSaved()) {
					fdDatabase.setText(dbFactory.getDbInHelper().getDatabase());
					importFileNameChanged(true);
				}
			} else {
				fdDatabase.setText(cbDatabases.getSelectedItem().toString());
				General.getSelectedFile(ConfigSoft.this, fdDatabase, myImportFile, "", true);
				importFileNameChanged(true);
			}
		});

		lTablesWorkSheets = GUIFactory.getJLabel(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE);
		bTablesWorksheets = new JComboBox<>();
		bTablesWorksheets.setToolTipText(GUIFactory.getToolTip(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE));

		XGridBagConstraints c = new XGridBagConstraints();

		JPanel gPanel = new JPanel(new GridBagLayout());
		gPanel.add(bDatabase, c.gridCell(0, 0, 0, 0));
		gPanel.add(cbDatabases, c.gridmultipleCell(1, 0, 2, 0, 3, 1));
		gPanel.add(btBrowse, c.gridCell(4, 0, 0, 0));

		gPanel.add(lTablesWorkSheets, c.gridCell(1, 1, 0, 0));
		gPanel.add(bTablesWorksheets, c.gridCell(2, 1, 0, 0));
		gPanel.add(textImport, c.gridmultipleCell(1, 3, 2, 0, 3, 1));
		gPanel.setBorder(BorderFactory.createTitledBorder(GUIFactory.getText("exportFrom")));

		reloadImportFiles(true, false);

		myView = isNewProfile ? "" : cbDatabases.getSelectedItem().toString();

		if (isNewProfile) {
			JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p1.setBorder(BorderFactory.createEtchedBorder());
			p1.add(Box.createVerticalStrut(40));
			p1.add(GUIFactory.getJLabel(FUNC_NEW));
			p1.add(Box.createHorizontalStrut(10));
			p1.add(profile);
			panel.add(p1);
		} else {
			FilterData filter = filterDataMap.computeIfAbsent(myView, e -> new FilterData());
			filter.loadProfile(pdaSettings);
			SortData data = sortDataMap.computeIfAbsent(myView, e -> new SortData());
			data.loadProfile(pdaSettings);
		}

		panel.add(gPanel);
		panel.add(Box.createVerticalStrut(10));
		panel.add(configDb);
		return panel;
	}

	private JPanel createFieldPanel() {
		JPanel result = new JPanel(new BorderLayout());
		result.add(fieldSelect.createFieldPanel(), BorderLayout.CENTER);
		return result;
	}

	private void importFileNameChanged(boolean isAddNew) {
		dbFactory.getDbInHelper()
				.setDatabase(isAddNew ? fdDatabase.getText() : cbDatabases.getSelectedItem().toString());

		if (reloadImportFiles(false, isAddNew)) {
			verifyDatabase();
		}

		setTablesOrWorksheets();
	}

	private void importFileTypeChanged() {
		ExportFile software = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());
		String db = dbVerified.getDatabase();

		if (!isNewProfile && software != myImportFile && !db.isEmpty()
				&& !General.showConfirmMessage(this,
						GUIFactory.getMessage("funcSelectDb", myImportFile.getName(), software.getName()),
						GUIFactory.getTitle("warning"))) {

			bDatabase.setSelectedItem(myImportFile.getName());
			return;
		}

		myImportFile = software;
		dbVerified = new DatabaseHelper(db, myImportFile);
		dbFactory.getTableOrSheetNames().clear();
		dbFactory.getDbInHelper().update(dbVerified);
		setTablesOrWorksheets();

		if (reloadImportFiles(true, false)) {
			verifyDatabase();
		}
	}

	private boolean reloadImportFiles(boolean isInit, boolean isAddNew) {
		boolean result = false;
		cbDatabases.removeActionListener(funcSelectImportFile);
		cbDatabases.removeAllItems();
		dbVerified = dbFactory.getDbInHelper();

		List<String> dbFiles = dbSettings.getDatabaseFiles(myImportFile);
		String dbFile = dbVerified.getDatabase();

		boolean dbExist = dbFiles.contains(dbFile);

		if (isAddNew && !dbExist) {
			dbFiles.add(dbFile);
			Collections.sort(dbFiles);
			dbExist = true;
			result = !isInit;
		}

		if (!dbExist && dbFiles.size() > 1) {
			// Take last database from the list
			dbFile = dbFiles.get(dbFiles.size() - 1);
			dbExist = true;
			result = true;
		}

		dbFiles.forEach(db -> cbDatabases.addItem(db));
		cbDatabases.setSelectedItem(dbExist ? dbFile : "");

		if (dbExist) {
			String node = dbSettings.getNodename(dbFile, myImportFile.getName());
			if (node != null) {
				dbSettings.setNode(node);
				if (isInit) {
					dbVerified.update(dbSettings);
				}
			}
		} else {
			dbVerified.setDatabase("");
		}

		cbDatabases.addActionListener(funcSelectImportFile);
		return result;
	}

	private void tableOrWorksheetChanged() {
		pdaSettings.setTableName(bTablesWorksheets.getSelectedItem().toString(), false);
		try {
			dbFactory.setupDBTranslation(isNewProfile);
			fieldSelect.loadFieldPanel(dbFactory.getDbUserFields());
		} catch (Exception e) {
			General.errorMessage(this, e, GUIFactory.getTitle("configError"), null);
			dbFactory.close();
		}
	}

	@Override
	public void verifyDatabase() {
		dbVerified = dbFactory.getDbInHelper();
		dbVerified.setDatabaseType(myImportFile);

		if (!dbVerified.getDatabase().isEmpty()) {
			try {
				if (myImportFile.isConnectHost() || General.isFileExtensionOk(dbVerified.getDatabase(), myImportFile)) {
					dbFactory.connect2DB(dbVerified);
					dbFactory.setupDBTranslation(isNewProfile);
					fieldSelect.loadFieldPanel(dbFactory.getDbUserFields());
				} else {
					throw FNProgException.getException("noValidExtension", dbVerified.getDatabase(),
							myImportFile.getName());
				}
			} catch (Exception e) {
				General.errorMessage(this, e, GUIFactory.getTitle("configError"), null);
				dbFactory.close();
			}
		}
		activateComponents();
	}

	private void setTablesOrWorksheets() {
		bTablesWorksheets.removeActionListener(funcSelectTableOrSheet);
		bTablesWorksheets.removeAllItems();
		bTablesWorksheets.setVisible(false);
		lTablesWorkSheets.setVisible(false);

		if (dbFactory.getDatabaseFilename().isEmpty()) {
			return;
		}

		if (!(myImportFile.isDatabase() || myImportFile.isSpreadSheet())) {
			// Tables and worksheets are not supported
			return;
		}

		List<String> names = dbFactory.getTableOrSheetNames();
		if (names.isEmpty()) {
			return;
		}

		names.forEach(name -> bTablesWorksheets.addItem(name));

		bTablesWorksheets.setVisible(true);
		lTablesWorkSheets.setVisible(true);
		bTablesWorksheets.setEnabled(names.size() > 1);
		bTablesWorksheets.setSelectedItem(pdaSettings.getTableName());

		lTablesWorkSheets.setText(GUIFactory.getText(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE));
		bTablesWorksheets.setToolTipText(GUIFactory.getToolTip(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE));

		bTablesWorksheets.addActionListener(funcSelectTableOrSheet);
	}

	@Override
	protected void showHelp() {
		if (tabPane.getSelectedIndex() == 0) {
			setHelpFile("exportfiles_db");
		} else if (tabPane.getSelectedIndex() == 1) {
			setHelpFile("exportfields");
		}
		super.showHelp();
	}

	@Override
	protected void save() throws Exception {
		myExportFile = configDb.getExportFile();

		String projectID = myExportFile.getName();
		String profileID = profile.getText().trim();

		if (!isNewProfile) {
			ProfileObject obj = model.getProfileObject();
			if (!obj.getProjectID().equals(projectID)) {
				if (pdaSettings.profileExists(projectID, profileID)) {
					throw FNProgException.getException("profileExists", profileID, projectID);
				}

				pdaSettings.deleteNode(obj.getProjectID(), profileID);
				model.removeRecord(obj);
				isNewProfile = true;
			}
		}

		String node = dbSettings.getNodename(dbVerified.getDatabase(), myImportFile.getName());
		if (node == null) {
			node = dbSettings.getNextDatabaseID();
		}

		dbSettings.setNode(node);
		dbSettings.setDatabaseType(myImportFile);
		dbSettings.setDatabase(dbVerified.getDatabase());

		if (myImportFile.isPasswordSupported()) {
			dbSettings.setUser(dbVerified.getUser());
			dbSettings.setPassword(dbVerified.getPassword());
		}

		if (myImportFile.isConnectHost()) {
			saveRemoteDatabase();
		}

		pdaSettings.setProject(projectID);
		pdaSettings.setProfile(profileID);

		pdaSettings.setUserList(fieldSelect.getFieldList());
		pdaSettings.setDatabaseFromFile(node);

		if (myImportFile == ExportFile.TEXTFILE) {
			textImport.setProperties();
		} else if (myImportFile.isDatabase() || myImportFile.isSpreadSheet()) {
			pdaSettings.setTableName(bTablesWorksheets.getSelectedItem().toString(), true);
		}

		pdaSettings.setLastIndex(0);
		pdaSettings.setLastModified("");
		configDb.setProperties();

		filterDataMap.getOrDefault(myView, new FilterData()).saveProfile(pdaSettings);
		sortDataMap.getOrDefault(myView, new SortData()).saveProfile(pdaSettings);

		dialog.updateProfile(isNewProfile ? Action.ADD : Action.EDIT);
	}

	private void saveRemoteDatabase() {
		boolean isNotSsh = !dbVerified.isUseSsh();
		boolean isNotSsl = !dbVerified.isUseSsl();

		dbSettings.setHost(dbVerified.getHost());
		dbSettings.setPort(dbVerified.getPort());
		dbSettings.setUseSsh(dbVerified.isUseSsh());
		dbSettings.setSshHost(isNotSsh ? "" : dbVerified.getSshHost());
		dbSettings.setSshPort(isNotSsh ? 0 : dbVerified.getSshPort());
		dbSettings.setSshUser(isNotSsh ? "" : dbVerified.getSshUser());
		dbSettings.setSshPassword(isNotSsh ? "" : dbVerified.getSshPassword());
		dbSettings.setPrivateKeyFile(isNotSsh ? "" : dbVerified.getPrivateKeyFile());

		dbSettings.setUseSsl(dbVerified.isUseSsl());
		dbSettings.setSslMode(isNotSsl ? "" : dbVerified.getSslMode());
		dbSettings.setKeyStore(isNotSsl ? "" : dbVerified.getKeyStore());
		dbSettings.setKeyStorePassword(isNotSsl ? "" : dbVerified.getKeyStorePassword());
		dbSettings.setServerSslCert(isNotSsl ? "" : dbVerified.getServerSslCert());
		dbSettings.setHostNameInCertificate(isNotSsl ? "" : dbVerified.getHostNameInCertificate());
		dbSettings.setTrustServerCertificate(isNotSsl ? isNotSsl : dbVerified.isTrustServerCertificate());
	}

	@Override
	public void activateComponents() {
		boolean isTextFile = myImportFile == ExportFile.TEXTFILE;
		boolean isFileValid = dbFactory.isConnected();

		boolean isValidProfile = true;
		String profileID = profile.getText().trim();

		if (isFileValid && isNewProfile) {
			isValidProfile = !profileID.isEmpty();
			if (isValidProfile) {
				myExportFile = configDb.getExportFile();
				isValidProfile = !pdaSettings.profileExists(myExportFile.getName(), profileID);
			}
		}

		btSave.setEnabled(isFileValid && isValidProfile);
		btFilter.setEnabled(isFileValid);
		btSortOrder.setEnabled(isFileValid);
		btRelationships.setVisible(isFileValid && myImportFile.isSqlDatabase());
		myView = dbFactory.getDatabaseFilename();

		if (btFilter.isEnabled()) {
			FilterData data = filterDataMap.computeIfAbsent(myView, e -> new FilterData());
			data.setTvBSoftware(TvBSoftware.DBCONVERT);
			data.setProfileID(profileID);
		}

		if (btFilter.isEnabled()) {
			sortDataMap.computeIfAbsent(myView, e -> new SortData());
		}

		if (tabPane != null) {
			setTablesOrWorksheets();
			tabPane.setEnabledAt(1, isFileValid);
			textImport.activateComponents();
			textImport.setVisible(isTextFile);
		}
	}
}
