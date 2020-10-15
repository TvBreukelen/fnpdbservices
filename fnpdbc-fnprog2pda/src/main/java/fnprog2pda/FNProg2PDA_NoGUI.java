package fnprog2pda;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import application.interfaces.ExportFile;
import application.interfaces.FNPSoftware;
import application.interfaces.TvBSoftware;
import application.model.ViewerModel;
import application.preferences.Databases;
import application.utils.GUIFactory;
import application.utils.General;
import fnprog2pda.preferences.PrefFNProg;
import fnprog2pda.software.FNProgramvare;
import fnprog2pda.utils.ConvertOldVersion;

public class FNProg2PDA_NoGUI implements PropertyChangeListener {

	private String myProfileID;
	private ExportFile databaseType;

	private PrefFNProg pdaSettings = PrefFNProg.getInstance();
	private Databases dbSettings = Databases.getInstance(TvBSoftware.FNPROG2PDA);

	private FNProgramvare mySoftware;
	private boolean loadModel;

	public FNProg2PDA_NoGUI(String... args) {
		TvBSoftware software = TvBSoftware.FNPROG2PDA;
		System.out.println(software.getName() + " " + software.getVersion() + " - " + software.getCopyright() + "\n");
		General.setQuietMode();

		try {
			final int maxProfiles = args.length - 1;

			if (maxProfiles < 1) {
				System.out.println(GUIFactory.getMessage("parameterError"));
				return;
			}

			ConvertOldVersion.convert();

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
				mySoftware.closeFiles(true); // Close database and output file
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		int record = (int) evt.getNewValue();
		System.out.println(
				GUIFactory.getMessage((loadModel ? "recordsRead" : "recordsProcessed"), Integer.toString(record)));
	}

	private void runExport() throws Exception {
		if (pdaSettings.getExportFile().isEmpty()) {
			pdaSettings.setExportFile(General.getDefaultPDADatabase(databaseType));
		}

		ViewerModel tabModel;
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

		StringBuilder buf = new StringBuilder(maxLen + 5);
		for (int i = 0; i < 5; i++) {
			buf.append("  ");

			int addSpaces = maxLen - guiText[i].length();
			for (int j = 0; j < addSpaces; j++) {
				buf.append(" ");
			}
			buf.append(guiText[i]);
			buf.append(" : ");
			guiText[i] = buf.toString();
			buf.setLength(0);
		}

		System.out.println("--------------------------------------------------------");
		System.out.println(guiText[0] + myProfileID);
		System.out.println("--------------------------------------------------------");
		System.out.println(guiText[1] + dbSettings.getDatabaseFile());
		System.out.println(guiText[2]
				+ General.getSoftwareTypeVersion(dbSettings.getDatabaseType(), dbSettings.getDatabaseVersion()) + "\n");
		System.out.println(guiText[3] + pdaSettings.getExportFile());
		System.out.println(guiText[4] + pdaSettings.getProjectID());
		System.out.println("--------------------------------------------------------\n");

		FNPSoftware soft = FNPSoftware.getSoftware(dbSettings.getDatabaseType());
		mySoftware = FNProgramvare.getSoftware(soft);
		if (pdaSettings.getTableName().isEmpty()) {
			pdaSettings.setTableName(soft.getViews()[0], true);
		}

		loadModel = true;
		try {
			mySoftware.openFile(); // Connect to the FNProgramvare Access database
			mySoftware.setupDBTranslation(false);
			tabModel = new ViewerModel(mySoftware.getTableModelFields());
			mySoftware.checkNumberOfFields(false, tabModel); // Plausibility check 1
			mySoftware.setCategories(); // Obtain categories (when required)

			mySoftware.addObserver(this); // Start monitoring process
			mySoftware.refreshSpecialFields();
			mySoftware.processFiles(tabModel);
			mySoftware.close(); // Close Database
		} catch (Exception e) {
			General.errorMessage(null, e, GUIFactory.getTitle("databaseReadError"), null);
			mySoftware.close();
			return;
		}

		loadModel = false;
		mySoftware.checkNumberOfFields(true, tabModel); // Plausibility check 2
		mySoftware.openToFile();
		mySoftware.convertFromTableModel(tabModel, mySoftware.getDbOut());
		mySoftware.close();
		System.out.println();
		mySoftware.runConversionProgram(null);
		System.out.println();
		pdaSettings.setLastProfile(pdaSettings.getProfileID());
	}
}
