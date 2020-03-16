package fnprog2pda.software;

import java.util.List;
import java.util.Map;

public class SoftCAT extends FNProgramvare {
	/**
	 * Title: SoftCAT Description: SoftCAT Class Copyright: (c) 2003-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		myLastIndex = Math.max(myLastIndex, (Integer) dbDataRecord.get(myTableID));
	}
}