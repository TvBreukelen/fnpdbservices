package fnprog2pda.dialog;

import javax.swing.JMenu;

import application.dialog.ProgramDialog;
import application.interfaces.FNPSoftware;
import application.interfaces.IConfigSoft;
import application.interfaces.TvBSoftware;
import application.model.ProjectModel;
import application.preferences.Databases;
import application.utils.GUIFactory;
import dbconvert.dialog.ConfigDBConvert;
import fnprog2pda.FNProg2PDA;
import fnprog2pda.preferences.PrefFNProg;
import fnprog2pda.software.DatabaseFactory;

public class ConfigFNProg extends ProgramDialog {
	/**
	 * Title: ConfigFNProg Description: Main FNProg2PDA program Copyright: (c)
	 * 2004-2008
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = -9109798108105347350L;
	protected FNPSoftware myFnSoftware = FNPSoftware.UNDEFINED;

	public ConfigFNProg(boolean isMainScreen) {
		super(PrefFNProg.getInstance(), isMainScreen);
		dbSettings = Databases.getInstance(TvBSoftware.FNPROG2PDA);
		dbFactory = DatabaseFactory.getInstance();
		exportProcess = new ExportProcess();
		init();
	}

	private void init() {
		init(TvBSoftware.FNPROG2PDA.getName() + " " + TvBSoftware.FNPROG2PDA.getVersion());
		setHelpFile("fnprog2pda");
		setJMenuBar(createMenuBar());
		buildDialog();
		activateComponents();
		pack();
	}

	@Override
	protected JMenu createToolsMenu() {
		JMenu result = super.createToolsMenu();
		result.add(GUIFactory.getJMenuItem("DBConvert", e -> {
			ConfigDBConvert dbConvert = new ConfigDBConvert(false);
			dbConvert.setVisible(true);
		}));
		return result;
	}
	
	@Override
	public void activateComponents() {
		super.activateComponents();

		if (isProfileSet) {
			if (pdaSettings.getTableName().isEmpty()) {
				pdaSettings.setTableName(myFnSoftware.getViews().get(0), false);
			}

			myFnSoftware = FNPSoftware.getSoftware(dbSettings.getDatabaseType());
			lSoftwareID.setText(dbSettings.getDatabaseType() + " " + dbSettings.getDatabaseVersion() + " / "
					+ pdaSettings.getTableName());
		} else {
			lSoftwareID.setText("");
		}
	}

	@Override
	protected IConfigSoft getConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNewProfile) {
		return new ConfigSoft(dialog, model, isNewProfile);
	}

	@Override
	public void updateProfile(Action action) {
		super.updateProfile(action);
		myFnSoftware = FNPSoftware.getSoftware(dbSettings.getDatabaseType());
	}

	@Override
	public void refreshScreen() {
		setVisible(false);
		dispose();
		new FNProg2PDA().start();
	}


	@Override
	protected void close() {
		dbFactory.close();
		super.close();
	}
}
