package application.dialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import application.interfaces.ExportFile;
import application.interfaces.IConfigDb;
import application.interfaces.IConfigSoft;
import application.interfaces.TvBSoftware;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbengine.SqlDB;
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

	private JTextField dbFileName;
	private JTextField dbDatabase; // database, table or worksheet name
	private JLabel dbDatabaseLabel = new JLabel();
	private JPasswordField fdPassword;

	private JComboBox<String> bDatabase;
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

	transient ActionListener funcSelectExport;
	transient ActionListener funcSelectConvert;
	transient ActionListener funcSelectFile;
	transient ActionListener funcSelectDb;
	transient ActionListener funcShowSchema;

	private ExportFile myExportFile;
	private JButton btTableSchema;

	transient IConfigDb dbConfig;
	transient IConfigSoft dialog;
	transient Profiles profiles;
	transient ScFieldSelect scFieldSelect;
	private DatabaseHelper helper;

	private PropertyChangeSupport support;

	public ScConfigDb(IConfigSoft dialog, ScFieldSelect sc, ExportFile db, Profiles profiles) {
		myExportFile = db;
		scFieldSelect = sc;
		this.dialog = dialog;
		this.profiles = profiles;

		support = new PropertyChangeSupport(this);
		support.addPropertyChangeListener(scFieldSelect);
		helper = profiles.getToDatabase();

		init();
		buildDialog();
		activateComponents();
		setVisible(true);
	}

	private void init() {
		final String schema = "Schema";

		funcSelectExport = e -> {
			if (myExportFile == ExportFile.HANDBASE) {
				boolean enableImport = !rExists[0].isSelected();
				((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
			} else {
				btTableSchema.setVisible(myExportFile.isSqlDatabase() && rExists[0].isSelected());
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
				General.showMessage((JDialog) dialog, GUIFactory.getMessage("noTableDefined"), schema, true);
				return;
			}

			if (scFieldSelect.getFieldList().isEmpty()) {
				General.showMessage((JDialog) dialog, GUIFactory.getMessage("noFieldsDefined", dbName), "Schema", true);
				return;
			}

			SqlDB db;
			if (myExportFile == ExportFile.SQLITE) {
				db = new SQLite(profiles);
			} else {
				db = new PostgreSQL(profiles);
			}

			List<FieldDefinition> fields = new ArrayList<>();
			scFieldSelect.getFieldList().forEach(field -> fields.add(new FieldDefinition(field)));
			General.showMessage((JDialog) dialog, db.buildTableString(dbName, fields), schema, false);
		};

		funcSelectFile = e -> General.getSelectedFile((JDialog) dialog, dbFileName, myExportFile, General.EMPTY_STRING,
				false);

		funcSelectDb = e -> {
			ExportFile software = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());

			if (software != myExportFile && !dbDatabase.getText().isEmpty()) {
				if (software.isConnectHost() != myExportFile.isConnectHost()) {
					General.showMessage(this, GUIFactory.getMessage("invalidDatabaseSwitch", General.EMPTY_STRING),
							BasicDialog.CONFIG_ERROR, true);
					bDatabase.setSelectedItem(myExportFile.toString());
					return;
				}
			}

			support.firePropertyChange("exportfile", myExportFile, software);
			myExportFile = software;

			profiles.setProject(myExportFile.getName());
			helper.setDatabaseType(myExportFile);

			dbFileName.setToolTipText(myExportFile.getFileType());
			fdPassword.setText(General.decryptPassword(helper.getPassword()));
			fdPassword.setVisible(myExportFile.isPasswordSupported());
			dbFileName.setText(getDatabaseName(true));
			dbDatabase.setText(profiles.getDatabaseName());

			setRadioButtonText();

			// Restore Export Data options
			int index = profiles.isAppendRecords() ? 2 : 0;
			if (myExportFile.isSqlDatabase() || myExportFile == ExportFile.HANDBASE) {
				index = profiles.getExportOption();
			}
			rExists[index].setSelected(true);

			// Restore Conversion options
			cConvertImages
					.setSelected(profiles.isExportImages() && profiles.getTvBSoftware() == TvBSoftware.FNPROG2PDA);
			rImages[profiles.getImageOption()].setSelected(true);

			dbConfig = null;
			switch (myExportFile) {
			case HANDBASE:
				dbConfig = new ConfigHanDBase(dialog, profiles);
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
				if (myExportFile == ExportFile.POSTGRESQL) {
					rOnConflict[Profiles.ON_CONFLICT_ABORT].setVisible(false);
					rOnConflict[Profiles.ON_CONFLICT_FAIL].setVisible(false);
					rOnConflict[Profiles.ON_CONFLICT_ROLLBACK].setVisible(false);
				}
			}

			activateComponents();
			dialog.pack();
		};
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

	public ExportFile getExportFile() {
		return myExportFile;
	}

	private String getDatabaseName(boolean isInitial) {
		String result = isInitial ? helper.getDatabaseName() : dbFileName.getText().trim();
		if (result.isEmpty()) {
			result = General.getDefaultPDADatabase(myExportFile);
		} else if (!General.isFileExtensionOk(result, myExportFile)) {
			result = General.getBaseName(result, myExportFile);
		}
		return result;
	}

	private void buildDialog() {
		setLayout(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		dbFileName = new JTextField();
		bDatabase = new JComboBox<>(ExportFile.getExportFilenames(false));
		bDatabase.setToolTipText(GUIFactory.getToolTip("pcToFile"));
		bDatabase.addActionListener(funcSelectDb);

		Dimension dim = bDatabase.getPreferredSize();
		dim.setSize(dim.getWidth() + 10, dim.getHeight());
		bDatabase.setPreferredSize(dim);

		dbDatabase = GUIFactory.getJTextField("database", General.EMPTY_STRING);
		fdPassword = new JPasswordField(8);
		JButton bt1 = GUIFactory.getJButton("browseFile", funcSelectFile);
		btBackup = GUIFactory.getJCheckBox("createBackup", profiles.isCreateBackup());
		btSkipEmpty = GUIFactory.getJCheckBox("skipEmpty", profiles.isSkipEmptyRecords());

		Box dbNameBox = Box.createHorizontalBox();
		dbNameBox.add(dbDatabaseLabel);
		dbNameBox.add(Box.createHorizontalStrut(5));
		dbNameBox.add(Box.createHorizontalGlue());
		dbNameBox.add(dbDatabase);

		hModel = new SpinnerNumberModel(profiles.getImageHeight(), 0, 900, 10);
		wModel = new SpinnerNumberModel(profiles.getImageWidth(), 0, 900, 10);

		createRadioButtons(rExists, funcSelectExport, General.EMPTY_STRING, General.EMPTY_STRING, General.EMPTY_STRING);
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
		pExport.add(dbNameBox, c.gridCell(1, 4, 0, 0));
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

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p1.add(dbNameBox);
		p1.add(btSkipEmpty);
		p1.add(Box.createHorizontalStrut(5));
		p1.add(btBackup);
		p1.add(Box.createHorizontalGlue());

		add(bDatabase, c.gridCell(0, 0, 0, 0));
		add(dbFileName, c.gridCell(1, 0, 2, 0));
		add(bt1, c.gridCell(2, 0, 0, 0));
		add(p1, c.gridCell(1, 1, 0, 0));
		add(Box.createVerticalStrut(5), c.gridCell(1, 2, 0, 0));
		add(pTopContainer, c.gridCell(1, 3, 0, 0));
		add(pBottomContainer, c.gridCell(1, 4, 0, 0));

		setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("exportTo")));
		bDatabase.setSelectedItem(myExportFile.getName());
	}

	private void createRadioButtons(JRadioButton[] buttons, ActionListener action, String... ids) {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = GUIFactory.getJRadioButton(ids[i], action);
		}
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
		helper.setDatabase(getDatabaseName(false));
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
		dbDatabase.setVisible(myExportFile != ExportFile.TEXTFILE);
		dbDatabaseLabel.setVisible(myExportFile != ExportFile.TEXTFILE);
		btTableSchema.setVisible(myExportFile.isSqlDatabase() && rExists[0].isSelected());

		if (dbConfig != null) {
			dbConfig.activateComponents();
			if (myExportFile == ExportFile.HANDBASE) {
				boolean enableImport = !rExists[0].isSelected();
				((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
			}
		}
	}
}