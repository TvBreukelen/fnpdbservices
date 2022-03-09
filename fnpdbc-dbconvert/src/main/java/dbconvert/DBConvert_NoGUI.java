package dbconvert;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import application.interfaces.ExportFile;
import application.interfaces.TvBSoftware;
import application.model.ViewerModel;
import application.preferences.Databases;
import application.utils.GUIFactory;
import application.utils.General;
import dbconvert.preferences.PrefDBConvert;
import dbconvert.software.XConverter;

public class DBConvert_NoGUI implements PropertyChangeListener {
	private String myProfileID;
	private ExportFile databaseType;

	private PrefDBConvert pdaSettings = PrefDBConvert.getInstance();
	private Databases dbSettings = Databases.getInstance(TvBSoftware.DBCONVERT);

	private XConverter mySoftware;
	private ViewerModel tabModel;
	private boolean loadModel;

	public DBConvert_NoGUI(String... args) {
		TvBSoftware software = TvBSoftware.DBCONVERT;
		System.out.println(software.getName() + " " + software.getVersion() + " - " + software.getCopyright() + "\n");
		General.setQuietMode();

		try {
			final int maxProfiles = args.length - 1;

			if (maxProfiles < 1) {
				System.out.println(GUIFactory.getMessage("parameterError"));
				return;
			}

			String exportID = args[maxProfiles];
			databaseType = ExportFile.getExportFile(exportID);
			if (!exportID.equalsIgnoreCase(databaseType.getName())) {
				System.out.println(GUIFactory.getMessage("databaseIDError", exportID));
				return;
			}

			pdaSettings.setProject(databaseType.getName());
			for (int i = 0; i < maxProfiles; i++) {
				myProfileID = args[i];
				if (!pdaSettings.profileExists(myProfileID)) {
					System.out.println(GUIFactory.getMessage("profileNotExists", myProfileID));
					continue;
				}

				pdaSettings.setProfile(myProfileID);
				dbSettings.setNode(pdaSettings.getDatabaseFromFile());
				runExport();
			}

		} catch (Exception e) {
			General.errorMessage(null, e, GUIFactory.getTitle("exportError"), null);
			if (mySoftware != null) {
				mySoftware.closeFiles(true);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		int record = (int) evt.getNewValue();
		System.out.println(
				GUIFactory.getMessage(loadModel ? "recordsRead" : "recordsProcessed", Integer.toString(record)));
	}

	private void runExport() throws Exception {
		if (pdaSettings.getExportFile().isEmpty()) {
			pdaSettings.setExportFile(General.getDefaultPDADatabase(databaseType));
		}

		String[] guiText = new String[5];
		guiText[0] = GUIFactory.getText("profile");
		guiText[1] = GUIFactory.getText("exportFrom");
		guiText[2] = GUIFactory.getText("software");
		guiText[3] = GUIFactory.getTitle("exportTo");
		guiText[4] = GUIFactory.getText("database");

		int maxLen = guiText[0].length();
		for (int i = 1; i < 4; i++) {
			maxLen = Math.max(maxLen, guiText[i].length());
		}

		for (int i = 0; i < 5; i++) {
			StringBuilder buf = new StringBuilder(maxLen + 5);
			buf.append("  ");

			int addSpaces = maxLen - guiText[i].length();
			for (int j = 0; j < addSpaces; j++) {
				buf.append(" ");
			}
			buf.append(guiText[i]);
			buf.append(" : ");
			guiText[i] = buf.toString();
		}

		System.out.println("--------------------------------------------------------");
		System.out.println(guiText[0] + myProfileID);
		System.out.println("--------------------------------------------------------");
		System.out.println(guiText[1] + dbSettings.getDatabase());
		System.out.println(guiText[2]
				+ General.getSoftwareTypeVersion(dbSettings.getDatabaseTypeAsString(), dbSettings.getDatabaseVersion())
				+ "\n");
		System.out.println(guiText[3] + pdaSettings.getExportFile());
		System.out.println(guiText[4] + pdaSettings.getProjectID());
		System.out.println("--------------------------------------------------------\n");

		mySoftware = new XConverter();
		loadModel = true;

		try {
			mySoftware.connect2DB();
			mySoftware.setupDBTranslation(false);
			mySoftware.checkNumberOfFields();
			tabModel = new ViewerModel(mySoftware.getTableModelFields());
			mySoftware.openToFile();
			mySoftware.addObserver(this);
			mySoftware.loadInputFile();
			mySoftware.processFiles(tabModel);
			mySoftware.close(); // Close import file
		} catch (Exception e) {
			General.errorMessage(null, e, GUIFactory.getTitle("databaseReadError"), null);
			mySoftware.close();
			return;
		}

		// Start Export
		loadModel = false;
		mySoftware.openToFile();
		mySoftware.convertFromTableModel(tabModel, mySoftware.getDbOut());
		mySoftware.close(); // Close export file
		System.out.println();
		mySoftware.runConversionProgram(null);
		System.out.println();
		pdaSettings.setLastProfile(pdaSettings.getProfileID());
	}
}