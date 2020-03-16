package dbengine.export;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import dbengine.ExcelFile;

public class Referencer extends ExcelFile {
	/**
	 * Title: Referencer Description: Generic Class for the Casio Pocketviewer
	 * P.Referencer database Copyright: (c) 2003-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8+
	 */
	private String[] refFields = new String[4];

	private String oldChapter;
	private String oldParagraph;

	private boolean isMoreThanOneLine = false;
	private boolean isTitlePrinted = false;

	private String paragraphHeader = "";
	private String titleHeader = "";

	private CellStyle chapterFormat;
	private CellStyle paragraphFormat;
	private CellStyle memoFormat;
	private CellStyle bmpFormat;

	private int excelRow = 0;
	private List<Integer> userFields = new ArrayList<>();

	public Referencer(Profiles pref) {
		super(pref);
	}

	@Override
	public void createDbHeader() throws Exception {
		refFields[0] = myPref.getSortField(0);
		refFields[1] = myPref.getSortField(1);
		refFields[2] = myPref.getSortField(2);
		refFields[3] = myPref.getSortField(3);

		Font bold = wb.createFont();
		Font normal = wb.createFont();
		Font red = wb.createFont();

		bold.setBold(true);
		red.setColor(Font.COLOR_RED);

		chapterFormat = wb.createCellStyle();
		chapterFormat.setFont(bold);
		paragraphFormat = wb.createCellStyle();
		paragraphFormat.setFont(bold);
		paragraphFormat.setBorderBottom(BorderStyle.THIN);
		paragraphFormat.setBorderLeft(BorderStyle.THIN);
		paragraphFormat.setBorderRight(BorderStyle.THIN);
		paragraphFormat.setBorderTop(BorderStyle.THIN);
		memoFormat = wb.createCellStyle();
		memoFormat.setFont(normal);
		memoFormat.setWrapText(true);
		bmpFormat = wb.createCellStyle();
		bmpFormat.setFont(red);

		excelRow = 0;

		boolean useParagraphHeader = myPref.isUseParagraphHeader() && !refFields[1].isEmpty();
		boolean useTitleHeader = myPref.isUseTitleHeader() && !refFields[2].isEmpty();

		// Prepare list of fieldnumbers to write from the userlist that are not header,
		// paragraph or title
		int maxFields = dbInfo2Write.size();
		List<String> header = new ArrayList<>();
		for (int i = 0; i < maxFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);
			boolean found = false;
			for (String ref : refFields) {
				if (ref.equals(field.getFieldAlias())) {
					header.add(field.getFieldHeader().isEmpty() ? "" : field.getFieldHeader() + ": ");
					found = true;
					break;
				}
			}

			if (!found) {
				userFields.add(i);
			}
		}

		if (useParagraphHeader && header.size() > 1) {
			paragraphHeader = header.get(1);
		}

		if (useTitleHeader && header.size() > 2) {
			titleHeader = header.get(2);
		}
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		getChapterParagraphAndTitle(dbRecord);
		final String DOTS = "------------------------------";

		// Read the user defined list of DB fields
		Row row = sheet.createRow(excelRow++);
		int excelCell = 0;

