package dbengine.export;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.ExportFile;
import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.SqlRemote;
import dbengine.utils.DatabaseHelper;

public class SQLServer extends SqlRemote {
	private Properties info;
	private boolean insertAutoIncrement = false;

	public SQLServer(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.SQLSERVER);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Try to obtain the database connection
		if (isConnected) {
			closeFile();
		}

		if (myHelper.isUseSsh()) {
			getSshSession();
		}

		// Try to obtain a new database connection
		info = new Properties();
		info.put("databaseName", myHelper.getDatabase());

		StringBuilder url = new StringBuilder("jdbc:sqlserver://").append(myHelper.getHost()).append(":")
				.append(myHelper.isUseSsh() ? assignedPort : myHelper.getPort());

		addToProperies("user", myHelper.getUser());
		addToProperies("password", General.decryptPassword(myHelper.getPassword()));

		if (myHelper.isUseSsl()) {
			addToProperies("encrypt", myHelper.getSslMode());
			addToProperies("trustServerCertificate", myHelper.isTrustServerCertificate() ? "true" : "false");
			addToProperies("hostNameInCertificate", myHelper.getHostNameInCertificate());
			addToProperies("trustStore", myHelper.getKeyStore());
			addToProperies("trustStorePassword", General.decryptPassword(myHelper.getKeyStorePassword()));
		} else {
			addToProperies("encrypt", "false");
		}

		connection = DriverManager.getConnection(url.toString(), info);
		isConnected = true;
	}

	private void addToProperies(String key, String value) {
		if (StringUtils.isNotBlank(value)) {
			info.put(key, value);
		}
	}

	@Override
	protected String getPaginationSqlString() {
		if (myPref.getSqlSelectLimit() > 0 && myPref.isSortFieldDefined()) {
			return super.getPaginationSqlString();
		}

		// Without Sorting we cannot use pagination, we therefore fall back on a
		// SELECT TOP nnn statement to get the first max read number of records
		totalRecords = Math.min(totalRecords, myPref.getSqlSelectLimit());
		StringBuilder b = new StringBuilder(sqlQuery);
		if (myPref.getSqlSelectLimit() > 0) {
			b.insert(7, "TOP " + totalRecords + General.SPACE);
		}
		return b.toString();
	}

	@Override
	protected boolean setFieldType(FieldDefinition field) {
		if (field.getSQLType() == microsoft.sql.Types.DATETIMEOFFSET) {
			field.setFieldType(FieldTypes.TIMESTAMP);
			return true;
		}
		return super.setFieldType(field);
	}

	@Override
	protected Object getFieldValue(int colType, int colNo, ResultSet rs) throws SQLException, IOException {
		if (colType == microsoft.sql.Types.DATETIMEOFFSET) {
			try {
				microsoft.sql.DateTimeOffset date = rs.getObject(colNo, microsoft.sql.DateTimeOffset.class);
				return date == null ? General.EMPTY_STRING : date.getOffsetDateTime().toLocalDateTime();
			} catch (IllegalArgumentException e) {
				return General.EMPTY_STRING;
			}
		}
		return super.getFieldValue(colType, colNo, rs);
	}

	@Override
	public String buildTableString(String table, List<FieldDefinition> fields) {
		StringBuilder buf = new StringBuilder("CREATE TABLE ").append(getSqlFieldName(table)).append(" (\n");
		StringBuilder pkBuf = new StringBuilder();

		fields.forEach(field -> {
			String fieldName = getSqlFieldName(field.getFieldHeader());
			buf.append(fieldName);

			if (field.isOutputAsText()) {
				getTextOrVarchar(field.getSize(), buf);
			} else {
				switch (field.getFieldType()) {
				case BOOLEAN:
					buf.append(" BOOL");
					break;
				case DATE:
					buf.append(" DATE");
					break;
				case FLOAT:
					buf.append(" DECIMAL(").append(field.getSize()).append(",").append(field.getDecimalPoint())
							.append(")");
					break;
				case NUMBER:
					buf.append(" INT");
					if (field.isAutoIncrement()) {
						buf.append(" IDENTITY(1,1)");
						insertAutoIncrement = true;
					}
					break;
				case TEXT, MEMO:
					buf.append(" TEXT");
					break;
				case IMAGE, THUMBNAIL:
					buf.append(" IMAGE");
					break;
				case TIME:
					buf.append(" TIME");
					break;
				case TIMESTAMP:
					buf.append(" DATETIME");
					break;
				default:
					getTextOrVarchar(field.getSize(), buf);
					break;
				}
			}

			if (field.isPrimaryKey()) {
				pkBuf.append(fieldName).append(",");
			}

			if (field.isNotNullable()) {
				buf.append(" NOT NULL");
			}

			if (field.isUnique()) {
				buf.append(" UNIQUE");
			}

			buf.append(",\n");
		});

		buf.delete(buf.lastIndexOf(","), buf.length());

		if (!pkBuf.isEmpty()) {
			pkBuf.delete(pkBuf.length() - 1, pkBuf.length());
			buf.append(",\nPRIMARY KEY (").append(pkBuf).append(")");
		}

		buf.append(");");
		return buf.toString();
	}

	@Override
	protected void createPreparedStatement() throws SQLException {
		executeStatement(new StringBuilder("SET IDENTITY_INSERT ").append(getSqlFieldName(myPref.getDatabaseName()))
				.append(insertAutoIncrement ? " ON" : " OFF").toString());

		int maxFields = dbInfo2Write.size();
		StringBuilder buf = new StringBuilder("INSERT INTO ");

		buf.append(getSqlFieldName(myPref.getDatabaseName())).append(" (");
		dbInfo2Write.forEach(field -> buf.append(getSqlFieldName(field.getFieldHeader())).append(","));

		buf.deleteCharAt(buf.length() - 1);
		buf.append(")\n");
		buf.append("VALUES (");

		for (int i = 0; i < maxFields; i++) {
			buf.append("?, ");
		}
		buf.delete(buf.lastIndexOf(","), buf.length());
		buf.append(")");

		prepStmt = connection.prepareStatement(buf.toString());
		connection.setAutoCommit(false);
	}

	@Override
	public void closeData() throws Exception {
		if (!isConnected) {
			return;
		}

		try {
			connection.commit();
		} catch (SQLException ex) {
			connection.rollback();
			throwInsertException(ex);
		} finally {
			super.closeData();
		}
	}
}
