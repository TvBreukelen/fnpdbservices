package fnprog2pda.dialog;

import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.Viewer;
import application.interfaces.ExportStatus;
import application.interfaces.IExportProcess;
import application.utils.GUIFactory;
import application.utils.General;
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
			myProgram.progressBar.setMinimum(0);
			myProgram.progressBar.setValue(0);
			myProgram.progressBar.setStringPainted(true);
			myProgram.progressText.setEnabled(true);
			myProgram.progressBar.setEnabled(true);
		}

		t = new Thread(this);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	private void loadTableModel() {
		if (isRefresh || !mySoftware.isConnected()) {
			isNotLoaded = true;
			try {
				mySoftware = FNProgramvare.getSoftware(myProgram.myFnSoftware, myProgram);
				mySoftware.openFile(); // Connect to the FNProgramvare Access database
				mySoftware.setupDBTranslation(false);
				xView = new Viewer(mySoftware.getTableModelFields());

				mySoftware.checkNumberOfFields(false, xView.getTableModel()); // Plausibility check
				mySoftware.setCategories(); // Obtain categories (when required)

				mySoftware.startMonitoring(myProgram);
				mySoftware.processFiles(xView.getTableModel());
				mySoftware.refreshSpecialFields();
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
		if (status != ExportStatus.Export || isAborted) {
			return;
		}

		try {
			mySoftware.checkNumberOfFields(true, xView.getTableModel());
			mySoftware.openToFile();
			myProgram.progressBar.setMaximum(mySoftware.getTotalRecords());
			mySoftware.startMonitoring(myProgram); // Update the progressbar automatically
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
		if (!isAborted && status == ExportStatus.ShowViewer) {
			try {
				mySoftware.checkNumberOfFields(false, xView.getTableModel());
				xView.setVisible(true);
			} catch (Exception e) {
				General.errorMessage(myProgram, e, GUIFactory.getTitle("viewerError"), null);
			}
		}

		myProgram.enableForm(true);
		myProgram.updateProfile(Action.Edit);
	}
}