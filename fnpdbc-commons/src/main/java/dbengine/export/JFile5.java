package dbengine.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.PalmDB;

public class JFile5 extends PalmDB {
	/**
	 * Title: JFile Description: Generic Class for Palm-OS JFile 5+ database
	 * Copyright: (c) 2003-2012
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private int numFields;

	public JFile5(Profiles pref) {
		super(pref);
	}

	@Override
	public void createDbHeader() throws Exception {
		int totalRecords = mySoft.getTotalRecords();
		final int MAX_FIELDS = myExportFile.getMaxFields();

		// Check whether we are in append mode
		if (useAppend) {
			appendPilotDB(totalRecords, dbInfo2Write);
			return;
		}

		numFields = dbInfo2Write.size();
		final byte[] filler = new byte[21];
		int[] fieldTypes = new int[MAX_FIELDS];
		Arrays.fill(fieldTypes, 1); // Set Default Type to Normal Text Field

		// Create Pilot Record Database Format
		createPalmDB(totalRecords);

		// ApplicationInfo
		// field names (1)
		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);
			switch (field.getFieldType()) {
			case BOOLEAN:
				if (useBoolean) {
					fieldTypes[i] = 2;
				}
				break;
			case DATE:
				if (useDate) {
					fieldTypes[i] = 4;
				}
				break;
			case FLOAT:
				fieldTypes[i] = 16;
				break;
			case NUMBER:
				fieldTypes[i] = 8;
				break;
			case TIME:
				if (useTime) {
					fieldTypes[i] = 32;
				}
				break;
			default:
				break;
			}
			pdbRaf.write(General.getNullTerminatedString(field.getFieldHeader(), 21, encoding));
		}

		// field names (2)
		for (int i = numFields; i < MAX_FIELDS; i++) {
			pdbRaf.write(filler);
		}

		// Field types
		for (int i = 0; i < MAX_FIELDS; ++i) {
			pdbRaf.writeShort(fieldTypes[i]);
		}

		// Number of fields
		pdbRaf.writeShort(numFields);

		// Another ID?
		pdbRaf.writeShort(576);

		// Field length
		for (int i = -1; i < MAX_FIELDS; ++i) {
			pdbRaf.writeShort(80);
		}
		// Padding
		setPadding();
	}

	protected void setPadding() throws IOException {
		pdbRaf.write(new byte[450]);
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<FieldDefinition> dbDef = getTableModelFields();

		int dataSize = calculateFieldRecord();
		if (dataSize < 0) {
			// File is corrupt
			return result;
		}

		byte[] record = new byte[dataSize];
		readLn(record);
		String s = General.convertBytes2String(record, encoding);
		int[] index = new int[3];
		index[2] = s.indexOf('\0', index[0]);

		while (index[2] != -1 && index[0] < numFields) {
			FieldDefinition field = dbDef.get(index[0]++);
			Object dbValue = s.substring(index[1], index[2]);
			if (!dbValue.equals("")) {
				try {
					switch (field.getFieldType()) {
					case BOOLEAN:
						dbValue = Boolean.parseBoolean(dbValue.toString());
						break;
					case DATE:
						String date = General.convertDate2DB(dbValue.toString());
						if (date.isEmpty()) {
							field.setFieldType(FieldTypes.TEXT);
						} else {
							dbValue = date;
						}
						break;
					case NUMBER:
						dbValue = Integer.parseInt(dbValue.toString());
						break;
					case FLOAT:
						dbValue = Double.parseDouble(dbValue.toString());
						break;
					case TIME:
						dbValue = General.convertTime2DB(dbValue.toString());
						break;
					default:
						break;
					}
				} catch (Exception e) {
					// dbValue could not be converted, this condition will be handled by method
					// XConverter.loadInputFile
				}
			}
			result.put(field.getFieldAlias(), dbValue);
			index[1] = index[2] + 1;
			index[2] = s.indexOf('\0', index[1]);
		}
		return result;
	}

	protected int calculateFieldRecord() throws Exception {
		int[] recordID = setPointer2NextRecord();
		skipBytes(numFields * 2);
		return recordID[2] - (recordID[0] + numFields * 2);
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		numFields = dbInfo2Write.size();
		StringBuilder bf = new StringBuilder();

		// Read the user defined list of DB fields
		for (FieldDefinition field : dbInfo2Write) {
			String dbField = convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString();
			bf.append(dbField);
			bf.append('\0');
			pdbDas.writeShort(dbField.length() + 1);
		}

		pdbDas.write(General.convertString2Bytes(bf.toString(), encoding));
		writeRecord(pdbBaos.toByteArray(), 0);
		pdbBaos.reset();
	}

	@Override
	protected void readAppInfo() throws Exception {
		final int MAX_FIELDS = myImportFile.getMaxFields();
		int fieldLen = MAX_FIELDS * 21;
		byte[] fields = new byte[fieldLen];
		readAppInfo(0, fields);
		dbFieldNames = General.splitNullTerminatedString(fields, 21);
		numFields = dbFieldNames.size();

		fields = new byte[MAX_FIELDS * 2];
		readAppInfo(fieldLen, fields);

		dbFieldTypes = new ArrayList<>();
		int index = 1;
		for (int i = 0; i < MAX_FIELDS; i++) {
			switch (fields[index]) {
			case 2:
				dbFieldTypes.add(FieldTypes.BOOLEAN);
				break;
			case 4:
				dbFieldTypes.add(FieldTypes.DATE);
				break;
			case 8:
				dbFieldTypes.add(FieldTypes.NUMBER);
				break;
			case 16:
				dbFieldTypes.add(FieldTypes.FLOAT);
				break;
			case 32:
				dbFieldTypes.add(FieldTypes.TIME);
				break;
			default:
				dbFieldTypes.add(FieldTypes.TEXT);
			}
			index += 2;
		}
	}
}
