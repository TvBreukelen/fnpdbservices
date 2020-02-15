package fnprog2pda.software;

import java.awt.Component;
import java.util.List;
import java.util.Map;

public class AssetCAT extends FNProgramvare {
	public AssetCAT(Component myParent) throws Exception {
		super(myParent);
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		myLastIndex = Math.max(myLastIndex, (Integer) dbDataRecord.get(myTableID));
	}

}
