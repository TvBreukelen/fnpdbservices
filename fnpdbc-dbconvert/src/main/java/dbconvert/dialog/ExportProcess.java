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
import dbengine.export.ICalendar;
import dbengine.export.JFile;
import dbengine.export.ListDB;
import dbengine.export.MobileDB;
import dbengine.export.Paradox;
import dbengine.export.PilotDB;
import dbengine.export.VCard;

public class ExportProcess extends IExportProcess implements Runnable {
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
		isRefresh = !myProgram.isModelValid() || xView == null;

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
				mySoftware.addObserver(myProgram);
				mySoftware.connect2DB();
				mySoftware.setupDBTranslation(true);
				mySoftware.checkNumberOfFields();
				mySoftware.loadInputFile();
				myProgram.getProgressBar().setMaximum(mySoftware.getTotalRecords());
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
			mySoftware.convertFromTableModel(xView.getTableModel());
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
		mySoftware.processFiles(xView.getTableModel());
		xView.getTableModel().resetColumnVisibility();
		xView.buildViewer();
	}

	@Override
	public GeneralDB getDatabase(ExportFile db, Profiles profile) {
		switch (db) {
		case ICAL:
			return new ICalendar(profile);
		case PARADOX:
			return new Paradox(profile);
		case VCARD:
			return new VCard(profile);
		case JFILE:
			return new JFile(profile);
		case LIST:
			return new ListDB(profile);
		case MOBILEDB:
			return new MobileDB(profile);
		case PILOTDB:
			return new PilotDB(profile);
		default:
			return super.getDatabase(db, profile);
		}
	}
}
