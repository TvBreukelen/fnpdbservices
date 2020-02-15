package dbengine.export;

import java.io.IOException;
import java.util.Map;

import application.preferences.Profiles;
import application.utils.General;

public class JFile3 extends JFile4 {
	public JFile3(Profiles pref) {
		super(pref);
	}

	@Override
	protected int calculateFieldRecord() throws Exception {
		int[] recordID = setPointer2NextRecord();
		return recordID[2] - recordID[0];
	}

	@Override
	protected void setPadding() throws IOException {
		pdbRaf.write(new byte[62]);
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		StringBuilder bf = new StringBuilder();

		// Read the user defined list of DB fields
		dbInfo2Write.forEach(field -> bf
				.append(convertDataFields(dbRecord.get(field.getFieldAlias()), field).toString()).append('\0'));
		pdbDas.write(General.convertString2Bytes(bf.toString(), encoding));
		writeRecord(pdbBaos.toByteArray(), 0);
		pdbBaos.reset();
	}
}
