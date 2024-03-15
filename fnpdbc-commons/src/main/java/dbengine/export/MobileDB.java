package dbengine.export;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.PalmDB;

public class MobileDB extends PalmDB {
	/**
	 * Title: MobileDB Description: Generic Class for Palm-OS MobileDB Copyright:
	 * (c) 2003-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private int numFields;

	public MobileDB(Profiles pref) {
		super(pref);
		dbRecords.clear();
	}

	@Override
	public void readTableContents() throws Exception {
		super.readTableContents();
		List<FieldDefinition> dbDef = getTableModelFields();

		for (int index = 0; index < totalRecords; index++) {
			List<String> fields = getRecordFields(null);
			Map<String, Object> result = new HashMap<>();

			for (int i = 0; i < numFields; i++) {
				FieldDefinition field = dbDef.get(i);
				Object dbValue = fields.get(i);

				switch (field.getFieldType()) {
				case BOOLEAN:
					dbValue = dbValue.equals("true") || dbValue.equals("ja") || dbValue.equals("yes")
							|| dbValue.equals("1");
					break;
				case DATE:
					dbValue = convertDate2DB(dbValue.toString());
					break;
				case TIME:
					dbValue = General.convertTime2DB(dbValue.toString(), General.sdInternalTime);
					break;
				default:
					break;
				}
				result.put(field.getFieldAlias(), dbValue);
			}
			dbRecords.add(result);
		}
		setFieldSizes();
	}

	/**
	 * Converts a MobileDB date back to a LocalDate
	 */
	private LocalDate convertDate2DB(String pDate) {
		if (pDate == null || pDate.trim().length() == 0) {
			return null;
		}

		int palmDate = Integer.parseInt(pDate.trim());
		return LocalDate.of(1904, 1, 1).plusDays(palmDate);
	}

	@Override
	protected void readAppInfo() throws Exception {
		// Need to find the 1st record containing the FieldNames
		gotoRecord(0);
		int[] recordID = setPointer2NextRecord();

		while (Integer.toHexString(recordID[1]).charAt(0) != '1') {
			recordID = setPointer2NextRecord();
		}
		dbFieldNames = getRecordFields(recordID);

		// remove empty field names
		Iterator<String> iter = dbFieldNames.iterator();
		while (iter.hasNext()) {
			if (iter.next().equals(General.EMPTY_STRING)) {
				iter.remove();
			}
		}
		numFields = dbFieldNames.size();

		// Need to find the 5th record containing the FieldTypes
		gotoRecord(0);
		recordID = setPointer2NextRecord();
		while (Integer.toHexString(recordID[1]).charAt(0) != '5') {
			recordID = setPointer2NextRecord();
		}

		dbFieldTypes.clear();
		List<String> fields = getRecordFields(recordID);
		for (String element : fields) {
			element += General.SPACE;
			switch (element.charAt(0)) {
			case 'B':
				dbFieldTypes.add(FieldTypes.BOOLEAN);
				break;
			case 'D':
				dbFieldTypes.add(FieldTypes.DATE);
				break;
			case 'd':
				dbFieldTypes.add(FieldTypes.TIME);
				break;
			default:
				dbFieldTypes.add(FieldTypes.TEXT);
			}
		}
		gotoRecord(4);
	}

	private List<String> getRecordFields(int[] pRecordID) throws Exception {
		List<String> result = new ArrayList<>();
		int[] recordID = pRecordID == null ? setPointer2NextRecord() : pRecordID;
		byte[] bytes = new byte[recordID[2] - (recordID[0] + 7)];
		skipBytes(6);
		readLn(bytes);
		String s = General.convertByteArrayToString(bytes, null);

		final int MAX = s.length();
		int[] index = new int[2];
		index[1] = 2;

		while (index[1] < MAX) {
			index[0] = index[1];
			index[1] = s.indexOf('\0', index[0]);
			result.add(s.substring(index[0], index[1]));
			index[1] += 2;
		}
		return result;
	}
}