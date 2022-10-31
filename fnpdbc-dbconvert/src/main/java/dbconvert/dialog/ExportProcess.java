package dbconvert.dialog;

import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.Viewer;
import application.interfaces.ExportFile;
import application.interfaces.ExportStatus;
import application.interfaces.IExportProcess;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import dbconvert.software.XConverter;
import dbengine.GeneralDB;
import dbengine.export.Calc;
import dbengine.export.CsvFile;
import dbengine.export.DBaseFile;
import dbengine.export.Excel;
import dbengine.export.Firebird;
import dbengine.export.HanDBase;
import dbengine.export.JFile;
import dbengine.export.JsonFile;
import dbengine.export.ListDB;
import dbengine.export.MSAccess;
import dbengine.export.MariaDB;
import dbengine.export.MobileDB;
import dbengine.export.PilotDB;
import dbengine.export.PostgreSQL;
import dbengine.export.SQLServer;
import dbengine.export.SQLite;
import dbengine.export.VCard;
import dbengine.export.XmlFile;
import dbengine.export.YamlFile;

public class ExportProcess implements Runnable, IExportProcess {
	Thread t;

	private ExportStatus status;
	private ProgramDialog myProgram;
	private XConverter mySoftware;
	private boolean isAborted;
	private boolean isRefresh;
	private Viewer xView = null;

	@Override
	public void run() {
		loadTableModel();
		exportFromTableModel();
		finished();
	}

	@Override
	public void init(ProgramDialog pProgram, ExportStatus status) {
		myProgram = pProgram;

		this.status = status;
		isAborted = false;
		isRefresh = !myProgram.isModelValid();

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
		if (isRefresh) {
			try {
				mySoftware = new XConverter();
				mySoftware.connect2DB();
				mySoftware.setupDBTranslation(false);
				mySoftware.checkNumberOfFields();
				mySoftware.loadInputFile();
				mySoftware.sortTableModel();
				buildViewer();
				mySoftware.close(); // Close input file
			} catch (Exception e) {
				isAborted = true;
				mySoftware.close();
				General.errorMessage(myProgram, e, GUIFactory.getTitle("databaseReadError"), null);
			}
		}
	}

	private void exportFromTableModel() {
		if (status != ExportStatus.EXPORT || isAborted) {
			return;
		}

		try {
			// Start Export
			mySoftware.addObserver(myProgram);
			mySoftware.openToFile();
			mySoftware.convertFromTableModel(xView.getTableModel(), mySoftware.getDbOut());
			mySoftware.close(); // Close export file
			mySoftware.runConversionProgram(myProgram);
		} catch (Exception ex) {
			isAborted = true;
			General.errorMessage(myProgram, ex, GUIFactory.getTitle("exportError"), null);
			mySoftware.closeFiles(true);
		}
	}

	private void finished() {
		if (!isAborted && status == ExportStatus.SHOWVIEWER) {
			try {
				xView.setVisible(true);
			} catch (Exception e) {
				General.errorMessage(myProgram.getParent(), e, GUIFactory.getTitle("viewerError"), null);
			}
		}

		myProgram.enableForm(true);
		myProgram.updateProfile(Action.EDIT);
	}

	private void buildViewer() throws Exception {
		mySoftware.checkNumberOfFields();
		xView = new Viewer(mySoftware.getTableModelFields());
		myProgram.getProgressBar().setMaximum(mySoftware.getTotalRecords());
		mySoftware.addObserver(myProgram);
		mySoftware.processFiles(xView.getTableModel());
		xView.getTableModel().resetColumnVisibility();
		xView.buildViewer();
	}

	public static GeneralDB getDatabase(ExportFile db, Profiles profile) {
		switch (db) {
		case ACCESS:
			return new MSAccess(profile);
		case CALC:
			return new Calc(profile);
		case JSON:
			return new JsonFile(profile);
		case FIREBIRD:
			return new Firebird(profile);
		case MARIADB:
			return new MariaDB(profile);
		case POSTGRESQL:
			return new PostgreSQL(profile);
		case SQLSERVER:
			return new SQLServer(profile);
		case YAML:
			return new YamlFile(profile);
		case HANDBASE:
			return new HanDBase(profile);
		case JFILE:
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
		case VCARD:
			return new VCard(profile);
		case DBASE:
		case DBASE3:
		case DBASE4:
		case DBASE5:
		case FOXPRO:
			return new DBaseFile(profile);
		case XML:
			return new XmlFile(profile);
		case SQLITE:
			return new SQLite(profile);
		}
		return null;
	}
}
