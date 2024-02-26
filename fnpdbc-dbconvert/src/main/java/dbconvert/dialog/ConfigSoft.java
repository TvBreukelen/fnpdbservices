package dbconvert.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import application.dialog.ConfigDialog;
import application.dialog.ConfigTextFile;
import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.ScConfigDb;
import application.interfaces.ExportFile;
import application.interfaces.IConfigSoft;
import application.interfaces.IDatabaseFactory;
import application.interfaces.TvBSoftware;
import application.model.FilterData;
import application.model.ProfileObject;
import application.model.ProjectModel;
import application.model.SortData;
import application.utils.BasisField;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbconvert.preferences.PrefDBConvert;
import dbconvert.software.XConverter;
import dbengine.utils.DatabaseHelper;
import dbengine.utils.RelationData;

public class ConfigSoft extends ConfigDialog implements IConfigSoft {
	/**
	 * Title: ConfigXConverter Description: XConverter Configuration program
	 * Copyright: (c) 2004-2020
	 *
	 * @author Tom van Breukelen
	 * @version 5+
	 */
	private static final long serialVersionUID = -4831514718413166627L;

	private JTextField profile;
	private JTextField fdDatabase = new JTextField();

	private JButton btRelationships;
	private XGridBagConstraints c = new XGridBagConstraints();

	private JComboBox<String> bDatabase;
	private JComboBox<String> bTablesWorksheets;
	private JComboBox<String> cbDatabases;
	private JCheckBox ckPagination;
	private JSpinner spSqlLimit;
	private SpinnerNumberModel sModel;

	private JLabel lTablesWorkSheets;
	private JLabel lSqlLimit;
	private List<String> dbFiles;

	transient ActionListener funcSelectTableOrSheet;
	transient ActionListener funcSelectImportFile;
	transient ActionListener funcSelectImportFileType;

	private ExportFile myExportFile;
	private ExportFile myImportFile;

	transient ScConfigDb configDb;

	private ConfigTextFile textImport;
	private ICalendarConfig calendarImport;

	private static final String TABLE = "table";
	private static final String WORKSHEET = "worksheet";

	transient XConverter dbFactory = new XConverter();
	transient Map<String, RelationData> relationDataMap = new HashMap<>();

	private ProgramDialog dialog;
	private ProjectModel model;

