package dbengine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.FieldDefinition;
import application.utils.General;

public abstract class SqlDB extends GeneralDB implements IConvert {
	private String myTable;
	private boolean isFirstRead = true;

	private Map<Integer, List<FieldDefinition>> hTables;
	private List<String> aTables;

	protected Connection connection;
	protected boolean isConnected;
	private ResultSet dbResultSet;
	private Statement dbStatement;
	private Session session;
	protected int assignedPort;

	protected SqlDB(Profiles pref) {
		super(pref);
	}

	protected void getSshSession() throws Exception {
		// Remote host
		final String remoteHost = myHelper.getSshHost();

		boolean usePrivateKey = !myHelper.getPrivateKeyFile().isEmpty();
		boolean usePassword = !myHelper.getSshPassword().isEmpty();

		JSch jsch = new JSch();

		// Check if a private key is provided
		if (usePrivateKey) {
			verifyKeyFile(myHelper.getPrivateKeyFile());

			if (usePassword) {
				jsch.addIdentity(myHelper.getPrivateKeyFile(), General.decryptPassword(myHelper.getSshPassword()));
			} else {
				jsch.addIdentity(myHelper.getPrivateKeyFile());
			}
		}

		// Create SSH session. Port 22 is the default SSH port which is open in your
		// firewall setup.
		session = jsch.getSession(myHelper.getSshUser(), remoteHost, myHelper.getSshPort());

		if (usePassword && !usePrivateKey) {
			session.setPassword(General.decryptPassword(myHelper.getSshPassword()));
		}

		// Additional SSH options. See your ssh_config manual for more options. Set
		// options according to your requirements.
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no"); // Not really wanted..
		config.put("Compression", "yes");
		config.put("ConnectionAttempts", "2");

		session.setConfig(config);

		// Connect
		session.connect();

		// Create the tunnel through port forwarding. This is basically instructing jsch
		// session to send data received from local_port in the local machine to
		// remote_port of the remote_host assigned_port is the port assigned by jsch for
		// use, it may not always be the same as local_port.

		assignedPort = session.setPortForwardingL(0, remoteHost, myHelper.getPort());

		if (assignedPort == 0) {
			throw new JSchException("Port forwarding failed !");
		}
	}

	protected void verifyKeyFile(String keyFile) throws Exception {
		if (!General.existFile(keyFile)) {
			// Should not occur unless file has been deleted
			throw FNProgException.getException("noDatabaseExists", keyFile);
		}

		boolean hasBeginTag = false;
		boolean hasEndTag = false;

		try (Scanner sc = new Scanner(new File(keyFile))) {
			while (sc.hasNext()) {
				String line = sc.nextLine();
				if (line.startsWith("-----BEGIN ")) {
					hasBeginTag = true;
				} else if (line.startsWith("-----END ")) {
					hasEndTag = true;
					break;
				}
			}
		}

		if (!(hasBeginTag && hasEndTag)) {
			throw FNProgException.getException("invalidKeyfile", keyFile);
		}
	}

	@Override
	public void readTableContents() throws Exception {
		// read all tables in the database
		hTables = new HashMap<>();
		aTables = new ArrayList<>();

		int index = 0;
		String db = myImportFile.isConnectHost() ? myDatabase.substring(myDatabase.indexOf("/") + 1) : myDatabase;

		try {
			DatabaseMetaData metaData = connection.getMetaData();
			String[] types = myImportFile == ExportFile.POSTGRESQL ? new String[] { "TABLE", "VIEW" } : null;
			ResultSet rs = metaData.getTables(null, null, "%", types);
			while (rs.next()) {
				String tableCat = rs.getString("TABLE_CAT");
				if (tableCat != null && !tableCat.equals(db)) {
					continue;
				}

				String table = rs.getString(3);
				ResultSet columns;
				try {
					columns = metaData.getColumns(null, null, table, null);
				} catch (Exception e) {
					continue;
				}

				List<FieldDefinition> aFields = new ArrayList<>();
				while (columns.next()) {
					String field = columns.getString(4);
					FieldDefinition fieldDef = new FieldDefinition(field, field, FieldTypes.TEXT);
					fieldDef.setSQLType(columns.getInt(5));

					String type = columns.getString(6);
					if (!(type.isEmpty() || type.equals("TEXT"))) {
						if (!(setFieldType(fieldDef, type) || setFieldType(fieldDef))) {
							// Non SQL compatible field
							continue;
						}
					}
					aFields.add(fieldDef);
				}

				if (!aFields.isEmpty()) {
					hTables.put(index++, aFields);
					aTables.add(table);
				}
			}
		} catch (Exception e) {
			// should not occur
		}

		if (aTables.isEmpty()) {
			throw FNProgException.getException("noTablesFound", "");
		}

		myTable = myPref.getTableName();
		if (myTable.isEmpty() || !aTables.contains(myTable)) {
			myTable = aTables.get(0);
			myPref.setTableName(myTable, false);
		}

		Object obj = getDbFieldValues(null).get(0);
		myTotalRecords = ((Long) obj).intValue();
	}

