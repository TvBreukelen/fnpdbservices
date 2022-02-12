package dbengine.export;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.GeneralDB;
import dbengine.IConvert;
import dbengine.utils.DBFField;
import dbengine.utils.DBFHeader;
import dbengine.utils.DBFReader;
import dbengine.utils.DBFWriter;

public class DBaseFile extends GeneralDB implements IConvert {
	private DBFReader reader;
	private DBFWriter writer;
	private FileInputStream in;
	private int numFields;
	private File memoFile;
	private File outFile;

	public DBaseFile(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		String memoF = myDatabase.substring(0, myDatabase.length() - 3);

		if (isInputFile) {
			memoF += myImportFile == ExportFile.FOXPRO ? "fpt" : "dbt";
		} else {
			memoF += myExportFile == ExportFile.FOXPRO ? "fpt" : "dbt";
		}

		outFile = new File(myDatabase);
		memoFile = new File(memoF);

		if (hasBackup) {
			General.copyFile(memoF, memoFile + ".bak");
		}

		if (isInputFile) {
			useAppend = false;
		} else {
			if (useAppend) {
				useAppend = outFile.exists();
			} else {
				outFile.delete();
				memoFile.delete();
			}
		}

		this.isInputFile = isInputFile;

		if (isInputFile) {
			in = new FileInputStream(myDatabase);
			reader = new DBFReader(in, memoFile);
			reader.setCharactersetName(getOEMEncoding());
		} else {
			writer = new DBFWriter(outFile, memoFile);
			writer.setCharactersetName("");
		}
	}

	@Override
	public void closeFile() {
		try {
			if (isInputFile) {
				in.close();
				reader.closeDBFFile();
			} else {
				writer.closeDBFFile();
			}
		} catch (Exception e) {
			// Log the error
		}

		reader = null;
		writer = null;
		in = null;
	}

	@Override
	public void createDbHeader() throws Exception {
		numFields = dbInfo2Write.size();
		if (useAppend && numFields != writer.getFieldCount()) {
			throw FNProgException.getException("noMatchFieldsDatabase", Integer.toString(numFields),
					Integer.toString(writer.getFieldCount()));
		}

		DBFField[] fields = new DBFField[numFields];

		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);

			fields[i] = new DBFField();
			String header = field.getFieldHeader().toUpperCase();
			if (header.length() > 10) {
				header = header.substring(0, 10);
			}

			fields[i].setName(header);

