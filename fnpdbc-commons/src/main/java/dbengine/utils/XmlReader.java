package dbengine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import application.interfaces.FieldTypes;

public class XmlReader extends DefaultHandler {
	private ArrayList<String> fieldNames = new ArrayList<>();
	private ArrayList<FieldTypes> fieldTypes = new ArrayList<>();
	private HashSet<String> fieldSet = new HashSet<>();
	private ArrayList<HashMap<String, Object>> dbRecords = new ArrayList<>();
	private HashMap<String, Object> workRecord = new HashMap<>();
	private HashSet<String> processList = new HashSet<>();
	private StringBuilder buf = new StringBuilder();
	private String rootElement = "";

	@Override
	public void characters(char[] text, int start, int length) throws SAXException {
		boolean isLineFeed = false;
		try {
			if (text[start] == '\n') {
				start++;
				length--;
				isLineFeed = true;
			}

			String s = new String(text, start, length).trim();
			if (s.equals("")) {
				return;
			}

			if (isLineFeed) {
				buf.append("\n");
			}

			buf.append(s);
		} catch (Exception e) {
			throw new SAXException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void startElement(String uri, String name, String qName, Attributes atts) {
		if (rootElement.equals("")) {
			rootElement = name;
			return;
		}

		if (processList.contains(name)) {
			// New block
			dbRecords.add((HashMap<String, Object>) workRecord.clone());
			processList.clear();

			int index = fieldNames.indexOf(name);
			if (index == -1) {
				if (atts.getLength() > 0) {
					index = fieldNames.indexOf(name + "." + atts.getLocalName(0));
				}

				if (index == -1) {
					index = 0;
				}
			}

			for (int i = index; i < fieldNames.size(); i++) {
				workRecord.put(fieldNames.get(i), "");
			}
		}

		processList.add(name);

		for (int i = 0; i < atts.getLength(); i++) {
			String s = name + "." + atts.getLocalName(i);
			fillFieldNamesAndTypes(s);
			workRecord.put(s, atts.getValue(i));
		}
	}

	private void fillFieldNamesAndTypes(String name) {
		if (!fieldSet.contains(name)) {
			fieldSet.add(name);
			fieldNames.add(name);
			fieldTypes.add(FieldTypes.TEXT);
		}
	}

	@Override
	public void endElement(String uri, String name, String qName) {
		if (buf.length() > 0) {
			fillFieldNamesAndTypes(name);
			workRecord.put(name, buf.toString());
			buf = new StringBuilder();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void endDocument() {
		dbRecords.add((HashMap<String, Object>) workRecord.clone());
	}

	public String getRootElement() {
		return rootElement;
	}

	public ArrayList<String> getFieldNames() {
		return fieldNames;
	}

	public ArrayList<FieldTypes> getFieldTypes() {
		return fieldTypes;
	}

	public ArrayList<HashMap<String, Object>> getDbRecords() {
		return dbRecords;
	}
}
