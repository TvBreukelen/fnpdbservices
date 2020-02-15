package dbengine.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Hashtable;

public class CsvReader {
	/**
	 * Title: CsvReader Description: Helper Class for reading CSV Files
	 */
	private Reader inputStream;
	private String fileName;
	private Charset charset;

	private boolean trimWhitespace = true;
	private boolean useTextQualifier = true;
	private boolean hasMoreData = true;
	private char textQualifier = '"';
	private char delimiter = ',';
	private char recordDelimiter = ',';
	private char comment = '#';
	private char lastLetter = '\0';
	private int escapeMode = 1;
	private String headers[] = new String[0];
	private int headersCount;

	private Hashtable<String, Integer> headerIndexByName = new Hashtable<>();
	private char columnBuffer[] = new char[100];
	private int columnBufferSize = 100;
	private int maxColumnCount = 10;
	private int columnStarts[] = new int[10];
	private int columnLengths[] = new int[10];
	private char dataBuffer[] = new char[1024];

	private int usedColumnLength;
	private int columnStart;
	private int bufferPosition;
	private int bufferCount;
	private int columnsCount;

	private long currentRecord;
	private boolean startedColumn;
	private boolean startedWithQualifier;
	private boolean hasReadNextLine;
	private boolean readingHeaders;
	private boolean skippingRecord;
	private boolean initialized;
	private boolean closed;

	private boolean mergeConsecutive;
	private boolean useComments;
	private boolean useCustomRecordDelimiter;

	public static final int ESCAPE_MODE_DOUBLED = 1;
	public static final int ESCAPE_MODE_BACKSLASH = 2;

	public CsvReader(String s, char c, Charset charset1) {
		if (s == null) {
			throw new IllegalArgumentException("Parameter fileName can not be null.");
		}

		fileName = s;
		delimiter = c;
		charset = charset1;
	}

	public CsvReader(String s, char c) {
		this(s, c, null);
	}

	public CsvReader(String s) {
		this(s, ',');
	}

	public CsvReader(Reader reader, char c) {
		if (reader == null) {
			throw new IllegalArgumentException("Parameter inputStream can not be null.");
		}

		inputStream = reader;
		delimiter = c;
		initialized = true;
	}

	public CsvReader(Reader reader) {
		this(reader, ',');
	}

	public CsvReader(InputStream inputstream, char c, Charset charset1) {
		this(new InputStreamReader(inputstream, charset1), c);
	}

	public CsvReader(InputStream inputstream, Charset charset1) {
		this(new InputStreamReader(inputstream, charset1));
	}

	public int getColumnCount() {
		return columnsCount;
	}

	public long getCurrentRecord() {
		return currentRecord - 1L;
	}

	public boolean getTrimWhitespace() {
		return trimWhitespace;
	}

	public void setTrimWhitespace(boolean flag) {
		trimWhitespace = flag;
	}

	public char getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(char c) {
		delimiter = c;
	}

	public void setRecordDelimiter(char c) {
		useCustomRecordDelimiter = true;
		recordDelimiter = c;
	}

	public int getEscapeMode() {
		return escapeMode;
	}

	public void setEscapeMode(int i) throws IllegalArgumentException {
		if (i != 1 && i != 2) {
			throw new IllegalArgumentException("Parameter escapeMode must be a valid value.");
		}
		escapeMode = i;
	}

	public boolean getUseTextQualifier() {
		return useTextQualifier;
	}

	public void setUseTextQualifier(boolean flag) {
		useTextQualifier = flag;
	}

	public char getTextQualifier() {
		return textQualifier;
	}

	public void setTextQualifier(char c) {
		textQualifier = c;
	}

	public char getComment() {
		return comment;
	}

	public void setComment(char c) {
		comment = c;
	}

	public boolean getUseComments() {
		return useComments;
	}

	public void setUseComments(boolean flag) {
		useComments = flag;
	}

	public boolean getMergeConsecutive() {
		return mergeConsecutive;
	}

	public void setMergeConsecutive(boolean flag) {
		mergeConsecutive = flag;
	}

	public String[] getHeaders() throws Exception {
		checkClosed();
		return headers;
	}

	public int getHeaderCount() {
		return headersCount;
	}

	public String get(int i) throws Exception {
		checkClosed();
		String s;
		if (i > -1 && i < columnsCount) {
			int j = columnStarts[i];
			int k = columnLengths[i];
			if (k == 0) {
				s = "";
			} else {
				s = new String(columnBuffer, j, k);
			}
		} else {
			s = "";
		}
		return s;
	}

