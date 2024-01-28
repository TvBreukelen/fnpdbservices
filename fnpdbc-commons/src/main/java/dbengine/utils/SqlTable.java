package dbengine.utils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import application.utils.FieldDefinition;

public class SqlTable {
	private String name;
	private List<FieldDefinition> dbFields;
	private Set<String> pkList = new HashSet<>();
	private Map<String, ForeignKey> fkList = new LinkedHashMap<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<FieldDefinition> getDbFields() {
		return dbFields;
	}

	public void setDbFields(List<FieldDefinition> dbFields) {
		this.dbFields = dbFields;
	}

	public Set<String> getPkList() {
		return pkList;
	}

	public Map<String, ForeignKey> getFkList() {
		return fkList;
	}

	public void setFkList(Map<String, ForeignKey> fkMap) {
		fkList.clear();
		fkList.putAll(fkMap);
	}
}
