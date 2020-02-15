package dbconvert.dialog;

import application.dialog.ProgramDialog;
import application.dialog.ProgramDialog.Action;
import application.dialog.Viewer;
import application.interfaces.ExportStatus;
import application.interfaces.IExportProcess;
import application.utils.GUIFactory;
import application.utils.General;
import dbconvert.software.XConverter;

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
		if (isRefresh) {
			try {
				mySoftware = new XConverter(myProgram);
				mySoftware.connect2DB();
				mySoftware.setupDBTranslation(false);
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
		if (status != ExportStatus.Export || isAborted) {
			return;
		}

		try {
			// Start Export
			mySoftware.startMonitoring(myProgram);
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
		if (!isAborted && status == ExportStatus.ShowViewer) {
			try {
				xView.setVisible(true);
			} catch (Exception e) {
				General.errorMessage(myProgram.getParent(), e, GUIFactory.getTitle("viewerError"), null);
			}
		}

		myProgram.enableForm(true);
		myProgram.updateProfile(Action.Edit);
	}

	private void buildViewer() throws Exception {
		mySoftware.checkNumberOfFields();
		xView = new Viewer(mySoftware.getTableModelFields());
		myProgram.progressBar.setMaximum(mySoftware.getTotalRecords());
		mySoftware.startMonitoring(myProgram);
		mySoftware.processFiles(xView.getTableModel());
		xView.getTableModel().resetColumnVisibility();
		xView.buildViewer();
	}
}