	private boolean setFieldType(FieldDefinition field) {
		switch (field.getSQLType()) {
		case Types.BIGINT:
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
			field.setFieldType(FieldTypes.NUMBER);
			break;
		case Types.BIT:
		case Types.BOOLEAN:
			field.setFieldType(FieldTypes.BOOLEAN);
			break;
		case Types.DATE:
			field.setFieldType(FieldTypes.DATE);
			break;
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.REAL:
			field.setFieldType(FieldTypes.FLOAT);
			break;
		case Types.LONGVARCHAR:
			field.setFieldType(FieldTypes.MEMO);
			break;
		case Types.TIME:
			field.setFieldType(FieldTypes.TIME);
			break;
		case Types.TIMESTAMP:
			field.setFieldType(FieldTypes.TIMESTAMP);
			break;
		case Types.VARCHAR:
			return true;
		default:
			return false;
		}
		return true;
	}

	private boolean setFieldType(FieldDefinition field, String type) {
		// SqLite sets the SQL Type incorrectly, but the type name correct
		switch (type) {
		case "BOOL":
			field.setFieldType(FieldTypes.BOOLEAN);
			field.setSQLType(Types.BOOLEAN);
			break;
		case "DATE":
			field.setFieldType(FieldTypes.DATE);
			field.setSQLType(Types.DATE);
			break;
		case "DATETIME":
		case "TIMESTAMP":
			field.setFieldType(FieldTypes.TIMESTAMP);
			field.setSQLType(Types.TIMESTAMP);
			break;
		case "INTEGER":
			field.setFieldType(FieldTypes.NUMBER);
			field.setSQLType(Types.INTEGER);
			break;
		case "REAL":
			field.setFieldType(FieldTypes.FLOAT);
			field.setSQLType(Types.FLOAT);
			break;
		case "TIME":
			field.setFieldType(FieldTypes.TIME);
			field.setSQLType(Types.TIME);
			break;
		case "money":
			field.setFieldType(FieldTypes.FLOAT);
			field.setSQLType(Types.NUMERIC);
			break;
		// time and timestamps with time zones will be treated as string
		case "timestamptz":
		case "timetz":
			field.setFieldType(FieldTypes.TEXT);
			field.setSQLType(Types.VARCHAR);
			break;
		case "YEAR":
			field.setFieldType(FieldTypes.YEAR);
			field.setSQLType(Types.DATE);
			break;
		default:
			return false;
		}

		return true;
	}

	@Override
	public void closeFile() {
		try {
			connection.close();
			// Verify if we have a SSH session open
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		} catch (Exception e) {
			// Should not occur
		}
		isConnected = false;
	}

	@Override
	public List<FieldDefinition> getTableModelFields() {
		int index = aTables.indexOf(myTable);
		return hTables.get(index == -1 ? 1 : index);
	}

	@Override
	public String getPdaDatabase() {
		return myTable;
	}

	@Override
	public List<String> getTableOrSheetNames() {
		if (aTables == null) {
			return new ArrayList<>();
		}
		return aTables;
	}

