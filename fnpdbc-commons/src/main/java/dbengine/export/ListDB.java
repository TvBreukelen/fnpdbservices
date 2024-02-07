package dbengine.export;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

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
	private String[] listFields;
	private List<String> catList;
	private boolean useCategory;
	private int maxFields;
	private int[] fieldNum;
	private static final String DIVIDER = "\n----------------------------------------\n";

	public ListDB(Profiles pref) {
		super(pref);
	}

	@Override
	public void createDbHeader() throws Exception {
		int totalRecords = mySoft.getTotalRecords();
		String[] header;

		listFields = new String[3];
		listFields[0] = myPref.getSortField(0);
		listFields[1] = myPref.getSortField(1);
		listFields[2] = myPref.getCategoryField();

		header = listFields.clone();
		useCategory = !listFields[2].equals(General.EMPTY_STRING);
		maxFields = dbInfo2Write.size();

		// Prepare list of fieldnumbers to write from the userlist that are not Field1,
		// Field2 or Category
		List<Integer> tmp = new ArrayList<>(maxFields);

		int index = -1;
		for (FieldDefinition field : dbInfo2Write) {
			index++;
			boolean skipField = false;
			if (field.isExport()) {
				for (int i = 0; i < 3; i++) {
					if (field.getFieldAlias().equals(listFields[i])) {
						header[i] = field.getFieldHeader();
						skipField = true;
						break;
					}
				}

				if (skipField) {
					continue;
				}

				tmp.add(index);
			}
		}

		maxFields = tmp.size();
		fieldNum = new int[maxFields];
		for (int i = 0; i < maxFields; i++) {
			fieldNum[i] = tmp.get(i);
		}

		// Check whether we are in append mode
		if (useAppend) {
			updateCategoriesFromFile();
			appendPilotDB(totalRecords, null);
			return;
		}

		byte[] filler = new byte[16];

		// Create Pilot Record Database Format
		createPalmDB(totalRecords);

		// ApplicationInfo
		pdbRaf.writeShort(14); // unsigned int renamedCategories

		// Category List
		catList = mySoft.getCategories();
		int catSize = catList.size();
		for (int i = 0; i < catSize; i++) {
			pdbRaf.write(General.getNullTerminatedString(catList.get(i), null, 16));
		}

		for (int i = catSize; i < 16; i++) {
			pdbRaf.write(filler);
		}

		// byte[16] categoryUniqIDs
		for (int i = 0; i < 16; ++i) {
			pdbRaf.writeByte(i);
		}
		pdbRaf.writeByte(16); // byte lastUniqID

		// Custom FieldTypes
		pdbRaf.writeByte(128); // Display style
		pdbRaf.writeByte(0); // Write protect
		pdbRaf.writeByte(catSize - 1); // lastCategory used
		pdbRaf.write(General.getNullTerminatedString(header[0], null, 16));
		pdbRaf.write(General.getNullTerminatedString(header[1], null, 16));
		pdbRaf.write(new byte[202]); // padding
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

	@Override
	public int processData(Map<String, Object> dbRecord) throws Exception {
		StringBuilder sb = new StringBuilder();

		// For list we first obtain the category, datafield1 & 2
		String fieldData1 = dbRecord.containsKey(listFields[0]) ? dbRecord.get(listFields[0]).toString()
				: General.EMPTY_STRING;
		String fieldData2 = dbRecord.containsKey(listFields[1]) ? dbRecord.get(listFields[1]).toString()
				: General.EMPTY_STRING;
		String category = dbRecord.containsKey(listFields[2]) ? dbRecord.get(listFields[2]).toString()
				: General.EMPTY_STRING;

		int categoryID = getCategoryID(category);
		if (categoryID == 0) {
			// Didn't find the category or category = Unfiled
			category = General.EMPTY_STRING;
		}

		// Then we have to "assemble" the Notes field
		int notesSize = myExportFile.getMaxMemoSize() - (category.length() + fieldData1.length() + fieldData2.length());

		// Read the user defined list of DB fields
		for (int i = 0; i < maxFields; i++) {
			FieldDefinition field = dbInfo2Write.get(fieldNum[i]);
			String dbField = convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString();
			sb.append(format2Line(dbField, field.getFieldHeader(), field.getFieldType()));
		}

		if (sb.length() > 0) {
			// Check whether the Notes field is not too large
			if (sb.length() > notesSize) {
				// Truncate Notes field
				sb.delete(notesSize - 14, sb.length());
				sb.append("\ntrunctated...");
			}
		} else {
			sb.append(General.SPACE);
		}

		// Now we can buildup the record to be written to the Export file
		byte[] bData1 = General.convertStringToByteArray(fieldData1, null);
		byte[] bData2 = General.convertStringToByteArray(fieldData2, null);

		pdbDas.write(3); // Offset from start for field1
		pdbDas.write(3 + bData1.length + 1); // offset from start for field2
		pdbDas.write(3 + bData1.length + bData2.length + 2); // offset from start for notes
		pdbDas.write(bData1);
		pdbDas.write(0);
		pdbDas.write(bData2);
		pdbDas.write(0);
		pdbDas.write(General.convertStringToByteArray(sb.toString(), null));
		pdbDas.write(0);

		writeRecord(pdbBaos.toByteArray(), categoryID);
		pdbBaos.reset();
		return 1;
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

	private void updateCategoriesFromFile() throws Exception {
		boolean has2Update = false;
		readTableContents();
		getCategories();
		int catSize = catList.size();

		if (useCategory) {
			String s = null;
			if (catSize == 16) {
				// No more categories can be added
				return;
			}

			// Append latest Categories to the list
			List<String> newCat = mySoft.getCategories();
			if (CollectionUtils.isEmpty(newCat)) {
				// There are no categories to be added
				return;
			}

			for (int i = 0; i < newCat.size() && catSize < 16; i++) {
				s = newCat.get(i);
				if (s.length() > 15) {
					// Category Length must be < 16
					s = s.substring(0, 15);
				}
				if (!catList.contains(s)) {
					catList.add(s);
					has2Update = true;
					catSize++;
				}
			}
		} else {
			// User doesn't want to use Categories
			if (catSize > 0) {
				catList.clear();
				catSize = 0;
				has2Update = true;
			}
		}

		if (has2Update) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			byte[] filler = new byte[16];

			for (int i = 0; i < catSize; i++) {
				out.write(General.getNullTerminatedString(catList.get(i), null, 16));
			}

			for (int i = catSize; i < 16; i++) {
				out.write(filler);
			}

			// Update ApplicationInfoArea
			writeAppInfo(2, bytes.toByteArray());
		}
	}

	private int getCategoryID(String pCategory) {
		int result;
		if (!useCategory || pCategory.length() == 0 || catList.size() < 2) {
			return 0;
		}

		if (pCategory.length() > 15) {
			// Category Length must be < 16
			pCategory = pCategory.substring(0, 15);
		}

		result = catList.indexOf(pCategory);
		if (result < 0) {
			return 0;
		}
		return result << 24;
	}

	private String format2Line(String pValue, String pHeader, FieldTypes pType) {
		if (pValue == null || pValue.length() == 0) {
			return General.EMPTY_STRING;
		}

		StringBuilder result = new StringBuilder(General.EMPTY_STRING);
		if (pType == FieldTypes.MEMO) {
			result.append("\n" + pHeader + DIVIDER + pValue + DIVIDER + "\n");
		} else {
			result.append(pHeader + ": " + pValue + "\n");
		}
		return result.toString();
	}
}