package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.border.BevelBorder;

import application.interfaces.TvBSoftware;
import application.preferences.GeneralSettings;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.InternetSitesMenu;

public abstract class BasicFrame extends JFrame {
	protected JButton btExit;
	protected JButton btHelp;
	protected JButton btSave;

	protected ActionListener funcHelp;
	protected ActionListener funcExit;
	protected ActionListener funcSave;

	protected GeneralSettings generalSettings = GeneralSettings.getInstance();

	protected ActionListener funcConfigGeneral;
	protected ActionListener funcConfigCharset;
	protected ActionListener funcLanguage;
	protected ActionListener funcLookAndFeel;
	private ActionListener funcSites;

	private String myHelpFile;
	private boolean isMainScreen = false;
	protected TvBSoftware software;
	private Component centerScreen;

	private static final long serialVersionUID = -4427269633857070104L;

	public BasicFrame(TvBSoftware software, boolean mainScreen) {
		// Only relevant for MacOS
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.fileDialogForDirectories", "false");

		this.software = software;
		isMainScreen = mainScreen;
	}

	protected void init(String title) {
		setTitle(title);

		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}
		});

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

		funcConfigGeneral = e -> {
			ConfigGeneral general = new ConfigGeneral();
			general.setVisible(true);
			activateComponents();
		};

		funcConfigCharset = e -> {
			ConfigCharset general = new ConfigCharset();
			general.setVisible(true);
		};

		funcExit = e -> close();

		funcSites = e -> {
			try {
				General.gotoWebsite(e.getActionCommand());
			} catch (Exception ex) {
				General.errorMessage(BasicFrame.this, ex, GUIFactory.getTitle("connectionError"),
						GUIFactory.getMessage("connectionError", "http://" + e.getActionCommand()));
			}
		};

		funcHelp = e -> showHelp();

		funcSave = e -> {
			try {
				save();
				close();
			} catch (FNProgException ex) {
				General.errorMessage(BasicFrame.this, ex, GUIFactory.getTitle("configError"), null);
				close();
			} catch (Exception ex) {
				General.errorMessage(BasicFrame.this, ex, GUIFactory.getTitle("configError"), null);
			}
		};

		btSave = General.createToolBarButton(GUIFactory.getToolTip("menuSave"), "Save.png", funcSave);
		btHelp = General.createToolBarButton(GUIFactory.getToolTip("menuHelp"), "Help.png", funcHelp);
		btExit = General.createToolBarButton(GUIFactory.getToolTip(isMainScreen ? "menuExitPgm" : "menuExitScr"),
				"Exit.png", funcExit);
	}

	protected Component createLeftPanel() {
		return Box.createHorizontalStrut(5);
	}

	protected void buildDialog() {
		JComponent toolbar = createToolBar();
		centerScreen = createCenterPanel();
		toolbar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		getContentPane().add(toolbar, BorderLayout.NORTH);
		getContentPane().add(createLeftPanel(), BorderLayout.WEST);
		getContentPane().add(centerScreen, BorderLayout.CENTER);
	}

	protected void replaceCenterScreen(Component comp) {
		getContentPane().remove(centerScreen);
		getContentPane().add(comp, BorderLayout.CENTER);
		getContentPane().validate();
		centerScreen = comp;
	}

	protected void close() {
		if (isMainScreen) {
			System.exit(0);
		}
		setVisible(false);
		dispose();
	}

	protected void showHelp() {
		HelpDialog help = new HelpDialog(GUIFactory.getText("menuHelp") + " - " + getTitle(), myHelpFile);
		help.setVisible(true);
	}

	public void setHelpFile(String helpFile) {
		myHelpFile = helpFile;
	}

	protected boolean isDBConvert() {
		return software == TvBSoftware.DBCONVERT;
	}

	protected JMenu createConfigMenu() {
		JMenu result = GUIFactory.getJMenu("menuConfig");
		result.setMnemonic(KeyEvent.VK_C);

		// Add General JMenuItem
		result.add(GUIFactory.getJMenuItem("menuConfigGeneral", funcConfigGeneral));
		result.add(GUIFactory.getJMenuItem("menuConfigCharset", funcConfigCharset));

		if (!isMainScreen) {
			return result;
		}

		boolean[] selected;
		JRadioButtonMenuItem rbMenuItem;
		JMenu menu = GUIFactory.getJMenu("menuLanguage");
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
				menu.add(rbMenuItem);
				languages.add(rbMenuItem);
			}
		} catch (Exception e) {
			General.errorMessage(this, e, "Configuration Error", null);
			menu.setEnabled(false);
		}
		result.add(menu);

		menu = GUIFactory.getJMenu("menuLookAndFeel");
		ButtonGroup skins = new ButtonGroup();
		Map<String, String> lfMap = General.getLookAndFeels();
		String lfName = generalSettings.getLookAndFeel();

		for (String lf : lfMap.keySet()) {
			rbMenuItem = new JRadioButtonMenuItem(lf);
			rbMenuItem.setActionCommand(lf);
			rbMenuItem.setSelected(lf.equals(lfName));
			rbMenuItem.addActionListener(funcLookAndFeel);
			menu.add(rbMenuItem);
			skins.add(rbMenuItem);
		}
		result.add(menu);
		return result;
	}

	protected JMenu createSitesMenu() {
		InternetSitesMenu handler = new InternetSitesMenu(funcSites, software);
		return handler.getInternetSitesMenu();
	}

	@Override
	public void setVisible(boolean b) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int divX = isMainScreen ? 8 : 5;
		int divY = isMainScreen ? 8 : 5;
		setLocation((dim.width - getSize().width) / divX, (dim.height - getSize().height) / divY);
		super.setVisible(b);
	}

	protected JComponent createToolBar() {
		Box result = Box.createHorizontalBox();
		result.add(Box.createHorizontalGlue());
		result.add(btHelp);
		result.add(btExit);
		return result;
	}

	public boolean isMainScreen() {
		return isMainScreen;
	}

	public void activateComponents() {
		// Nothing to do here on this level
	}

	protected void save() throws Exception {
		// Nothing to do here on this level
	}

	protected abstract Component createCenterPanel();

	protected abstract void init();

	protected abstract void refreshScreen();
}
