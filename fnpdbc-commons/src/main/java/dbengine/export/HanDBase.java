package dbengine.export;

import java.util.ArrayList;
import java.util.List;

import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.General;
import dbengine.utils.DatabaseHelper;

public class HanDBase extends CsvFile {
	/**
	 * Title: HanDBase Description: HanDBase class Copyright: (c) 2003-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8.2
	 */
	final private String exportFile = System.getProperty("java.io.tmpdir") + "\\" + "handbase.csv";
	private String pdbFile;

	public HanDBase(Profiles pref) {
		super(pref);
	}

	@Override
	public void openFile(DatabaseHelper helper, boolean createBackup, boolean isInputFile) throws Exception {
		// Check if the database conversion program is defined and exists
		if (handbaseProgram.length() == 0) {
			throw FNProgException.getException("noHandbaseDesktop");
		}

		if (!General.existFile(handbaseProgram)) {
			throw FNProgException.getException("noHandbaseDesktopExists", handbaseProgram);
		}

		// We write to a temporary work file instead of to the pdb file
		pdbFile = helper.getDatabase();
		helper.setDatabase(exportFile);

		if (createBackup) {
			General.copyFile(pdbFile, pdbFile + ".bak");
		}
		super.openFile(helper, false, isInputFile);
	}

	/*
	 * Method to run the database conversion program for HanDBase
	 */
	public void runConversionProgram(Profiles pref) throws Exception {
		List<String> cmd = new ArrayList<>();
		final String outFile = pdbFile;
		final String dbName = pref.getPdaDatabaseName();
		final int exportOption = pref.getExportOption();
		final int importOption = pref.getImportOption();

		cmd.add(handbaseProgram);
		cmd.add("CONVERT:TOPDB");
		cmd.add("INFILE:{" + exportFile + "}");
		cmd.add("OUTFILE:{" + outFile + "}");
		cmd.add("DBNAME:{" + dbName + "}");

		String s = pref.getAutoInstUser();
		if (s.length() > 0) {
			cmd.add("AUTOINSTALL:{" + s + "}");
		}

		if (exportOption > 0) {
			if (General.existFile(outFile)) {
				cmd.add("USEEXISTING:{" + outFile + "}");
				if (exportOption == 2) {
					cmd.add("PDB:APPEND");
				}
				if (importOption == 1) {
					cmd.add("PHYSICALORDER");
				}
			}
		}

		String password = pref.getExportPassword();
		if (!password.isEmpty()) {
			cmd.add("PASSWORD:{" + password + "}");
		}

		cmd.add("HASQUOTES");
		cmd.add("FIELDNAMES");
		cmd.add("OVERWRITE");

		String[] result = new String[cmd.size()];
		for (int i = 0; i < cmd.size(); i++) {
			result[i] = cmd.get(i);
		}

		General.executeProgram(result);
		if (!General.existFile(pdbFile)) {
			throw FNProgException.getException("handbaseDesktopError", handbaseProgram, pdbFile);
		}
	}
}