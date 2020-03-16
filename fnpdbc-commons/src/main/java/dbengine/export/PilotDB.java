package dbengine.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
	private int offSet;

	public PilotDB(Profiles pref) {
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

		// Create Pilot Record Database Format
		createPalmDB(totalRecords);

		// ApplicationInfo

		// Pilot-DB default flag
		pdbRaf.writeShort(1);
		pdbRaf.writeShort(numFields);

		// field names
		int[] fieldTypes = new int[numFields];
		StringBuilder buf = new StringBuilder(numFields * 16);
		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);
			switch (field.getFieldType()) {
			case BOOLEAN:
				if (useBoolean) {
					fieldTypes[i] = 1;
				}
				break;
			case DATE:
				if (useDate) {
					fieldTypes[i] = 3;
				}
				break;
			case FLOAT:
				fieldTypes[i] = 8;
				break;
			case MEMO:
				fieldTypes[i] = 5;
				break;
			case NUMBER:
				fieldTypes[i] = 2;
				break;
			case TIME:
				if (useTime) {
					fieldTypes[i] = 4;
				}
			default:
				break;
			}
			buf.append(new String(General.getNullTerminatedString(field.getFieldHeader(), 0, "")));
		}

		pdbRaf.writeShort(0);
		pdbRaf.writeShort(buf.length());
		pdbRaf.writeBytes(buf.toString());

		// Field types
		pdbRaf.writeShort(1);
		pdbRaf.writeShort(numFields * 2);
		for (int i = 0; i < numFields; ++i) {
			pdbRaf.writeShort(fieldTypes[i]);
		}

		// View and sort fields?
		pdbRaf.writeShort(65);
		pdbRaf.writeShort(4);
		pdbRaf.write(General.getNullTerminatedString(null, 4, ""));

		pdbRaf.writeShort(128);
		pdbRaf.writeShort(2);
		pdbRaf.write(General.getNullTerminatedString(null, 2, ""));
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		numFields = dbInfo2Write.size();
		int start = numFields * 2;

		ByteArrayOutputStream baoNames = new ByteArrayOutputStream();
		DataOutputStream dasNames = new DataOutputStream(baoNames);

		// Read the user defined list of DB fields
		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);
			offSet = dasNames.size() + start;
			pdbDas.writeShort(offSet);
			try {
				dasNames.write(convertData(dbRecord.get(field.getFieldAlias()), field.getFieldType()));
			} catch (Exception e) {
				throw FNProgException.getException("fieldConvertError", field.getFieldAlias(), e.getMessage());
			}
		}

		// Add temp to result and write result
		pdbDas.write(baoNames.toByteArray());
		writeRecord(pdbBaos.toByteArray(), 0);
		pdbBaos.reset();
	}

	private byte[] convertData(Object dbField, FieldTypes pIndex) throws Exception {
		byte[] result = new byte[1];

		if (dbField == null || dbField.equals("")) {
			return result;
		}
		String dbValue = dbField.toString();

		switch (pIndex) {
		case BOOLEAN:
			boolean b = (Boolean) dbField;
			if (useBoolean) {
				result[0] = b ? (byte) 1 : 0;
				return result;
			}
			return General.getNullTerminatedString(b ? booleanTrue : booleanFalse, 0, "");
		case DATE:
			return useDate ? (byte[]) convertDate(dbValue)
					: General.getNullTerminatedString(General.convertDate(dbValue), 0, "");
		case FUSSY_DATE:
			return General.getNullTerminatedString(General.convertFussyDate(dbValue), 0, "");
		case FLOAT:
			return DOUBLE((Double) dbField);
		case MEMO:
			return convertMemo(dbValue);
		case NUMBER:
			return INT((Integer) dbField);
		case TIME:
			dbValue = General.convertTime(dbValue);
			return useTime ? convertTime(dbValue) : General.getNullTerminatedString(dbValue, 0, "");
		case DURATION:
			return General.getNullTerminatedString(General.convertDuration((Number) dbField), 0, "");
		default:
			result = General.getNullTerminatedString(dbValue, 0, encoding);
			if (result.length > myExportFile.getMaxTextSize()) {
				throw FNProgException.getException("fieldLengthError", dbValue);
			}
		}

		return result;
	}

	@Override
	protected void readAppInfo() throws Exception {
		int pos = 4; // We skip the Flag Integer
		byte[] data = new byte[appInfoDataLength];
		readAppInfo(0, data);

		while (pos < appInfoDataLength) {
			byte[] fieldData = new byte[2];
			System.arraycopy(data, pos, fieldData, 0, 2);

			int chunkType = SHORT(fieldData);
			if (chunkType > 512) {
				throw FNProgException.getException("fileheaderCorrupt", myFilename);
			}

			pos += 2;
			System.arraycopy(data, pos, fieldData, 0, 2);
			int dataSize = SHORT(fieldData);

			if (dataSize < 1 || dataSize > appInfoDataLength) {
				throw FNProgException.getException("fileheaderCorrupt", myFilename);
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
		short fieldNum = SHORT(fieldData);

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
		String result = General.convertBytes2String(data, encoding) + "\0";
		return result.substring(0, result.indexOf('\0'));
	}

	/**
	 * Converts a PilotDB date back to a YYYYMMDD database date format
	 */
	private String convertDate2DB(byte[] data) throws Exception {
		if (data == null || data.length < 2) {
			return "";
		}

		ByteArrayInputStream bIn = new ByteArrayInputStream(data);
		DataInputStream dIn = new DataInputStream(bIn);

		short year = dIn.readShort();
		if (year == 0) {
			return "";
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
		return result.toString();
	}

	@Override
	protected Object convertDate(String pDate) {
		byte[] result = new byte[4];
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			out.writeShort(Integer.parseInt(pDate.substring(0, 4)));
			out.writeByte(Integer.parseInt(pDate.substring(4, 6)));
			out.writeByte(Integer.parseInt(pDate.substring(6, 8)));
			return bytes.toByteArray();
		} catch (Exception e) {
			// Nothing to do
		}

		return result;
	}

	private String convertTime2DB(byte[] data) {
		if (data == null || data[0] == 24) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		if (data[0] < 10) {
			result.append("0");
		}
		result.append(data[0]);
		result.append(":");
		if (data[1] < 10) {
			result.append("0");
		}
		result.append(data[1]);
		return General.convertTime2DB(result.toString());
	}

	private byte[] convertTime(String pTime) {
		byte[] result = { 24, 0 };
		if (pTime == null || pTime.equals("")) {
			return result;
		}

		String[] time = pTime.split(":");
		result[0] = Byte.parseByte(time[0]);
		result[1] = Byte.parseByte(time[1]);
		return result;
	}

	private String convertListField2DB(byte[] data, int i) {
		if (dbList.isEmpty() || data[0] < 0) {
			return "";
		}

		List<String> dbFieldValues = dbList.get((short) i);
		return dbFieldValues.get(data[0]);
	}

	private String convertLinkedField2DB(byte[] data) {
		if (data == null || data.length < 5) {
			return "";
		}

		int length = data.length - 5;
		byte[] dataField = new byte[length];
		System.arraycopy(data, 4, dataField, 0, length);
		return General.convertBytes2String(dataField, "");
	}

	private String convertMemo2DB(byte[] fieldData, byte[] data, ArrayList<int[]> fieldLen, int fieldNum) {
		if (fieldData == null) {
			return "";
		}

		String s = General.convertBytes2String(fieldData, encoding);
		int index0 = s.indexOf('\0');
		String header = s.substring(0, index0);

		if (fieldData.length < index0 + 2) {
			// No note lines offset is found
			return header;
		}

		offSet = SHORT(new byte[] { fieldData[index0 + 1], fieldData[index0 + 2] });
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
				return General.convertBytes2String(dataField, encoding);
			}
		}
		return "";
	}

	private byte[] convertMemo(String pMemo) {
		if (pMemo.equals("")) {
			return new byte[4];
		}

		if (pMemo.length() > myExportFile.getMaxMemoSize()) {
			// Truncate field
			pMemo = pMemo.substring(0, myExportFile.getMaxMemoSize() - 15) + "\ntruncated...";
		}

		// Check for carriage return or next line (for memo header)
		int textLen = pMemo.indexOf('\r');
		if (textLen == -1) {
			textLen = pMemo.indexOf('\n');
			if (textLen == -1) {
				textLen = pMemo.length();
			}
		}

		if (textLen > 31) {
			textLen = 31;
		}

		ByteArrayOutputStream result = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(result);

		try {
			out.write(General.convertString2Bytes(pMemo.substring(0, textLen), encoding));
			out.writeByte(0);
			out.writeShort(offSet + out.size() + 2);
			out.write(General.convertString2Bytes(pMemo, encoding));
			out.writeByte(0);
			return result.toByteArray();
		} catch (Exception e) {
			// Log error
		}
		return new byte[4];
	}

	private Double convertFloat2DB(byte[] data) {
		if (data == null || data.length < 4) {
			return 0D;
		}
		return DOUBLE(data);
	}

	private Integer convertNumeric2DB(byte[] data) {
		if (data == null || data.length < 4) {
			return 0;
		}
		return INT(data);
	}
}
