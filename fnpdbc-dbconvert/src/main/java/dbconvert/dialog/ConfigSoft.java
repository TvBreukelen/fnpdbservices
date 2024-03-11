package dbconvert.dialog;

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
import application.dialog.HostConfig;
import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.interfaces.ExportFile;
import application.interfaces.IDatabaseFactory;
import application.interfaces.IExportProcess;
import application.interfaces.TvBSoftware;
import application.model.FilterData;
import application.model.ProfileObject;
import application.model.ProjectModel;
import application.model.SortData;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbconvert.preferences.PrefDBConvert;
import dbconvert.software.XConverter;
import dbengine.utils.DatabaseHelper;
import dbengine.utils.RelationData;

public class ConfigSoft extends ConfigDialog {
	/**
	 * Title: ConfigdbFactory Description: dbFactory Configuration program
	 * Copyright: (c) 2004-2024
	 *
	 * @author Tom van Breukelen
	 * @version 5+
	 */
	private static final long serialVersionUID = -4831514718413166627L;

	private JTextField fdDatabase = new JTextField();

	private JButton btRelationships;

	private JComboBox<String> bDatabase;
	private JComboBox<String> bTablesWorksheets;
	private JComboBox<String> cbDatabases;
	private JCheckBox ckPagination;
	private JSpinner spSqlLimit;
	private SpinnerNumberModel sModel;

	private JLabel lTablesWorkSheets;
	private JLabel lSqlLimit;

	transient ActionListener funcSelectTableOrSheet;
	transient ActionListener funcSelectFile;
	transient ActionListener funcSelectFileType;

	private ExportFile myExportFile;
	private ExportFile myImportFile;

	private ConfigTextFile textImport;
	private ICalendarConfig calendarImport;
	private XGridBagConstraints c = new XGridBagConstraints();

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

		btRelationships = GUIFactory.createToolBarButton(GUIFactory.getTitle("relationships"), "table_relationship.png",
				e -> {
					Relations relation = new Relations(dbFactory, fieldSelect,
							relationDataMap.computeIfAbsent(myView, r -> new RelationData()));
					relation.setVisible(true);
				});

