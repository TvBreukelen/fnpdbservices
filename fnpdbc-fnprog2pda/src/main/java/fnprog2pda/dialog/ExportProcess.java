package fnprog2pda.dialog;

import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.Viewer;
import application.interfaces.ExportFile;
import application.interfaces.ExportStatus;
import application.interfaces.IExportProcess;
import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.export.CsvFile;
import fnprog2pda.dbengine.MSAccess;
import fnprog2pda.dbengine.export.BookBuddy;
import fnprog2pda.dbengine.export.MovieBuddy;
import fnprog2pda.dbengine.export.MusicBuddy;
import fnprog2pda.software.FNProgramvare;

public class ExportProcess extends IExportProcess implements Runnable {
	Thread t;

	private ExportStatus status;
	private boolean isAborted;
	private ConfigFNProg myProgram;
	private static FNProgramvare mySoftware;

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
				mySoftware.getRoles(); // Obtain roles (when required)
				myProgram.getProgressBar().setMaximum(mySoftware.getTotalRecords());

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
			mySoftware.addObserver(myProgram); // Update the progressbar automatically
			mySoftware.convertFromTableModel(xView.getTableModel());
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

	@Override
	public GeneralDB getDatabase(ExportFile db, Profiles profile) {
		switch (db) {
		case ACCESS:
			return new MSAccess(profile);
		case TEXTFILE:
			String csvFormat = profile.getTextFileFormat();
			if (csvFormat.equals("buddyCsv")) {
				switch (FNProgramvare.whoAmI()) {
				case BOOKCAT:
					return new BookBuddy(profile);
				case CATRAXX:
					return new MusicBuddy(profile);
				case CATVIDS:
					return new MovieBuddy(profile);
				default:
					// Should not occur
					return new CsvFile(profile);
				}
			}
			return new CsvFile(profile);
		default:
			return super.getDatabase(db, profile);
		}
	}
}