package dbengine.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;

public class JFile extends PalmDB {
	/**
	 * Title: JFile Description: Generic Class for Palm-OS JFile 5+ database
	 * Copyright: (c) 2003-2022
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private int numFields;

	public JFile(Profiles pref) {
		super(pref);
	}

	@Override
	public void readTableContents() throws Exception {
		super.readTableContents();
		List<FieldDefinition> dbDef = getTableModelFields();

		for (int index = 0; index < totalRecords; index++) {
			Map<String, Object> result = new HashMap<>();
			int dataSize = calculateFieldRecord();
			if (dataSize < 0) {
				// File is corrupt
				throw FNProgException.getException("fileHeaderCorrupt", myDatabase);
			}

			byte[] bytes = new byte[dataSize];
			readLn(bytes);

			String s = General.convertByteArrayToString(bytes, null);
			int[] idx = new int[3];
			idx[2] = s.indexOf('\0', idx[0]);

			while (idx[2] != -1 && idx[0] < numFields) {
				FieldDefinition field = dbDef.get(idx[0]++);
				Object dbValue = s.substring(idx[1], idx[2]);
				if (!dbValue.equals(General.EMPTY_STRING)) {
					try {
						switch (field.getFieldType()) {
						case BOOLEAN:
							dbValue = Boolean.parseBoolean(dbValue.toString());
							break;
						case DATE:
							dbValue = General.convertDate2DB(dbValue.toString(), General.sdInternalDate);
							break;
						case NUMBER:
							dbValue = Integer.parseInt(dbValue.toString());
							break;
						case FLOAT:
							dbValue = Double.parseDouble(dbValue.toString());
							break;
						case TIME:
							dbValue = General.convertTime2DB(dbValue.toString(), General.sdInternalTime);
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
				idx[1] = idx[2] + 1;
				idx[2] = s.indexOf('\0', idx[1]);
			}
			dbRecords.add(result);
		}
		setFieldSizes();
	}

	protected int calculateFieldRecord() throws Exception {
		int[] recordID = setPointer2NextRecord();
		skipBytes(numFields * 2);
		return recordID[2] - (recordID[0] + numFields * 2);
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
