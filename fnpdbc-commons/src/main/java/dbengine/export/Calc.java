package dbengine.export;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.miachm.sods.OfficeCurrency;
import com.github.miachm.sods.OfficePercentage;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import dbengine.GeneralDB;
import dbengine.IConvert;

public class Calc extends GeneralDB implements IConvert {
	private File outFile;

	private Map<String, List<FieldDefinition>> hSheets;
	private Map<String, List<Map<String, Object>>> hRecords;

	private int currentRecord = 0;
	private int calcRow = 0;
	private boolean isAppend;

	private SpreadSheet wb;
	private Sheet sheet;
	private String sheetName;

	public Calc(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		outFile = new File(myDatabase);
		isAppend = myPref.isAppendRecords() && outFile.exists();

		this.isInputFile = isInputFile;
		if (outFile.exists()) {
			sheetName = isInputFile ? myPref.getTableName() : myPref.getDatabaseName();
			wb = new SpreadSheet(outFile);
			int noOfSheets = wb.getNumSheets();

			// Read all sheets in the workbook
			hSheets = new HashMap<>(noOfSheets);
			hRecords = new HashMap<>(noOfSheets);

			for (int i = 0; i < noOfSheets; i++) {
				// Get TableModelFields and records for each sheet
				sheet = wb.getSheet(i);
				getFieldDefinitonsAndRecords();
			}
		} else {
			sheetName = myPref.getDatabaseName();
			wb = new SpreadSheet();
		}

		if (isAppend && !hSheets.containsKey(sheetName)) {
			isAppend = false;
		}

		createNewSheet(totalRecords, dbInfo2Write.size());
	}

	@Override
	public void readTableContents() throws Exception {
		if (hSheets.isEmpty()) {
			throw FNProgException.getException("noSheets", myDatabase);
		}

		if (!hSheets.containsKey(sheetName)) {
			// Get the first sheet
			sheetName = hSheets.keySet().iterator().next();
		}

		sheet = wb.getSheet(sheetName);

		totalRecords = hRecords.get(sheetName).size();
		if (totalRecords < 1) {
			throw FNProgException.getException("noRecordsInSheet", sheetName, myDatabase);
		}
	}

	private void createNewSheet(int rows, int columns) {
		if (isAppend || isInputFile) {
			return;
		}

		Optional<Sheet> sheetOpt = Optional.ofNullable(wb.getSheet(sheetName));
		if (sheetOpt.isPresent()) {
			wb.deleteSheet(sheetOpt.get());
		}

		sheet = new Sheet(sheetName, rows, columns);
		wb.appendSheet(sheet);
	}

	private void getFieldDefinitonsAndRecords() {
		dbFieldNames.clear();
		if (sheet.getMaxRows() < 2) {
			return;
		}

		int maxColumns = sheet.getMaxColumns();
		List<Map<String, Object>> records = new ArrayList<>();

		Range range = sheet.getRange(0, 0, 1, maxColumns); // Assumes that the 1st row contains the fieldnames
		Object[][] cells = range.getValues();
		int counter = 0;
		for (Object cell : cells[0]) {
			counter++;
			dbFieldNames.add(cell == null ? "Header" + Integer.toString(counter) : cell.toString());
		}

		// Read all rows
		int numRows = sheet.getMaxRows() - 1;
		Map<String, FieldDefinition> fieldMap = new LinkedHashMap<>();

		for (int row = 1; row < numRows; row++) {
			range = sheet.getRange(row, 0, 1, maxColumns);
			cells = range.getValues();
			Object cell = null;

			// Read all columns in a row
			Map<String, Object> map = new HashMap<>();
			for (int column = 0; column < maxColumns; column++) {
				String fieldName = dbFieldNames.get(column);
				cell = cells[0][column];

				map.put(fieldName, cell);
				FieldDefinition field = fieldMap.getOrDefault(fieldName,
						new FieldDefinition(fieldName, fieldName, getFieldType(cell)));
				field.setSize(cell);
				fieldMap.putIfAbsent(fieldName, field);
			}
			records.add(map);
		}

		hSheets.put(sheet.getName(), new ArrayList<>(fieldMap.values()));
		hRecords.put(sheet.getName(), records);
	}

	private FieldTypes getFieldType(Object cell) {
		if (cell instanceof String) {
			return FieldTypes.TEXT;
		}

		if (cell instanceof Boolean) {
			return FieldTypes.BOOLEAN;
		}

		if (cell instanceof LocalDate) {
			return FieldTypes.DATE;
		}

		if (cell instanceof LocalDateTime) {
			return FieldTypes.TIMESTAMP;
		}

		if (cell instanceof LocalTime) {
			return FieldTypes.TIME;
		}

		if (cell instanceof Duration) {
			return FieldTypes.DURATION;
		}

		if (cell instanceof Float || cell instanceof Double) {
			return FieldTypes.FLOAT;
		}

		if (cell instanceof OfficeCurrency) {
			return FieldTypes.CURRENCY;
		}

		if (cell instanceof OfficePercentage) {
			return FieldTypes.TEXT;
		}

		return FieldTypes.TEXT;
	}

	@Override
	public List<String> getTableOrSheetNames() {
		if (wb == null) {
			return new ArrayList<>();
		}

		List<String> lSheets = new ArrayList<>(hSheets.keySet());
		Collections.sort(lSheets);
		return lSheets;
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		List<Map<String, Object>> records = hRecords.get(sheetName);
		return records.get(currentRecord++);
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		return hSheets.get(sheetName);
	}

	@Override
	public void closeFile() {
		try {
			if (!isInputFile) {
				wb.save(outFile);
			}
		} catch (Exception e) {
			// Nothing that can be done about this
		}
	}

	@Override
	public void createDbHeader() throws Exception {
		if (isAppend) {
			validateAppend(getTableModelFields());
			calcRow = sheet.getMaxRows();
			return;
		}

		if (myPref.isUseHeader()) {
			sheet.appendRow();
			Range range = sheet.getRange(calcRow++, 0, 1, dbInfo2Write.size());
			List<String> headers = dbInfo2Write.stream().map(FieldDefinition::getFieldHeader).toList();
			range.setValues(headers.toArray());
			if (myPref.isBoldHeader()) {
				range.setFontBold(true);
			}
		}
	}

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		int row = calcRow++;
		int col = 0;
		sheet.appendRow();

		for (FieldDefinition field : dbInfo2Write) {
			Range cell = sheet.getRange(row, col++);
			cell.setValue(convertDataFields(dbRecord.get(field.getFieldAlias()), field));
		}

		return 1;
	}
}
