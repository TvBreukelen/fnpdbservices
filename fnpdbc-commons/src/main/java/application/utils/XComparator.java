package application.utils;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import application.interfaces.FieldTypes;

public class XComparator implements Comparator<Object> {
	/**
	 * Title: XComparator Description: Comparator Class for sorting an List of Lists
	 * Copyright: (c) 2004-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private Map<String, FieldTypes> stringFields; // String of fields to be sorted
	private FieldTypes field;

	public XComparator(Map<String, FieldTypes> fields) {
		stringFields = fields;
	}

	public XComparator(FieldTypes field) {
		this.field = field;
	}

	@Override
	public int compare(Object obj1, Object obj2) {
		if (stringFields != null) {
			return compareMap(obj1, obj2);
		}
		return compareField(obj1, obj2, field);
	}

	@SuppressWarnings("unchecked")
	private int compareMap(Object obj1, Object obj2) {
		int result = 0;
		Map<String, Object> flds1 = (Map<String, Object>) obj1;
		Map<String, Object> flds2 = (Map<String, Object>) obj2;

		for (Entry<String, FieldTypes> entry : stringFields.entrySet()) {
			result = compareField(flds1.get(entry.getKey()), flds2.get(entry.getKey()), entry.getValue());

			if (result != 0) {
				return result;
			}
		}
		return result;
	}

	private int compareField(Object obj1, Object obj2, FieldTypes field) {
		String s1 = obj1 == null ? "" : obj1.toString();
		String s2 = obj2 == null ? "" : obj2.toString();
		switch (field) {
		case FLOAT:
		case NUMBER:
			Double d1 = s1.isEmpty() ? Double.MIN_VALUE : Double.valueOf(s1);
			Double d2 = s2.isEmpty() ? Double.MIN_VALUE : Double.valueOf(s2);
			return d1.compareTo(d2);
		default:
			return s1.compareTo(s2);
		}
	}
}