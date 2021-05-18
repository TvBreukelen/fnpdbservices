package dbengine.export;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.miachm.sods.Range;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.OdsFile;

public class Calc extends OdsFile {
	private int calcRow = 0;

	public Calc(Profiles pref) {
		super(pref);
	}

	@Override
	public void createDbHeader() throws Exception {
		createNewSheet(myTotalRecords, dbInfo2Write.size());

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
			Object dbField = dbRecord.get(field.getFieldAlias());
			if (dbField == null || dbField.equals("")) {
				cell.setValue("");
				continue;
			}

			String dbValue = dbField.toString();
			switch (field.getFieldType()) {
			case BOOLEAN:
				boolean b = (Boolean) dbField;
				if (field.isOutputAsText()) {
					cell.setValue(b ? getBooleanTrue() : getBooleanFalse());
				} else {
					cell.setValue(b);
				}
				break;
			case DATE:
				if (field.isOutputAsText()) {
					cell.setValue(General.convertDate(dbValue));
				} else {
					LocalDate date = General.convertDB2Date(dbValue);
					cell.setValue(date == null ? dbValue : date);
				}
				break;
			case FUSSY_DATE:
				cell.setValue(General.convertFussyDate(dbValue));
				break;
			case FLOAT:
			case NUMBER:
				cell.setValue(dbField);
				break;
			case TIME:
				cell.setValue(field.isOutputAsText() ? General.convertTime(dbValue)
						: Duration.ofSeconds(Long.parseLong(dbValue)));
				break;
			case DURATION:
				cell.setValue(General.convertDuration((Number) dbField));
				break;
			default:
				// Change Tabs to spaces, remove carriage returns and the trailing line feed
				// char
				dbValue = dbValue.replace("\t", "  ");
				dbValue = dbValue.replace("\r", "");
				if (dbValue.endsWith("\n")) {
					dbValue = dbValue.substring(0, dbValue.length() - 1);
				}
				cell.setValue(dbValue);
			}
		}
	}
}