	public String get(String s) throws Exception {
		checkClosed();
		return get(getIndex(s));
	}

	public static CsvReader parse(String s) {
		if (s == null) {
			throw new IllegalArgumentException("Parameter data can not be null.");
		}

		return new CsvReader(new StringReader(s));
	}

	public boolean readRecord() throws Exception {
		checkClosed();
		checkInit();
		clearColumns();
		hasReadNextLine = false;
		if (hasMoreData) {
			do {
				label0: do {
					char c;
					do {
						if (hasReadNextLine || bufferPosition >= bufferCount) {
							break label0;
						}
						c = dataBuffer[bufferPosition++];
						if (useTextQualifier && c == textQualifier) {
							lastLetter = c;
							startedColumn = true;
							startedWithQualifier = true;
							boolean flag = false;
							if (escapeMode == 1) {
								boolean flag2 = false;
								do {
									while (bufferPosition < bufferCount && startedColumn) {
										c = dataBuffer[bufferPosition++];
										if (flag2) {
											if (c == delimiter) {
												endColumn();
											} else if (!useCustomRecordDelimiter && (c == '\r' || c == '\n')
													|| useCustomRecordDelimiter && c == recordDelimiter) {
												endColumn();
												endRecord();
											}
										} else if (c == textQualifier) {
											if (flag) {
												addLetter(textQualifier);
												flag = false;
											} else {
												flag = true;
											}
										} else {
											if (flag) {
												if (c == delimiter) {
													endColumn();
												} else if (!useCustomRecordDelimiter && (c == '\r' || c == '\n')
														|| useCustomRecordDelimiter && c == recordDelimiter) {
													endColumn();
													endRecord();
												} else {
													flag2 = true;
												}
												flag = false;
											} else {
												addLetter(c);
											}
											flag = false;
										}
										lastLetter = c;
									}
									checkDataLength();
								} while (hasMoreData && startedColumn);
							} else {
								boolean flag3 = false;
								boolean flag5 = false;
								boolean flag6 = false;
								int k = 1;
								int l = 0;
								char c2 = '\0';
								do {
									while (startedColumn && bufferPosition < bufferCount) {
										c = dataBuffer[bufferPosition++];
										if (flag3) {
											if (c == delimiter) {
												endColumn();
											} else if (!useCustomRecordDelimiter && (c == '\r' || c == '\n')
													|| useCustomRecordDelimiter && c == recordDelimiter) {
												endColumn();
												endRecord();
											}
										} else if (flag6) {
											l++;
											switch (k) {
											case 1: // '\001'
												c2 *= '\020';
												c2 += hexToDec(c);
												if (l == 4) {
													flag6 = false;
												}
												break;

											case 2: // '\002'
												c2 *= '\b';
												c2 += (char) (c - 48);
												if (l == 3) {
													flag6 = false;
												}
												break;

											case 3: // '\003'
												c2 *= '\n';
												c2 += (char) (c - 48);
												if (l == 3) {
													flag6 = false;
												}
												break;

											case 4: // '\004'
												c2 *= '\020';
												c2 += hexToDec(c);
												if (l == 2) {
													flag6 = false;
												}
												break;
											}
											if (!flag6) {
												addLetter(c2);
											}
										} else if (flag5) {
											switch (c) {
											case 110: // 'n'
												addLetter('\n');
												break;

											case 114: // 'r'
												addLetter('\r');
												break;

											case 116: // 't'
												addLetter('\t');
												break;

											case 98: // 'b'
												addLetter('\b');
												break;

											case 102: // 'f'
												addLetter('\f');
												break;

											case 101: // 'e'
												addLetter('\033');
												break;

											case 118: // 'v'
												addLetter('\013');
												break;

											case 97: // 'a'
												addLetter('\007');
												break;

											case 48: // '0'
											case 49: // '1'
											case 50: // '2'
											case 51: // '3'
											case 52: // '4'
											case 53: // '5'
											case 54: // '6'
											case 55: // '7'
												k = 2;
												flag6 = true;
												l = 1;
												c2 = (char) (c - 48);
												break;

											case 100: // 'd'
											case 111: // 'o'
											case 117: // 'u'
											case 120: // 'x'
												switch (c) {
												case 117: // 'u'
													k = 1;
													break;

												case 120: // 'x'
													k = 4;
													break;

												case 111: // 'o'
													k = 2;
													break;

												case 100: // 'd'
													k = 3;
													break;
												}
												flag6 = true;
												l = 0;
												c2 = '\0';
												break;

											case 56: // '8'
											case 57: // '9'
											case 58: // ':'
											case 59: // ';'
											case 60: // '<'
											case 61: // '='
											case 62: // '>'
											case 63: // '?'
											case 64: // '@'
											case 65: // 'A'
											case 66: // 'B'
											case 67: // 'C'
											case 68: // 'D'
											case 69: // 'E'
											case 70: // 'F'
											case 71: // 'G'
											case 72: // 'H'
											case 73: // 'I'
											case 74: // 'J'
											case 75: // 'K'
											case 76: // 'L'
											case 77: // 'M'
											case 78: // 'N'
											case 79: // 'O'
											case 80: // 'P'
											case 81: // 'Q'
											case 82: // 'R'
											case 83: // 'S'
											case 84: // 'T'
											case 85: // 'U'
											case 86: // 'V'
											case 87: // 'W'
											case 88: // 'X'
											case 89: // 'Y'
											case 90: // 'Z'
											case 91: // '['
											case 92: // '\\'
											case 93: // ']'
											case 94: // '^'
											case 95: // '_'
											case 96: // '`'
											case 99: // 'c'
											case 103: // 'g'
											case 104: // 'h'
											case 105: // 'i'
											case 106: // 'j'
											case 107: // 'k'
											case 108: // 'l'
											case 109: // 'm'
											case 112: // 'p'
											case 113: // 'q'
											case 115: // 's'
											case 119: // 'w'
											default:
												addLetter(c);
												break;
											}
											flag5 = false;
										} else if (c == '\\') {
											flag5 = true;
										} else if (c == textQualifier) {
											flag3 = true;
										} else if (flag) {
											if (c == delimiter) {
												endColumn();
											} else if (!useCustomRecordDelimiter && (c == '\r' || c == '\n')
													|| useCustomRecordDelimiter && c == recordDelimiter) {
												endColumn();
												endRecord();
											} else {
												addLetter(textQualifier);
												addLetter(c);
											}
											flag = false;
										} else {
											addLetter(c);
										}
										lastLetter = c;
									}
									checkDataLength();
								} while (hasMoreData && startedColumn);
							}
						} else if (c == delimiter) {
							lastLetter = c;
							if (!mergeConsecutive) {
								endColumn();
							}
						} else if (!useCustomRecordDelimiter && (c == '\r' || c == '\n')
								|| useCustomRecordDelimiter && c == recordDelimiter) {
							if (columnsCount > 0 || columnStart != usedColumnLength) {
								endColumn();
								endRecord();
							}
							lastLetter = c;
						} else {
							if (!useComments || columnsCount != 0 || c != comment) {
								continue;
							}
							lastLetter = c;
							skipLine();
						}
						continue label0;
					} while (trimWhitespace && (c == ' ' || c == '\t'));
					startedColumn = true;
					startedWithQualifier = false;
					boolean flag1 = false;
					boolean flag4 = false;
					int i = 1;
					int j = 0;
					char c1 = '\0';
					boolean flag7 = true;
					do {
						do {
							if (!flag7) {
								c = dataBuffer[bufferPosition++];
							}
							if (!useTextQualifier && escapeMode == 2 && c == '\\') {
								if (flag1) {
									addLetter('\\');
									flag1 = false;
								} else {
									flag1 = true;
								}
							} else if (flag4) {
								j++;
								switch (i) {
								case 1: // '\001'
									c1 *= '\020';
									c1 += hexToDec(c);
									if (j == 4) {
										flag4 = false;
									}
									break;

								case 2: // '\002'
									c1 *= '\b';
									c1 += (char) (c - 48);
									if (j == 3) {
										flag4 = false;
									}
									break;

								case 3: // '\003'
									c1 *= '\n';
									c1 += (char) (c - 48);
									if (j == 3) {
										flag4 = false;
									}
									break;

								case 4: // '\004'
									c1 *= '\020';
									c1 += hexToDec(c);
									if (j == 2) {
										flag4 = false;
									}
									break;
								}
								if (!flag4) {
									addLetter(c1);
								}
							} else if (!useTextQualifier && escapeMode == 2 && flag1) {
								switch (c) {
								case 110: // 'n'
									addLetter('\n');
									break;

								case 114: // 'r'
									addLetter('\r');
									break;

								case 116: // 't'
									addLetter('\t');
									break;

								case 98: // 'b'
									addLetter('\b');
									break;

								case 102: // 'f'
									addLetter('\f');
									break;

								case 101: // 'e'
									addLetter('\033');
									break;

								case 118: // 'v'
									addLetter('\013');
									break;

								case 97: // 'a'
									addLetter('\007');
									break;

								case 48: // '0'
								case 49: // '1'
								case 50: // '2'
								case 51: // '3'
								case 52: // '4'
								case 53: // '5'
								case 54: // '6'
								case 55: // '7'
									i = 2;
									flag4 = true;
									j = 1;
									c1 = (char) (c - 48);
									break;

								case 100: // 'd'
								case 111: // 'o'
								case 117: // 'u'
								case 120: // 'x'
									switch (c) {
									case 117: // 'u'
										i = 1;
										break;

									case 120: // 'x'
										i = 4;
										break;

									case 111: // 'o'
										i = 2;
										break;

									case 100: // 'd'
										i = 3;
										break;
									}
									flag4 = true;
									j = 0;
									c1 = '\0';
									break;

								case 56: // '8'
								case 57: // '9'
								case 58: // ':'
								case 59: // ';'
								case 60: // '<'
								case 61: // '='
								case 62: // '>'
								case 63: // '?'
								case 64: // '@'
								case 65: // 'A'
								case 66: // 'B'
								case 67: // 'C'
								case 68: // 'D'
								case 69: // 'E'
								case 70: // 'F'
								case 71: // 'G'
								case 72: // 'H'
								case 73: // 'I'
								case 74: // 'J'
								case 75: // 'K'
								case 76: // 'L'
								case 77: // 'M'
								case 78: // 'N'
								case 79: // 'O'
								case 80: // 'P'
								case 81: // 'Q'
								case 82: // 'R'
								case 83: // 'S'
								case 84: // 'T'
								case 85: // 'U'
								case 86: // 'V'
								case 87: // 'W'
								case 88: // 'X'
								case 89: // 'Y'
								case 90: // 'Z'
								case 91: // '['
								case 92: // '\\'
								case 93: // ']'
								case 94: // '^'
								case 95: // '_'
								case 96: // '`'
								case 99: // 'c'
								case 103: // 'g'
								case 104: // 'h'
								case 105: // 'i'
								case 106: // 'j'
								case 107: // 'k'
								case 108: // 'l'
								case 109: // 'm'
								case 112: // 'p'
								case 113: // 'q'
								case 115: // 's'
								case 119: // 'w'
								default:
									addLetter(c);
									break;
								}
								flag1 = false;
							} else if (c == delimiter) {
								endColumn();
							} else if (!useCustomRecordDelimiter && (c == '\r' || c == '\n')
									|| useCustomRecordDelimiter && c == recordDelimiter) {
								endColumn();
								endRecord();
							} else {
								addLetter(c);
							}
							lastLetter = c;
							flag7 = false;
						} while (startedColumn && bufferPosition < bufferCount);
						checkDataLength();
					} while (hasMoreData && startedColumn);
				} while (true);
				checkDataLength();
			} while (hasMoreData && !hasReadNextLine);
			if (!hasMoreData
					&& (lastLetter == delimiter || !useCustomRecordDelimiter && lastLetter != '\r' && lastLetter != '\n'
							|| useCustomRecordDelimiter && lastLetter != recordDelimiter)) {
				endColumn();
				endRecord();
			}
		}
		return hasReadNextLine;
	}

