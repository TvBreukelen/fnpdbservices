package dbengine.export;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	private CellStyle formatTimestamp;

	public Excel(Profiles pref) {
		super(pref);
	}

	@Override
	public void createDbHeader() throws Exception {
		String font = myPref.getFont();
		int fontSize = myPref.getFontSize();

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

		formatTimestamp = wb.createCellStyle();
		formatTimestamp.setDataFormat(helper.createDataFormat().getFormat(General.getTimestampFormat()));
		excelRow = 0;

		if (myPref.isUseHeader()) {
			// Read the user defined list of DB fields
			Row row = sheet.createRow(excelRow++);
			int i = 0;
			for (FieldDefinition field : dbInfo2Write) {
				Cell cell = row.createCell(i++);
				cell.setCellStyle(formatHeader);
				cell.setCellValue(field.getFieldHeader());
			}

			if (myPref.isLockHeader()) {
				setFreeze(1, myPref.isLock1stColumn() ? 1 : 0);
			}
			excelRow = 1;
		} else if (myPref.isLock1stColumn()) {
			setFreeze(0, 1);
		}
	}

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		int excelCol = 0;

		// Read the user defined list of DB fields
		Row row = sheet.createRow(excelRow);
		for (FieldDefinition field : dbInfo2Write) {
			Object dbValue = dbRecord.get(field.getFieldAlias());
			if (dbValue == null || dbValue.equals(General.EMPTY_STRING)) {
				// Skip current column
				excelCol++;
				continue;
			}

			Cell cell = row.createCell(excelCol++);
			cell.setCellStyle(format);

			switch (field.getFieldType()) {
			case BOOLEAN:
				boolean b = (Boolean) dbValue;
				if (field.isOutputAsText()) {
					cell.setCellValue(b ? getBooleanTrue() : getBooleanFalse());
				} else {
					cell.setCellValue(b);
				}
				break;
			case DATE:
				if (field.isOutputAsText()) {
					cell.setCellValue(convertDate(dbValue, field).toString());
				} else {
					cell.setCellStyle(formatDate);
					cell.setCellValue((LocalDate) dbValue);
				}
				break;
			case FUSSY_DATE:
				cell.setCellValue(General.convertFussyDate(dbValue.toString()));
				break;
			case FLOAT:
				cell.setCellValue((Double) dbValue);
				break;
			case NUMBER:
				cell.setCellValue((Integer) dbValue);
				break;
			case TIME:
				String timeStr = convertTime(dbValue, field).toString();
				if (field.isOutputAsText()) {
					cell.setCellValue(timeStr);
				} else {
					cell.setCellStyle(formatTime);
					cell.setCellValue(DateUtil.convertTime(timeStr));
				}
				break;
			case TIMESTAMP:
				if (field.isOutputAsText()) {
					cell.setCellValue(convertTimestamp(dbValue, field).toString());
				} else {
					cell.setCellStyle(formatTimestamp);
					cell.setCellValue((LocalDateTime) dbValue);
				}
				break;
			case DURATION:
				cell.setCellValue(General.convertDuration((Duration) dbValue));
				break;
			default:
				cell.setCellValue(convertString(dbValue));
			}
		}

		if (excelRow > maxRowsInSheet) {
			createNextSheet();
		} else {
			excelRow++;
		}

		return 1;
	}
}