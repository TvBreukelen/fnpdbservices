package dbconvert.dialog;

import javax.swing.JFrame;
import javax.swing.JMenu;

import application.dialog.ProgramDialog;
import application.interfaces.IConfigSoft;
import application.interfaces.TvBSoftware;
import application.model.ProjectModel;
import application.utils.General;
import dbconvert.DBConvert;
import dbconvert.preferences.PrefDBConvert;
import dbconvert.software.XConverter;

public class ConfigDBConvert extends ProgramDialog {
	private static final long serialVersionUID = 4550349967205529656L;

	private JFrame externFrame;

	public ConfigDBConvert(JFrame frame) {
		super(PrefDBConvert.getInstance(), false);
		externFrame = frame;
		General.setEnabled(frame, false);
		init();
	}

	public ConfigDBConvert(boolean isMainScreen) {
		super(PrefDBConvert.getInstance(), isMainScreen);
		init();
	}

	@Override
	protected void init() {
		super.init();
		dbFactory = new XConverter(this);
		init(TvBSoftware.DBCONVERT.getName() + " " + TvBSoftware.DBCONVERT.getVersion());
		setHelpFile("dbconvert");
		exportProcess = new ExportProcess();

		setJMenuBar(createMenuBar());
		buildDialog();
		activateComponents();
		pack();
	}

	@Override
	protected JMenu createToolsMenu() {
		return null;
	}

	@Override
	protected void close() {
		super.close();
		General.setEnabled(externFrame, true);
	}

	@Override
	public void refreshScreen() {
		setVisible(false);
		dispose();
		new DBConvert().start();
	}

	@Override
	protected IConfigSoft getConfigSoft(ProgramDialog dialog, ProjectModel model, boolean isNewProfile) {
		return new ConfigSoft(dialog, model, isNewProfile);
	}
}