	public ConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNew) {
		this.dialog = dialog;
		this.model = model;
		isNewProfile = isNew;
		setMinimumSize(new Dimension(500, 350));
		init(dbFactory);
	}

	private void init(IDatabaseFactory factory) {
		super.init(factory, PrefDBConvert.getInstance());

		if (isNewProfile) {
			profiles.reset();
			dbFactory.getDbInHelper().setDatabase(General.EMPTY_STRING);
		}

		myImportFile = dbVerified.getDatabaseType();
		myExportFile = dbExport.getDatabaseType();

		configDb = new ScConfigDb(ConfigSoft.this, fieldSelect, myExportFile, profiles);

		btRelationships = GUIFactory.createToolBarButton(GUIFactory.getTitle("relationships"), "table_relationship.png",
				e -> {
					Relations relation = new Relations(dbFactory, fieldSelect,
							relationDataMap.computeIfAbsent(myView, r -> new RelationData()));
					relation.setVisible(true);
				});

		funcSelectTableOrSheet = e -> tableOrWorksheetChanged();
		funcSelectImportFileType = e -> importFileTypeChanged();
		funcSelectImportFile = e -> importFileNameChanged(false);

		profile = GUIFactory.getJTextField(FUNC_NEW, isNewProfile ? General.EMPTY_STRING : profiles.getProfileID());
		profile.getDocument().addDocumentListener(funcDocumentChange);
		profile.setPreferredSize(new Dimension(100, 30));
		profiles.setNewProfile(isNewProfile);

		buildDialog();
		verifyDatabase();
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
	protected JPanel createSelectionPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(Box.createVerticalStrut(15));

		textImport = new ConfigTextFile(this, profiles);
		calendarImport = new ICalendarConfig(profiles);

		bDatabase = new JComboBox<>(ExportFile.getExportFilenames(true));
		bDatabase.setSelectedItem(myImportFile.getName());
		bDatabase.addActionListener(funcSelectImportFileType);
		bDatabase.setPreferredSize(configDb.getComboBoxSize());
		cbDatabases = new JComboBox<>();
		dbFiles = dbSettings.getDatabaseFiles(myImportFile);

		JButton btBrowse = GUIFactory.getJButton("browseFile", e -> {
			if (myImportFile.isConnectHost()) {
				HostConfig config = new HostConfig(dbFactory.getDbInHelper());
				config.setVisible(true);
				if (config.isSaved()) {
					fdDatabase.setText(dbFactory.getDbInHelper().getDatabase());
					importFileNameChanged(true);
				}
			} else {
				String dbFile = cbDatabases.getSelectedItem() == null ? General.EMPTY_STRING
						: cbDatabases.getSelectedItem().toString();
				fdDatabase.setText(dbFile);
				General.getSelectedFile(ConfigSoft.this, fdDatabase, myImportFile, General.EMPTY_STRING, true);
				if (!fdDatabase.getText().isBlank()) {
					importFileNameChanged(true);
				}
			}
		});

		JPanel gPanel = new JPanel(new GridBagLayout());
		gPanel.add(bDatabase, c.gridCell(0, 0, 0, 0));
		gPanel.add(cbDatabases, c.gridmultipleCell(1, 0, 2, 0, 3, 1));
		gPanel.add(btBrowse, c.gridCell(4, 0, 0, 0));

		gPanel.add(createTableAndWorksheetPanel(), c.gridmultipleCell(1, 1, 2, 0, 3, 2));
		gPanel.add(textImport, c.gridmultipleCell(1, 3, 2, 0, 3, 1));
		gPanel.add(calendarImport, c.gridmultipleCell(1, 5, 2, 0, 3, 1));
		gPanel.setBorder(BorderFactory.createTitledBorder(GUIFactory.getText("exportFrom")));

		reloadImportFiles();

		if (isNewProfile) {
			myView = General.EMPTY_STRING;
			JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p1.setBorder(BorderFactory.createEtchedBorder());
			p1.add(Box.createVerticalStrut(40));
			p1.add(GUIFactory.getJLabel(FUNC_NEW));
			p1.add(Box.createHorizontalStrut(10));
			p1.add(profile);
			panel.add(p1);
		} else {
			myView = cbDatabases.getSelectedItem().toString() + profiles.getTableName();
			FilterData filter = filterDataMap.computeIfAbsent(myView, e -> new FilterData());
			SortData sort = sortDataMap.computeIfAbsent(myView, e -> new SortData());
			RelationData relation = relationDataMap.computeIfAbsent(myView, e -> new RelationData());
			filter.loadProfile(profiles);
			sort.loadProfile(profiles);
			relation.loadProfile(profiles);
		}

		panel.add(gPanel);
		panel.add(Box.createVerticalStrut(10));
		panel.add(configDb);
		return panel;
	}

	private JComponent createTableAndWorksheetPanel() {
		JPanel result = new JPanel(new GridBagLayout());

		ckPagination = GUIFactory.getJCheckBox("pagination", profiles.isPagination());
		lTablesWorkSheets = GUIFactory.getJLabel(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE);
		bTablesWorksheets = new JComboBox<>();
		bTablesWorksheets.setToolTipText(GUIFactory.getToolTip(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE));

		lSqlLimit = GUIFactory.getJLabel("sqlLimit");
		sModel = new SpinnerNumberModel(profiles.getSqlSelectLimit(), 0, 10000, 100);
		spSqlLimit = new JSpinner(sModel);
		spSqlLimit.addChangeListener(e -> ckPagination.setEnabled(sModel.getNumber().intValue() > 0));
		ckPagination.setEnabled(profiles.getSqlSelectLimit() > 0);

		result.add(lTablesWorkSheets, c.gridCell(0, 0, 0, 0));
		result.add(bTablesWorksheets, c.gridCell(1, 0, 2, 0));
		result.add(spSqlLimit, c.gridCell(2, 0, 0, 0));
		result.add(lSqlLimit, c.gridCell(3, 0, 0, 0));
		result.add(ckPagination, c.gridCell(2, 1, 0, 0));

		return result;
	}

	private void importFileNameChanged(boolean isAddNew) {
		dbFactory.getDbInHelper()
				.setDatabase(isAddNew ? fdDatabase.getText() : cbDatabases.getSelectedItem().toString());

		if (reloadImportFiles()) {
			verifyDatabase();
		}

		setTablesOrWorksheets();
	}

	private void importFileTypeChanged() {
		ExportFile software = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());
		String db = dbVerified.getDatabase();

		if (software != myImportFile && !db.isEmpty()) {
			boolean abort = false;
			if (software.isConnectHost() != myImportFile.isConnectHost()) {
				General.showMessage(this, GUIFactory.getMessage("invalidDatabaseSwitch", General.EMPTY_STRING),
						CONFIG_ERROR, true);
				abort = true;
			} else {
				abort = !General.showConfirmMessage(this,
						GUIFactory.getMessage("funcSelectDb", myImportFile.getName(), software.getName()),
						GUIFactory.getTitle("warning"));
			}

			if (abort) {
				bDatabase.removeActionListener(funcSelectImportFileType);
				bDatabase.setSelectedItem(myImportFile.getName());
				bDatabase.addActionListener(funcSelectImportFileType);
				return;
			}
		}

		myImportFile = software;
		dbVerified = new DatabaseHelper(db, myImportFile);
		dbFactory.getTableOrSheetNames().clear();
		dbFactory.getDbInHelper().update(dbVerified);
		setTablesOrWorksheets();
		dbFiles = dbSettings.getDatabaseFiles(myImportFile);

		if (reloadImportFiles()) {
			verifyDatabase();
		}
	}

	private boolean reloadImportFiles() {
		boolean result = false;
		cbDatabases.removeActionListener(funcSelectImportFile);
		cbDatabases.removeAllItems();

		dbVerified = dbFactory.getDbInHelper();
		myImportFile = dbVerified.getDatabaseType();

		String dbFile = dbVerified.getDatabase();
		String dbNewFile = dbFiles.size() < 2 ? dbFile : dbFiles.get(dbFiles.size() - 1);

		if (General.isFileExtensionOk(dbNewFile, myImportFile) && !dbFiles.contains(dbNewFile)) {
			dbFiles.add(dbNewFile);
			Collections.sort(dbFiles);
		} else {
			profiles.setTableName(General.EMPTY_STRING, false);
		}

		if (myImportFile.isConnectHost()) {
			result = true;
		} else {
			result = !dbNewFile.isBlank() && General.existFile(dbNewFile);
		}

		dbFiles.forEach(db -> cbDatabases.addItem(db));
		cbDatabases.setSelectedItem(dbNewFile);

		if (result) {
			String node = dbSettings.getNodename(dbNewFile, myImportFile);
			if (node != null) {
				dbSettings.setNode(node);
				dbVerified.update(dbSettings);
			}
		} else {
			if (!dbNewFile.isBlank()) {
				General.showMessage(this, GUIFactory.getMessage("noDatabaseExists", dbNewFile), CONFIG_ERROR, true);
			}

			btSave.setEnabled(false);
			btFilter.setEnabled(false);
			btSortOrder.setEnabled(false);

			if (tabPane.getTabCount() == 2) {
				tabPane.setEnabledAt(1, false);
			}
		}

		cbDatabases.addActionListener(funcSelectImportFile);
		return result;
	}

	private void tableOrWorksheetChanged() {
		profiles.setTableName(bTablesWorksheets.getSelectedItem().toString(), false);
		myView = cbDatabases.getSelectedItem().toString() + profiles.getTableName();

		try {
			dbFactory.setupDBTranslation(true);
			fieldSelect.loadFieldPanel(dbFactory.getDbUserFields(), true);
		} catch (Exception e) {
			General.errorMessage(this, e, CONFIG_ERROR, null);
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
					dbFactory.setupDBTranslation(true);
					fieldSelect.loadFieldPanel(dbFactory.getDbUserFields(), true);
				} else {
					dbFactory.close();
					dbVerified.setDatabase(General.EMPTY_STRING);
					cbDatabases.setSelectedItem(General.EMPTY_STRING);
					setTablesOrWorksheets();
				}
			} catch (Exception e) {
				General.errorMessage(this, e, CONFIG_ERROR, null);
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
		lSqlLimit.setVisible(false);
		spSqlLimit.setVisible(false);
		ckPagination.setVisible(false);

		if (dbFactory.getDatabaseFilename().isEmpty()
				|| !(myImportFile.isSqlDatabase() || myImportFile.isSpreadSheet())) {
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
		bTablesWorksheets.setSelectedItem(profiles.getTableName());

		lTablesWorkSheets.setText(GUIFactory.getText(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE));
		bTablesWorksheets.setToolTipText(GUIFactory.getToolTip(myImportFile.isSpreadSheet() ? WORKSHEET : TABLE));

		bTablesWorksheets.addActionListener(funcSelectTableOrSheet);

		if (myImportFile.isSqlDatabase()) {
			spSqlLimit.setValue(Integer.valueOf(profiles.getSqlSelectLimit()));
			lSqlLimit.setVisible(true);
			spSqlLimit.setVisible(true);
			ckPagination.setVisible(true);
		}
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
		checkDuplicatelFieldNames();

		String projectID = myExportFile.getName();
		String profileID = profile.getText().trim();

		if (!isNewProfile) {
			ProfileObject obj = model.getProfileObject();
			if (!obj.getProjectID().equals(projectID)) {
				if (profiles.profileExists(projectID, profileID)) {
					throw FNProgException.getException("profileExists", profileID, projectID);
				}

				profiles.deleteNode(obj.getProjectID(), profileID);
				model.removeRecord(obj);
				isNewProfile = true;
			}
		}

		profiles.setProject(projectID);
		profiles.setProfile(profileID);
		profiles.setUserList(fieldSelect.getFieldList());
		profiles.setFromDatabase(profiles.setDatabase(dbVerified));

		if (myImportFile == ExportFile.TEXTFILE) {
			textImport.setProperties();
		} else if (myImportFile == ExportFile.ICAL) {
			calendarImport.setProperties();
		}

		profiles.setTableName(myImportFile.isSqlDatabase() || myImportFile.isSpreadSheet()
				? bTablesWorksheets.getSelectedItem().toString()
				: General.EMPTY_STRING, true);
		profiles.setSqlSelectLimit(myImportFile.isSqlDatabase() ? sModel.getNumber().intValue() : 0);
		profiles.setPagination(ckPagination.isEnabled() && ckPagination.isSelected());

		profiles.setLastExported(General.EMPTY_STRING);
		configDb.setProperties();

		filterDataMap.getOrDefault(myView, new FilterData()).saveProfile(profiles);
		sortDataMap.getOrDefault(myView, new SortData()).saveProfile(profiles);
		relationDataMap.getOrDefault(myView, new RelationData()).saveProfile(profiles);

		// SQL Server requires Pagination with Sort
		if (myImportFile == ExportFile.SQLSERVER && profiles.getSqlSelectLimit() > 0 && profiles.isPagination()
				&& !profiles.isSortFieldDefined()) {
			General.showMessage(this, GUIFactory.getText("paginationWarning"), GUIFactory.getTitle("paginationWarning"),
					false);
		}

		dialog.updateProfile(isNewProfile ? Action.ADD : Action.EDIT);
	}

	private void checkDuplicatelFieldNames() throws FNProgException {
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

	@Override
	public void activateComponents() {
		boolean isTextFile = myImportFile == ExportFile.TEXTFILE;
		boolean isICalFile = myImportFile == ExportFile.ICAL;
		boolean isFileValid = dbFactory.isConnected();

		boolean isValidProfile = true;
		String profileID = profile.getText().trim();

		if (isFileValid && isNewProfile) {
			isValidProfile = !profileID.isEmpty();
			if (isValidProfile) {
				myExportFile = configDb.getExportFile();
				isValidProfile = !profiles.profileExists(myExportFile.getName(), profileID);
			}
		}

		btSave.setEnabled(isFileValid && isValidProfile);
		btFilter.setEnabled(isFileValid);
		btSortOrder.setEnabled(isFileValid);

		if (btFilter.isEnabled()) {
			FilterData data = filterDataMap.computeIfAbsent(myView, e -> new FilterData());
			data.setTvBSoftware(TvBSoftware.DBCONVERT);
			data.setProfileID(profileID);
		}

		if (btFilter.isEnabled()) {
			sortDataMap.computeIfAbsent(myView, e -> new SortData());
		}

		if (btRelationships.isVisible()) {
			relationDataMap.computeIfAbsent(myView, e -> new RelationData());
		}

		if (tabPane != null) {
			setTablesOrWorksheets();
			tabPane.setEnabledAt(1, isFileValid);
			textImport.activateComponents();
			textImport.setVisible(isTextFile);
			calendarImport.setVisible(isICalFile);
			btRelationships.setVisible(isFileValid && myImportFile.isSqlDatabase() && bTablesWorksheets.isEnabled());
			pack();
		}
	}
}
