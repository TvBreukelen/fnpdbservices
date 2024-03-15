package dbengine.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.IConvert;

public class CsvFile extends GeneralDB implements IConvert {
	/**
	 * Title: CsvFile Description: Generic FileWriter Class for CSV- and Text files
	 * Copyright: (c) 2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private File outFile;

	private boolean useNoLineBreaks = false;

	private int currentRecord = 0;
	private int maxSize = 0;
	private int fileSize = 0;
	private int fileCounter = 1;

	private List<Map<String, String>> dbRecords;
	private List<String> exportFiles;

	private CsvMapper mapper = new CsvMapper();
	private CsvSchema schema;
	protected SequenceWriter writer;

	public CsvFile(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		outFile = new File(myDatabase);
		exportFiles = new ArrayList<>();
		exportFiles.add(myDatabase);
		useNoLineBreaks = !myPref.isUseLinebreak();

		this.isInputFile = isInputFile;

		if (!isInputFile) {
			schema = CsvSchema.emptySchema() //
					.withColumnSeparator(myPref.getImportFieldSeparator().charAt(0)) //
					.withQuoteChar(myPref.getImportTextDelimiter().charAt(0));

			outFile.delete();
			writer = mapper.writer(schema).writeValues(outFile);
			maxSize = myPref.getMaxFileSize();
			if (maxSize != 0) {
				maxSize = 1024 * (int) Math.pow(2, maxSize + 8);
			}
		}
	}

	@Override
	public void readTableContents() throws Exception {
		schema = mapper.schemaWithHeader() //
				.withColumnSeparator(myPref.getImportFieldSeparator().charAt(0)) //
				.withQuoteChar(myPref.getImportTextDelimiter().charAt(0));
		try {
			MappingIterator<Map<String, String>> csvIter = mapper//
					.readerForMapOf(String.class)//
					.with(schema) //
					.readValues(outFile);

			dbRecords = csvIter.readAll();
			totalRecords = dbRecords.size();
			if (totalRecords == 0) {
				return;
			}

			dbFieldNames.clear();
			dbFieldTypes.clear();

			Map<String, String> map = dbRecords.get(0);
			map.entrySet().forEach(entry -> {
				dbFieldNames.add(entry.getKey());
				dbFieldTypes.add(FieldTypes.TEXT);

			});
		} catch (Exception ex) {
			throw FNProgException.getException("noFields", myDatabase);
		}

		// Set the max. size of every field
		getTableModelFields().forEach(field -> dbRecords
				.forEach(map -> field.setSize(map.getOrDefault(field.getFieldHeader(), General.EMPTY_STRING))));
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		return new HashMap<>(dbRecords.get(currentRecord++));
	}

	@Override
	public void createDbHeader() throws Exception {
		if (!myPref.isUseHeader()) {
			// There is nothing else to do.
			return;
		}

		// Read the user defined list of DB fields and write the headers
		List<String> list = dbInfo2Write.stream().map(FieldDefinition::getFieldHeader).toList();
		writer.write(list);
		fileSize = list.toString().length();
	}

	public String getExportFiles(String file, String files) {
		StringBuilder result = new StringBuilder();
		boolean isMultipleFile = exportFiles.size() > 1;

		result.append(isMultipleFile ? files : file);
		result.append(General.SPACE);

		for (String s : exportFiles) {
			result.append("'");
			result.append(s);
			result.append("', ");
		}

		result.delete(result.length() - 2, result.length());
		return result.toString();
	}

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		// Read the user defined list of DB fields
		List<String> list = dbInfo2Write.stream()
				.map(field -> convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString()).toList();

		writeOutputFile(list);
		return 1;
	}

	protected void writeOutputFile(List<String> list) throws IOException {
		if (useNoLineBreaks) {
			list = list.stream().map(e -> e.replace("\n", General.SPACE).replace("\r", General.EMPTY_STRING)).toList();
		}

		writer.write(list);
		if (maxSize == 0) {
			// File size is unlimited
			return;
		}

		fileSize += list.toString().length();

		// Check if we have to create a new output file
		if (maxSize <= fileSize) {
			StringBuilder buf = new StringBuilder();
			buf.append(myDatabase.substring(0, myDatabase.lastIndexOf('.')));
			buf.append("_");
			buf.append(fileCounter++);
			buf.append(myDatabase.substring(myDatabase.lastIndexOf('.')));

			outFile = new File(buf.toString());
			exportFiles.add(buf.toString());
			outFile.delete();
			fileSize = 0;

			closeFile();
			writer = mapper.writer(schema).writeValues(outFile);
		}
	}

	@Override
	public void closeFile() {
		if (!isInputFile && writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				// Nothing to do
			}
		}
	}

	@Override
	public List<Object> getDbFieldValues(String field) throws Exception {
		Set<Object> result = new HashSet<>();
		dbRecords.forEach(m -> result.add(m.getOrDefault(field, General.EMPTY_STRING)));
		return new ArrayList<>(result);
	}
}