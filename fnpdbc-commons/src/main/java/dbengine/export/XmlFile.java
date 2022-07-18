package dbengine.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import application.preferences.Profiles;
import application.utils.FieldDefinition;
import dbengine.GeneralDB;
import dbengine.IConvert;
import dbengine.utils.XmlReader;

public class XmlFile extends GeneralDB implements IConvert {
	private File outFile;
	private Writer out;
	private Map<String, String> hElements;
	private Map<Integer, FieldDefinition> hFields;
	private List<FieldDefinition> dbWrite;
	private Document doc;
	private Element results;
	private Element[] nodes;
	private int index;
	private XmlReader handler;
	private int currentRecord = 0;

	protected List<Map<String, Object>> dbRecords;

	public XmlFile(Profiles pref) {
		super(pref);
		hElements = new LinkedHashMap<>();
		hFields = new HashMap<>();
		dbWrite = new ArrayList<>();
		index = 0;
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		outFile = new File(myDatabase);
		this.isInputFile = isInputFile;

		if (isInputFile) {
			splitDbInfoToWrite();

			handler = new XmlReader();
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			parser.parse(myDatabase, handler);
		} else {
			outFile.delete();
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
		}
	}

	@Override
	public String getPdaDatabase() {
		return handler == null ? null : handler.getRootElement();
	}

	@Override
	public void closeFile() {
		if (out != null) {
			try {
				out.flush();
				out.close();
			} catch (Exception e) {
				// Do Nothing?
			}
			out = null;
			outFile = null;
		}
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		if (hElements.isEmpty()) {
			nodes[index] = doc.createElement("Row");
		} else {
			int i = -1;
			for (Entry<String, String> entry : hElements.entrySet()) {
				i++;
				String oValue = entry.getValue();
				String nValue = dbRecord.get(entry.getKey()).toString();

				if (!oValue.equals(nValue)) {
					hElements.put(entry.getKey(), nValue);

					nodes[i] = doc.createElement(hFields.get(i).getFieldHeader());
					nodes[i].setAttribute("value", nValue);

					if (i == 0) {
						results.appendChild(nodes[i]);
					} else {
						nodes[i - 1].appendChild(nodes[i]);
					}

					int idx = i + 1;
					for (int j = idx; j < hElements.size(); j++) {
						hElements.put(hFields.get(j).getFieldAlias(), "x!x!x!x!x");
					}

					index = i;
				}
			}
		}

		createXmlDocument(dbRecord);
	}

	private void createXmlDocument(Map<String, Object> dbRecord) {
		for (FieldDefinition field : dbWrite) {
			Object dbField = convertDataFields(dbRecord.get(field.getFieldAlias()), field);
			if (StringUtils.isEmpty(dbField.toString())) {
				continue;
			}

			Element el = doc.createElement(field.getFieldHeader());
			el.appendChild(doc.createTextNode(dbField.toString()));
			nodes[index].appendChild(el);
		}

		if (hElements.isEmpty()) {
			results.appendChild(nodes[index]);
		}
	}

	@Override
	public void createDbHeader() throws Exception {
		String xmlRoot = eliminateIllegalXmlCharacters(myPref.getPdaDatabaseName());
		if (xmlRoot.equals("")) {
			xmlRoot = "Results";
		}

		DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

		DocumentBuilder builder = df.newDocumentBuilder();
		doc = builder.newDocument();
		results = doc.createElement(xmlRoot);
		doc.appendChild(results);

		hElements.clear();
		myPref.getGroupFields().forEach(s -> hElements.put(s, ""));
		nodes = new Element[hElements.isEmpty() ? 1 : 4];
		splitDbInfoToWrite();
	}

	private void splitDbInfoToWrite() {
		boolean isSortFields = !isInputFile && myPref.isGroupFieldDefined();
		hFields.clear();
		dbWrite.clear();

		for (FieldDefinition field : dbInfo2Write) {
			// Clone FieldDefinition Field and remove all illegal XML characters from the
			// header
			FieldDefinition fld = field.copy();
			fld.setFieldHeader(eliminateIllegalXmlCharacters(field.getFieldHeader()));

			// Split DbInfoToWrite in "sort" and "normal" elements
			if (isSortFields) {
				int i = -1;
				boolean isFound = false;
				for (String s : hElements.keySet()) {
					i++;
					if (s.equals(field.getFieldAlias())) {
						hFields.put(i, fld);
						isFound = true;
						break;
					}
				}

				if (!isFound) {
					dbWrite.add(fld);
				}
			} else {
				dbWrite.add(fld);
			}
		}
	}

	@Override
	public void closeData() {
		DOMSource domSource = new DOMSource(doc);
		TransformerFactory tf = TransformerFactory.newInstance();
		tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		tf.setAttribute("indent-number", 4);

		try {
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StreamResult sr = new StreamResult(out);
			transformer.transform(domSource, sr);
		} catch (Exception e) {
			// Nothing to do
		}
	}

	@Override
	public void readTableContents() throws Exception {
		dbFieldNames = handler.getFieldNames();
		dbFieldTypes = handler.getFieldTypes();
		dbRecords = handler.getDbRecords();
		totalRecords = dbRecords.size();
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		Map<String, Object> result = dbRecords.get(currentRecord);
		dbRecords.set(currentRecord, null); // Cleanup memory usage
		currentRecord++;
		return result;
	}

	private String eliminateIllegalXmlCharacters(String element) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < element.length(); i++) {
			char c = element.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				buf.append(element.charAt(i));
			} else {
				if (c == ' ') {
					buf.append("_");
				}
			}
		}
		return buf.toString();
	}
}
