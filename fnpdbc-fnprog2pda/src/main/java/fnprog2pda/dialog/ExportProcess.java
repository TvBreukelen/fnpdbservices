package fnprog2pda.dialog;

import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.Viewer;
import application.interfaces.ExportFile;
import application.interfaces.ExportStatus;
import application.interfaces.IExportProcess;
import application.utils.GUIFactory;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.export.Calc;
import dbengine.export.CsvFile;
import dbengine.export.DBaseFile;
import dbengine.export.Excel;
import dbengine.export.HanDBase;
import dbengine.export.JFile;
import dbengine.export.JsonFile;
import dbengine.export.ListDB;
import dbengine.export.MSAccess;
import dbengine.export.MobileDB;
import dbengine.export.PilotDB;
import dbengine.export.XmlFile;
import dbengine.export.YamlFile;
import fnprog2pda.preferences.PrefFNProg;
import fnprog2pda.software.FNProgramvare;

public class ExportProcess implements Runnable, IExportProcess {
	Thread t;

	private ExportStatus status;
	private boolean isAborted;
	private ConfigFNProg myProgram;
	private FNProgramvare mySoftware;

	private Viewer xView = null;
	private boolean isRefresh;
	private static boolean isNotLoaded = true;

	@Override
	public void run() {
		loadTableModel();
		exportFromTableModel();
		finished();
	}

	@Override
	public void init(ProgramDialog pProgram, ExportStatus status) {
		myProgram = (ConfigFNProg) pProgram;

		this.status = status;
		isAborted = false;
		isRefresh = !myProgram.isModelValid() || isNotLoaded;

		if (isRefresh) {
			myProgram.getProgressBar().setMinimum(0);
			myProgram.getProgressBar().setValue(0);
			myProgram.getProgressBar().setStringPainted(true);
			myProgram.getProgressText().setEnabled(true);
			myProgram.getProgressBar().setEnabled(true);
		}

		t = new Thread(this);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	private void loadTableModel() {
		if (isRefresh || !mySoftware.isConnected()) {
			isNotLoaded = true;
			try {
				mySoftware = FNProgramvare.getSoftware(myProgram.myFnSoftware);
				mySoftware.openFile(); // Connect to the FNProgramvare Access database
				mySoftware.setupDBTranslation(false);
				xView = new Viewer(mySoftware.getTableModelFields());

				mySoftware.checkNumberOfFields(false, xView.getTableModel()); // Plausibility check
				mySoftware.setCategories(); // Obtain categories (when required)

				mySoftware.addObserver(myProgram);
				mySoftware.setupDbInfoToWrite();
				mySoftware.processFiles(xView.getTableModel());
				xView.getTableModel().resetColumnVisibility();

				xView.buildViewer();
				isNotLoaded = false;
			} catch (Exception e) {
				isAborted = true;
				if (mySoftware != null) {
					mySoftware.close();
				}
				General.errorMessage(myProgram, e, GUIFactory.getTitle("databaseReadError"), null);
			}
		}
	}

	private void exportFromTableModel() {
		if (status != ExportStatus.EXPORT || isAborted) {
			return;
		}

		try {
			mySoftware.checkNumberOfFields(true, xView.getTableModel());
			mySoftware.openToFile();
			myProgram.getProgressBar().setMaximum(mySoftware.getTotalRecords());
			mySoftware.addObserver(myProgram); // Update the progressbar automatically
			mySoftware.convertFromTableModel(xView.getTableModel(), mySoftware.getDbOut());
			mySoftware.close(); // Close output file
			mySoftware.runConversionProgram(myProgram);
		} catch (Exception e) {
			isAborted = true;
			mySoftware.closeFiles(true); // Close output file
			General.errorMessage(myProgram, e, GUIFactory.getTitle("exportError"), null);
		}
	}

	private void finished() {
		if (!isAborted && status == ExportStatus.SHOWVIEWER) {
			try {
				mySoftware.checkNumberOfFields(false, xView.getTableModel());
				xView.setVisible(true);
			} catch (Exception e) {
				General.errorMessage(myProgram, e, GUIFactory.getTitle("viewerError"), null);
			}
		}

		myProgram.enableForm(true);
		myProgram.updateProfile(Action.EDIT);
	}

	public static GeneralDB getDatabase(ExportFile db, PrefFNProg profile) {
		switch (db) {
		case ACCESS:
			return new MSAccess(profile);
		case CALC:
			return new Calc(profile);
		case JSON:
			return new JsonFile(profile);
		case YAML:
			return new YamlFile(profile);
		case HANDBASE:
			return new HanDBase(profile);
		case JFILE3:
		case JFILE4:
		case JFILE5:
			return new JFile(profile);
		case LIST:
			return new ListDB(profile);
		case MOBILEDB:
			return new MobileDB(profile);
		case PILOTDB:
			return new PilotDB(profile);
		case EXCEL:
			return new Excel(profile);
		case TEXTFILE:
			return new CsvFile(profile);
		case DBASE:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
			return new DBaseFile(profile);
		case XML:
			return new XmlFile(profile);
		default:
			return null;
		}
	}
}