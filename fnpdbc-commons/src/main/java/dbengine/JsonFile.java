package dbengine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;

public class JsonFile extends GeneralDB implements IConvert {
	private File outFile;
	private File backupFile;
	private ObjectMapper mapper = new ObjectMapper();
	private SequenceWriter writer;

	public JsonFile(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean createBackup, boolean isInputFile) throws Exception {
		hasBackup = false;

		if (createBackup) {
			hasBackup = General.copyFile(myFilename, myFilename + ".bak");
		}

		outFile = new File(myFilename);
		backupFile = new File(myFilename + ".bak");

		this.isInputFile = isInputFile;
		if (isInputFile) {
			// TODO load Json file
		} else {
			writer = mapper.writerWithDefaultPrettyPrinter().writeValuesAsArray(outFile);
		}
	}

	@Override
	public void closeFile() {
		try {
			if (!isInputFile) {
				writer.close();
			}
		} catch (Exception e) {
			// Nothing that can be done about this
		}
	}

	@Override
	public void deleteFile() {
		closeFile();
		if (outFile.exists()) {
			outFile.delete();
		}
		if (hasBackup) {
			backupFile.renameTo(outFile);
		}
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		Map<String, Object> map = new LinkedHashMap<>();

		dbInfo2Write.forEach(field -> {
			Object obj = convertDataFields(dbRecord.get(field.getFieldAlias()), field);
			if (!obj.equals("")) {
				map.putIfAbsent(field.getFieldHeader(), obj);
			}
		});

		if (!map.isEmpty()) {
			writer.write(map);
		}
	}

	@Override
	public void createDbHeader() throws Exception {
		// Not used
	}

	@Override
	public void verifyDatabase(List<FieldDefinition> newFields) throws Exception {
		// Not used
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		return null;
	}

}