	private void checkDataLength() throws Exception {
		if (bufferPosition == bufferCount) {
			try {
				bufferCount = inputStream.read(dataBuffer, 0, 1024);
			} catch (IOException ioexception) {
				close();
				throw ioexception;
			}
			bufferPosition = 0;
			if (bufferCount <= 0) {
				hasMoreData = false;
			}
		}
	}

	private void clearColumns() {
		columnsCount = 0;
		columnStart = 0;
		usedColumnLength = 0;
	}

	private void checkInit() throws FileNotFoundException {
		if (!initialized) {
			if (fileName != null) {
				if (charset != null) {
					inputStream = new InputStreamReader(new FileInputStream(fileName), charset);
				} else {
					inputStream = new InputStreamReader(new FileInputStream(fileName));
				}
			}

			initialized = true;
		}
	}

	public boolean readHeaders() throws Exception {
		readingHeaders = true;
		boolean flag = readRecord();
		readingHeaders = false;
		clearColumns();
		return flag;
	}

	public String getHeader(int i) throws Exception {
		checkClosed();
		if (i > -1 && i < headersCount) {
			return headers[i];
		}

		return "";
	}

	private void endColumn() throws Exception {
		startedColumn = false;
		if (!skippingRecord) {
			if (columnsCount > 0x186a0) {
				close();
				throw new Exception("Max column count of 100000 exceeded in record " + currentRecord + ".");
			}
			if (columnsCount == maxColumnCount) {
				int i = maxColumnCount + Math.max(1, (int) (maxColumnCount * 1.0D / 2D));
				int ai[] = new int[i];
				int ai1[] = new int[i];
				System.arraycopy(columnStarts, 0, ai, 0, maxColumnCount);
				System.arraycopy(columnLengths, 0, ai1, 0, maxColumnCount);
				columnStarts = ai;
				columnLengths = ai1;
				maxColumnCount = i;
			}
			if (usedColumnLength - columnStart > 0) {
				if (trimWhitespace && !startedWithQualifier) {
					int j = columnStart;
					int k = usedColumnLength - 1;
					for (; j < usedColumnLength && (columnBuffer[j] == ' ' || columnBuffer[j] == '\t'); j++) {

					}
					if (j < usedColumnLength - 1) {
						for (; k > j && (columnBuffer[k] == ' ' || columnBuffer[k] == '\t'); k--) {

						}
					}
					columnStarts[columnsCount] = j;
					columnLengths[columnsCount] = k - j + 1;
				} else {
					columnStarts[columnsCount] = columnStart;
					columnLengths[columnsCount] = usedColumnLength - columnStart;
				}
				columnStart = usedColumnLength;
			} else {
				columnStarts[columnsCount] = 0;
				columnLengths[columnsCount] = 0;
			}
			columnsCount++;
		}
	}