		init(dbFactory);
		pack();
	}

	private void init(IDatabaseFactory factory) {
		super.init(factory, PrefDBConvert.getInstance());

		if (isNewProfile) {
			profiles.reset();
			dbVerified = dbFactory.getDbInHelper();
			dbVerified.setDatabase(General.EMPTY_STRING);
			dbVerified.setDatabaseType(ExportFile.ACCESS);
		}

		myImportFile = dbVerified.getDatabaseType();

		funcSelectTableOrSheet = e -> tableOrWorksheetChanged();
		funcSelectFileType = e -> fileTypeChanged();
		funcSelectFile = e -> fileNameChanged(false);

		profile = GUIFactory.getJTextField(FUNC_NEW, isNewProfile ? General.EMPTY_STRING : profiles.getProfileID());
		profile.getDocument().addDocumentListener(funcDocumentChange);
		profile.setPreferredSize(new Dimension(100, 30));

		buildDialog();
		verifyDatabase();
	}

	@Override
	public IExportProcess getExportProcess() {
		return new ExportProcess();
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
		bDatabase.addActionListener(funcSelectFileType);
		bDatabase.setPreferredSize(configDb.getComboBoxSize());
		cbDatabases = new JComboBox<>();

		JButton btBrowse = GUIFactory.getJButton("browseFile", e -> {
			if (myImportFile.isConnectHost()) {
				HostConfig config = new HostConfig(dbVerified, dialog.getExportProcess());
				config.setVisible(true);
				if (config.isSaved()) {
					fdDatabase.setText(dbVerified.getDatabase());
					fileNameChanged(true);
				}
			} else {
				String dbFile = cbDatabases.getSelectedItem() == null ? General.EMPTY_STRING
						: cbDatabases.getSelectedItem().toString();
				fdDatabase.setText(dbFile);
				General.getSelectedFile(ConfigSoft.this, fdDatabase, myImportFile, General.EMPTY_STRING, true);
				if (!fdDatabase.getText().isBlank()) {
					fileNameChanged(true);
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

		reloadFiles();

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
		// spSqlLimit.addChangeListener(e ->
		// ckPagination.setEnabled(sModel.getNumber().intValue() > 0));
		ckPagination.setEnabled(profiles.getSqlSelectLimit() > 0);

		result.add(lTablesWorkSheets, c.gridCell(0, 0, 0, 0));
		result.add(bTablesWorksheets, c.gridCell(1, 0, 2, 0));
		result.add(spSqlLimit, c.gridCell(2, 0, 0, 0));
		result.add(lSqlLimit, c.gridCell(3, 0, 0, 0));
		result.add(ckPagination, c.gridCell(2, 1, 0, 0));

		return result;
	}

	private void fileNameChanged(boolean isAddNew) {
		dbVerified.setDatabase(isAddNew ? fdDatabase.getText()
				: DatabaseHelper.extractDatabase(cbDatabases.getSelectedItem().toString()));

		if (reloadFiles()) {
			verifyDatabase();
		}

		setTablesOrWorksheets();
	}

	private void fileTypeChanged() {
		ExportFile software = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());

		if (software != myImportFile && !dbVerified.getDatabase().isEmpty()) {
			if (General.showConfirmMessage(this,
					GUIFactory.getMessage("funcSelectDb", myImportFile.getName(), software.getName()),
					GUIFactory.getTitle("warning"))) {
				dbVerified.setDatabase(General.EMPTY_STRING);
			} else {
				bDatabase.removeActionListener(funcSelectFileType);
				bDatabase.setSelectedItem(myImportFile.getName());
				bDatabase.addActionListener(funcSelectFileType);
				return;
			}
		}

		// Reinitialise dbVerified
		myImportFile = software;
		dbVerified = new DatabaseHelper(dbVerified.getDatabase(), myImportFile);
		dbFactory.getTableOrSheetNames().clear();
		setTablesOrWorksheets();

		if (reloadFiles()) {
			verifyDatabase();
		}
		activateComponents();
	}

	private boolean reloadFiles() {
		boolean result = false;
		boolean isPrevious = false;

		cbDatabases.removeActionListener(funcSelectFile);
		cbDatabases.removeAllItems();

		myImportFile = dbVerified.getDatabaseType();
		String dbFile = dbVerified.getDatabaseName();
		List<String> dbFiles = dbSettings.getDatabaseFiles(myImportFile);

		if (dbFile.isEmpty() && !dbFiles.isEmpty()) {
			// Take the last entry in the prev. defined database list
			dbFile = dbFiles.get(dbFiles.size() - 1);
			result = true;
			isPrevious = true;
		}

		if (!result) {
			result = !dbFile.isBlank() && General.isFileExtensionOk(dbFile, myImportFile);
		}

		if (result && !dbFiles.contains(dbFile)) {
			dbFiles.add(dbFile);
			Collections.sort(dbFiles);
		}

		dbFiles.forEach(db -> cbDatabases.addItem(db));
		cbDatabases.setSelectedItem(dbFile);
		dbFile = DatabaseHelper.extractDatabase(dbFile);

		if (result) {
			if (isPrevious) {
				String node = dbSettings.getNodename(dbFile, myImportFile);
				if (node != null) {
					dbSettings.setNode(node);
					dbVerified.update(dbSettings);
				}
			}
		} else {
			if (!dbFile.isBlank()) {
				General.showMessage(this, GUIFactory.getMessage("noDatabaseExists", dbFile), CONFIG_ERROR, true);
			}

			btSave.setEnabled(false);
			btFilter.setEnabled(false);
			btSortOrder.setEnabled(false);

			if (tabPane.getTabCount() == 2) {
				tabPane.setEnabledAt(1, false);
			}
		}

		cbDatabases.addActionListener(funcSelectFile);
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
		if (!dbVerified.getDatabase().isEmpty()) {
			try {
				if (myImportFile.isConnectHost() || General.isFileExtensionOk(dbVerified.getDatabase(), myImportFile)) {
					dbFactory.connect2DB(dbVerified);
					dbFactory.setupDBTranslation(true);
					fieldSelect.loadFieldPanel(dbFactory.getDbUserFields(), true);
				} else {
					dbFactory.close();
				}
			} catch (Exception e) {
				dbFactory.close();
				General.errorMessage(this, e, CONFIG_ERROR, null);
			}
		}

		activateComponents();
		dialog.activateComponents();
	}

	private void setTablesOrWorksheets() {
		bTablesWorksheets.removeActionListener(funcSelectTableOrSheet);
		bTablesWorksheets.removeAllItems();
		bTablesWorksheets.setVisible(false);
		lTablesWorkSheets.setVisible(false);
		lSqlLimit.setVisible(false);
		spSqlLimit.setVisible(false);
		ckPagination.setVisible(false);

		if (dbVerified.getDatabaseName().isEmpty() || !(myImportFile.isSqlDatabase() || myImportFile.isSpreadSheet())) {
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
		myExportFile = configDb.getDatabaseHelper().getDatabaseType();
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

	@Override
	public void activateComponents() {
		super.activateComponents();
		boolean isTextFile = myImportFile == ExportFile.TEXTFILE;
		boolean isICalFile = myImportFile == ExportFile.ICAL;

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
