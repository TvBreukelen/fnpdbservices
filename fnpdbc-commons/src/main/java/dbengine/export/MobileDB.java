package dbengine.export;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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
	private static final byte[] RECORD_HEADER = new byte[] { -1, -1, -1, 1, -1, 0 };
	private static final byte[] RECORD_FOOTER = new byte[] { 0, -1 };

	private int numFields;

	public MobileDB(Profiles pref) {
		super(pref);
	}

	@Override
	public void createDbHeader() throws Exception {
		int totalRecords = mySoft.getTotalRecords();

		// Check whether we are in append mode
		if (useAppend) {
			appendPilotDB(totalRecords, dbInfo2Write);
			return;
		}

		numFields = dbInfo2Write.size();
		String[] fieldTypes = new String[myExportFile.getMaxFields()];

		Arrays.fill(fieldTypes, "T"); // Set Default Field Type to Normal Text

		// Create Pilot Record Database Format
		createPalmDB(totalRecords + 4);

		// ApplicationInfo
		pdbRaf.writeShort(0); // unsigned int renamedCategories
		pdbRaf.write(General.getNullTerminatedString("Unfiled", 16, ""));
		pdbRaf.write(General.getNullTerminatedString("FieldLabels", 16, ""));
		pdbRaf.write(General.getNullTerminatedString("DataRecords", 16, ""));
		pdbRaf.write(General.getNullTerminatedString("DataRecordsFout", 16, ""));
		pdbRaf.write(General.getNullTerminatedString("Preferences", 16, ""));
		pdbRaf.write(General.getNullTerminatedString("DataType", 16, ""));
		pdbRaf.write(new byte[160]); // padding

		// byte[16] categoryUniqIDs
		for (int i = 0; i < 16; ++i) {
			pdbRaf.writeByte(i);
		}

		pdbRaf.writeByte(15); // byte lastUniqID?
		pdbRaf.writeByte(0); // byte reserved1
		pdbRaf.writeShort(0); // short reserved2
		pdbRaf.write(new byte[149]); // padding

		// field names
		pdbDas.write(RECORD_HEADER);

		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);
			pdbDas.writeShort(i);
			pdbDas.writeBytes(field.getFieldHeader().isEmpty() ? field.getFieldAlias() : field.getFieldHeader());
			boolean isNoTextExport = !field.isOutputAsText();

			switch (field.getFieldType()) {
			case BOOLEAN:
				if (isNoTextExport) {
					fieldTypes[i] = "B";
				}
				break;
			case DATE:
				if (isNoTextExport) {
					fieldTypes[i] = "D";
				}
				break;
			case TIME:
				if (isNoTextExport) {
					fieldTypes[i] = "d";
				}
			default:
				break;
			}
		}

		pdbDas.write(RECORD_FOOTER);
		writeRecord(pdbBaos.toByteArray(), 1 << 24); // Record 1
		pdbBaos.reset();

		// prefs (4)
		pdbDas.write(RECORD_HEADER);
		for (int i = 0; i < myExportFile.getMaxFields(); ++i) {
			pdbDas.writeShort(i);
		}

		pdbDas.write(RECORD_FOOTER);
		writeRecord(pdbBaos.toByteArray(), 4 << 24); // Record 2
		pdbBaos.reset();

		// field type (5)
		pdbDas.write(RECORD_HEADER);
		for (int i = 0; i < myExportFile.getMaxFields(); ++i) {
			pdbDas.writeShort(i);
			pdbDas.writeBytes(fieldTypes[i]);
		}

		pdbDas.write(RECORD_FOOTER);
		writeRecord(pdbBaos.toByteArray(), 5 << 24); // Record 3
		pdbBaos.reset();

		// field widths (6)
		pdbDas.write(RECORD_HEADER);
		for (int i = 0; i < 19; ++i) {
			pdbDas.writeShort(i);
			pdbDas.writeBytes("80");
		}

		pdbDas.write(RECORD_FOOTER);
		writeRecord(pdbBaos.toByteArray(), 6 << 24); // Record 4
		pdbBaos.reset();
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<FieldDefinition> dbDef = getTableModelFields();
		List<String> fields = getRecordFields(null);

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
			default:
				break;
			}
			result.put(field.getFieldAlias(), dbValue);
		}
		return result;
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
	public void processData(Map<String, Object> dbRecord) throws Exception {
		numFields = dbInfo2Write.size();

		// Read the user defined list of DB fields
		pdbDas.write(RECORD_HEADER);
		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);
			String dbField = convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString();
			pdbDas.writeShort(i);
			pdbDas.write(General.convertString2Bytes(dbField, encoding));
		}

		pdbDas.write(RECORD_FOOTER);
		// Write to Export File
		writeRecord(pdbBaos.toByteArray(), 2 << 24);
		pdbBaos.reset();
	}

	/**
	 * Converts a date stored in the database table a MobileDB database date
	 */
	protected Object convertDate(LocalDate pDate) {
		if (pDate == null) {
			return "";
		}

		// Converts a LocalDate to a MobileDB date (no. of days since 01.01.1904)
		LocalDate palmDate = LocalDate.of(1904, 1, 1);

		return String.valueOf(ChronoUnit.DAYS.between(palmDate, pDate));
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
			if (iter.next().equals("")) {
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
			element += " ";
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
		byte[] record = new byte[recordID[2] - (recordID[0] + 7)];
		skipBytes(6);
		readLn(record);
		String s = General.convertBytes2String(record, encoding);

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