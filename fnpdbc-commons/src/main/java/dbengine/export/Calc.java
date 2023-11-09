package dbengine.export;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	private int currentRecord = 1;
	private int maxColumns;
	private int calcRow = 0;

	protected int noOfSheets;

	protected SpreadSheet wb;
	protected Sheet sheet;

	public Calc(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		outFile = new File(myDatabase);

		this.isInputFile = isInputFile;
		if (isInputFile) {
			wb = new SpreadSheet(outFile);
			noOfSheets = wb.getNumSheets();
		}
	}

	@Override
	public void readTableContents() throws Exception {
		// Read all sheets in the workbook
		hSheets = new HashMap<>(noOfSheets);
		for (int i = 0; i < noOfSheets; i++) {
			// Get TableModelFields for each sheet and put them in the HashMap
			sheet = wb.getSheet(i);
			List<FieldDefinition> temp = getDBFieldNamesAndTypes();
			if (!temp.isEmpty()) {
				hSheets.put(sheet.getName(), temp);
			}
		}

		getCurrentSheet();
		if (sheet == null) {
			throw FNProgException.getException("noSheets", myDatabase);
		}

		totalRecords = sheet.getLastRow() - 1;
		if (totalRecords < 1) {
			throw FNProgException.getException("noRecordsInSheet", sheet.getName(), myDatabase);
		}
	}

	private void createNewSheet(int rows, int columns) {
		wb = new SpreadSheet();
		String sheetName = myPref.getPdaDatabaseName();
		sheet = new Sheet(sheetName.isEmpty() ? "Sheet1" : sheetName, rows, columns);
		wb.appendSheet(sheet);
	}

	private List<FieldDefinition> getDBFieldNamesAndTypes() {
		List<FieldDefinition> result = new ArrayList<>();
		dbFieldNames.clear();

		if (sheet.getLastRow() < 2) {
			return result;
		}

		maxColumns = sheet.getMaxColumns();
		Range range = sheet.getRange(0, 0, 1, maxColumns); // Assumes that the 1st row contains the fieldnames
		Object[][] cells = range.getValues();
		int counter = 0;
		for (Object cell : cells[0]) {
			counter++;
			dbFieldNames.add(cell == null ? "Header" + Integer.toString(counter) : cell.toString());
		}

		int numRows = sheet.getMaxRows() - 1;

		// Read one complete column at the time
		for (int column = 0; column < maxColumns; column++) {
			range = sheet.getRange(1, column, numRows);
			cells = range.getValues();

			// Read each cell in the column until a value is found
			Object cell = null;
			for (int row = 0; numRows > row; row++) {
				cell = cells[row][0];
				if (cell != null) {
					break;
				}
			}
			String name = dbFieldNames.get(column);
			result.add(new FieldDefinition(name, name, cell == null ? FieldTypes.TEXT : getFieldType(cell)));
		}

		return result;
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

		if (cell instanceof Double) {
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
	public String getPdaDatabase() {
		return sheet == null ? null : sheet.getName();
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

	private String getCurrentSheet() {
		String sheetName = myPref.getTableName();
		for (String s : hSheets.keySet()) {
			if (sheetName.equals(s)) {
				sheet = wb.getSheet(sheetName);
				return sheetName;
			}
		}

		sheet = wb.getSheet(0);
		return sheet.getName();
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		List<FieldDefinition> dbDef = getTableModelFields();
		Map<String, Object> result = new HashMap<>();
		Range row = sheet.getRange(currentRecord++, 0, 1, maxColumns);

		int index = 0;
		Object[][] cells = row.getValues();
		for (Object cell : cells[0]) {
			FieldDefinition field = dbDef.get(index++);
			result.put(field.getFieldAlias(), cell);
		}
		return result;
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		return hSheets.get(getCurrentSheet());
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
		createNewSheet(totalRecords, dbInfo2Write.size());

		if (myPref.isUseHeader()) {
			sheet.appendRow();
			Range range = sheet.getRange(calcRow++, 0, 1, dbInfo2Write.size());
			List<String> headers = dbInfo2Write.stream().map(FieldDefinition::getFieldHeader)
					.collect(Collectors.toList());
			range.setValues(headers.toArray());
			if (myPref.isBoldHeader()) {
				range.setFontBold(true);
			}
		}
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		int row = calcRow++;
		int col = 0;
		sheet.appendRow();

		for (FieldDefinition field : dbInfo2Write) {
			Range cell = sheet.getRange(row, col++);
			cell.setValue(convertDataFields(dbRecord.get(field.getFieldAlias()), field));
		}
	}
}
