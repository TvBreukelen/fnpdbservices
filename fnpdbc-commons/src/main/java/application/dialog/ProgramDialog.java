package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTitledPanel;

import com.github.lgooddatepicker.tableeditors.DateTimeTableEditor;

import application.interfaces.ExportFile;
import application.interfaces.ExportStatus;
import application.interfaces.IConfigSoft;
import application.interfaces.IDatabaseFactory;
import application.interfaces.IExportProcess;
import application.interfaces.TvBSoftware;
import application.model.ProjectModel;
import application.preferences.Databases;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.table.ButtonRenderer;
import application.table.ButtonTableCellEditor;
import application.table.DateTimeRenderer;
import application.table.ETable;
import application.table.ExportToTableCellEditor;
import application.table.FilenameRenderer;
import application.table.MainTableSelectionListener;
import application.table.StringActionTableCellEditor;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.InternetSitesMenu;

public abstract class ProgramDialog extends JFrame implements PropertyChangeListener {
	public enum Action {
		ADD, EDIT, CLONE, DELETE, TABCHANGE
	}

	private JButton btClone;
	private JButton btEdit;
	private JButton btRemove;
	private JButton btView;
	private JButton btExport;

	private JProgressBar progressBar;
	private JLabel progressText;

	private JCheckBox bIncremental;
	private JCheckBox bNewRecords;
	private JCheckBox bNoFilter;

	transient ActionListener funcNew;
	transient ActionListener funcEdit;
	transient ActionListener funcHelp;
	transient ActionListener funcLanguage;
	transient ActionListener funcLookAndFeel;
	transient ActionListener funcExit;
	transient ActionListener funcSave;

	protected IExportProcess exportProcess;
	protected IDatabaseFactory dbFactory;

	private long oldGeneralCRC;
	private String oldLastModified = "";
	private String myHelpFile;
	transient GeneralSettings generalSettings = GeneralSettings.getInstance();

	protected JTextField lSoftware = new JTextField();
	protected JTextField lSoftwareID = new JTextField();
	private ExportFile myExportFile;

	protected boolean isProfileSet = true;
	private boolean isInit = true;
	private boolean isMainScreen = false;
	private TvBSoftware software;
	private Component centerScreen;

	private JTabbedPane tabPane;
	private ProjectModel model;

	private Map<String, JTable> projects = new HashMap<>();

	protected Profiles pdaSettings;
	protected Databases dbSettings;

	private static final String CONFIG_ERROR = "configError";
	private static final String FUNC_REMOVE = "funcRemove";
	private static final String MENU_HELP = "menuHelp";
	private static final String MENU_WELCOME = "menuWelcome";
	private static final String PROFILE_ERROR = "profileError";
	private static final String CHECK_VERSION = "checkVersion";

	private static final long serialVersionUID = 7285565159492721667L;

	public ProgramDialog(Profiles profile, boolean mainScreen) {
		// Only relevant for MacOS
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.fileDialogForDirectories", "false");

		software = profile.getTvBSoftware();
		isMainScreen = mainScreen;

		funcNew = e -> {
			try {
				IConfigSoft configSoft = getConfigSoft(this, model, true);
				configSoft.setVisible(true);
			} catch (Exception ex) {
				General.errorMessage(this, ex, GUIFactory.getTitle(PROFILE_ERROR), null);
			}
		};

		funcEdit = e -> {
			try {
				IConfigSoft config = getConfigSoft(this, model, false);
				config.setVisible(true);
			} catch (Exception ex) {
				General.errorMessage(this, ex, GUIFactory.getTitle(PROFILE_ERROR), null);
			}
		};

		funcHelp = e -> {
			HelpDialog help = new HelpDialog(GUIFactory.getText(MENU_HELP) + " - " + getTitle(), myHelpFile);
			help.setVisible(true);
		};

		funcLanguage = e -> {
			String language = e.getActionCommand();
			if (generalSettings.getLanguage().equals(language)) {
				return;
			}

			generalSettings.setLanguage(language);
			GUIFactory.refresh();
			refreshScreen();
		};

		funcLookAndFeel = e -> {
			String lookAndFeel = e.getActionCommand();
			if (generalSettings.getLookAndFeel().equals(lookAndFeel)) {
				return;
			}

			generalSettings.setLookAndFeel(lookAndFeel);
			refreshScreen();
		};

		funcExit = e -> close();

		pdaSettings = profile;
		dbSettings = profile.getDbSettings();
		model = new ProjectModel(pdaSettings);
	}

