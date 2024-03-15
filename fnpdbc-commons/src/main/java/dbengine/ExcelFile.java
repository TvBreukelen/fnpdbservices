package dbengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;

public abstract class ExcelFile extends GeneralDB implements IConvert {
	/**
	 * Title: ExcelFile Description: Generic abstract class for writing Excel Files
	 * Copyright: (c) 2003-2019
	 *
	 * @author Tom van Breukelen
	 * @version 8+
	 */
	private Map<String, List<FieldDefinition>> hSheets;
	private Map<String, List<Map<String, Object>>> hRecords;

	private int currentRecord = 0;
	protected int noOfSheets;

	private File outFile;
	private DataFormatter formatter = new DataFormatter();

	protected Workbook wb;
	protected Sheet sheet;
	protected String sheetName;
	protected CreationHelper helper;
	protected boolean isAppend;

	protected int maxRowsInSheet;

	protected ExcelFile(Profiles pref) {
		super(pref);
	}

	@Override
	public void openFile(boolean isInputFile) throws Exception {
		outFile = new File(myDatabase);
		boolean isXlsx = myDatabase.toLowerCase().endsWith("x");
		maxRowsInSheet = isXlsx ? 1048576 : 65536;
		isAppend = myPref.isAppendRecords() && outFile.exists();
		this.isInputFile = isInputFile;

		if (outFile.exists()) {
			InputStream fileIn = new FileInputStream(outFile);
			wb = WorkbookFactory.create(fileIn);
			noOfSheets = wb.getNumberOfSheets();
			sheetName = isInputFile ? myPref.getTableName() : myPref.getDatabaseName();

			// Read all sheets in the workbook
			hSheets = new HashMap<>(noOfSheets);
			hRecords = new HashMap<>(noOfSheets);

			for (int i = 0; i < noOfSheets; i++) {
				// Get TableModelFields for each sheet and put them in the HashMap
				sheet = wb.getSheetAt(i);
				getFieldDefinitonsAndRecords();
			}
		} else {
			sheetName = myPref.getDatabaseName();
			wb = isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
		}

		if (isAppend && !hSheets.containsKey(sheetName)) {
			isAppend = false;
		}

		createNewSheet();
		helper = wb.getCreationHelper();
	}

	private void createNewSheet() {
		if (isAppend || isInputFile) {
			return;
		}

		Optional<Sheet> sheetOpt = Optional.ofNullable(wb.getSheet(sheetName));
		if (sheetOpt.isPresent()) {
			wb.removeSheetAt(wb.getSheetIndex(sheetOpt.get()));
		}

		sheet = wb.createSheet(sheetName);
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		return hSheets.get(sheetName);
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

	private void getFieldDefinitonsAndRecords() {
		dbFieldNames.clear();

		if (sheet.getLastRowNum() < 2) {
			return;
		}

		List<Map<String, Object>> records = new ArrayList<>();
		Row names = sheet.getRow(0); // Assumes that the 1st row contains the field names
		int maxColumns = names.getLastCellNum();

		for (Cell cell : names) {
			dbFieldNames.add(cell.getRichStringCellValue().getString());
		}

		// Read all rows
		int numRows = sheet.getLastRowNum() - 1;
		Map<String, FieldDefinition> fieldMap = new LinkedHashMap<>();

		for (int rowNo = 1; rowNo < numRows; rowNo++) {
			Row row = sheet.getRow(rowNo);

			// Read all columns in a row
			Map<String, Object> map = new HashMap<>();
			int index = 0;
			for (Cell cell : row) {
				String fieldName = dbFieldNames.get(index++);
				FieldDefinition field = fieldMap.getOrDefault(fieldName,
						new FieldDefinition(fieldName, fieldName, getFieldType(cell)));

				Object obj = convertCell(cell, field);
				map.put(fieldName, obj);
				field.setSize(obj);
				fieldMap.putIfAbsent(fieldName, field);

				if (index > maxColumns) {
					break;
				}
			}
			records.add(map);
		}

		hSheets.put(sheet.getSheetName(), new ArrayList<>(fieldMap.values()));
		hRecords.put(sheet.getSheetName(), records);
	}

	protected void setFreeze(int row, int col) {
		sheet.createFreezePane(col, row, col, row);
	}

	private FieldTypes getFieldType(Cell pCell) {
		switch (pCell.getCellType()) {
		case BOOLEAN:
			return FieldTypes.BOOLEAN;
		case BLANK, STRING:
			String contents = pCell.getRichStringCellValue().getString();
			if (contents.length() > 256 || contents.indexOf('\n') > -1) {
				return FieldTypes.MEMO;
			}
			return FieldTypes.TEXT;
		case FORMULA:
			return FieldTypes.TEXT;
		case NUMERIC:
			if (DateUtil.isCellDateFormatted(pCell)) {
				String format = pCell.getCellStyle().getDataFormatString();
				boolean isDate = format.indexOf('d') != -1;
				boolean isTime = format.indexOf(':') != -1;

				if (isDate && isTime) {
					return FieldTypes.TIMESTAMP;
				}

				if (isTime) {
					return FieldTypes.TIME;
				}

				return FieldTypes.DATE;
			}

			String s = formatter.formatCellValue(pCell);
			return s.indexOf('.') != -1 ? FieldTypes.FLOAT : FieldTypes.NUMBER;
		default:
			return FieldTypes.TEXT;
		}
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

	public Object convertCell(Cell cell, FieldDefinition field) {
		switch (cell.getCellType()) {
		case STRING:
			return cell.getRichStringCellValue().getString();
		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				LocalDateTime date = new Timestamp(cell.getDateCellValue().getTime()).toLocalDateTime();

				switch (field.getFieldType()) {
				case DATE:
					return date.toLocalDate();
				case TIME:
					return date.toLocalTime();
				case TIMESTAMP:
					return date;
				default:
					return date.toLocalDate();
				}
			} else {
				double value = cell.getNumericCellValue();
				if (field.getFieldType() == FieldTypes.FLOAT) {
					return value;
				} else {
					return (int) value;
				}
			}
		case BOOLEAN:
			return cell.getBooleanCellValue();
		case FORMULA:
			return cell.getCellFormula();
		default:
			return cell.getRichStringCellValue().getString();
		}
	}

	@Override
	public Map<String, Object> readRecord() {
		List<Map<String, Object>> records = hRecords.get(sheetName);
		return records.get(currentRecord++);
	}

	@Override
	public void closeFile() {
		try {
			if (!isInputFile) {
				OutputStream fileOut = new FileOutputStream(outFile);
				wb.write(fileOut);
				fileOut.close();
			}
		} catch (Exception e) {
			// Nothing that can be done about this
		}
	}

	protected void createNextSheet() {
		int numOfSheets = wb.getNumberOfSheets();
		sheet = wb.createSheet("Page " + (numOfSheets + 1));
		sheetName = sheet.getSheetName();
	}

	@Override
	public void closeData() throws Exception {
		Row row1 = sheet.getRow(0);
		if (row1 == null) {
			return;
		}

		for (int i = 0; i < row1.getLastCellNum(); i++) {
			sheet.autoSizeColumn(i);
		}
	}
}