	private void endRecord() throws Exception {
		if (!skippingRecord) {
			if (columnsCount > 1 || usedColumnLength > 0 || startedWithQualifier) {
				hasReadNextLine = true;
				if (readingHeaders) {
					headersCount = columnsCount;
					headers = new String[headersCount];
					for (int i = 0; i < headersCount; i++) {
						String s = get(i);
						headers[i] = s;
						headerIndexByName.put(s, i);
					}
				} else {
					currentRecord++;
				}
			} else {
				clearColumns();
			}
		} else {
			hasReadNextLine = true;
		}
	}

	private void addLetter(char c) throws Exception {
		if (!skippingRecord) {
			if (usedColumnLength > 0x186a0) {
				close();
				throw new Exception("Max column length of 100000 exceeded in column " + columnsCount + " in record "
						+ currentRecord + ".");
			}
			if (usedColumnLength == columnBufferSize) {
				int i = columnBufferSize + Math.max(1, (int) (columnBufferSize * 1.0D / 2D));
				char ac[] = new char[i];
				System.arraycopy(columnBuffer, 0, ac, 0, columnBufferSize);
				columnBuffer = ac;
				columnBufferSize = i;
			}
			columnBuffer[usedColumnLength++] = c;
		}
	}

	public int getIndex(String s) throws Exception {
		checkClosed();
		Integer result = headerIndexByName.get(s);
		if (result != null) {
			return result;
		}

		return -1;
	}