	protected void init(String title) {
		setTitle(title);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		myExportFile = ExportFile.getExportFile(pdaSettings.getInitialProject());

		btEdit = General.createToolBarButton(GUIFactory.getToolTip("funcEdit"), "Edit.png", funcEdit);

		btClone = General.createToolBarButton(GUIFactory.getToolTip("funcClone"), "Copy.png", e -> {
			try {
				ConfigClone config = new ConfigClone(pdaSettings, ProgramDialog.this);
				config.setVisible(true);
			} catch (Exception ex) {
				General.errorMessage(this, ex, GUIFactory.getTitle(PROFILE_ERROR), null);
			}
		});

		btRemove = General.createToolBarButton(GUIFactory.getToolTip(FUNC_REMOVE), "Delete.png", e -> {
			String mesg = GUIFactory.getMessage(FUNC_REMOVE, pdaSettings.getProfileID());

			if (!General.showConfirmMessage(this, mesg, GUIFactory.getTitle(FUNC_REMOVE))) {
				return;
			}

			updateProfile(Action.DELETE);
		});

		btView = General.createToolBarButton(GUIFactory.getToolTip("funcViewer"), "Viewer.png", e -> {
			enableForm(false);
			exportProcess.init(ProgramDialog.this, ExportStatus.SHOWVIEWER);
		});

		btExport = GUIFactory.getJButton("funcExport", e -> {
			try {
				if (myExportFile.isSpecialFieldSort() && !pdaSettings.isSortFieldDefined()) {
					throw FNProgException.getException("noSortfieldDefined", myExportFile.getName());
				}
				enableForm(false);
				exportProcess.init(ProgramDialog.this, ExportStatus.EXPORT);
			} catch (Exception ex) {
				General.errorMessage(ProgramDialog.this, ex, GUIFactory.getTitle(CONFIG_ERROR), null);
			}
		});

		bIncremental = GUIFactory.getJCheckBox("funcIncremental", generalSettings.isIncrementalExport(), e -> {
			generalSettings.setIncrementalExport(bIncremental.isSelected());
			if (bIncremental.isSelected() && bNewRecords.isSelected()) {
				bNewRecords.setSelected(false);
				return;
			}

			pdaSettings.setLastSaved();
		});

		bNewRecords = GUIFactory.getJCheckBox("funcNewRecords", generalSettings.isNewExport(), e -> {
			generalSettings.setNewExport(bNewRecords.isSelected());
			if (bIncremental.isSelected() && bNewRecords.isSelected()) {
				bIncremental.setSelected(false);
				return;
			}

			pdaSettings.setLastSaved();
		});

		bNoFilter = GUIFactory.getJCheckBox("funcNoFilter", generalSettings.isNoFilterExport(), e -> {
			generalSettings.setNoFilterExport(bNoFilter.isSelected());
			pdaSettings.setLastSaved();
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

	}

	protected void buildDialog() {
		centerScreen = createCenterPanel();
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		getContentPane().add(centerScreen, BorderLayout.CENTER);
		getContentPane().add(createStatusbarPanel(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(generalSettings.getWidth(), generalSettings.getHeight()));
	}

	private void replaceCenterScreen(Component comp) {
		getContentPane().remove(centerScreen);
		getContentPane().add(comp, BorderLayout.CENTER);
		getContentPane().validate();
		centerScreen = comp;
	}

	private boolean isDBConvert() {
		return software == TvBSoftware.DBCONVERT;
	}

	@Override
	public void setVisible(boolean b) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int divX = isMainScreen ? 8 : 5;
		int divY = isMainScreen ? 8 : 5;
		setLocation((dim.width - getSize().width) / divX, (dim.height - getSize().height) / divY);
		super.setVisible(b);
	}

	public boolean isMainScreen() {
		return isMainScreen;
	}

	protected JComponent createToolBar() {
		JButton btNew = General.createToolBarButton(GUIFactory.getToolTip("funcNew"), "New.png", funcNew);
		Box result = Box.createHorizontalBox();
		result.add(Box.createHorizontalStrut(5));
		result.add(btNew);
		result.add(btEdit);
		result.add(btClone);
		result.add(btRemove);
		result.add(Box.createHorizontalStrut(160));
		result.add(btView);
		result.add(Box.createHorizontalGlue());
		result.add(General.createToolBarButton(GUIFactory.getToolTip(MENU_HELP), "Help.png", funcHelp));
		result.add(General.createToolBarButton(GUIFactory.getToolTip(isMainScreen ? "menuExitPgm" : "menuExitScr"),
				"Exit.png", funcExit));
		result.setBorder(BorderFactory.createRaisedBevelBorder());

		return result;
	}

	protected Component createCenterPanel() {
		List<String> proj = pdaSettings.getProjects();
		if (proj.isEmpty()) {
			isInit = false;
			return createWelcomeScreen();
		}

		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

		tabPane = new JTabbedPane();
		for (String project : proj) {
			tabPane.addTab(project, getTabComponent(project));
		}

		tabPane.addChangeListener(e -> {
			int index = tabPane.getSelectedIndex();
			if (index == -1) {
				replaceCenterScreen(createWelcomeScreen());
				isProfileSet = false;
				activateComponents();
			} else {
				if (!isInit) {
					updateProfile(Action.TABCHANGE);
				}
			}
		});

		result.add(tabPane);

		JXPanel panel = new JXPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackgroundPainter(General.getPainter());
		panel.add(btExport);

		if (!isDBConvert()) {
			panel.add(bNewRecords);
			panel.add(bIncremental);
		}

		panel.add(bNoFilter);
		panel.add(Box.createHorizontalGlue());
		result.add(panel);

		if (isInit) {
			isInit = false;
			String initProject = pdaSettings.getInitialProject();
			String initProfile = pdaSettings.getInitialProfile();

			if (!initProfile.isEmpty() && !initProject.isEmpty()
					&& pdaSettings.profileExists(initProject, initProfile)) {
				pdaSettings.setProject(initProject);
				pdaSettings.setProfile(initProfile);
				updateProfile(Action.EDIT);
			}
		}

		return result;
	}

	public void clickEdit() {
		btEdit.doClick();
	}

	private Component createWelcomeScreen() {
		isProfileSet = false;

		JXTitledPanel result = new JXTitledPanel(GUIFactory.getText(MENU_WELCOME));
		JEditorPane helpInfo = new JEditorPane();
		helpInfo.setContentType("text/html");
		helpInfo.setText(GUIFactory.getText("welcome"));
		helpInfo.setEditable(false);
		result.add(helpInfo);
		return result;
	}

	private void checkVersion(boolean isSilent) throws FNProgException {
		Client client = ClientBuilder.newClient();
		WebTarget webTarget = client.target(software.getReleaseInfo());
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();

		if (response.getStatus() == 200) {
			// Extract version from response
			String result = response.readEntity(String.class);
			String version = result.substring(result.indexOf("filename") + 13,
					result.indexOf("/" + software.getName()));

			// Check if there is a later version
			if (version.compareTo(software.getVersion()) > 0) {
				if (General.showConfirmMessage(this, GUIFactory.getMessage("newVersion", version, software.getName()),
						GUIFactory.getText(CHECK_VERSION))) {
					General.gotoWebsite(software.getSupport());
				}
			} else if (!isSilent) {
				General.showMessage(this, GUIFactory.getText("checkVersionOK"), GUIFactory.getText(CHECK_VERSION),
						false);
			}
		} else {
			throw FNProgException.getException("websiteError", software.getReleaseInfo(),
					"Status: " + response.getStatus() + ", Status Info: " + response.getStatusInfo().getReasonPhrase());
		}

		generalSettings.setCheckVersionDate();
	}

	private boolean isProjectOnTabPane(String projectID) {
		if (tabPane == null) {
			return false;
		}

		for (int i = 0; i < tabPane.getComponentCount(); i++) {
			if (tabPane.getTitleAt(i).equals(projectID)) {
				tabPane.setSelectedIndex(i);
				return true;
			}
		}
		return false;
	}

	public void updateProfile(Action action) {
		String projectID = pdaSettings.getProjectID();
		String profileID = pdaSettings.getProfileID();

		switch (action) {
		case ADD:
		case CLONE:
			model.addRecord(projectID, profileID);
			break;
		case TABCHANGE:
			projectID = tabPane.getTitleAt(tabPane.getSelectedIndex());
			break;
		default:
			break;
		}

		if (!isProjectOnTabPane(projectID)) {
			// Project doesn't have it's own tab
			replaceCenterScreen(createCenterPanel());
			if (!isProjectOnTabPane(projectID)) {
				return;
			}
		}

		try {
			JTable table = projects.get(projectID);
			int row = table.getSelectedRow();
			int modelIndex = -1;

			switch (action) {
			case DELETE:
				pdaSettings.deleteNode(pdaSettings.getProfileID());
				modelIndex = table.convertRowIndexToModel(row);
				model.removeRecord(modelIndex);

				if (row == table.getRowCount()) {
					row--;
				}

				if (row > -1) {
					modelIndex = table.convertRowIndexToModel(row);
				}

				if (pdaSettings.getProfiles(projectID).isEmpty()) {
					tabPane.remove(tabPane.getSelectedIndex());
					projects.remove(projectID);
					pdaSettings.removeProject(projectID);
					return;
				}
				break;
			case CLONE:
			case ADD:
			case EDIT:
				modelIndex = model.getProfileIndex(projectID, profileID);
				break;
			default:
				break;
			}

			isProfileSet = true;
			if (action != Action.TABCHANGE) {
				row = modelIndex > -1 ? table.convertRowIndexToView(modelIndex) : -1;
			}

			if (row == -1) {
				row = 0;
			}

			// Tricker a List Selection Event in MainTableSelectionListener
			table.removeRowSelectionInterval(row, row);
			table.addRowSelectionInterval(row, row);
			activateComponents();
		} catch (Exception ex) {
			General.errorMessage(ProgramDialog.this, ex, GUIFactory.getTitle(PROFILE_ERROR), null);
		}
	}

	private Component getTabComponent(String project) {
		TableRowSorter<TableModel> sortModel = new TableRowSorter<>(model);
		sortModel.setRowFilter(RowFilter.regexFilter(project, ProjectModel.HEADER_PROJECT));

		JTable table = new ETable(model);
		model.setParent(tabPane);
		projects.put(project, table);

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					btEdit.doClick();
				}
			}
		});

		MainTableSelectionListener listener = new MainTableSelectionListener(table, this);

		table.setRowSorter(sortModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getSelectionModel().addListSelectionListener(listener);

		TableColumn editProfile = table.getColumnModel().getColumn(ProjectModel.HEADER_EDIT);
		ButtonTableCellEditor btEditor = new ButtonTableCellEditor(listener);
		editProfile.setCellEditor(btEditor);
		editProfile.setCellRenderer(new ButtonRenderer());

		TableColumn importFile = table.getColumnModel().getColumn(ProjectModel.HEADER_IMPORTFILE);
		importFile.setCellRenderer(new FilenameRenderer());

		TableColumn exportFile = table.getColumnModel().getColumn(ProjectModel.HEADER_EXPORTFILE);
		exportFile.setCellEditor(new ExportToTableCellEditor());
		exportFile.setCellRenderer(new FilenameRenderer());

		General.packColumns(table);

		DateTimeTableEditor edit = new DateTimeTableEditor();
		edit.getDatePickerSettings().setFormatForDatesCommonEra(General.getDateFormat());
		edit.getTimePickerSettings().setFormatForDisplayTime(General.sdInternalTime);
		edit.getTimePickerSettings().setFormatForMenuTimes(General.sdInternalTime);

		edit.addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingStopped(ChangeEvent arg0) {
				activateComponents();
			}

			@Override
			public void editingCanceled(ChangeEvent arg0) {
				// Nothing to do
			}
		});

		TableColumn lastExport = table.getColumnModel().getColumn(ProjectModel.HEADER_LASTEXPORT);
		lastExport.setCellEditor(edit);
		lastExport.setCellRenderer(new DateTimeRenderer());
		lastExport.setPreferredWidth(210);

		TableColumn notes = table.getColumnModel().getColumn(ProjectModel.HEADER_NOTES);
		notes.setCellEditor(new StringActionTableCellEditor());
		notes.setPreferredWidth(250);

		TableColumn header = table.getColumnModel().getColumn(ProjectModel.HEADER_PROJECT);
		table.removeColumn(header);

		return new JScrollPane(table);
	}

	protected void backupRestore(boolean isBackup) {
		ConfigBackup conf = new ConfigBackup(isBackup);
		conf.setVisible(true);
		if (conf.isRestored()) {
			refreshScreen();
		}
	}

	public JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = GUIFactory.getJMenu("menuFile");
		menuBar.add(menu);

		menu.add(GUIFactory.getJMenuItem("menuNew", funcNew));
		menu.addSeparator();

		menu.add(GUIFactory.getJMenuItem("funcBackup", e -> backupRestore(true)));
		menu.add(GUIFactory.getJMenuItem("funcRestore", e -> backupRestore(false)));

		if (!General.IS_MAC_OSX) {
			menu.addSeparator();
			menu.add(GUIFactory.getJMenuItem("menuExit", funcExit));
		}

		// Build the Tools menu
		menuBar.add(createToolsMenu());

		// Build the Sites menu
		menuBar.add(createSitesMenu());

		// Build the Help menu
		menu = GUIFactory.getJMenu(MENU_HELP);
		menu.setMnemonic(KeyEvent.VK_H);

		menu.add(GUIFactory.getJMenuItem(MENU_WELCOME, e -> {
			HelpDialog help = new HelpDialog(GUIFactory.getText(MENU_WELCOME),
					isDBConvert() ? "welcome_db" : "welcome_fn");
			help.setVisible(true);
		}));

		menu.add(GUIFactory.getJMenuItem("menuHelpContents", funcHelp));
		menu.addSeparator();

		menu.add(GUIFactory.getJMenuItem("menuAbout", e -> {
			AboutBox about = new AboutBox(pdaSettings.getTvBSoftware(), ProgramDialog.this);
			about.setVisible(true);
		}));

		menu.add(GUIFactory.getJMenuItem("menuAcknow", e -> {
			HelpDialog help = new HelpDialog(GUIFactory.getText("menuAcknow"), "acknowledgements");
			help.setVisible(true);
		}));
		menu.addSeparator();

		menu.add(GUIFactory.getJMenuItem(CHECK_VERSION, e -> {
			ProgramDialog.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				checkVersion(false);
			} catch (Exception ex) {
				General.errorMessage(ProgramDialog.this, ex, GUIFactory.getTitle(CONFIG_ERROR), null);
			}
			ProgramDialog.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}));
		menuBar.add(menu);
		return menuBar;
	}

	private JMenu createLanguageMenu() {
		JMenu result = GUIFactory.getJMenu("menuLanguage");
		boolean[] selected;
		JRadioButtonMenuItem rbMenuItem;
		ButtonGroup languages = new ButtonGroup();

		try {
			List<String> guiLanguage = Arrays.asList("Deutsch", "English", "Nederlands");

			int index = guiLanguage.indexOf(generalSettings.getLanguage());
			if (index == -1) {
				index = guiLanguage.indexOf("English");
				if (index == -1) {
					index = 0;
				}
			}

			selected = new boolean[3];
			selected[index] = true;

			for (int i = 0; i < 3; i++) {
				rbMenuItem = new JRadioButtonMenuItem(guiLanguage.get(i), selected[i]);
				rbMenuItem.setActionCommand(guiLanguage.get(i));
				rbMenuItem.addActionListener(funcLanguage);
				result.add(rbMenuItem);
				languages.add(rbMenuItem);
			}
		} catch (Exception e) {
			General.errorMessage(this, e, "Configuration Error", null);
			result.setEnabled(false);
		}
		return result;
	}

	private JMenu createLookAndFeelMenu() {
		JMenu result = GUIFactory.getJMenu("menuLookAndFeel");
		JRadioButtonMenuItem rbMenuItem;
		ButtonGroup skins = new ButtonGroup();
		Map<String, String> lfMap = General.getLookAndFeels();
		String lfName = generalSettings.getLookAndFeel();

		for (String lf : lfMap.keySet()) {
			rbMenuItem = new JRadioButtonMenuItem(lf);
			rbMenuItem.setActionCommand(lf);
			rbMenuItem.setSelected(lf.equals(lfName));
			rbMenuItem.addActionListener(funcLookAndFeel);
			result.add(rbMenuItem);
			skins.add(rbMenuItem);
		}
		return result;
	}

	protected JMenu createToolsMenu() {
		JMenu result = GUIFactory.getJMenu("menuTools");

		if (isMainScreen) {
			result.add(createLanguageMenu());
			result.add(createLookAndFeelMenu());
		}

		result.add(GUIFactory.getJMenuItem("menuConfigGeneral", e -> {
			ConfigGeneral view = new ConfigGeneral();
			view.setVisible(true);
			activateComponents();
		}));

		return result;
	}

	private JMenu createSitesMenu() {
		InternetSitesMenu handler = new InternetSitesMenu(e -> {
			try {
				General.gotoWebsite(e.getActionCommand());
			} catch (Exception ex) {
				General.errorMessage(this, ex, GUIFactory.getTitle("connectionError"),
						GUIFactory.getMessage("connectionError", "http://" + e.getActionCommand()));
			}
		}, software);
		return handler.getInternetSitesMenu();
	}

	private Component createStatusbarPanel() {
		Box result = Box.createHorizontalBox();
		progressText = new JLabel();
		progressBar = new JProgressBar(0, 100);
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, progressBar, progressText);

		pane.setDividerLocation(700);
		pane.setToolTipText(GUIFactory.getToolTip("statusbar"));
		pane.setBorder(BorderFactory.createEtchedBorder());

		result.add(pane);
		return result;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		int newValue = (int) evt.getNewValue();
		int oldValue = (int) evt.getOldValue();

		if (oldValue != progressBar.getMaximum()) {
			// filter has been applied, need to lower progress bar max value
			progressBar.setMaximum(oldValue);
		}

		progressBar.setValue(newValue);
		progressText.setText(" " + Integer.toString(newValue) + " Records");
	}

	public void enableForm(boolean enable) {
		General.setEnabled(ProgramDialog.this, enable);

		if (enable) {
			progressBar.setValue(0);
			progressBar.setStringPainted(false);
			progressText.setText("");
			requestFocus(true);
		} else {
			ProgramDialog.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
	}

	protected void close() {
		// Check if automated version check has been enabled
		if (isMainScreen() && !generalSettings.isNoVersionCheck()
				&& generalSettings.getCheckVersionDate().isBefore(LocalDate.now())) {
			try {
				// Make a "silent" check
				checkVersion(true);
				generalSettings.setCheckVersionDate();
			} catch (FNProgException ex) {
				General.errorMessage(ProgramDialog.this, ex, GUIFactory.getTitle(CONFIG_ERROR), null);
			}
		}

		generalSettings.setWidth(getWidth());
		generalSettings.setHeight(getHeight());
		dbSettings.cleanupNodes(pdaSettings);
		pdaSettings.setLastProject();
		pdaSettings.setLastProfile(pdaSettings.getProfileID());

		if (isMainScreen) {
			System.exit(0);
		}

		setVisible(false);
		dispose();
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public JLabel getProgressText() {
		return progressText;
	}

	public void activateComponents() {
		btExport.setEnabled(isProfileSet);
		btEdit.setEnabled(isProfileSet);
		btClone.setEnabled(isProfileSet);
		btRemove.setEnabled(isProfileSet);
		btView.setEnabled(isProfileSet);
		bNewRecords.setEnabled(isProfileSet && pdaSettings.getLastIndex() > 0);
		bIncremental.setEnabled(
				isProfileSet && (!pdaSettings.getLastModified().isEmpty() || pdaSettings.getLastIndex() > 0));
		bNoFilter.setEnabled(isProfileSet && !pdaSettings.isNoFilters());
		myExportFile = ExportFile.getExportFile(pdaSettings.getProjectID());
	}

	public boolean isModelValid() {
		long checksum = generalSettings.getGeneralCRC32Checksum();
		boolean isTrue = oldGeneralCRC == checksum && oldLastModified.equals(pdaSettings.getLastSaved());
		oldGeneralCRC = checksum;
		oldLastModified = pdaSettings.getLastSaved();
		return isTrue;
	}

	public void setHelpFile(String helpFile) {
		myHelpFile = helpFile;
	}

	protected abstract IConfigSoft getConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNewProfile);

	protected abstract void refreshScreen();
}