	@Override
	public Map<String, Object> readRecord() throws Exception {
		if (isFirstRead) {
			dbStatement = connection.createStatement();

			StringBuilder buf = new StringBuilder("SELECT ");
			getTableModelFields().forEach(field -> {
				buf.append(getSqlFieldOrTable(field.getFieldName()));
				if (field.getSQLType() == Types.NUMERIC && myImportFile == ExportFile.POSTGRESQL) {
					// cast money field to numeric, otherwise a string preceded by a currency sign
					// will be returned (like $1,000.99)
					buf.append("::numeric");
				}
				buf.append(", ");
			});
			buf.delete(buf.length() - 2, buf.length());
			buf.append(" FROM ").append(getSqlFieldOrTable(myTable));
			dbResultSet = dbStatement.executeQuery(buf.toString());
			isFirstRead = false;
		}

		Map<String, Object> result = new HashMap<>();
		int index = 1;

		if (dbResultSet.next()) {
			for (FieldDefinition field : getTableModelFields()) {
				try {
					Object obj = getFieldValue(field.getSQLType(), index++, dbResultSet);
					result.put(field.getFieldAlias(), obj);
				} catch (Exception e) {
					throw new Exception(
							"Unable to read database field '" + field.getFieldName() + "', due to\n" + e.toString());
				}
			}
		} else {
			dbStatement.close();
		}
		return result;
	}

	private String getSqlFieldOrTable(String value) {
		if (StringUtils.isEmpty(value) || value.matches("^[a-zA-Z0-9_]*$")) {
			return value;
		}

		return "[" + value + "]";
	}

	@Override
	public List<Object> getDbFieldValues(String field) throws Exception {
		List<Object> result = new ArrayList<>();

		StringBuilder sql = new StringBuilder(50);
		if (StringUtils.isEmpty(field)) {
			sql.append("SELECT COUNT(*) FROM ");
		} else {
			sql.append("SELECT DISTINCT ").append(getSqlFieldOrTable(field)).append(" FROM ");
		}
		sql.append(getSqlFieldOrTable(myTable));

		try (Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(sql.toString())) {
			while (rs.next()) {
				result.add(getFieldValue(rs.getMetaData().getColumnType(1), 1, rs));
			}
		} catch (SQLException e) {
			throw new Exception(
					"Internal Program Error:\n\nSQL Statement: '" + sql.toString() + "' could not be excecuted."
							+ "\nThe JDBC driver returned the following error: '" + e.getMessage() + "'");
		}

		return result;
	}

	private Object getFieldValue(int colType, int colNo, ResultSet rs) throws Exception {
		int c = 0;
		switch (colType) {
		case Types.LONGVARBINARY:
			InputStream isr;
			try {
				isr = rs.getBinaryStream(colNo);
				if (isr == null) {
					return "";
				}
			} catch (NullPointerException ex) {
				return "";
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] rb = new byte[1024];
			// process blob
			while ((c = isr.read(rb)) != -1) {
				out.write(rb, 0, c);
			}
			byte[] b = out.toByteArray();
			isr.close();
			out.close();
			return b;
		case Types.LONGVARCHAR:
			return readMemoField(rs, colNo);
		case Types.BIT:
		case Types.BOOLEAN:
			return rs.getBoolean(colNo);
		case Types.DOUBLE:
			return rs.getDouble(colNo);
		case Types.DATE:
			try {
				LocalDate date = rs.getObject(colNo, LocalDate.class);
				return date == null ? "" : date;
			} catch (IllegalArgumentException e) {
				// We are dealing with a Year "0000" (MariaDB)
				return "";
			}
		case Types.FLOAT:
			return rs.getFloat(colNo);
		case Types.INTEGER:
			return rs.getInt(colNo);
		case Types.SMALLINT:
			return (int) rs.getShort(colNo);
		case Types.BIGINT:
			return rs.getLong(colNo);
		case Types.TIMESTAMP:
			Timestamp ts = rs.getTimestamp(colNo);
			return ts == null ? "" : ts.toLocalDateTime();
		case Types.TIME:
			LocalTime time = rs.getObject(colNo, LocalTime.class);
			return time == null ? "" : time;
		default:
			String s = rs.getString(colNo);
			return s == null ? "" : s;
		}
	}

	private String readMemoField(ResultSet rs, int colNo) throws Exception {
		Reader reader;
		int c = 0;
		try {
			reader = rs.getCharacterStream(colNo);
			if (reader == null) {
				return "";
			}
		} catch (NullPointerException ex) {
			return "";
		}

		StringBuilder buf = new StringBuilder();
		while (c != -1) {
			c = reader.read();
			if (c != -1) {
				buf.append((char) c);
			}
		}
		reader.close();
		return buf.toString();
	}

	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public void processData(Map<String, Object> dbRecord) throws Exception {
		// We are currently not writing to the SQL databases
	}
}
