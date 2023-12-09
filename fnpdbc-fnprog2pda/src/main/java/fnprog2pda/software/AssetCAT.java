package fnprog2pda.software;

import java.util.List;
import java.util.Map;

public class AssetCAT extends FNProgramvare {
	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		lastIndex = Math.max(lastIndex, (Integer) dbDataRecord.get(myTableID));
	}

}
