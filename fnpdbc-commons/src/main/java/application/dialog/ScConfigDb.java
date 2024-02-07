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
import dbengine.export.SQLite;

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

	private JTextField fdPDA;
	private JTextField dbFileName;
	private JLabel dbFileNameLabel = new JLabel();
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
	transient Profiles pdaSettings;
	transient ScFieldSelect scFieldSelect;

	private PropertyChangeSupport support;

	public ScConfigDb(IConfigSoft dialog, ScFieldSelect sc, ExportFile db, Profiles profiles) {
		myExportFile = db;
		pdaSettings = profiles;
		scFieldSelect = sc;
		this.dialog = dialog;

		support = new PropertyChangeSupport(this);
		support.addPropertyChangeListener(scFieldSelect);

		init();
		buildDialog();
		activateComponents();
		setVisible(true);
	}

	private void init() {
		funcSelectExport = e -> {
			if (myExportFile == ExportFile.HANDBASE) {
				boolean enableImport = !rExists[0].isSelected();
				((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
			} else {
				btTableSchema.setVisible(myExportFile == ExportFile.SQLITE && rExists[0].isSelected());
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
			String dbName = dbFileName.getText();
			if (dbName.isBlank()) {
				General.showMessage((JDialog) dialog, GUIFactory.getMessage("noTableDefined"), "Schema", true);
				return;
			}

			if (scFieldSelect.getFieldList().isEmpty()) {
				General.showMessage((JDialog) dialog, GUIFactory.getMessage("noFieldsDefined", dbName), "Schema", true);
				return;
			}

			SQLite db = new SQLite(pdaSettings);
			List<FieldDefinition> fields = new ArrayList<>();
			scFieldSelect.getFieldList().forEach(field -> fields.add(new FieldDefinition(field)));
			General.showMessage((JDialog) dialog, db.buildTableString(dbName, fields), "Schema", false);
		};

		funcSelectFile = e -> General.getSelectedFile((JDialog) dialog, fdPDA, myExportFile, General.EMPTY_STRING,
				false);

		funcSelectDb = e -> {
			ExportFile exportFile = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());
			support.firePropertyChange("exportfile", myExportFile, exportFile);
			myExportFile = exportFile;

			pdaSettings.setProject(myExportFile.getName());
			fdPDA.setToolTipText(myExportFile.getFileType());
			fdPDA.setText(getDatabaseName(true));

			fdPassword.setVisible(myExportFile.isPasswordSupported());
			dbFileName.setText(pdaSettings.getPdaDatabaseName());

			if (myExportFile.isSqlDatabase()) {
				rExists[0].setText(GUIFactory.getText("intoNewTable"));
				rExists[1].setText(GUIFactory.getText("replaceTableRecords"));
				rExists[2].setText(GUIFactory.getText("appendTableRecords"));
				dbFileNameLabel.setText(GUIFactory.getText("table"));
			} else if (myExportFile.isSpreadSheet()) {
				dbFileNameLabel.setText(GUIFactory.getText("worksheet"));
			} else if (myExportFile == ExportFile.XML) {
				dbFileNameLabel.setText(GUIFactory.getText("xmlRoot"));
			} else {
				rExists[0].setText(GUIFactory.getText("intoNewDatabase"));
				rExists[1].setText(GUIFactory.getText("replaceDatabaseRecords"));
				rExists[2].setText(GUIFactory.getText("appendDatabaseRecords"));
				dbFileNameLabel.setText(GUIFactory.getText("database"));
			}

			if (myExportFile.isPasswordSupported()) {
				fdPassword.setText(pdaSettings.getExportPassword());
			}

			// Restore Export Data options
			int index = pdaSettings.isAppendRecords() ? 2 : 0;
			if (myExportFile.isSqlDatabase() || myExportFile == ExportFile.HANDBASE) {
				index = pdaSettings.getExportOption();
			}
			rExists[index].setSelected(true);

			// Restore Conversion options
			cConvertImages.setSelected(
					pdaSettings.isExportImages() && pdaSettings.getTvBSoftware() == TvBSoftware.FNPROG2PDA);
			rImages[pdaSettings.getImageOption()].setSelected(true);

			dbConfig = null;
			switch (myExportFile) {
			case HANDBASE:
				dbConfig = new ConfigHanDBase(dialog, pdaSettings);
				break;
			case CALC:
				dbConfig = new ConfigCalc(pdaSettings);
				break;
			case EXCEL:
				dbConfig = new ConfigExcel(pdaSettings);
				break;
			case TEXTFILE:
				dbConfig = new ConfigTextFile(pdaSettings, true, dialog.getBuddyExport());
				break;
			case DBASE:
				dbConfig = new XBaseCharsets(pdaSettings);
				break;
			default:
				break;
			}

			pOtherOptions.removeAll();
			if (dbConfig != null) {
				pOtherOptions.add((JComponent) dbConfig);
			} else if (myExportFile == ExportFile.SQLITE) {
				pOtherOptions.add(General.addVerticalButtons(GUIFactory.getTitle("onConflict"), rOnConflict));
				rOnConflict[pdaSettings.getOnConflict()].setSelected(true);
			}

			activateComponents();
			dialog.pack();
		};
	}

	public ExportFile getExportFile() {
		return myExportFile;
	}

	public String getDatabaseName(boolean isInitial) {
		String result = isInitial ? pdaSettings.getExportFile() : fdPDA.getText().trim();
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

		fdPDA = new JTextField();
		bDatabase = new JComboBox<>(ExportFile.getExportFilenames(false));
		bDatabase.setToolTipText(GUIFactory.getToolTip("pcToFile"));
		bDatabase.addActionListener(funcSelectDb);

		Dimension dim = bDatabase.getPreferredSize();
		dim.setSize(dim.getWidth() + 10, dim.getHeight());
		bDatabase.setPreferredSize(dim);

		dbFileName = GUIFactory.getJTextField("database", General.EMPTY_STRING);
		fdPassword = new JPasswordField(8);
		JButton bt1 = GUIFactory.getJButton("browseFile", funcSelectFile);
		btBackup = GUIFactory.getJCheckBox("createBackup", pdaSettings.isCreateBackup());
		btSkipEmpty = GUIFactory.getJCheckBox("skipEmpty", pdaSettings.isSkipEmptyRecords());

		Box dbNameBox = Box.createHorizontalBox();
		dbNameBox.add(dbFileNameLabel);
		dbNameBox.add(Box.createHorizontalStrut(5));
		dbNameBox.add(Box.createHorizontalGlue());
		dbNameBox.add(dbFileName);

		hModel = new SpinnerNumberModel(pdaSettings.getImageHeight(), 0, 900, 10);
		wModel = new SpinnerNumberModel(pdaSettings.getImageWidth(), 0, 900, 10);

		if (myExportFile.isSqlDatabase()) {
			createRadioButtons(rExists, funcSelectExport, "intoNewTable", "replaceTableRecords", "appendTableRecords");
		} else {
			createRadioButtons(rExists, funcSelectExport, "intoNewDatabase", "replaceDatabaseRecords",
					"appendDatabaseRecords");
		}

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
		add(fdPDA, c.gridCell(1, 0, 2, 0));
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
			dbConfig = new ConfigTextFile(pdaSettings, true, dialog.getBuddyExport());
			pOtherOptions.removeAll();
			pOtherOptions.add((JComponent) dbConfig);
			activateComponents();
			dialog.pack();
		}
	}

	@Override
	public void setProperties() throws Exception {
		int index = getSelected(rExists);
		pdaSettings.setExportOption(index);
		pdaSettings.setAppendRecords(index == 2);
		pdaSettings.setExportFile(getDatabaseName(false));

		if (cConvertImages.isVisible()) {
			index = getSelected(rImages);
			pdaSettings.setExportImages(cConvertImages.isSelected());
			pdaSettings.setImageOption(index);
			pdaSettings.setImageHeight(hModel.getNumber().intValue());
			pdaSettings.setImageWidth(wModel.getNumber().intValue());
		} else {
			pdaSettings.setExportImages(false);
			pdaSettings.setImageOption(0);
			pdaSettings.setImageHeight(0);
			pdaSettings.setImageWidth(0);
		}

		if (myExportFile == ExportFile.SQLITE) {
			pdaSettings.setOnConflict(getSelected(rOnConflict));
		} else {
			pdaSettings.setOnConflict(4);
		}

		pdaSettings.setCreateBackup(btBackup.isEnabled() && btBackup.isSelected());
		pdaSettings.setSkipEmptyRecords(btSkipEmpty.isEnabled() && btSkipEmpty.isSelected());
		pdaSettings.setExportPassword(fdPassword.getPassword());

		String dbFile = dbFileName.getText().trim();
		if (dbFile.isEmpty()) {
			dbFile = pdaSettings.getProfileID();
			dbFileName.setText(dbFile);
		}

		pdaSettings.setPdaDatabaseName(dbFile);
		pdaSettings.setTextFileFormat(ConfigTextFile.STANDARD_CSV);

		if (dbConfig != null) {
			dbConfig.setProperties();
		}

		pdaSettings.setLastSaved();
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
		case CALC:
		case EXCEL:
			pTopContainer.add(pOtherOptions);
			break;
		case DBASE:
		case SQLITE:
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
		dbFileName.setVisible(myExportFile != ExportFile.TEXTFILE);
		dbFileNameLabel.setVisible(myExportFile != ExportFile.TEXTFILE);
		btTableSchema.setVisible(myExportFile == ExportFile.SQLITE && rExists[0].isSelected());

		if (dbConfig != null) {
			dbConfig.activateComponents();
			if (myExportFile == ExportFile.HANDBASE) {
				boolean enableImport = !rExists[0].isSelected();
				((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
			}
		}
	}
}