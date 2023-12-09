package fnprog2pda.software;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StampCAT extends FNProgramvare {
	/**
	 * Title: StampCAT Description: StampCAT Class Copyright: (c) 2003-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private boolean useCatalogNo = false;

	@Override
	protected List<String> getSystemFields(List<String> userFields) {
		List<String> result = new ArrayList<>(10);

		useCatalogNo = userFields.contains("CatalogNo");
		if (useCatalogNo) {
			result.add("CatalogNo1");
			result.add("CatalogNo2");
			result.add("CatalogNo3");
		}

		return result;
	}

	@Override
	protected void setDatabaseData(Map<String, Object> dbDataRecord, Map<String, List<Map<String, Object>>> hashTable)
			throws Exception {
		if (useCatalogNo) {
			StringBuilder buf = new StringBuilder(dbDataRecord.get("CatalogNo1").toString().trim());
			buf.append(dbDataRecord.get("CatalogNo2").toString().trim());
			buf.append(dbDataRecord.get("CatalogNo3").toString().trim());
			dbDataRecord.put("CatalogNo", buf.toString());
		}
		lastIndex = Math.max(lastIndex, (Integer) dbDataRecord.get(myTableID));
	}
}