package dbengine.export;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.SqlRemote;

public class SQLServer extends SqlRemote {
	private Properties info;

	public SQLServer(Profiles pref) {
		super(pref);
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

}
