package application.utils;

import java.util.Comparator;
import java.util.Map;

import application.interfaces.FieldTypes;

public class XComparator implements Comparator<Object> {
	/**
	 * Title: XComparator Description: Comparator Class for sorting an List of Lists
	 * Copyright: (c) 2004-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private Map<String, FieldTypes> _stringFields; // String of fields to be sorted
	private FieldTypes _field;

	public XComparator(Map<String, FieldTypes> fields) {
		_stringFields = fields;
	}

	public XComparator(FieldTypes field) {
		_field = field;
	}

	@Override
	public int compare(Object obj1, Object obj2) {
		if (_stringFields != null) {
			return compareMap(obj1, obj2);
		}
		return compareField(obj1, obj2);
	}

	@SuppressWarnings("unchecked")
	public int compareMap(Object obj1, Object obj2) {
		int result = 0;
		Map<String, Object> flds1 = (Map<String, Object>) obj1;
		Map<String, Object> flds2 = (Map<String, Object>) obj2;

		for (String string : _stringFields.keySet()) {
			_field = _stringFields.get(string);
			result = compareField(flds1.get(string), flds2.get(string));

			if (result != 0) {
				return result;
			}
		}
		return result;
	}

	public int compareField(Object obj1, Object obj2) {
		String s1 = obj1.toString();
		String s2 = obj2.toString();
		switch (_field) {
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