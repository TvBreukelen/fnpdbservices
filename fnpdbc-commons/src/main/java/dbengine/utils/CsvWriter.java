package dbengine.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;

import application.utils.FNProgException;

public class CsvWriter {
	/**
	 * Title: CsvWriter Description: Helper Class for writing CSV Files
	 */
	private File csvFile;
	private RandomAccessFile csvRaf;
	private ByteArrayOutputStream baos;
	private String csvEncoding;

	private boolean firstColumn = true;
	private char fieldDelimiter = ',';
	private char textQualifier = '"';
	private boolean useTextQualifier = true;
	private String singleQualifier;
	private String doubleQualifier;
	private long prevPointer;
	private int maxLineSize;

	public CsvWriter(File file, String encoding) throws Exception {
		csvFile = file;
		csvEncoding = encoding;

		try {
			csvRaf = new RandomAccessFile(csvFile, "rw");
			baos = new ByteArrayOutputStream();
		} catch (Exception e) {
			throw FNProgException.getException("cannotOpen", csvFile.getAbsolutePath(), e.getMessage());
		}

		initTextQualifier();
	}

	public char getDelimiter() {
		return fieldDelimiter;
	}

	public void setDelimiter(char c) {
		fieldDelimiter = c;
	}

	public char getTextQualifier() {
		return textQualifier;
	}

	public void setTextQualifier(char c) {
		textQualifier = c;
		initTextQualifier();
	}

	public boolean getUseTextQualifier() {
		return useTextQualifier;
	}

	public void setUseTextQualifier(boolean flag) {
		useTextQualifier = flag;
	}

	public void write(String s, boolean flag) throws Exception {
		Writer w = csvEncoding.equals("") ? new OutputStreamWriter(baos) : new OutputStreamWriter(baos, csvEncoding);

		if (s == null) {
			s = "";
		}
		int i = s.length();
		if (firstColumn) {
			if (i == 0) {
				w.write(doubleQualifier);
			}
			firstColumn = false;
		} else {
			w.write(fieldDelimiter);
		}
		if (i > 0) {
			boolean flag1 = false;
			if (!flag) {
				s = s.trim();
			} else if (useTextQualifier) {
				char c = s.charAt(0);
				if (c == ' ' || c == '\t') {
					flag1 = true;
				}
				if (!flag1 && i > 1) {
					char c1 = s.charAt(i - 1);
					if (c1 == ' ' || c1 == '\t') {
						flag1 = true;
					}
				}
			}
			if (!flag1 && (s.indexOf(fieldDelimiter) > -1 || s.indexOf('\n') > -1 || s.indexOf('\r') > -1
					|| s.indexOf(textQualifier) > -1)) {
				flag1 = true;
			}
			if (flag1) {
				w.write(textQualifier);
				s = s.replaceAll(singleQualifier, doubleQualifier);
			}
			w.write(s);
			if (flag1) {
				w.write(textQualifier);
			}
		}

		w.flush();
		w.close();
		csvRaf.write(baos.toByteArray());
		baos.reset();
	}

	public void write(String s) throws Exception {
		write(s, false);
	}

	private void initTextQualifier() {
		singleQualifier = "";
		singleQualifier += textQualifier;
		doubleQualifier = singleQualifier + singleQualifier;
	}

	public void endRecord() throws Exception {
		csvRaf.writeBytes("\r\n");
		firstColumn = true;
		maxLineSize = Math.max(maxLineSize, (int) (getSize() - prevPointer));
		prevPointer = getSize();
	}

	public long getSize() throws Exception {
		return csvRaf.getFilePointer();
	}

	public int getMaxLineSize() {
		return maxLineSize;
	}

	public void close() {
		if (csvRaf != null) {
			try {
				csvRaf.close();
			} catch (Exception e) {
				// Nothing to to here
			}
			csvRaf = null;
		}
	}
}