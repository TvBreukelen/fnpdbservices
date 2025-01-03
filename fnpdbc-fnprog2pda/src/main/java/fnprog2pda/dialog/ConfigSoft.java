package fnprog2pda.dialog;

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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import application.dialog.ConfigDialog;
import application.dialog.ConfigTextFile.BuddyExport;
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
import dbengine.utils.DatabaseHelper;
import fnprog2pda.dbengine.MSAccess;
import fnprog2pda.model.MiscellaneousData;
import fnprog2pda.preferences.PrefFNProg;
import fnprog2pda.software.DatabaseFactory;
import fnprog2pda.software.FNPSoftware;
import fnprog2pda.software.FNProgramvare;

public class ConfigSoft extends ConfigDialog {
	/**
	 * Title: ConfigSoft Description: FNProgramvare Software Configuration
	 * Copyright: (c) 2004-2020
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private static final long serialVersionUID = -5786862663126955551L;
	private JComboBox<String> fdView;

	private JButton btMisc;
	private JTextField fdDatabase;

	transient ActionListener funcSelectFile;
	transient ActionListener funcSelectView;

	private FNPSoftware myImportFile = FNPSoftware.UNDEFINED;
	transient DatabaseFactory dbFactory = DatabaseFactory.getInstance();

	transient Map<String, MiscellaneousData> miscDataMap = new HashMap<>();

	private ProgramDialog dialog;
	private ProjectModel model;

	private static final String FUNC_NEW = "funcNew";

	public ConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNew) {
		this.dialog = dialog;
		this.model = model;

		isNewProfile = isNew;
		init(dbFactory);
	}

	private void init(IDatabaseFactory factory) {
		super.init(factory, PrefFNProg.getInstance());

		myExportFile = ExportFile.getExportFile(profiles.getProjectID());
		myImportFile = isNewProfile ? FNPSoftware.UNDEFINED
				: FNPSoftware.getSoftware(dbVerified.getDatabaseTypeAsString());
		myView = isNewProfile ? General.EMPTY_STRING : profiles.getTableName();

		btMisc = GUIFactory.createToolBarButton(GUIFactory.getTitle("miscSettings"), "Properties.png", e -> {
			ConfigMiscellaneous miscDialog = new ConfigMiscellaneous(myImportFile, miscDataMap.get(myView));
			miscDialog.setVisible(true);
		});

		funcSelectFile = e -> {
			General.getSelectedFile(ConfigSoft.this, fdDatabase, ExportFile.ACCESS, General.EMPTY_STRING, true);
			if (!fdDatabase.getText().isBlank()) {
				verifyDatabase(false);
			}
		};

		funcSelectView = e -> {
			String view = fdView.getSelectedItem().toString();
			if (factory.isConnected()) {
				try {
					reloadFieldSelect(view);
				} catch (Exception ex) {
					General.errorMessage(ConfigSoft.this, ex, GUIFactory.getTitle("configError"), null);
					fdView.setSelectedItem(myView);
					return;
				}
			}
			activateComponents();
		};

		fdView = new JComboBox<>(myImportFile.getViews());
		fdView.setPreferredSize(configDb.getComboBoxSize());

		profile = GUIFactory.getJTextField(FUNC_NEW, isNewProfile ? General.EMPTY_STRING : profiles.getProfileID());
		profile.getDocument().addDocumentListener(funcDocumentChange);

		verifyDatabase(true);
		buildDialog();
		activateComponents();
		pack();
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
		result.add(Box.createHorizontalStrut(2));
		result.add(btMisc);
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
		JPanel gPanel = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		if (myImportFile != FNPSoftware.UNDEFINED) {
			if (profiles.getTableName().isEmpty()) {
				fdView.setSelectedIndex(0);
				myView = fdView.getSelectedItem().toString();
			} else {
				fdView.setSelectedItem(profiles.getTableName());
			}
		}

		fdView.addActionListener(funcSelectView);
		fdDatabase = GUIFactory.getJTextField("fnpDatabase",
				isNewProfile ? General.EMPTY_STRING : dbVerified.getDatabaseName());
		fdDatabase.setEditable(false);
		JButton btBrowse = GUIFactory.getJButton("browseDatabase", funcSelectFile);

		gPanel.add(fdView, c.gridCell(0, 0, 0, 0));
		gPanel.add(fdDatabase, c.gridCell(1, 0, 2, 0));
		gPanel.add(btBrowse, c.gridCell(2, 0, 0, 0));
		gPanel.setBorder(BorderFactory.createTitledBorder(GUIFactory.getText("exportFrom")));

		if (isNewProfile) {
			JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p1.setBorder(BorderFactory.createEtchedBorder());
			p1.add(Box.createVerticalStrut(40));
			p1.add(GUIFactory.getJLabel(FUNC_NEW));
			p1.add(Box.createHorizontalStrut(10));
			p1.add(profile);
			panel.add(p1);
		} else {
			MiscellaneousData misc = miscDataMap.computeIfAbsent(myView, e -> new MiscellaneousData());
			misc.loadProfile(profiles);
			FilterData filter = filterDataMap.computeIfAbsent(myView, e -> new FilterData());
			filter.loadProfile(profiles);
			SortData data = sortDataMap.computeIfAbsent(myView, e -> new SortData());
			data.loadProfile(profiles);
		}

		panel.add(gPanel);
		panel.add(configDb);
		return panel;
	}

	@Override
	protected void showHelp() {
		if (tabPane.getSelectedIndex() == 0) {
			setHelpFile("exportfiles_fn");
		} else if (tabPane.getSelectedIndex() == 1) {
			setHelpFile("exportfields");
		}
		super.showHelp();
	}

	@Override
	protected void close() {
		dbFactory.close();
		super.close();
	}

	@Override
	protected void save() throws Exception {
		myExportFile = configDb.getDatabaseHelper().getDatabaseType();
		checkDuplicatelFieldNames();
		checkReservedWordFieldNames();

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

		dbVerified.setDatabase(fdDatabase.getText().trim());
		dbVerified.setDatabaseType(myImportFile.getName());
		dbVerified.setDatabaseVersion(dbFactory.getDatabaseVersion());

		myExportFile = configDb.getDatabaseHelper().getDatabaseType();

		profiles.setProject(myExportFile.getName());
		profiles.setProfile(profile.getText());
		profiles.setTableName(myView, true);
		profiles.setFromDatabase(profiles.setDatabase(dbVerified));

		profiles.setUserList(fieldSelect.getFieldList());
		profiles.setLastIndex(0);
		profiles.setLastExported(General.EMPTY_STRING);
		configDb.setProperties();

		if (btMisc.isEnabled()) {
			miscDataMap.getOrDefault(myView, new MiscellaneousData()).saveProfile(profiles);
		}

		filterDataMap.getOrDefault(myView, new FilterData()).saveProfile(profiles);
		sortDataMap.getOrDefault(myView, new SortData()).saveProfile(profiles);
		profiles.setLastSaved();

		dialog.updateProfile(isNewProfile ? Action.ADD : Action.EDIT);
	}

	private void verifyDatabase(boolean isFirstRun) {
		String db = isNewProfile ? General.EMPTY_STRING : dbVerified.getDatabaseName();
		String dbCheck = isFirstRun ? db : fdDatabase.getText().trim();
		if (dbCheck.isEmpty()) {
			return;
		}

		try {
			dbFactory.connect2DB(new DatabaseHelper(dbCheck, ExportFile.ACCESS));
			dbFactory.verifyDatabase(dbCheck);

			if (isFirstRun) {
				MSAccess msAccess = (MSAccess) dbFactory.getInputFile();
				if (!msAccess.getFileOpenWarning().isEmpty()) {
					General.showMessage(this, msAccess.getFileOpenWarning(), GUIFactory.getTitle("warning"), false);
					msAccess.setFileOpenWarning(General.EMPTY_STRING);
				}
			}

			if (dbFactory.getDatabaseType() != myImportFile) {
				if (!isNewProfile) {
					General.showMessage(this, GUIFactory.getMessage("funcSelectDb", myImportFile.getName(),
							dbFactory.getDatabaseType().getName()), GUIFactory.getTitle("warning"), false);
				}

				refreshViews();

				fdView.setSelectedIndex(0);
				myView = fdView.getSelectedItem().toString();
				fdView.addActionListener(funcSelectView);
			} else {
				refreshViews();
			}

			reloadFieldSelect(myView);
		} catch (Exception e) {
			General.errorMessage(isFirstRun ? getParent() : this, e, GUIFactory.getTitle("configError"), null);
			dbFactory.close();
		}
		activateComponents();
	}

	private void refreshViews() {
		myImportFile = dbFactory.getDatabaseType();
		fdView.removeActionListener(funcSelectView);
		fdView.setModel(new DefaultComboBoxModel<>(myImportFile.getViews()));
		fdView.setSelectedItem(myView);
		fdView.addActionListener(funcSelectView);
	}

	private void reloadFieldSelect(String view) throws Exception {
		dbFactory.loadConfiguration(view);
		FNProgramvare mySoft = FNProgramvare.getSoftware(myImportFile);
		mySoft.setupDBTranslation(isNewProfile); // Load user fields
		fieldSelect.loadFieldPanel(mySoft.getDbUserFields(), true);
		myView = view;
		configDb.reload();
	}

	@Override
	public void activateComponents() {
		super.activateComponents();

		// Wait till the dialog has been completely created
		if (tabPane != null && tabPane.getTabCount() > 0) {
			btMisc.setEnabled(isFileValid && myImportFile.isUseMisc(myView));
			fdView.setEnabled(isFileValid);

			if (btFilter.isEnabled()) {
				FilterData data = filterDataMap.computeIfAbsent(myView, e -> new FilterData());
				data.setTvBSoftware(TvBSoftware.FNPROG2PDA);
				data.setProfileID(profileID);
			}

			if (btSortOrder.isEnabled()) {
				sortDataMap.putIfAbsent(myView, new SortData());
			}

			if (btMisc.isEnabled()) {
				MiscellaneousData data = miscDataMap.computeIfAbsent(myView, e -> new MiscellaneousData());
				data.setTableName(myView);
				data.setProfileID(profileID);
			}

			tabPane.setEnabledAt(1, isFileValid);
		}
	}

	@Override
	public IExportProcess getExportProcess() {
		return new ExportProcess();
	}

	@Override
	public BuddyExport getBuddyExport() {
		switch (myImportFile) {
		case BOOKCAT:
			return myView.equals("Book") ? BuddyExport.BOOK_BUDDY : BuddyExport.NONE;
		case CATRAXX:
			return myView.equals("Album") ? BuddyExport.MUSIC_BUDDY : BuddyExport.NONE;
		case CATVIDS:
			return myView.equals("Contents") ? BuddyExport.MOVIE_BUDDY : BuddyExport.NONE;
		default:
			return BuddyExport.NONE;
		}
	}

}