package dbengine.export;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.PalmDB;

public class ListDB extends PalmDB {
	/**
	 * Title: ListDB Description: Generic Class for the Palm-OS List database
	 * Copyright: (c) 2003-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private List<String> catList;

	public ListDB(Profiles pref) {
		super(pref);
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		int[] recordID = setPointer2NextRecord();
		int recordLength = recordID[2] - recordID[0];

		byte[] bytes = new byte[recordLength];
		int categoryID = recordID[1] >> 24;
		if (categoryID > 63) {
			categoryID -= 64;
		}

		readLn(bytes);

		String s = General.convertByteArrayToString(bytes, null).substring(3);
		String[] rec = new String[4];
		Arrays.fill(rec, General.EMPTY_STRING);

		for (int i = 0; i < 3; i++) {
			int index = s.indexOf('\0');
			if (index == -1) {
				break;
			}
			rec[i] = s.substring(0, index);
			s = s.substring(index + 1);
		}

		if (!catList.isEmpty()) {
			rec[3] = catList.get(categoryID > catList.size() ? 0 : categoryID);
		}

		List<FieldDefinition> dbDef = getTableModelFields();
		int i = 0;
		for (FieldDefinition field : dbDef) {
			result.put(field.getFieldAlias(), rec[i++]);
		}
		return result;
	}

	private void getCategories() throws Exception {
		byte[] cat = new byte[16 * 16];
		readAppInfo(2, cat);
		catList = General.splitNullTerminatedString(cat, 16);
	}

	@Override
	protected void readAppInfo() throws Exception {
		byte[] fld = new byte[2 * 16];
		readAppInfo(16 * 17 + 6, fld);
		List<String> tmp = General.splitNullTerminatedString(fld, 16);

		dbFieldNames.clear();
		dbFieldNames.add(tmp.isEmpty() ? General.EMPTY_STRING : tmp.get(0)); // Field 1
		dbFieldNames.add(tmp.size() > 1 ? tmp.get(1) : General.EMPTY_STRING); // Field 2
		dbFieldNames.add("Notes"); // Notes

		dbFieldTypes.clear();
		dbFieldTypes.add(FieldTypes.TEXT); // Field 1
		dbFieldTypes.add(FieldTypes.TEXT); // Field 2
		dbFieldTypes.add(FieldTypes.MEMO); // Notes

		if (catList == null) {
			getCategories();
		}

		if (catList.size() > 1) {
			dbFieldNames.add("Category");
			dbFieldTypes.add(FieldTypes.TEXT);
		}
	}
}