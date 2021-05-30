package dbengine.export;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.miachm.sods.Range;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
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
			cell.setValue(convertDataFields(dbRecord.get(field.getFieldAlias()), field));
		}
	}

	@Override
	protected Object convertDate(Object dbValue, FieldDefinition field) {
		return field.isOutputAsText() ? super.convertDate(dbValue, field) : dbValue;
	}

	@Override
	protected Object convertDuration(Object dbValue, FieldDefinition field) {
		return field.isOutputAsText() ? super.convertDuration(dbValue, field) : dbValue;
	}

	@Override
	protected Object convertTime(Object dbValue, FieldDefinition field) {
		return field.isOutputAsText() ? super.convertTime(dbValue, field) : dbValue;
	}

	@Override
	protected Object convertTimestamp(Object dbValue, FieldDefinition field) {
		return field.isOutputAsText() ? super.convertTimestamp(dbValue, field) : dbValue;
	}
}
