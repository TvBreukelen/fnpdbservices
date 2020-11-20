package dbengine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import application.interfaces.FieldTypes;

public class XmlReader extends DefaultHandler {
	private List<String> fieldNames = new ArrayList<>();
	private List<FieldTypes> fieldTypes = new ArrayList<>();
	private Set<String> fieldSet = new HashSet<>();
	private List<HashMap<String, Object>> dbRecords = new ArrayList<>();
	private HashMap<String, Object> workRecord = new HashMap<>();
	private Set<String> processList = new HashSet<>();
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
			if (s.isEmpty()) {
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
			rootElement = qName;
			return;
		}

		if (processList.contains(qName)) {
			// New block
			dbRecords.add((HashMap<String, Object>) workRecord.clone());
			processList.clear();

			int index = fieldNames.indexOf(qName);
			if (index == -1) {
				if (atts.getLength() > 0) {
					index = fieldNames.indexOf(qName + "." + atts.getLocalName(0));
				}

				if (index == -1) {
					index = 0;
				}
			}

			for (int i = index; i < fieldNames.size(); i++) {
				workRecord.put(fieldNames.get(i), "");
			}
		}

		processList.add(qName);

		for (int i = 0; i < atts.getLength(); i++) {
			String s = qName + "." + atts.getLocalName(i);
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
			fillFieldNamesAndTypes(qName);
			workRecord.put(qName, buf.toString());
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

	public List<String> getFieldNames() {
		return fieldNames;
	}

	public List<FieldTypes> getFieldTypes() {
		return fieldTypes;
	}

	public List<HashMap<String, Object>> getDbRecords() {
		return dbRecords;
	}
}
