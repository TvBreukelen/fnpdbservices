package dbengine.utils;

import java.util.HashSet;
import java.util.Set;

public class SpecialFields {
	private Set<String> specialFields = new HashSet<>();

	public void addField(String field) {
		specialFields.add(field);
	}

	public Set<String> getSpecialFields() {
		return specialFields;
	}
}
