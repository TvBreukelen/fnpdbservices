package application.dialog;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

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
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ScConfigDb extends JPanel implements IConfigDb {
	/**
	 * Title: ScConfigDb Description: PDA Database Configuration Copyright: (c) 2016
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private static final long serialVersionUID = 8000165106403251367L;

	private JTextField fdPDA;
	private JTextField dbFileName;
	private JLabel dbFileNameLabel = new JLabel();
	private JTextField fdEncoding;
	private JPasswordField fdPassword;
	private JButton btEncoding;

	private JComboBox<String> bDatabase;
	private JPanel pExport;
	private JPanel pConvert;
	private JPanel pOtherOptions;
	private JPanel pBottomContainer;
	private JPanel pTopContainer;
	private Box passwordBox;

	private JRadioButton[] rExists = new JRadioButton[3];
	private JRadioButton[] rImages = new JRadioButton[3];
	private SpinnerNumberModel hModel;
	private SpinnerNumberModel wModel;
	private JSpinner spHeight;
	private JSpinner spWidth;
	private JLabel lHeight;
	private JLabel lWidth;

	private JCheckBox[] cConvert = new JCheckBox[4];
	private JCheckBox btBackup;
	private IConfigSoft _dialog;

	private ActionListener funcSelectExport;
	private ActionListener funcSelectConvert;
	private ActionListener funcSelectFile;
	private ActionListener funcSelectDb;
	private ActionListener funcEncoding;
	private ExportFile myExportFile;

	private IConfigDb dbConfig;
	private Profiles pdaSettings;
	private boolean isImport;

	public ScConfigDb(IConfigSoft dialog, ExportFile db, Profiles profiles) {
		_dialog = dialog;
		myExportFile = db;
		pdaSettings = profiles;

		funcSelectExport = e -> {
			if (myExportFile == ExportFile.HANDBASE) {
				boolean enableImport = !rExists[0].isSelected();
				((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
			}
		};

		funcSelectConvert = e -> {
			rImages[0].setEnabled(cConvert[3].isSelected());
			rImages[1].setEnabled(cConvert[3].isSelected());
			rImages[2].setEnabled(cConvert[3].isSelected());
			spHeight.setEnabled(cConvert[3].isSelected() && myExportFile != ExportFile.REFERENCER);
			spWidth.setEnabled(spHeight.isEnabled());
		};

		funcSelectDb = e -> {
			myExportFile = ExportFile.getExportFile(bDatabase.getSelectedItem().toString());
			
			pdaSettings.setProject(myExportFile.getName());
			fdPDA.setToolTipText(myExportFile.getFileType());
			fdPDA.setText(getDatabaseName(true));
			setEncodingText();

			fdEncoding.setVisible(myExportFile.isEncodingSupported());
			btEncoding.setVisible(fdEncoding.isVisible());
			fdPassword.setVisible(myExportFile.isPasswordSupported());
			dbFileName.setText(pdaSettings.getPdaDatabaseName());
			
			String resourceID = "database";
			switch(myExportFile) {
			case ACCESS:
			case SQLITE:
				resourceID = "table";
				break;
			case EXCEL:
				resourceID = "sheet";
			case XML:
				resourceID = "xmlRoot";
			default:
				break;
			}
			
			dbFileNameLabel.setText(GUIFactory.getText(resourceID));

			if (myExportFile.isPasswordSupported()) {
				fdPassword.setText(pdaSettings.getExportPassword());
			}

			// Restore Export Data options
			int index = 0;
			if (myExportFile == ExportFile.HANDBASE) {
				index = pdaSettings.getExportOption();
			} else {
				index = pdaSettings.isAppendRecords() ? 2 : 0;
			}
			rExists[index].setSelected(true);

			// Restore Conversion options
			cConvert[0].setSelected(pdaSettings.isExportBoolean());
			cConvert[1].setSelected(pdaSettings.isExportDate());
			cConvert[2].setSelected(pdaSettings.isExportTime());
			cConvert[3].setSelected(pdaSettings.isExportImages());
			rImages[pdaSettings.getImageOption()].setSelected(true);

			dbConfig = null;
			switch (myExportFile) {
			case HANDBASE:
				dbConfig = new ConfigHanDBase(_dialog, pdaSettings);
				break;
			case REFERENCER:
				dbConfig = new ConfigReferencer(pdaSettings);
				break;
			case EXCEL:
				dbConfig = new ConfigExcel(pdaSettings);
				break;
			case TEXTFILE:
				dbConfig = new ConfigTextFile(pdaSettings, true);
			default:
				break;
			}

			pOtherOptions.removeAll();
			if (dbConfig != null) {
				pOtherOptions.add((JComponent) dbConfig);
			}

			activateComponents();
			_dialog.pack();
			_dialog.activateComponents();
		};

		funcSelectFile = e -> General.getSelectedFile((JDialog) _dialog, fdPDA, myExportFile, "", false);

		funcEncoding = e -> {
			ConfigCharset config = new ConfigCharset(ScConfigDb.this, pdaSettings);
			config.setVisible(true);
			setEncodingText();
		};

		buildDialog();
		activateComponents();
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
		dbFileName = GUIFactory.getJTextField("database", "");
		fdPassword = new JPasswordField(8);
		JButton bt1 = GUIFactory.getJButton("browseFile", funcSelectFile);
		btBackup = GUIFactory.getJCheckBox("createBackup", pdaSettings.isCreateBackup());

		Box dbNameBox = Box.createHorizontalBox();
		dbNameBox.add(dbFileNameLabel);
		dbNameBox.add(Box.createHorizontalStrut(5));
		dbNameBox.add(Box.createHorizontalGlue());
		dbNameBox.add(dbFileName);
		
		fdEncoding = new JTextField();
		fdEncoding.setEditable(false);

		btEncoding = GUIFactory.getJButton("funcEncoding", funcEncoding);
		setEncodingText();

		hModel = new SpinnerNumberModel(pdaSettings.getImageHeight(), 0, 900, 10);
		wModel = new SpinnerNumberModel(pdaSettings.getImageWidth(), 0, 900, 10);

		rExists[0] = GUIFactory.getJRadioButton("intoNewDatabase");
		rExists[1] = GUIFactory.getJRadioButton("replaceRecords");
		rExists[2] = GUIFactory.getJRadioButton("appendRecords");

		rImages[0] = GUIFactory.getJRadioButton("imageToBitmap");
		rImages[1] = GUIFactory.getJRadioButton("imageToJpeg");
		rImages[2] = GUIFactory.getJRadioButton("imageToPng");

		cConvert[0] = GUIFactory.getJCheckBox("booleansToCheckbox", true);
		cConvert[1] = GUIFactory.getJCheckBox("datesToDate", true);
		cConvert[2] = GUIFactory.getJCheckBox("timesToTime", true);
		cConvert[3] = GUIFactory.getJCheckBox("imageToImageFile", false);

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

		pExport = General.addVerticalButtons(GUIFactory.getTitle("exportData"), funcSelectExport, rExists);
		pExport.add(dbNameBox, c.gridCell(1, 4, 0, 0));
		pExport.add(passwordBox, c.gridCell(1, 5, 0, 0));

		pConvert = General.addVerticalButtons(GUIFactory.getTitle("convert"), funcSelectConvert, cConvert);
		pConvert.add(General.addVerticalButtons(null, null, rImages[0], rImages[1], rImages[2]),
				c.gridCell(1, 4, 2, 0));
		pConvert.add(box, c.gridCell(1, 5, 0, 0));
		pConvert.add(Box.createVerticalGlue());

		pOtherOptions = new JPanel();
		pOtherOptions.setLayout(new BoxLayout(pOtherOptions, BoxLayout.X_AXIS));

		pBottomContainer = new JPanel();
		pBottomContainer.setLayout(new BoxLayout(pBottomContainer, BoxLayout.X_AXIS));

		pTopContainer = new JPanel(new GridLayout(1, 2));

		JPanel p1 = new JPanel(new GridBagLayout());
		p1.add(dbNameBox, c.gridCell(0, 0, 0, 0));
		p1.add(btBackup, c.gridCell(1, 0, 2, 0));
		p1.add(btEncoding, c.gridCell(2, 0, 0, 0));
		p1.add(fdEncoding, c.gridCell(3, 0, 2, 0));

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

	private void setEncodingText() {
		String encoding = isImport ? pdaSettings.getImportFileEncoding() : pdaSettings.getEncoding();
		fdEncoding.setText(encoding.isEmpty() ? GUIFactory.getText("default") : encoding);
	}

	@Override
	public void setProperties() throws Exception {
		int index = 0;
		for (int i = 0; i < 3; i++) {
			if (rExists[i].isSelected()) {
				index = i;
				break;
			}
		}

		if (myExportFile == ExportFile.HANDBASE) {
			pdaSettings.setExportOption(index);
		} else {
			pdaSettings.setAppendRecords(index == 2);
		}

		index = 0;
		for (int i = 0; i < 3; i++) {
			if (rImages[i].isSelected()) {
				index = i;
				break;
			}
		}

		pdaSettings.setExportFile(getDatabaseName(false));
		pdaSettings.setCreateBackup(btBackup.isEnabled() && btBackup.isSelected());
		pdaSettings.setExportBoolean(cConvert[0].isVisible() ? cConvert[0].isSelected() : false);
		pdaSettings.setExportDate(cConvert[1].isVisible() ? cConvert[1].isSelected() : false);
		pdaSettings.setExportTime(cConvert[2].isVisible() ? cConvert[2].isSelected() : false);
		pdaSettings.setExportImages(cConvert[3].isVisible() ? cConvert[3].isSelected() : false);
		pdaSettings.setImageOption(cConvert[3].isVisible() ? index : 0);
		pdaSettings.setImageHeight(cConvert[3].isVisible() ? hModel.getNumber().intValue() : 0);
		pdaSettings.setImageWidth(cConvert[3].isVisible() ? wModel.getNumber().intValue() : 0);
		pdaSettings.setExportPassword(fdPassword.getPassword());
		
		String dbFile = dbFileName.getText().trim();
		if (dbFile.isEmpty()) {
			dbFile = pdaSettings.getProfileID();
			dbFileName.setText(dbFile);
		}
		
		pdaSettings.setPdaDatabaseName(dbFile);

		if (pOtherOptions.isVisible()) {
			IConfigDb myDialog = (IConfigDb) pOtherOptions.getComponent(0);
			myDialog.setProperties();
		}
		pdaSettings.setLastSaved();
	}

	public Dimension getComboBoxSize() {
		return bDatabase.getPreferredSize();
	}

	public Dimension getCheckBoxSize() {
		return btBackup.getPreferredSize();
	}

	@Override
	public void activateComponents() {
		pTopContainer.removeAll();
		pBottomContainer.removeAll();

		switch (myExportFile) {
		case REFERENCER:
			pTopContainer.add(pConvert);
			pTopContainer.add(pOtherOptions);
			break;
		case EXCEL:
			pTopContainer.add(pConvert);
			pBottomContainer.add(pOtherOptions);
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
		rExists[1].setVisible(myExportFile == ExportFile.HANDBASE);
		passwordBox.setVisible(myExportFile.isPasswordSupported());

		cConvert[0].setVisible(myExportFile.isBooleanExport());
		cConvert[1].setVisible(myExportFile.isDateExport());
		cConvert[2].setVisible(myExportFile.isTimeExport());
		cConvert[3].setVisible(myExportFile.isImageExport());

		spHeight.setVisible(cConvert[3].isVisible());
		spWidth.setVisible(cConvert[3].isVisible());
		lHeight.setVisible(cConvert[3].isVisible());
		lWidth.setVisible(cConvert[3].isVisible());

		rImages[1].setVisible(cConvert[3].isVisible() && myExportFile != ExportFile.REFERENCER);
		rImages[0].setVisible(rImages[1].isVisible());
		rImages[2].setVisible(rImages[1].isVisible());
		rImages[0].setEnabled(cConvert[3].isSelected());
		rImages[1].setEnabled(cConvert[3].isSelected());
		rImages[2].setEnabled(cConvert[3].isSelected());

		spHeight.setEnabled(cConvert[3].isSelected() && myExportFile != ExportFile.REFERENCER);
		spWidth.setEnabled(spHeight.isEnabled());

		pConvert.setVisible(cConvert[0].isVisible() || cConvert[1].isVisible() || cConvert[2].isVisible()
				|| cConvert[3].isVisible());
		pOtherOptions.setVisible(pOtherOptions.getComponentCount() > 0);

		dbFileName.setVisible(myExportFile != ExportFile.TEXTFILE);
		dbFileNameLabel.setVisible(myExportFile != ExportFile.TEXTFILE);
		
		if (pTopContainer.isVisible() && myExportFile != ExportFile.LIST && myExportFile != ExportFile.XML) {
			pTopContainer.setBorder(BorderFactory.createRaisedBevelBorder());
		} else {
			pTopContainer.setBorder(null);
		}

		if (dbConfig != null) {
			dbConfig.activateComponents();
		}

		if (myExportFile == ExportFile.HANDBASE) {
			boolean enableImport = !rExists[0].isSelected();
			((ConfigHanDBase) dbConfig).setImportEnabled(enableImport);
		}
	}
}