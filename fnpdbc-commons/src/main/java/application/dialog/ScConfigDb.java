package application.dialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import application.interfaces.ExportFile;
import application.interfaces.IConfigDb;
import application.interfaces.TvBSoftware;
import application.preferences.Databases;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbengine.SqlDB;
import dbengine.export.Firebird;
import dbengine.export.PostgreSQL;
import dbengine.export.SQLite;
import dbengine.utils.DatabaseHelper;

public class ScConfigDb extends JPanel implements IConfigDb {
	/**
	 * Title: ScConfigDb
	 *
	 * @apiNote Database Configuration
	 * @since 2016
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private static final long serialVersionUID = 8000165106403251367L;

	private JPasswordField fdPassword;
	private XGridBagConstraints c = new XGridBagConstraints();

	private JTextField fdDatabase = new JTextField();
	private JComboBox<String> bDatabase;
	private JComboBox<String> cbDatabases;
	private JLabel dbDatabaseLabel = new JLabel();
	private JTextField dbDatabase; // database, table or worksheet name

	private JPanel pExport;
	private JPanel pConvert;
	private JPanel pOtherOptions;
	private JPanel pBottomContainer;
	private JPanel pTopContainer;
	private Box passwordBox;

	private JRadioButton[] rExists = new JRadioButton[3];
	private JRadioButton[] rImages = new JRadioButton[3];
	private JRadioButton[] rOnConflict = new JRadioButton[5];

	private SpinnerNumberModel hModel;
	private SpinnerNumberModel wModel;
	private JSpinner spHeight;
	private JSpinner spWidth;
	private JLabel lHeight;
	private JLabel lWidth;

	private JCheckBox cConvertImages = new JCheckBox();
	private JCheckBox btBackup;
	private JCheckBox btSkipEmpty;

	transient ActionListener funcSelectFile;
	transient ActionListener funcSelectConvert;
	transient ActionListener funcShowSchema;

	private ExportFile myExportFile;
	private JButton btTableSchema;

	transient IConfigDb dbConfig;
	transient ConfigDialog dialog;
	transient Profiles profiles;
	transient ScFieldSelect scFieldSelect;
	private DatabaseHelper helper;
	transient Databases dbSettings;

	private PropertyChangeSupport support;

	public ScConfigDb(ConfigDialog dialog, ScFieldSelect sc, Profiles profiles) {
		scFieldSelect = sc;
		this.dialog = dialog;
		this.profiles = profiles;
		dbSettings = profiles.getDbSettings();

		support = new PropertyChangeSupport(this);
		support.addPropertyChangeListener("exportfile", scFieldSelect);

		helper = profiles.getToDatabase();
		myExportFile = helper.getDatabaseType();

		init();
		buildDialog();
		fileTypeChanged(false);
	}

	private void init() {
		final String schema = "Schema";

		funcSelectFile = e -> {
			if (myExportFile == ExportFile.HANDBASE) {
				boolean enableImport = !rExists[0].isSelected();
				((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
			} else {
				btTableSchema.setVisible(myExportFile.isSqlDatabase() && rExists[0].isSelected());
				dialog.pack();
			}
		};

		funcSelectConvert = e -> {
			rImages[0].setEnabled(cConvertImages.isSelected());
			rImages[1].setEnabled(cConvertImages.isSelected());
			rImages[2].setEnabled(cConvertImages.isSelected());
			spHeight.setEnabled(cConvertImages.isSelected());
			spWidth.setEnabled(spHeight.isEnabled());
		};

		funcShowSchema = e -> {
			String dbName = dbDatabase.getText();
			if (dbName.isBlank()) {
				General.showMessage(dialog, GUIFactory.getMessage("noTableDefined"), schema, true);
				return;
			}

			if (scFieldSelect.getFieldList().isEmpty()) {
				General.showMessage(dialog, GUIFactory.getMessage("noFieldsDefined", dbName), "Schema", true);
				return;
			}

			SqlDB db;
			switch (myExportFile) {
			case FIREBIRD:
				db = new Firebird(profiles);
				break;
			case SQLITE:
				db = new SQLite(profiles);
				break;
			default:
				db = new PostgreSQL(profiles);
				break;
			}

			List<FieldDefinition> fields = new ArrayList<>();
			scFieldSelect.getFieldList().forEach(field -> fields.add(new FieldDefinition(field)));
			General.showMessage(dialog, db.buildTableString(dbName, fields), schema, false);
		};
	}

	private void fileTypeChanged(boolean isAddNew) {
		if (isAddNew) {
			ExportFile software = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());
			support.firePropertyChange("exportfile", myExportFile, software);

			myExportFile = software;
			profiles.setProject(myExportFile.getName());

			// Reinitialise DatabaseHelper
			helper = new DatabaseHelper(helper.getDatabase(), myExportFile);
			helper.setDatabase(General.EMPTY_STRING);
		}

		dbDatabase.setText(profiles.getDatabaseName());
		setRadioButtonText();

		// Restore Export Data options
		int index = profiles.isAppendRecords() ? 2 : 0;
		if (myExportFile.isSqlDatabase() || myExportFile == ExportFile.HANDBASE) {
			index = profiles.getExportOption();
		}
		rExists[index].setSelected(true);

		// Restore Conversion options
		cConvertImages.setSelected(profiles.isExportImages() && profiles.getTvBSoftware() == TvBSoftware.FNPROG2PDA);
		rImages[profiles.getImageOption()].setSelected(true);

		dbConfig = null;
		switch (myExportFile) {
		case HANDBASE:
			dbConfig = new ConfigHanDBase(profiles);
			break;
		case CALC:
			dbConfig = new ConfigCalc(profiles);
			break;
		case EXCEL:
			dbConfig = new ConfigExcel(profiles);
			break;
		case TEXTFILE:
			dbConfig = new ConfigTextFile(profiles, true, dialog.getBuddyExport());
			break;
		case DBASE:
			dbConfig = new XBaseCharsets(profiles);
			break;
		default:
			break;
		}

		pOtherOptions.removeAll();
		if (dbConfig != null) {
			pOtherOptions.add((JComponent) dbConfig);
		} else if (myExportFile.isSqlDatabase()) {
			pOtherOptions.add(General.addVerticalButtons(GUIFactory.getTitle("onConflict"), rOnConflict));
			rOnConflict[profiles.getOnConflict()].setSelected(true);
			rOnConflict[Profiles.ON_CONFLICT_ABORT].setVisible(myExportFile == ExportFile.SQLITE);
			rOnConflict[Profiles.ON_CONFLICT_FAIL].setVisible(myExportFile == ExportFile.SQLITE);
		}

		reloadFiles();
		activateComponents();
		dialog.pack();
	}

	private void setRadioButtonText() {
		String[] ids = new String[0];
		if (myExportFile.isSqlDatabase()) {
			ids = new String[] { "intoNewTable", "replaceTableRecords", "appendTableRecords" };
			dbDatabaseLabel.setText(GUIFactory.getText("table"));
		} else if (myExportFile.isSpreadSheet()) {
			ids = new String[] { "intoNewWorksheet", General.EMPTY_STRING, "appendWorksheetRecords" };
			dbDatabaseLabel.setText(GUIFactory.getText("worksheet"));
		} else if (myExportFile == ExportFile.XML) {
			dbDatabaseLabel.setText(GUIFactory.getText("xmlRoot"));
		} else {
			ids = new String[] { "intoNewDatabase", "replaceDatabaseRecords", "appendDatabaseRecords" };
			dbDatabaseLabel.setText(GUIFactory.getText("database"));
		}

		if (ids.length > 0) {
			for (int i = 0; i < ids.length; i++) {
				if (!ids[i].isEmpty()) {
					rExists[i].setText(GUIFactory.getText(ids[i]));
					rExists[i].setToolTipText(GUIFactory.getToolTip(ids[i]));
				}
			}
		}
	}

	public DatabaseHelper getDatabaseHelper() {
		return helper;
	}

	private void buildDialog() {
		setLayout(new GridBagLayout());
		bDatabase = new JComboBox<>(ExportFile.getExportFilenames(false));
		bDatabase.setSelectedItem(myExportFile.getName());
		bDatabase.setToolTipText(GUIFactory.getToolTip("pcToFile"));
		bDatabase.addActionListener(e -> fileTypeChanged(true));
		cbDatabases = new JComboBox<>();

		Dimension dim = bDatabase.getPreferredSize();
		dim.setSize(dim.getWidth() + 10, dim.getHeight());
		bDatabase.setPreferredSize(dim);

		dbDatabase = GUIFactory.getJTextField("database", General.EMPTY_STRING);
		dbDatabase.setText(profiles.getDatabaseName());
		fdPassword = new JPasswordField(8);

		JButton btBrowse = GUIFactory.getJButton("browseFile", e -> {
			if (myExportFile.isConnectHost()) {
				HostConfig config = new HostConfig(helper, dialog.getExportProcess());
				config.setVisible(true);
				if (config.isSaved()) {
					reloadFiles();
				}
			} else {
				General.getSelectedFile(dialog, fdDatabase, myExportFile, General.EMPTY_STRING, false);
				if (!fdDatabase.getText().isBlank()) {
					helper.setDatabase(fdDatabase.getText());
					reloadFiles();
				}
			}
		});

		hModel = new SpinnerNumberModel(profiles.getImageHeight(), 0, 900, 10);
		wModel = new SpinnerNumberModel(profiles.getImageWidth(), 0, 900, 10);

		createRadioButtons(rExists, funcSelectFile, General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING);
		setRadioButtonText();

		btTableSchema = GUIFactory.getJButton("funcShowSchema", funcShowSchema);
		createRadioButtons(rImages, null, "imageToBitmap", "imageToJpeg", "imageToPng");
		createRadioButtons(rOnConflict, null, "onConflictAbort", "onConflictFail", "onConflictIgnore",
				"onConflictReplace", "onConflictRollback");

		cConvertImages = GUIFactory.getJCheckBox("imageToImageFile", false, funcSelectConvert);

		spHeight = new JSpinner(hModel);
		lHeight = GUIFactory.getJLabel("height");
		spWidth = new JSpinner(wModel);
		lWidth = GUIFactory.getJLabel("width");
		spHeight.setToolTipText(GUIFactory.getToolTip("height"));
		spWidth.setToolTipText(GUIFactory.getToolTip("width"));

		Box box = Box.createHorizontalBox();
		box.add(lHeight);
		box.add(Box.createHorizontalStrut(2));
		box.add(spHeight);
		box.add(Box.createHorizontalStrut(5));
		box.add(lWidth);
		box.add(Box.createHorizontalStrut(2));
		box.add(spWidth);

		passwordBox = Box.createHorizontalBox();
		passwordBox.add(GUIFactory.getJLabel("password"));
		passwordBox.add(Box.createHorizontalStrut(5));
		passwordBox.add(Box.createHorizontalGlue());
		passwordBox.add(fdPassword);

		pExport = General.addVerticalButtons(GUIFactory.getTitle("exportData"), rExists);
		pExport.add(passwordBox, c.gridCell(1, 5, 0, 0));
		pExport.add(btTableSchema);

		pConvert = General.addVerticalButtons(GUIFactory.getTitle("convert"), cConvertImages);
		pConvert.add(General.addVerticalButtons(null, rImages), c.gridCell(1, 4, 2, 0));
		pConvert.add(box, c.gridCell(1, 5, 0, 0));
		pConvert.add(Box.createVerticalGlue());

		pOtherOptions = new JPanel();
		pOtherOptions.setLayout(new BoxLayout(pOtherOptions, BoxLayout.X_AXIS));

		pBottomContainer = new JPanel();
		pBottomContainer.setLayout(new BoxLayout(pBottomContainer, BoxLayout.X_AXIS));

		pTopContainer = new JPanel(new GridLayout(1, 2));

		add(bDatabase, c.gridCell(0, 0, 0, 0));
		add(cbDatabases, c.gridmultipleCell(1, 0, 2, 0, 3, 1));
		add(btBrowse, c.gridCell(4, 0, 0, 0));
		add(createDatabaseSelectionPanel(), c.gridmultipleCell(1, 1, 2, 0, 2, 2));

		add(Box.createVerticalStrut(10), c.gridCell(1, 2, 0, 0));
		add(pTopContainer, c.gridCell(1, 3, 0, 0));
		add(pBottomContainer, c.gridCell(1, 4, 0, 0));

		setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("exportTo")));
	}

	private void createRadioButtons(JRadioButton[] buttons, ActionListener action, String... ids) {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = GUIFactory.getJRadioButton(ids[i], action);
		}
	}

	private void reloadFiles() {
		boolean result = false;
		boolean isPrevious = false;

		cbDatabases.removeActionListener(funcSelectFile);
		cbDatabases.removeAllItems();

		myExportFile = helper.getDatabaseType();
		String dbFile = helper.getDatabaseName();
		List<String> dbFiles = dbSettings.getDatabaseFiles(myExportFile, false);

		if (dbFile.isEmpty() && !dbFiles.isEmpty()) {
			// Take the last entry in the prev. defined database list
			dbFile = dbFiles.get(dbFiles.size() - 1);
			result = true;
			isPrevious = true;
		}

		if (!result) {
			result = !dbFile.isBlank() && General.isFileExtensionOk(dbFile, myExportFile);
		}

		if (result && !dbFiles.contains(dbFile)) {
			dbFiles.add(dbFile);
			Collections.sort(dbFiles);
		}

		dbFiles.forEach(db -> cbDatabases.addItem(db));
		cbDatabases.setSelectedItem(dbFile);
		dbFile = DatabaseHelper.extractDatabase(dbFile);

		if (result && isPrevious) {
			String node = dbSettings.getNodename(dbFile, myExportFile);
			if (node != null) {
				dbSettings.setNode(node);
				helper.update(dbSettings);
			}
		}

		fdPassword.setText(General.decryptPassword(helper.getPassword()));
		fdPassword.setVisible(myExportFile.isPasswordSupported());
		dbDatabase.setVisible(myExportFile != ExportFile.TEXTFILE);
		dbDatabaseLabel.setVisible(dbDatabase.isVisible());
		cbDatabases.addActionListener(funcSelectFile);
		dialog.activateComponents();
	}

	private JComponent createDatabaseSelectionPanel() {
		JPanel result = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btBackup = GUIFactory.getJCheckBox("createBackup", profiles.isCreateBackup());
		btSkipEmpty = GUIFactory.getJCheckBox("skipEmpty", profiles.isSkipEmptyRecords());

		result.add(dbDatabaseLabel);
		result.add(dbDatabase);
		result.add(btSkipEmpty);
		result.add(btBackup);
		return result;
	}

	public void reload() {
		if (myExportFile == ExportFile.TEXTFILE) {
			dbConfig = new ConfigTextFile(profiles, true, dialog.getBuddyExport());
			pOtherOptions.removeAll();
			pOtherOptions.add((JComponent) dbConfig);
			activateComponents();
			dialog.pack();
		}
	}

	@Override
	public void setProperties() throws Exception {
		helper.setPassword(General.encryptPassword(fdPassword.getPassword()));

		int index = getSelected(rExists);
		profiles.setExportOption(index);
		profiles.setAppendRecords(index == 2);
		profiles.setToDatabase(profiles.setDatabase(helper));

		if (cConvertImages.isVisible()) {
			index = getSelected(rImages);
			profiles.setExportImages(cConvertImages.isSelected());
			profiles.setImageOption(index);
			profiles.setImageHeight(hModel.getNumber().intValue());
			profiles.setImageWidth(wModel.getNumber().intValue());
		} else {
			profiles.setExportImages(false);
			profiles.setImageOption(0);
			profiles.setImageHeight(0);
			profiles.setImageWidth(0);
		}

		if (myExportFile.isSqlDatabase()) {
			profiles.setOnConflict(getSelected(rOnConflict));
		} else {
			profiles.setOnConflict(Profiles.ON_CONFLICT_REPLACE);
		}

		profiles.setCreateBackup(btBackup.isEnabled() && btBackup.isSelected());
		profiles.setSkipEmptyRecords(btSkipEmpty.isEnabled() && btSkipEmpty.isSelected());

		String dbFile = dbDatabase.getText().trim();
		if (dbFile.isEmpty()) {
			dbFile = profiles.getProfileID();
			dbDatabase.setText(dbFile);
		}

		profiles.setDatabaseName(dbFile);
		profiles.setTextFileFormat(ConfigTextFile.STANDARD_CSV);

		if (dbConfig != null) {
			dbConfig.setProperties();
		}

		profiles.setLastSaved();
	}

	private int getSelected(JRadioButton[] buttons) {
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i].isSelected()) {
				return i;
			}
		}
		return 0;
	}

	public Dimension getComboBoxSize() {
		return bDatabase.getPreferredSize();
	}

	@Override
	public void activateComponents() {
		pTopContainer.removeAll();
		pBottomContainer.removeAll();

		switch (myExportFile) {
		case EXCEL:
			pTopContainer.add(pOtherOptions);
			pBottomContainer.add(pExport);
			break;
		case CALC, DBASE, SQLITE, POSTGRESQL:
			pTopContainer.add(pExport);
			pTopContainer.add(pOtherOptions);
			break;
		case FIREBIRD:
			pTopContainer.add(pExport);
			break;
		case TEXTFILE:
			pTopContainer.add(pOtherOptions);
			pBottomContainer.add(pConvert);
			break;
		default:
			pTopContainer.add(pExport);
			pTopContainer.add(pConvert);
			pBottomContainer.add(pOtherOptions);
		}

		pExport.setVisible(myExportFile.isAppend());
		rExists[1].setVisible(myExportFile.isSqlDatabase() || myExportFile == ExportFile.HANDBASE);
		passwordBox.setVisible(myExportFile.isPasswordSupported());
		cConvertImages.setVisible(myExportFile.isImageExport());

		spHeight.setVisible(cConvertImages.isVisible());
		spWidth.setVisible(cConvertImages.isVisible());
		lHeight.setVisible(cConvertImages.isVisible());
		lWidth.setVisible(cConvertImages.isVisible());

		rImages[0].setVisible(cConvertImages.isVisible());
		rImages[1].setVisible(cConvertImages.isVisible());
		rImages[2].setVisible(cConvertImages.isVisible());
		rImages[0].setEnabled(cConvertImages.isSelected());
		rImages[1].setEnabled(cConvertImages.isSelected());
		rImages[2].setEnabled(cConvertImages.isSelected());

		spHeight.setEnabled(cConvertImages.isSelected());
		spWidth.setEnabled(spHeight.isEnabled());

		pConvert.setVisible(cConvertImages.isVisible());
		pOtherOptions.setVisible(pOtherOptions.getComponentCount() > 0);
		btTableSchema.setVisible(myExportFile.isSqlDatabase() && rExists[0].isSelected());
		btBackup.setEnabled(!myExportFile.isConnectHost());

		if (dbConfig != null) {
			dbConfig.activateComponents();
			if (myExportFile == ExportFile.HANDBASE) {
				boolean enableImport = !rExists[0].isSelected();
				((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
			}
		}
	}
}