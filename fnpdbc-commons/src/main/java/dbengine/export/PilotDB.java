package dbengine.export;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.PalmDB;

public class PilotDB extends PalmDB {
	/**
	 * Title: PilotDB Description: Generic Class for Palm-OS PilotDB Copyright: (c)
	 * 2003-2006
	 *
	 * @author Tom van Breukelen
	 * @version 5.0
	 */
	private Map<Short, List<String>> dbList = new HashMap<>();

	private int numFields;

	public PilotDB(Profiles pref) {
		super(pref);
	}

	@Override
	protected void readAppInfo() throws Exception {
		int pos = 4; // We skip the Flag Integer
		byte[] data = new byte[appInfoDataLength];
		readAppInfo(0, data);

		while (pos < appInfoDataLength) {
			byte[] fieldData = new byte[2];
			System.arraycopy(data, pos, fieldData, 0, 2);

			int chunkType = getShort(fieldData);
			if (chunkType > 512) {
				throw FNProgException.getException("fileheaderCorrupt", myDatabase);
			}

			pos += 2;
			System.arraycopy(data, pos, fieldData, 0, 2);
			int dataSize = getShort(fieldData);

			if (dataSize < 1 || dataSize > appInfoDataLength) {
				throw FNProgException.getException("fileheaderCorrupt", myDatabase);
			}

			pos += 2;
			fieldData = new byte[dataSize];
			System.arraycopy(data, pos, fieldData, 0, dataSize);

			switch (chunkType) {
			case 0: // Field Names
				parseFieldNamesChunk(fieldData);
				break;
			case 1: // Field Types
				parseFieldTypesChunk(fieldData);
				break;
			default: // Field Data
				parseFieldDataChunk(fieldData);
			}
			pos += dataSize;
		}
	}

	private void parseFieldNamesChunk(byte[] data) {
		String fieldData = new String(data);
		String[] fields = fieldData.split("\0");

		numFields = fields.length;
		dbFieldNames.clear();
		dbFieldNames.addAll(Arrays.asList(fields));
	}

	private void parseFieldTypesChunk(byte[] data) throws Exception {
		dbFieldTypes.clear();
		ByteArrayInputStream bIn = new ByteArrayInputStream(data);
		DataInputStream dIn = new DataInputStream(bIn);

		for (int i = 0; i < numFields; i++) {
			short type = dIn.readShort();
			switch (type) {
			case 0:
				dbFieldTypes.add(FieldTypes.TEXT);
				break;
			case 1:
				dbFieldTypes.add(FieldTypes.BOOLEAN);
				break;
			case 2:
				dbFieldTypes.add(FieldTypes.NUMBER);
				break;
			case 3:
				dbFieldTypes.add(FieldTypes.DATE);
				break;
			case 4:
				dbFieldTypes.add(FieldTypes.TIME);
				break;
			case 5:
				dbFieldTypes.add(FieldTypes.MEMO);
				break;
			case 6:
				dbFieldTypes.add(FieldTypes.LIST);
				break;
			case 7:
				dbFieldTypes.add(FieldTypes.LINKED);
				break;
			case 8:
				dbFieldTypes.add(FieldTypes.FLOAT);
				break;
			case 9:
				dbFieldTypes.add(FieldTypes.TEXT);
				break;
			case 10:
				dbFieldTypes.add(FieldTypes.TEXT);
				break;
			default:
				throw FNProgException.getException("unsuportedFieldType", Integer.toString(i), Integer.toString(type));
			}
		}
	}

	private void parseFieldDataChunk(byte[] data) {
		byte[] fieldData = new byte[2];
		System.arraycopy(data, 0, fieldData, 0, 2);
		short fieldNum = getShort(fieldData);

		FieldTypes fieldType = dbFieldTypes.get(fieldNum);
		if (fieldType == FieldTypes.LIST) {
			final int stringLen = data.length - 6;
			fieldData = new byte[stringLen];
			System.arraycopy(data, 6, fieldData, 0, stringLen);

			String fldData = new String(fieldData);
			String[] fields = fldData.split("\0");

			int maxFields = fields.length;
			List<String> dbListValues = new ArrayList<>(maxFields);
			dbListValues.addAll(Arrays.asList(fields));
			dbList.put(fieldNum, dbListValues);
		}
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<FieldDefinition> dbDef = getTableModelFields();
		int[] recordID = setPointer2NextRecord();
		byte[] data = new byte[recordID[2] - recordID[0]];
		readLn(data);

		ByteArrayInputStream bIn = new ByteArrayInputStream(data);
		DataInputStream dIn = new DataInputStream(bIn);

		int[] fieldLen = new int[numFields + 1];
		for (int i = 0; i < numFields; i++) {
			fieldLen[i] = dIn.readShort(); // get position of each field in the data byte array
		}
		fieldLen[numFields] = data.length;

		ArrayList<int[]> lenHash = new ArrayList<>(numFields * 2);
		for (int i = 0; i < numFields; i++) {
			lenHash.add(new int[] { fieldLen[i], fieldLen[i + 1] });
		}

		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbDef.get(i);
			fieldLen = lenHash.get(i);

			byte[] fieldData = new byte[fieldLen[1] - fieldLen[0]];
			System.arraycopy(data, fieldLen[0], fieldData, 0, fieldData.length);

			switch (field.getFieldType()) {
			case TEXT:
				result.put(field.getFieldAlias(), convertText2DB(fieldData));
				break;
			case BOOLEAN:
				result.put(field.getFieldAlias(), fieldData[0] == 1);
				break;
			case DATE:
				result.put(field.getFieldAlias(), convertDate2DB(fieldData));
				break;
			case FLOAT:
				result.put(field.getFieldAlias(), convertFloat2DB(fieldData));
				break;
			case LINKED:
				result.put(field.getFieldAlias(), convertLinkedField2DB(fieldData));
				break;
			case LIST:
				result.put(field.getFieldAlias(), convertListField2DB(fieldData, i));
				break;
			case MEMO:
				result.put(field.getFieldAlias(), convertMemo2DB(fieldData, data, lenHash, i));
				break;
			case NUMBER:
				result.put(field.getFieldAlias(), convertNumeric2DB(fieldData));
				break;
			case TIME:
				result.put(field.getFieldAlias(), convertTime2DB(fieldData));
				break;
			default:
				break;
			}
		}

