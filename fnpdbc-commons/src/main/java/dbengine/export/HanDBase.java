package dbengine.export;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import application.interfaces.FieldTypes;
import application.preferences.GeneralSettings;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.utils.DatabaseHelper;

public class HanDBase extends CsvFile {
	/**
	 * Title: HanDBase Description: HanDBase class Copyright: (c) 2003-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8.2
	 */
	private final String exportFile = System.getProperty("java.io.tmpdir") + "\\" + "handbase.csv";
	private String pdbFile;
	private int fileCounter;
	private GeneralSettings generalSettings = GeneralSettings.getInstance();

	public HanDBase(Profiles pref) {
		super(pref);
		fileCounter = 1;
	}

	@Override
	public void openFile(DatabaseHelper helper, boolean isInputFile) throws Exception {
		// Check if the database conversion program is defined and exists
		if (handbaseProgram.isEmpty()) {
			throw FNProgException.getException("noHandbaseDesktop");
		}

		if (!General.existFile(handbaseProgram)) {
			throw FNProgException.getException("noHandbaseDesktopExists", handbaseProgram);
		}

		// We write to a temporary work file instead of to the pdb file
		pdbFile = helper.getDatabase();
		helper.setDatabase(exportFile);

		super.openFile(helper, isInputFile);
	}

	@Override
	public Object convertDataFields(Object dbValue, FieldDefinition field) {
		if (dbValue instanceof ImageIcon icon) {
			return convertImage(field.getFieldHeader(), icon, field.getFieldType() != FieldTypes.THUMBNAIL);
		}

		return super.convertDataFields(dbValue, field);
	}

	private String convertImage(String field, ImageIcon icon, boolean isScaled) {
		String[] types = { ".bmp", ".jpg", ".png" };

		if (!myPref.isExportImages()) {
			return General.EMPTY_STRING;
		}

		StringBuilder buf = new StringBuilder(100);
		buf.append(generalSettings.getDefaultImageFolder());
		buf.append("/");
		buf.append(myPref.getProfileID());
		buf.append("_");
		buf.append(field);
		buf.append("_");
		buf.append(fileCounter++);
		buf.append(types[myPref.getImageOption()]);

		try {
			if (General.convertImage(icon, myExportFile, myPref, buf.toString(), isScaled)) {
				if (myPref.getImageOption() != 0) { // BMP files are imported directly
					buf.delete(0, generalSettings.getDefaultImageFolder().length());
					if (generalSettings.isNoImagePath()) {
						buf.delete(0, 1);
					} else {
						buf.insert(0, generalSettings.getDefaultPdaFolder());
					}
				}
				return buf.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return General.EMPTY_STRING;
	}

	/*
	 * Method to run the database conversion program for HanDBase
	 */
	public void runConversionProgram() throws Exception {
		List<String> cmd = new ArrayList<>();
		final String outFile = pdbFile;
		final String dbName = myPref.getDatabaseName();
		final int exportOption = myPref.getExportOption();
		final int importOption = myPref.getImportOption();

		// Read input file and convert to Ansi
		super.closeFile();
		String exp = General.readFile(exportFile);

		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(exportFile), "windows-1252")) {
			writer.write(exp);
		}

		cmd.add(handbaseProgram);
		cmd.add("CONVERT:TOPDB");
		cmd.add("INFILE:{" + exportFile + "}");
		cmd.add("OUTFILE:{" + outFile + "}");
		cmd.add("DBNAME:{" + dbName + "}");

		String s = myPref.getAutoInstUser();
		if (s.length() > 0) {
			cmd.add("AUTOINSTALL:{" + s + "}");
		}

		if (exportOption > 0 && General.existFile(outFile)) {
			cmd.add("USEEXISTING:{" + outFile + "}");
			if (exportOption == 2) {
				cmd.add("PDB:APPEND");
			}
			if (importOption == 1) {
				cmd.add("PHYSICALORDER");
			}
		}

//TODO		String password = myPref.getExportPassword();
//		if (!password.isEmpty()) {
//			cmd.add("PASSWORD:{" + password + "}");
//		}

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