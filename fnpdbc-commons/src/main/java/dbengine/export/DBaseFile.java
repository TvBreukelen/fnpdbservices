package dbengine.export;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linuxense.javadbf.DBFCharsetHelper;
import com.linuxense.javadbf.DBFDataType;
import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFWriter;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import dbengine.GeneralDB;
import dbengine.IConvert;

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
			reader = new DBFReader(in);
			if (memoFile.exists()) {
				reader.setMemoFile(memoFile);
			}
		} else {
			if (useAppend) {
				in = new FileInputStream(myDatabase);
				reader = new DBFReader(in);
				in.close();
				reader.close();
			}

			writer = new DBFWriter(outFile, DBFCharsetHelper.getCharsetByByte(myPref.getLanguageDriver()));
		}
	}

	@Override
	public void closeFile() {
		try {
			if (isInputFile) {
				in.close();
				reader.close();
			} else {
				writer.close();
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
		if (useAppend && numFields != reader.getFieldCount()) {
			throw FNProgException.getException("noMatchFieldsDatabase", Integer.toString(numFields),
					Integer.toString(reader.getFieldCount()));
		}

		DBFField[] fields = new DBFField[numFields];
		int index = 0;

		for (FieldDefinition field : dbInfo2Write) {
			DBFField dbField = new DBFField();
			String header = field.getFieldHeader().toUpperCase();
			if (header.length() > 10) {
				header = header.substring(0, 10);
			}

			dbField.setName(header);
			dbField.setType(DBFDataType.CHARACTER);

			switch (field.getFieldType()) {
			case BOOLEAN:
				if (field.isOutputAsText()) {
					int t = getBooleanTrue().length();
					int f = getBooleanFalse().length();
					dbField.setLength(Math.max(t, f));
				} else {
					dbField.setType(DBFDataType.LOGICAL);
				}
				break;
			case DATE:
				if (field.isOutputAsText()) {
					dbField.setLength(10);
				} else {
					dbField.setType(DBFDataType.DATE);
				}
				break;
			case MEMO:
				dbField.setLength(myExportFile.getMaxTextSize());
				break;
			case TIMESTAMP:
				dbField.setLength(18);
				break;
			case DURATION:
				dbField.setLength(7);
				break;
			case BIG_DECIMAL:
			case FLOAT:
				dbField.setType(DBFDataType.FLOATING_POINT);
				dbField.setLength(getNumericalSize(field));
				dbField.setDecimalCount(field.getDecimalPoint());
				break;
			case FUSSY_DATE:
				dbField.setLength(10);
				break;
			case NUMBER:
				// We assume a normal integer
				dbField.setType(DBFDataType.NUMERIC);
				dbField.setLength(getNumericalSize(field));
				dbField.setDecimalCount(0);
				break;
			case TIME:
				dbField.setLength(5);
				break;
			default:
				dbField.setLength(field.getSize() > myExportFile.getMaxTextSize() ? myExportFile.getMaxTextSize()
						: field.getSize());
			}

			fields[index++] = dbField;
		}

		if (useAppend) {
			// Verify if fields match in type and size
			for (int i = 0; i < numFields; i++) {
				DBFField field1 = reader.getField(i);
				DBFField field2 = fields[i];

				if (!field1.getName().equals(field2.getName())) {
					throw FNProgException.getException("noMatchFieldName", Integer.toString(i + 1), field1.getName(),
							field2.getName());
				}

				if (field1.getType() != field2.getType()) {
					throw FNProgException.getException("noMatchFieldType", field1.getName(), field2.getName());
				}

				if (field1.getLength() < field2.getLength()) {
					throw FNProgException.getException("noMatchFieldLength", field1.getName(),
							Integer.toString(field1.getLength()), Integer.toString(field2.getLength()));
				}
			}
		} else {
			writer.setFields(fields);
		}
	}

	private int getNumericalSize(FieldDefinition field) {
		int size = field.getSize();
		if (size < 10) {
			size = 10;
		} else if (size > 20) {
			size = 20;
		}
		return size;
	}

	@Override
	public void deleteFile() {
		closeFile();

		if (!useAppend || hasBackup) {
			if (outFile.exists()) {
				outFile.delete();
			}

			if (memoFile.exists()) {
				memoFile.delete();
			}
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
		// Read the user defined list of DB fields
		int index = 0;
		Object[] rowData = new Object[dbInfo2Write.size()];
		for (FieldDefinition field : dbInfo2Write) {
			Object dbValue = dbRecord.get(field.getFieldAlias());
			if (dbValue != null && !dbValue.equals("")) {
				dbValue = convertDataFields(dbValue, field);
			}

			rowData[index++] = dbValue;
		}

		try {
			writer.addRecord(rowData);
		} catch (DBFException ex) {
			throw new FNProgException(ex.getMessage());
		}
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<FieldDefinition> dbDef = getTableModelFields();

		Object[] rowObjects = reader.nextRecord();
		if (rowObjects == null) {
			return result;
		}

		for (int i = 0; i < numFields; i++) {
			FieldDefinition field = dbDef.get(i);
			Object dbValue = rowObjects[i];
			if (field.getFieldType() == FieldTypes.NUMBER && dbValue instanceof Double) {
				dbValue = ((Double) dbValue).intValue();
			}
			result.put(dbFieldNames.get(i), dbValue);
		}
		return result;
	}

	@Override
	public void readTableContents() throws Exception {
		numFields = reader.getFieldCount();
		if (numFields < 1) {
			throw FNProgException.getException("noFields", myDatabase);
		}

		totalRecords = reader.getRecordCount();
		if (reader.getRecordCount() < 1) {
			throw FNProgException.getException("noRecords", myDatabase);
		}

		dbFieldNames.clear();
		dbFieldTypes.clear();

		for (int i = 0; i < numFields; i++) {
			DBFField dbField = reader.getField(i);
			dbFieldNames.add(dbField.getName());
			switch (dbField.getType()) {
			case DATE:
				dbFieldTypes.add(FieldTypes.DATE);
				break;
			case AUTOINCREMENT:
			case CURRENCY:
			case DOUBLE:
			case FLOATING_POINT:
			case LONG:
			case NUMERIC:
				dbFieldTypes.add(dbField.getDecimalCount() == 0 ? FieldTypes.NUMBER : FieldTypes.FLOAT);
				break;
			case LOGICAL:
				dbFieldTypes.add(FieldTypes.BOOLEAN);
				break;
			case MEMO:
				dbFieldTypes.add(FieldTypes.MEMO);
				break;
			case TIMESTAMP:
			case TIMESTAMP_DBASE7:
				dbFieldTypes.add(FieldTypes.TIMESTAMP);
				break;
			default:
				dbFieldTypes.add(FieldTypes.TEXT);
			}
		}
	}
}