		return result;
	}

	/**
	 * Converts a PilotDB text back to a String
	 */
	private String convertText2DB(byte[] data) {
		String result = General.convertByteArrayToString(data, null) + "\0";
		return result.substring(0, result.indexOf('\0'));
	}

	/**
	 * Converts a PilotDB date back to a YYYYMMDD database date format
	 */
	private LocalDate convertDate2DB(byte[] data) throws Exception {
		if (data == null || data.length < 2) {
			return null;
		}

		ByteArrayInputStream bIn = new ByteArrayInputStream(data);
		DataInputStream dIn = new DataInputStream(bIn);

		short year = dIn.readShort();
		if (year == 0) {
			return null;
		}

		byte month = dIn.readByte();
		byte day = dIn.readByte();

		StringBuilder result = new StringBuilder();
		result.append(year);
		if (month < 10) {
			result.append("0");
		}
		result.append(month);

		if (day < 10) {
			result.append("0");
		}
		result.append(day);
		return General.convertDate2DB(result.toString(), General.sdInternalDate);
	}

	private LocalTime convertTime2DB(byte[] data) {
		if (data == null || data[0] == 24) {
			return null;
		}

		return LocalTime.of(data[0], data[1]);
	}

	private String convertListField2DB(byte[] data, int i) {
		if (dbList.isEmpty() || data[0] < 0) {
			return General.EMPTY_STRING;
		}

		List<String> dbFieldValues = dbList.get((short) i);
		return dbFieldValues.get(data[0]);
	}

	private String convertLinkedField2DB(byte[] data) {
		if (data == null || data.length < 5) {
			return General.EMPTY_STRING;
		}

		int length = data.length - 5;
		byte[] dataField = new byte[length];
		System.arraycopy(data, 4, dataField, 0, length);
		return General.convertByteArrayToString(dataField, null);
	}

	private String convertMemo2DB(byte[] fieldData, byte[] data, ArrayList<int[]> fieldLen, int fieldNum) {
		if (fieldData == null) {
			return General.EMPTY_STRING;
		}

		String s = General.convertByteArrayToString(fieldData, null);
		int index0 = s.indexOf('\0');
		String header = s.substring(0, index0);

		if (fieldData.length < index0 + 2) {
			// No note lines offset is found
			return header;
		}

		int offSet = getShort(new byte[] { fieldData[index0 + 1], fieldData[index0 + 2] });
		if (offSet == 0) {
			// No additonal memo lines were found
			return header;
		}

		int[] len = fieldLen.get(fieldNum);
		if (len[1] > offSet) {
			// We have all the memo data we need
			return s.substring(index0 + 3, s.length() - 1);
		}

		// Data needs to be obtained from the remainder of the record, starting at the
		// offset position
		fieldNum++;
		for (int i = fieldNum; i < numFields; i++) {
			len = fieldLen.get(i);
			if (len[1] > offSet) {
				// We now have the correct end of the memofield
				byte[] dataField = new byte[len[1] - (offSet + 1)];
				len[1] = offSet;
				fieldLen.set(i, len);
				System.arraycopy(data, offSet, dataField, 0, dataField.length);
				return General.convertByteArrayToString(dataField, null);
			}
		}
		return General.EMPTY_STRING;
	}

	private Double convertFloat2DB(byte[] data) {
		if (data == null || data.length < 4) {
			return 0D;
		}
		return getDouble(data);
	}

	private Integer convertNumeric2DB(byte[] data) {
		if (data == null || data.length < 4) {
			return 0;
		}
		return getInt(data);
	}
}
