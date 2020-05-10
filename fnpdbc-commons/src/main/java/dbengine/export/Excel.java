package dbengine.export;

import java.time.LocalDate;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.ExcelFile;

public class Excel extends ExcelFile {
	/**
	 * Title: Excel Description: Generic Class for MS-Excel tables Copyright: (c)
	 * 2004-2016
	 *
	 * @author Tom van Breukelen
	 * @version 8+
	 */
	private int excelRow = 0;
	private CellStyle format;
	private CellStyle formatDate;
	private CellStyle formatTime;

	public Excel(Profiles pref) {
		super(pref);
	}

	@Override
	public void createDbHeader() throws Exception {
		String font = myPref.getFont();
		int fontSize = myPref.getFontSize();
		boolean useHeader = myPref.isUseHeader();
		boolean lockHeader = myPref.isLockHeader();
		boolean lock1stCol = myPref.isLock1stColumn();

		Font normal = wb.createFont();
		normal.setFontName(font);
		normal.setFontHeightInPoints((short) fontSize);

		Font bold = wb.createFont();
		bold.setFontName(font);
		bold.setBold(true);
		bold.setFontHeightInPoints((short) fontSize);

		CellStyle formatHeader = wb.createCellStyle();
		if (myPref.isBoldHeader()) {
			formatHeader.setFont(bold);
		}

		format = wb.createCellStyle();
		format.setFont(normal);
		format.setWrapText(true);
		format.setVerticalAlignment(VerticalAlignment.TOP);

		formatDate = wb.createCellStyle();
		formatDate.setDataFormat(helper.createDataFormat().getFormat(General.getDateFormat()));

		formatTime = wb.createCellStyle();
		formatTime.setDataFormat(helper.createDataFormat().getFormat(General.getTimeFormat()));

		excelRow = 0;

		if (useHeader) {
			// Read the user defined list of DB fields
			Row row = sheet.createRow(excelRow++);
			int i = 0;
			for (FieldDefinition field : dbInfo2Write) {
				Cell cell = row.createCell(i++);
				cell.setCellStyle(formatHeader);
				cell.setCellValue(field.getFieldHeader());
			}

			if (lockHeader) {
				setFreeze(1, lock1stCol ? 1 : 0);
			}
			excelRow = 1;
		} else if (lock1stCol) {
			setFreeze(0, 1);
		}
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		int excelCol = 0;
		String dbValue;

		// Read the user defined list of DB fields
		Row row = sheet.createRow(excelRow);
		for (FieldDefinition field : dbInfo2Write) {
			Object dbField = dbRecord.get(field.getFieldAlias());
			if (dbField == null || dbField.equals("")) {
				// Skip current column
				excelCol++;
				continue;
			}

			dbValue = dbField.toString();
			Cell cell = row.createCell(excelCol++);
			cell.setCellStyle(format);

			switch (field.getFieldType()) {
			case BOOLEAN:
				boolean b = (Boolean) dbField;
				if (field.isOutputAsText()) {
					cell.setCellValue(b ? getBooleanTrue() : getBooleanFalse());
				} else {
					cell.setCellValue(b);
				}
				break;
			case DATE:
				if (field.isOutputAsText()) {
					cell.setCellValue(General.convertDate(dbValue));
				} else {
					LocalDate date = General.convertDB2Date(dbValue);
					if (date == null) {
						cell.setCellValue(dbValue);
					} else {
						cell.setCellStyle(formatDate);
						cell.setCellValue(date);
					}
				}
				break;
			case FUSSY_DATE:
				cell.setCellValue(General.convertFussyDate(dbValue));
				break;
			case FLOAT:
				cell.setCellValue((Double) dbField);
				break;
			case NUMBER:
				cell.setCellValue((Integer) dbField);
				break;
			case TIME:
				String timeStr = General.convertTime(dbValue);
				if (field.isOutputAsText()) {
					cell.setCellValue(timeStr);
				} else {
					cell.setCellStyle(formatTime);
					cell.setCellValue(DateUtil.convertTime(timeStr));
				}
				break;
			case DURATION:
				cell.setCellValue(General.convertDuration((Number) dbField));
				break;
			default:
				// Change Tabs to spaces, remove carriage returns and the trailing line feed
				// char
				dbValue = dbValue.replaceAll("\t", "  ");
				dbValue = dbValue.replaceAll("\r", "");
				if (dbValue.endsWith("\n")) {
					dbValue = dbValue.substring(0, dbValue.length() - 1);
				}
				cell.setCellValue(dbValue);
			}
		}

		if (excelRow > maxRowsInSheet) {
			createNextSheet();
		} else {
			excelRow++;
		}
	}
}