			switch (field.getFieldType()) {
			case BOOLEAN:
				if (field.isOutputAsText()) {
					int t = getBooleanTrue().length();
					int f = getBooleanFalse().length();
					fields[i].setFieldLength(Math.max(t, f));
					fields[i].setDataType(DBFField.FIELD_TYPE_C);
				} else {
					fields[i].setDataType(DBFField.FIELD_TYPE_L);
				}
				break;
			case DATE:
				if (field.isOutputAsText()) {
					fields[i].setFieldLength(10);
					fields[i].setDataType(DBFField.FIELD_TYPE_C);
				} else {
					fields[i].setDataType(DBFField.FIELD_TYPE_D);
				}
				break;
			case DURATION:
				fields[i].setFieldLength(7);
				fields[i].setDataType(DBFField.FIELD_TYPE_C);
				break;
			case FLOAT:
				fields[i].setDataType(DBFField.FIELD_TYPE_F);
				fields[i].setFieldLength(field.getSize() < 10 ? 10 : field.getSize());
				fields[i].setDecimalCount(field.getDecimalPoint());
				break;
			case FUSSY_DATE:
				fields[i].setDataType(DBFField.FIELD_TYPE_C);
				fields[i].setFieldLength(10);
				break;
			case MEMO:
				fields[i].setDataType(DBFField.FIELD_TYPE_M);
				fields[i].setFieldLength(myExportFile == ExportFile.FOXPRO ? 4 : 10);
				break;
			case NUMBER:
				// We assume a normal integer
				fields[i].setDataType(DBFField.FIELD_TYPE_N);
				fields[i].setFieldLength(field.getSize());
				fields[i].setDecimalCount(0);
				break;
			case TIME:
				fields[i].setFieldLength(5);
				fields[i].setDataType(DBFField.FIELD_TYPE_C);
				break;
			default:
				if (field.getSize() > myExportFile.getMaxTextSize()) {
					field.setFieldType(FieldTypes.MEMO);
					fields[i].setDataType(DBFField.FIELD_TYPE_M);
				} else {
					fields[i].setDataType(DBFField.FIELD_TYPE_C);
					fields[i].setFieldLength(field.getSize());
				}
			}
		}

		if (useAppend) {
			// Verify if fields match in type and size
			for (int i = 0; i < numFields; i++) {
				DBFField field1 = writer.getFields(i);
				DBFField field2 = fields[i];

				if (!field1.getName().equals(field2.getName())) {
					throw FNProgException.getException("noMatchFieldName", Integer.toString(i + 1), field1.getName(),
							field2.getName());
				}

				if (field1.getDataType() != field2.getDataType()) {
					if (field1.getDataType() == DBFField.FIELD_TYPE_M) {
						field2.setDataType(DBFField.FIELD_TYPE_M);
						FieldDefinition field = dbInfo2Write.get(i);
						field.setFieldType(FieldTypes.MEMO);
					} else {
						throw FNProgException.getException("noMatchFieldType", field1.getName(), field2.getName());
					}
				}

				if (field1.getFieldLength() < field2.getFieldLength()) {
					throw FNProgException.getException("noMatchFieldLength", field1.getName(),
							Integer.toString(field1.getFieldLength()), Integer.toString(field2.getFieldLength()));
				}
			}
		} else {
			writer.setFields(fields, getDBaseSignature());
		}
	}

	private byte getDBaseSignature() {
		switch (myImportFile) {
		case DBASE3:
			return DBFHeader.SIG_DBASE_III;
		case DBASE4:
		case DBASE5:
			return DBFHeader.SIG_DBASE_IV;
		case FOXPRO:
			return DBFHeader.SIG_FOXPRO;
		default:
			return DBFHeader.SIG_DBASE_III;
		}
	}

	@Override
	public void deleteFile() {
		closeFile();
		if (outFile.exists()) {
			outFile.delete();
		}

		if (memoFile.exists()) {
			memoFile.delete();
		}

		if (hasBackup) {
			File backupFile = new File(myDatabase + ".bak");
			backupFile.renameTo(outFile);

			backupFile = new File(memoFile.getAbsoluteFile() + ".bak");
			if (backupFile.exists()) {
				backupFile.renameTo(memoFile);
			}
		}
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		Object[] rowData = new Object[numFields];

		// Read the user defined list of DB fields
		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbInfo2Write.get(i);
			Object dbField = dbRecord.get(field.getFieldAlias());
			if (dbField == null || dbField.equals("")) {
				rowData[i] = "";
				continue;
			}

			switch (field.getFieldType()) {
			case BOOLEAN:
				boolean b = (Boolean) dbField;
				if (field.isOutputAsText()) {
					rowData[i] = b ? getBooleanTrue() : getBooleanFalse();
				} else {
					rowData[i] = b;
				}
				break;
			case DATE:
				rowData[i] = field.isOutputAsText()
						? General.convertDate((LocalDate) dbField, General.getSimpleDateFormat())
						: dbField;
				break;
			case FUSSY_DATE:
				rowData[i] = General.convertFussyDate(dbField.toString());
				break;
			default:
				rowData[i] = dbField;
			}
		}
		writer.addRecord(rowData);
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<FieldDefinition> dbDef = getTableModelFields();

		Object[] rowObjects = reader.nextRecord();
		if (rowObjects.length == 0) {
			return result;
		}

		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbDef.get(i);
			Object dbField = rowObjects[i] == null ? "" : rowObjects[i];
			if (field.getFieldType() == FieldTypes.NUMBER && rowObjects[i] instanceof Double) {
				dbField = ((Double) dbField).intValue();
			}
			result.put(dbFieldNames.get(i), dbField);
		}
		return result;
	}

	@Override
	public void readTableContents() throws Exception {
		numFields = reader.getFieldCount();
		if (numFields < 1) {
			throw FNProgException.getException("noFields", myDatabase);
		}

		myTotalRecords = reader.getRecordCount();
		if (reader.getRecordCount() < 1) {
			throw FNProgException.getException("noRecords", myDatabase);
		}

		dbFieldNames.clear();
		dbFieldTypes.clear();

		for (int i = 0; i < numFields; i++) {
			DBFField dbField = reader.getField(i);
			dbFieldNames.add(dbField.getName());
			switch (dbField.getDataType()) {
			case DBFField.FIELD_TYPE_D:
				dbFieldTypes.add(FieldTypes.DATE);
				break;
			case DBFField.FIELD_TYPE_I:
				dbFieldTypes.add(FieldTypes.NUMBER);
				break;
			case DBFField.FIELD_TYPE_F:
			case DBFField.FIELD_TYPE_N:
			case DBFField.FIELD_TYPE_Y:
				dbFieldTypes.add(dbField.getDecimalCount() == 0 ? FieldTypes.NUMBER : FieldTypes.FLOAT);
				break;
			case DBFField.FIELD_TYPE_L:
				dbFieldTypes.add(FieldTypes.BOOLEAN);
				break;
			case DBFField.FIELD_TYPE_M:
				dbFieldTypes.add(FieldTypes.MEMO);
				break;
			case DBFField.FIELD_TYPE_T:
				dbFieldTypes.add(FieldTypes.TIMESTAMP);
				break;
			default:
				dbFieldTypes.add(FieldTypes.TEXT);
			}
		}
	}

	private String getOEMEncoding() {
		byte languageDriver = reader.getDBFHeader().getLanguageDriver();
		Properties properties = General.getProperties(myImportFile == ExportFile.FOXPRO ? "foxpro" : "dBase");
		String result = properties.getProperty(Byte.toString(languageDriver), "");
		if (result.isEmpty()) {
			return "";
		}

		String[] charset = result.split(",");
		String[] charSets = General.getCharacterSets();

		result = "";
		for (String cSet : charSets) {
			if (cSet.equals(charset[1])) {
				return cSet;
			}
		}

		return result;
	}
}