	public boolean skipRecord() throws Exception {
		checkClosed();
		boolean flag = false;
		if (hasMoreData) {
			skippingRecord = true;
			flag = readRecord();
			skippingRecord = false;
		}
		return flag;
	}

	public boolean skipLine() throws Exception {
		checkClosed();
		clearColumns();
		boolean flag = false;
		if (hasMoreData) {
			checkInit();
			boolean flag1;
			do {
				for (flag1 = false; bufferPosition < bufferCount && !flag1;) {
					flag = true;
					char c = dataBuffer[bufferPosition++];
					if (c == '\r' || c == '\n') {
						flag1 = true;
					}
					lastLetter = c;
				}

				checkDataLength();
			} while (hasMoreData && !flag1);
		}
		return flag;
	}

	public int getLength(int i) throws Exception {
		checkClosed();
		if (i < columnsCount && i > -1) {
			return columnLengths[i];
		}

		return 0;
	}

	public void close() {
		if (!closed) {
			close(true);
			closed = true;
		}
	}

	protected void close(boolean flag) {
		if (flag) {
			charset = null;
			headers = null;
			headerIndexByName = null;
			columnBuffer = null;
			dataBuffer = null;
			columnStarts = null;
			columnLengths = null;
		}
		try {
			if (initialized) {
				inputStream.close();
			}
			inputStream = null;
		} catch (Exception exception) {
		}
	}

	private void checkClosed() throws Exception {
		if (closed) {
			throw new Exception("CSV Resources have already been freed");
		}
	}

	@Override
	protected void finalize() {
		close(false);
	}

	private static char hexToDec(char c) {
		char c1;
		if (c >= 'a') {
			c1 = (char) (c - 97 + 10);
		} else if (c >= 'A') {
			c1 = (char) (c - 65 + 10);
		} else {
			c1 = (char) (c - 48);
		}
		return c1;
	}
}