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
import java.util.List;
import java.util.Map;

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

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;

public abstract class ExcelFile extends GeneralDB implements IConvert {
	/**
	 * Title: ExcelFile Description: Generic abstract class for writing Excel Files
	 * Copyright: (c) 2003-2019
	 *
	 * @author Tom van Breukelen
	 * @version 8+
	 */
	private File backupFile;
	private Map<String, List<FieldDefinition>> hSheets;
	private int myCurrentRecord = 1;
	protected int noOfSheets;

	private File outFile;
	private DataFormatter formatter = new DataFormatter();

	protected Workbook wb;
	protected Sheet sheet;
	protected CreationHelper helper;

	protected int maxRowsInSheet;

	public ExcelFile(Profiles pref) {
		super(pref);
	}

	@Override
	public void openFile(boolean createBackup, boolean isInputFile) throws Exception {
		hasBackup = false;

		if (createBackup) {
			hasBackup = General.copyFile(myFilename, myFilename + ".bak");
		}

		outFile = new File(myFilename);
		backupFile = new File(myFilename + ".bak");

		this.isInputFile = isInputFile;
		if (isInputFile) {
			InputStream fileIn = new FileInputStream(outFile);
			wb = WorkbookFactory.create(fileIn);
			noOfSheets = wb.getNumberOfSheets();

			// Read all sheets in the workbook
			hSheets = new HashMap<>(noOfSheets);
			for (int i = 0; i < noOfSheets; i++) {
				// Get TableModelFields for each sheet and put them in the HashMap
				sheet = wb.getSheetAt(i);
				List<FieldDefinition> temp = getDBFieldNamesAndTypes();
				if (!temp.isEmpty()) {
					hSheets.put(sheet.getSheetName(), temp);
				}
			}
		} else {
			boolean isXlsx = myFilename.toLowerCase().endsWith("x");
			wb = isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
			String sheetName = myPref.getPdaDatabaseName();
			sheet = wb.createSheet(sheetName.isEmpty() ? "Sheet1" : sheetName);
			maxRowsInSheet = isXlsx ? 1048576 : 65536;
		}
		helper = wb.getCreationHelper();
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		return hSheets.get(getCurrentSheet());
	}

	@Override
	public String getPdaDatabase() {
		return sheet == null ? null : sheet.getSheetName();
	}

	@Override
	public List<String> getSheetNames() {
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

		sheet = wb.getSheetAt(0);
		return sheet.getSheetName();
	}

	private List<FieldDefinition> getDBFieldNamesAndTypes() {
		dbFieldNames.clear();

		if (sheet.getLastRowNum() < 2) {
			return new ArrayList<>();
		}

		Row names = sheet.getRow(0); // Assumes that the 1st row contains the fieldnames
		for (Cell cell : names) {
			dbFieldNames.add(cell.getRichStringCellValue().getString());
		}

		Row types = sheet.getRow(1); // Assumes that the 2nd row contains ALL field values
		final int index = dbFieldNames.size();
		List<FieldDefinition> result = new ArrayList<>();

		for (int i = 0; i < index; i++) {
			Cell cell = types.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			String name = dbFieldNames.get(i);
			result.add(new FieldDefinition(name, name, cell == null ? FieldTypes.TEXT : getFieldType(cell)));
		}

		return result;
	}

	protected void setFreeze(int row, int col) {
		sheet.createFreezePane(col, row, col, row);
	}

	private FieldTypes getFieldType(Cell pCell) {
		switch (pCell.getCellType()) {
		case BOOLEAN:
			return FieldTypes.BOOLEAN;
		case BLANK:
		case STRING:
			String contents = pCell.getRichStringCellValue().getString();
			if (contents.length() > ExportFile.EXCEL.getMaxTextSize() || contents.indexOf('\n') > -1) {
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
	public void verifyDatabase(List<FieldDefinition> newFields) throws Exception {
		getCurrentSheet();
		if (sheet == null) {
			throw FNProgException.getException("noSheets", myFilename);
		}

		myTotalRecords = sheet.getLastRowNum() + 1; // Rows start with Row number 0
		if (myTotalRecords < 2) {
			throw FNProgException.getException("noRecordsInSheet", sheet.getSheetName(), myFilename);
		}
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		Row row = sheet.getRow(myCurrentRecord++);
		if (row == null) {
			// File is corrupt
			return result;
		}

		List<FieldDefinition> dbDef = getTableModelFields();
		final int MAX = dbDef.size();
		int index = 0;

		for (Cell cell : row) {
			index = cell.getColumnIndex();
			if (index > MAX) {
				break;
			}

			FieldDefinition field = dbDef.get(index);

			switch (cell.getCellType()) {
			case STRING:
				result.put(field.getFieldAlias(), cell.getRichStringCellValue().getString());
				break;
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					LocalDateTime date = new Timestamp(cell.getDateCellValue().getTime()).toLocalDateTime();

					switch (field.getFieldType()) {
					case DATE:
						result.put(field.getFieldAlias(), date.toLocalDate());
						break;
					case TIME:
						result.put(field.getFieldAlias(), date.toLocalTime());
						break;
					case TIMESTAMP:
						result.put(field.getFieldAlias(), date);
						break;
					default:
						result.put(field.getFieldAlias(), date.toLocalDate());
					}
				} else {
					double value = cell.getNumericCellValue();
					if (field.getFieldType() == FieldTypes.FLOAT) {
						result.put(field.getFieldAlias(), value);
					} else {
						result.put(field.getFieldAlias(), (int) value);
					}
				}
				break;
			case BOOLEAN:
				result.put(field.getFieldAlias(), cell.getBooleanCellValue());
				break;
			case FORMULA:
				result.put(field.getFieldAlias(), cell.getCellFormula());
				break;
			default:
				result.put(field.getFieldAlias(), cell.getRichStringCellValue().getString());
			}

		}
		return result;
	}

	@Override
	public void deleteFile() {
		closeFile();
		if (outFile.exists()) {
			outFile.delete();
		}
		if (hasBackup) {
			backupFile.renameTo(outFile);
		}
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

	@Override
	public abstract void createDbHeader() throws Exception;

	@Override
	public abstract void processData(Map<String, Object> data) throws Exception;
}
