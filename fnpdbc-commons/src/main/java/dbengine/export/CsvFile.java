package dbengine.export;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.IConvert;
import dbengine.utils.CsvReader;
import dbengine.utils.CsvWriter;

public class CsvFile extends GeneralDB implements IConvert {
	/**
	 * Title: CsvFile Description: Generic FileWriter Class for CSV- and Text files
	 * Copyright: (c) 2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private CsvWriter writer;
	private CsvReader reader;
	private String categoryField;
	private File outFile;

	private boolean useHeaders = true;
	private boolean useNoLineBreaks = false;
	private boolean useCategory = false;

	private int myCurrentRecord = 0;
	private int maxSize = 0;
	private int fileCounter = 1;

	private List<HashMap<String, Object>> dbRecords;
	private List<Integer> dbFieldSize = new ArrayList<>();
	private List<String> exportFiles;
	
	public CsvFile(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean createBackup, boolean isInputFile) throws Exception {
		hasBackup = false;
		outFile = new File(myFilename);
		exportFiles = new ArrayList<>();
		exportFiles.add(myFilename);
		this.isInputFile = isInputFile;

		if (createBackup) {
			hasBackup = General.copyFile(myFilename, myFilename + ".bak");
		}

		if (isInputFile) {
			if (encoding.isEmpty()) {
				reader = new CsvReader(myFilename);
			} else {
				reader = new CsvReader(myFilename, ',', Charset.forName(encoding));
			}
		} else {
			outFile.delete();
			writer = new CsvWriter(outFile, encoding);

			maxSize = myPref.getMaxFileSize();
			if (maxSize != 0) {
				maxSize = 1024 * (int) Math.pow(2, maxSize + 8);
			}
		}
	}

	@Override
	public void verifyDatabase(List<FieldDefinition> newFields) throws Exception {
		if (myImportFile == ExportFile.TEXTFILE) {
			reader.setDelimiter(myPref.getImportFieldSeparator().charAt(0));
			reader.setTextQualifier(myPref.getImportTextDelimiter().charAt(0));
		}

		if (reader.readHeaders()) {
			dbFieldNames.clear();
			dbFieldTypes.clear();
			dbFieldSize.clear();

			int numFields = reader.getHeaderCount();
			for (int i = 0; i < numFields; i++) {
				dbFieldNames.add(reader.getHeader(i));
				dbFieldTypes.add(FieldTypes.TEXT);
				dbFieldSize.add(1);
			}

			dbRecords = new ArrayList<>();
			while (reader.readRecord()) {
				HashMap<String, Object> dbRead = new HashMap<>(numFields);
				for (int i = 0; i < numFields; i++) {
					String obj = reader.get(i);
					dbRead.put(dbFieldNames.get(i), obj);
					dbFieldSize.set(i, Math.max(dbFieldSize.get(i), obj.length()));
				}
				dbRecords.add(dbRead);
			}
			myTotalRecords = dbRecords.size();
		} else {
			throw FNProgException.getException("noFields", myFilename);
		}
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = dbRecords.get(myCurrentRecord);
		dbRecords.set(myCurrentRecord, null); // Cleanup memory usage
		myCurrentRecord++;
		return result;
	}

	@Override
	public void createDbHeader() throws Exception {
		String fieldSeparator = ",";
		String textDelimiter = "\"";

		categoryField = myPref.getCategoryField();
		useCategory = !categoryField.isEmpty();

		if (myExportFile == ExportFile.TEXTFILE) {
			fieldSeparator = myPref.getFieldSeparator();
			textDelimiter = myPref.getTextDelimiter();
			useHeaders = myPref.isUseHeader();
			useNoLineBreaks = !myPref.isUseLinebreak();
		}

		writer.setDelimiter(fieldSeparator.charAt(0));
		writer.setTextQualifier(textDelimiter.charAt(0));

		if (!useHeaders) {
			// There is nothing else to do.
			return;
		}

		// Used by SmartList only
		if (useCategory) {
			writer.write("Categories");
		}

		// Read the user defined list of DB fields and write the headers
		// Ignore header for the category field, because that has already been written
		Predicate<FieldDefinition> filter = field -> useCategory && field.getFieldAlias().equals(categoryField);
		dbInfo2Write.stream().filter(filter.negate()).forEach(field -> {
			try {
				writer.write(field.getFieldHeader());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		writer.endRecord();
	}

	@Override
	public void closeFile() {
		if (isInputFile && reader != null) {
			reader.close();
			reader = null;
			return;
		}
		closeOutputFile();
	}

	private void closeOutputFile() {
		if (writer != null) {
			writer.close();
			writer = null;
		}
	}

	@Override
	public void deleteFile() {
		closeFile();
		File oFile = new File(myFilename);

		if (oFile.exists()) {
			oFile.delete();
		}

		if (hasBackup) {
			File backupFile = new File(myFilename + ".bak");
			backupFile.renameTo(oFile);
		}
	}

	public String getExportFiles(String file, String files) {
		StringBuilder result = new StringBuilder();
		boolean isMultipleFile = exportFiles.size() > 1;

		result.append(isMultipleFile ? files : file);
		result.append(" ");

		for (String s : exportFiles) {
			result.append("'");
			result.append(s);
			result.append("', ");
		}

		result.delete(result.length() - 2, result.length());
		return result.toString();
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		// Read the Category
		if (useCategory) {
			String dbField = dbRecord.get(categoryField).toString();
			if (dbField == null || dbField.isEmpty()) {
				writer.write("Unfiled");
			} else {
				writer.write(dbField);
			}
		}

		// Read the user defined list of DB fields
		for (FieldDefinition field : dbInfo2Write) {
			if (useCategory && field.getFieldAlias().equals(categoryField)) {
				// There's no use to return the Category field a second time
				continue;
			}
			String dbField = convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString();
			if (useNoLineBreaks) {
				dbField = dbField.replaceAll("\n", " ");
				dbField = dbField.replaceAll("\r", "");
			}
			writer.write(dbField, true);
		}
		writer.endRecord();

		// Check if we have to create a new output file
		if (maxSize != 0 && maxSize - writer.getMaxLineSize() < writer.getSize()) {
			closeOutputFile();
			StringBuilder buf = new StringBuilder();
			buf.append(myFilename.substring(0, myFilename.lastIndexOf('.')));
			buf.append("_");
			buf.append(fileCounter++);
			buf.append(myFilename.substring(myFilename.lastIndexOf('.')));

			outFile = new File(buf.toString());
			exportFiles.add(buf.toString());

			outFile.delete();
			writer = new CsvWriter(outFile, encoding);
		}
	}

	@Override
	public List<FieldDefinition> getTableModelFields() throws Exception {
		List<FieldDefinition> result = new ArrayList<>();
		int index = 0;
		for (String name : dbFieldNames) {
			FieldDefinition fieldDef = new FieldDefinition(name, name, dbFieldTypes.get(index));
			fieldDef.setSize(dbFieldSize.get(index++).intValue());
			result.add(fieldDef);
		}
		return result;
	}
}