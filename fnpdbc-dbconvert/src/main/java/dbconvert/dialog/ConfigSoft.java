package dbconvert.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.HashMap;
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
import application.dialog.ConfigCharset;
import application.dialog.ConfigFilter;
import application.dialog.ConfigSort;
import application.dialog.ConfigTextFile;
import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.ScConfigDb;
import application.dialog.ScFieldSelect;
import application.interfaces.ExportFile;
import application.interfaces.IConfigSoft;
import application.interfaces.IEncoding;
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

public class ConfigSoft extends BasicDialog implements IConfigSoft, IEncoding {
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
	private JTextField fdDatabase;
	private JTextField fdEncoding;

	private JButton btEncoding;
	private JButton btFilter;
	private JButton btSortOrder;

	private JComboBox<String> bDatabase;
	private JComboBox<String> bWorkSheets;
	private JComboBox<String> bTables;
	private JLabel lWorksheet;
	private JLabel lTable;

	transient ActionListener funcSelectWorksheet;
	transient ActionListener funcSelectTable;

	private ExportFile myExportFile;
	private ExportFile myImportFile;

	transient DatabaseHelper dbVerified = new DatabaseHelper("");

	transient ScFieldSelect fieldSelect;
	transient ScConfigDb configDb;

	private ConfigTextFile textImport;
	private boolean isNewProfile = false;
	private String encoding;
	private String myView;
	private static final String FUNC_NEW = "funcNew";

	transient XConverter dbFactory;
	transient PrefDBConvert pdaSettings = PrefDBConvert.getInstance();
	transient Databases dbSettings = pdaSettings.getDbSettings();
	transient Map<String, FilterData> filterDataMap = new HashMap<>();
	transient Map<String, SortData> sortDataMap = new HashMap<>();

	private ProgramDialog dialog;
	private ProjectModel model;

