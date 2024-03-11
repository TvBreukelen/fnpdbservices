package fnprog2pda.dialog;

import application.dialog.ConfigDialog;
import application.dialog.ProgramDialog;
import application.interfaces.TvBSoftware;
import application.model.ProjectModel;
import application.utils.General;
import dbengine.utils.DatabaseHelper;
import fnprog2pda.FNProg2PDA;
import fnprog2pda.preferences.PrefFNProg;
import fnprog2pda.software.DatabaseFactory;
import fnprog2pda.software.FNPSoftware;

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

	public ConfigFNProg() {
		super(PrefFNProg.getInstance());
		dbFactory = DatabaseFactory.getInstance();
		exportProcess = new ExportProcess();
		init();
	}

	private void init() {
		init(TvBSoftware.FNPROG2PDA.getName() + General.SPACE + TvBSoftware.FNPROG2PDA.getVersion());
		setHelpFile("fnprog2pda");
		setJMenuBar(createMenuBar());
		buildDialog();
		activateComponents();
		pack();
	}

	@Override
	public void activateComponents() {
		super.activateComponents();
		int lastIndex = ((PrefFNProg) pdaSettings).getLastIndex();

		bNewRecords.setEnabled(isProfileSet && lastIndex > 0);
		bIncremental.setEnabled(isProfileSet && (!pdaSettings.getLastExported().isEmpty() || lastIndex > 0));

		if (isProfileSet) {
			if (pdaSettings.getTableName().isEmpty()) {
				pdaSettings.setTableName(myFnSoftware.getViews()[0], false);
			}

			DatabaseHelper helper = pdaSettings.getFromDatabase();
			myFnSoftware = FNPSoftware.getSoftware(helper.getDatabaseTypeAsString());
			lSoftwareID.setText(helper.getDatabaseType() + General.SPACE + helper.getDatabaseVersion() + " / "
					+ pdaSettings.getTableName());
		} else {
			lSoftwareID.setText(General.EMPTY_STRING);
		}
	}

	@Override
	protected ConfigDialog getConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNewProfile) {
		return new ConfigSoft(dialog, model, isNewProfile);
	}

	@Override
	public void updateProfile(Action action) {
		super.updateProfile(action);
		DatabaseHelper helper = pdaSettings.getFromDatabase();
		myFnSoftware = FNPSoftware.getSoftware(helper.getDatabase());
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