		for (int i : userFields) {
			FieldDefinition field = dbInfo2Write.get(i);
			String dbField = convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString();
			if (dbField == null || dbField.length() == 0) {
				continue;
			}
			isMoreThanOneLine = isTitlePrinted;

			// Change Tabs to spaces, remove carriage returns
			dbField = dbField.replaceAll("\t", "  ");
			dbField = dbField.replaceAll("\r", "");
			if (dbField.endsWith("\n")) {
				dbField = dbField.substring(0, dbField.length() - 1);
			}

			Cell cell = row.createCell(excelCell++);

			switch (field.getFieldType()) {
			case IMAGE:
				if (useImages) {
					// Put the image filename in Red
					cell.setCellValue(dbField);
					cell.setCellStyle(bmpFormat);
				} else {
					String value = field.getFieldHeader() + ": " + dbField;
					cell.setCellValue(value);
				}
				break;
			case MEMO:
				String[] tokens = dbField.split("\n");
				int count = tokens.length;
				if (count == 1) {
					String value = field.getFieldHeader() + ": " + dbField;
					cell.setCellValue(value);
					cell.setCellStyle(memoFormat);
				} else {
					excelCell--;
					row = sheet.createRow(excelRow++);
					cell = row.createCell(excelCell);
					cell.setCellValue(DOTS);
					cell.setCellStyle(memoFormat);
					row = sheet.createRow(excelRow++);
					cell = row.createCell(excelCell);
					cell.setCellValue(field.getFieldHeader());
					cell.setCellStyle(memoFormat);
					row = sheet.createRow(excelRow++);
					cell = row.createCell(excelCell);
					cell.setCellValue(DOTS);
					cell.setCellStyle(memoFormat);
					for (int j = 0; j < count; j++) {
						row = sheet.createRow(excelRow++);
						cell = row.createCell(excelCell);
						cell.setCellValue(tokens[j]);
						cell.setCellStyle(memoFormat);
					}
					row = sheet.createRow(excelRow++);
					cell = row.createCell(excelCell++);
					cell.setCellValue("------------------------------");
					cell.setCellStyle(memoFormat);
				}
				break;
			default:
				String value = field.getFieldHeader() + ": " + dbField;
				cell.setCellValue(value);
			}
			isTitlePrinted = true;
		}

		if (excelRow > ExcelFile.MAX_ROWS_IN_SHEET) {
			createNextSheet();
		}
	}

	private void getChapterParagraphAndTitle(Map<String, Object> dbRecord) {
		String chapter = dbRecord.containsKey(refFields[0]) ? dbRecord.get(refFields[0]).toString() : "";
		String paragraph = dbRecord.containsKey(refFields[1]) ? dbRecord.get(refFields[1]).toString() : "";
		String title1 = dbRecord.containsKey(refFields[2]) ? dbRecord.get(refFields[2]).toString() : "";
		String title2 = dbRecord.containsKey(refFields[3]) ? dbRecord.get(refFields[3]).toString() : "";

		if (chapter.isEmpty()) {
			chapter = "- None -";
		}

		Row row = sheet.createRow(excelRow++);
		boolean isOldChapter = chapter.equals(oldChapter);
		if (isOldChapter) {
			if (isMoreThanOneLine) {
				// Skip one row
				excelRow++;
			}
		} else {
			Cell cell = row.createCell(0);
			cell.setCellValue(chapter);
			cell.setCellStyle(chapterFormat);
			oldParagraph = "";
			oldChapter = chapter;
		}

		boolean isOldParagraph = paragraph == null || paragraph.equals(oldParagraph);

		if (!isOldParagraph && !paragraph.isEmpty()) {
			if (isOldChapter && !isMoreThanOneLine) {
				// Skip one row
				row = sheet.createRow(excelRow++);
			}

			Cell cell = row.createCell(1);
			cell.setCellValue(paragraphHeader + paragraph);
			cell.setCellStyle(paragraphFormat);
			oldParagraph = paragraph;
		}

		if (title1.isEmpty()) {
			isTitlePrinted = false;
		} else {
			StringBuilder bf = new StringBuilder(titleHeader);
			bf.append(title1);
			if (!title2.isEmpty() && !title1.equalsIgnoreCase(title2)) {
				bf.append(" (" + title2 + ")");
			}
			Cell cell = row.createCell(1);
			cell.setCellValue(bf.toString());
			isTitlePrinted = true;
		}
		isMoreThanOneLine = false;
	}

	@Override
	public void closeData() throws Exception {
		// Write closing line to the Excel spreadsheet
		Row row = sheet.createRow(excelRow);
		Cell cell = row.createCell(0);
		cell.setCellValue("###END");
		super.closeData();
	}
}