	public ConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNew) {
		this.dialog = dialog;
		this.model = model;
		isNewProfile = isNew;
		setMinimumSize(new Dimension(500, 350));
		init();
	}

	@Override
	protected void init() {
		init(isNewProfile ? GUIFactory.getTitle(FUNC_NEW)
				: pdaSettings.getProfileID() + " " + GUIFactory.getText("configuration"));

		btSortOrder = General.createToolBarButton(GUIFactory.getTitle("sortOrder"), "Sort.png", e -> {
			ConfigSort sort = new ConfigSort(dbFactory, sortDataMap.get(myView));
			sort.setVisible(true);
		});

		btFilter = General.createToolBarButton(GUIFactory.getTitle("filter"), "Filter.png", e -> {
			ConfigFilter filter = new ConfigFilter(dbFactory, filterDataMap.get(myView));
			filter.setVisible(true);
		});

		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
		myImportFile = isNewProfile ? ExportFile.JFILE5 : ExportFile.getExportFile(dbSettings.getDatabaseType());
		dbFactory = new XConverter();

		funcSelectWorksheet = e -> worksheetChanged();
		funcSelectTable = e -> tableChanged();

		profile = GUIFactory.getJTextField(FUNC_NEW, isNewProfile ? "" : pdaSettings.getProfileID());
		profile.getDocument().addDocumentListener(funcDocumentChange);
		profile.setPreferredSize(new Dimension(100, 25));

		fieldSelect = new ScFieldSelect(dbFactory);
		configDb = new ScConfigDb(ConfigSoft.this, myExportFile, pdaSettings);

		verifyDatabase(true);
		buildDialog();
		activateComponents();
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
		bDatabase.addActionListener(e -> importFileChanged());
		bDatabase.setPreferredSize(configDb.getComboBoxSize());

		fdDatabase = new JTextField(isNewProfile ? "" : dbSettings.getDatabaseFile());
		fdDatabase.getDocument().addDocumentListener(funcDocumentChange);

		JButton btBrowse = GUIFactory.getJButton("browseFile", e -> {
			General.getSelectedFile(ConfigSoft.this, fdDatabase, myImportFile, "", true);
			verifyDatabase(false);
		});

		fdEncoding = new JTextField();
		fdEncoding.setEditable(false);
		encoding = pdaSettings.getImportFileEncoding();
		setEncodingText();

		btEncoding = GUIFactory.getJButton("funcEncoding", e -> {
			ConfigCharset config = new ConfigCharset(ConfigSoft.this, ConfigSoft.this);
			config.setVisible(true);
			setEncodingText();
			activateComponents();
		});

		lWorksheet = GUIFactory.getJLabel("worksheet");
		bWorkSheets = new JComboBox<>();
		bWorkSheets.setToolTipText(GUIFactory.getToolTip("worksheet"));

		lTable = GUIFactory.getJLabel("table");
		bTables = new JComboBox<>();
		bTables.setToolTipText(GUIFactory.getToolTip("table"));

		XGridBagConstraints c = new XGridBagConstraints();

		JPanel p2 = new JPanel(new GridBagLayout());
		p2.add(btEncoding, c.gridCell(1, 0, 0, 0));
		p2.add(fdEncoding, c.gridCell(2, 0, 2, 0));

		JPanel gPanel = new JPanel(new GridBagLayout());
		gPanel.add(bDatabase, c.gridCell(0, 0, 0, 0));
		gPanel.add(fdDatabase, c.gridmultipleCell(1, 0, 2, 0, 4, 1));
		gPanel.add(btBrowse, c.gridCell(5, 0, 0, 0));

		gPanel.add(lTable, c.gridCell(0, 1, 0, 0));
		gPanel.add(bTables, c.gridCell(1, 1, 0, 0));
		gPanel.add(lWorksheet, c.gridCell(0, 1, 0, 0));
		gPanel.add(bWorkSheets, c.gridCell(1, 1, 0, 0));
		gPanel.add(p2, c.gridmultipleCell(1, 3, 2, 0, 4, 1));
		gPanel.add(textImport, c.gridmultipleCell(1, 4, 2, 0, 4, 1));
		gPanel.setBorder(BorderFactory.createTitledBorder(GUIFactory.getText("exportFrom")));

		myView = isNewProfile ? "" : fdDatabase.getText().trim();

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

	private void importFileChanged() {
		ExportFile software = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());
		if (!isNewProfile && software != myImportFile && !dbSettings.getDatabaseFile().isEmpty()
				&& !General.showConfirmMessage(this,
						GUIFactory.getMessage("funcSelectDb", myImportFile.getName(), software.getName()),
						GUIFactory.getTitle("warning"))) {

			bDatabase.setSelectedItem(myImportFile.getName());
			return;
		}

		myImportFile = software;
		dbVerified.setDatabase("");
		verifyDatabase(false);
	}

	private void worksheetChanged() {
		pdaSettings.setTableName(bWorkSheets.getSelectedItem().toString(), false);
		dbVerified.setDatabase("");
		verifyDatabase(false);
	}

	private void tableChanged() {
		pdaSettings.setTableName(bTables.getSelectedItem().toString(), false);
		dbVerified.setDatabase("");
		verifyDatabase(false);
	}

	@Override
	public void verifyDatabase(boolean isFirstRun) {
		if (isFirstRun) {
			dbVerified = isNewProfile ? new DatabaseHelper("", "", "")
					: new DatabaseHelper(dbSettings.getDatabaseFile(), dbSettings.getDatabaseUser(),
							dbSettings.getDatabasePassword());
		} else {
			String docValue = fdDatabase.getText();
			if (!docValue.isEmpty()) {
				String node = dbSettings.getNodename(docValue);
				if (node != null) {
					dbSettings.setNode(node);
				}
			}

			if (dbVerified.getDatabase().equals(docValue)) {
				return;
			}

			dbVerified = new DatabaseHelper(docValue, "", "");
		}

		if (dbVerified.getDatabase().isEmpty()) {
			return;
		}

		try {
			if (General.isFileExtensionOk(dbVerified.getDatabase(), myImportFile)) {
				dbFactory.connect2DB(dbVerified, myImportFile);
				dbFactory.setupDBTranslation(isNewProfile);
				fieldSelect.loadFieldPanel(dbFactory.getDbUserFields());
			} else {
				throw FNProgException.getException("noValidExtension", dbVerified.getDatabase(),
						myImportFile.getName());
			}
		} catch (Exception e) {
			General.errorMessage(isFirstRun ? getParent() : this, e, GUIFactory.getTitle("configError"), null);
			dbFactory.close();
		}
		activateComponents();
	}

	private void setEncodingText() {
		fdEncoding.setText(encoding.isEmpty() ? GUIFactory.getText("default") : encoding);
	}

	private void setWorksheets() {
		bWorkSheets.removeActionListener(funcSelectWorksheet);
		bWorkSheets.removeAllItems();

		if (myImportFile != ExportFile.EXCEL || dbVerified.getDatabase().isEmpty()) {
			return;
		}

		String[] worksheets = dbFactory.getWorksheets();
		if (worksheets == null) {
			return;
		}

		for (String s : worksheets) {
			bWorkSheets.addItem(s);
		}

		String worksheet = pdaSettings.getTableName();
		if (worksheet.isEmpty()) {
			bWorkSheets.setSelectedIndex(0);
		} else {
			bWorkSheets.setSelectedItem(worksheet);
		}

		bWorkSheets.setVisible(worksheets.length > 1);
		bWorkSheets.addActionListener(funcSelectWorksheet);
	}

	private void setTables() {
		bTables.removeActionListener(funcSelectTable);
		bTables.removeAllItems();

		if (dbVerified.getDatabase().isEmpty()) {
			return;
		}

		String[] tables = dbFactory.getTables();
		if (tables == null) {
			return;
		}

		for (String s : tables) {
			bTables.addItem(s);
		}

		bTables.setVisible(tables.length > 1);
		bTables.setSelectedItem(pdaSettings.getTableName());
		bTables.addActionListener(funcSelectTable);
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

		String node = dbSettings.getNodename(dbVerified.getDatabase());
		if (node == null) {
			node = dbSettings.getNextDatabaseID();
		}

		dbSettings.setNode(node);
		dbSettings.setDatabaseFile(dbVerified.getDatabase());
		dbSettings.setDatabaseType(myImportFile.getName());
		dbSettings.setDatabaseUser(dbVerified.getUser());
		dbSettings.setDatabasePassword(dbVerified.getPassword());

		pdaSettings.setProject(projectID);
		pdaSettings.setProfile(profileID);

		pdaSettings.setUserList(fieldSelect.getFieldList());
		pdaSettings.setDatabaseFromFile(node);

		switch (myImportFile) {
		case TEXTFILE:
			textImport.setProperties();
			break;
		case EXCEL:
			if (lWorksheet != null) {
				pdaSettings.setTableName(bWorkSheets.getSelectedItem().toString(), true);
			}
			break;
		case ACCESS:
		case SQLITE:
			if (lTable != null) {
				pdaSettings.setTableName(bTables.getSelectedItem().toString(), true);
			}
		default:
			break;
		}

		if (myImportFile.isEncodingSupported()) {
			pdaSettings.setImportFileEncoding(encoding);
		}

		pdaSettings.setLastIndex(0);
		pdaSettings.setLastModified("");
		configDb.setProperties();

		filterDataMap.getOrDefault(myView, new FilterData()).saveProfile(pdaSettings);
		sortDataMap.getOrDefault(myView, new SortData()).saveProfile(pdaSettings);

		dialog.updateProfile(isNewProfile ? Action.ADD : Action.EDIT);
	}

	@Override
	public void activateComponents() {
		boolean isTextFile = myImportFile == ExportFile.TEXTFILE;
		boolean isEncoding = myImportFile.isEncodingSupported();
		boolean isExcel = myImportFile == ExportFile.EXCEL;
		boolean isFileValid = false;
		boolean isFurtherCheck = true;

		String docValue = fdDatabase == null ? null : fdDatabase.getText().trim();

		if (docValue == null || docValue.isEmpty() || docValue.equals(dbVerified.getDatabase())) {
			isFileValid = dbFactory.isConnected();
			isFurtherCheck = false;
		}

		if (isFurtherCheck) {
			isFileValid = General.isFileExtensionOk(docValue, myImportFile) && General.existFile(docValue);
			if (isFileValid) {
				verifyDatabase(false);
				return;
			}
		}

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
		myView = docValue;

		if (btFilter.isEnabled()) {
			FilterData data = filterDataMap.computeIfAbsent(myView, e -> new FilterData());
			data.setTvBSoftware(TvBSoftware.DBCONVERT);
			data.setProfileID(profileID);
		}

		if (btFilter.isEnabled()) {
			sortDataMap.computeIfAbsent(myView, e -> new SortData());
		}

		if (tabPane != null) {
			setWorksheets();
			setTables();

			tabPane.setEnabledAt(1, isFileValid);
			bTables.setVisible(bTables.getItemCount() > 1);
			lTable.setVisible(bTables.isVisible());
			bWorkSheets.setVisible(isExcel && bWorkSheets.getItemCount() > 1);
			lWorksheet.setVisible(bWorkSheets.isVisible());
			textImport.activateComponents();
			textImport.setVisible(isTextFile);
			btEncoding.setVisible(isEncoding);
			fdEncoding.setVisible(isEncoding);
			setEncodingText();
		}
	}

	@Override
	public String getEncoding() {
		return encoding;
	}

	@Override
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
}
