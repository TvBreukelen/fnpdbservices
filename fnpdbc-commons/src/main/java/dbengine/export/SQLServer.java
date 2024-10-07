package dbengine.export;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
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

	public SQLServer(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.SQLSERVER);
	}

	static {
		reservedWords.clear();
		reservedWords.add("ADD");
		reservedWords.add("ALL");
		reservedWords.add("ALTER");
		reservedWords.add("AND");
		reservedWords.add("ANY");
		reservedWords.add("AS");
		reservedWords.add("ASC");
		reservedWords.add("AUTHORIZATION");
		reservedWords.add("BACKUP");
		reservedWords.add("BEGIN");
		reservedWords.add("BETWEEN");
		reservedWords.add("BREAK");
		reservedWords.add("BROWSE");
		reservedWords.add("BULK");
		reservedWords.add("BY");
		reservedWords.add("CASCADE");
		reservedWords.add("CASE");
		reservedWords.add("CHECK");
		reservedWords.add("CHECKPOINT");
		reservedWords.add("CLOSE");
		reservedWords.add("CLUSTERED");
		reservedWords.add("COALESCE");
		reservedWords.add("COLLATE");
		reservedWords.add("COLUMN");
		reservedWords.add("COMMIT");
		reservedWords.add("COMPUTE");
		reservedWords.add("CONSTRAINT");
		reservedWords.add("CONTAINS");
		reservedWords.add("CONTAINSTABLE");
		reservedWords.add("CONTINUE");
		reservedWords.add("CONVERT");
		reservedWords.add("CREATE");
		reservedWords.add("CROSS");
		reservedWords.add("CURRENT");
		reservedWords.add("CURRENT_DATE");
		reservedWords.add("CURRENT_TIME");
		reservedWords.add("CURRENT_TIMESTAMP");
		reservedWords.add("CURRENT_USER");
		reservedWords.add("CURSOR");
		reservedWords.add("DATABASE");
		reservedWords.add("DBCC");
		reservedWords.add("DEALLOCATE");
		reservedWords.add("DECLARE");
		reservedWords.add("DEFAULT");
		reservedWords.add("DELETE");
		reservedWords.add("DENY");
		reservedWords.add("DESC");
		reservedWords.add("DISK");
		reservedWords.add("DISTINCT");
		reservedWords.add("DISTRIBUTED");
		reservedWords.add("DOUBLE");
		reservedWords.add("DROP");
		reservedWords.add("DUMP");
		reservedWords.add("ELSE");
		reservedWords.add("END");
		reservedWords.add("ERRLVL");
		reservedWords.add("ESCAPE");
		reservedWords.add("EXCEPT");
		reservedWords.add("EXEC");
		reservedWords.add("EXECUTE");
		reservedWords.add("EXISTS");
		reservedWords.add("EXIT");
		reservedWords.add("EXTERNAL");
		reservedWords.add("FETCH");
		reservedWords.add("FILE");
		reservedWords.add("FILLFACTOR");
		reservedWords.add("FOR");
		reservedWords.add("FOREIGN");
		reservedWords.add("FREETEXT");
		reservedWords.add("FREETEXTTABLE");
		reservedWords.add("FROM");
		reservedWords.add("FULL");
		reservedWords.add("FUNCTION");
		reservedWords.add("GOTO");
		reservedWords.add("GRANT");
		reservedWords.add("GROUP");
		reservedWords.add("HAVING");
		reservedWords.add("HOLDLOCK");
		reservedWords.add("IDENTITY");
		reservedWords.add("IDENTITYCOL");
		reservedWords.add("IDENTITY_INSERT");
		reservedWords.add("IF");
		reservedWords.add("IN");
		reservedWords.add("INDEX");
		reservedWords.add("INNER");
		reservedWords.add("INSERT");
		reservedWords.add("INTERSECT");
		reservedWords.add("INTO");
		reservedWords.add("IS");
		reservedWords.add("JOIN");
		reservedWords.add("KEY");
		reservedWords.add("KILL");
		reservedWords.add("LEFT");
		reservedWords.add("LIKE");
		reservedWords.add("LINENO");
		reservedWords.add("LOAD");
		reservedWords.add("MERGE");
		reservedWords.add("NATIONAL");
		reservedWords.add("NOCHECK");
		reservedWords.add("NONCLUSTERED");
		reservedWords.add("NOT");
		reservedWords.add("NULL");
		reservedWords.add("NULLIF");
		reservedWords.add("OF");
		reservedWords.add("OFF");
		reservedWords.add("OFFSETS");
		reservedWords.add("ON");
		reservedWords.add("OPEN");
		reservedWords.add("OPENDATASOURCE");
		reservedWords.add("OPENQUERY");
		reservedWords.add("OPENROWSET");
		reservedWords.add("OPENXML");
		reservedWords.add("OPTION");
		reservedWords.add("OR");
		reservedWords.add("ORDER");
		reservedWords.add("OUTER");
		reservedWords.add("OVER");
		reservedWords.add("PERCENT");
		reservedWords.add("PIVOT");
		reservedWords.add("PLAN");
		reservedWords.add("PRECISION");
		reservedWords.add("PRIMARY");
		reservedWords.add("PRINT");
		reservedWords.add("PROC");
		reservedWords.add("PROCEDURE");
		reservedWords.add("PUBLIC");
		reservedWords.add("RAISERROR");
		reservedWords.add("READ");
		reservedWords.add("READTEXT");
		reservedWords.add("RECONFIGURE");
		reservedWords.add("REFERENCES");
		reservedWords.add("REPLICATION");
		reservedWords.add("RESTORE");
		reservedWords.add("RESTRICT");
		reservedWords.add("RETURN");
		reservedWords.add("REVERT");
		reservedWords.add("REVOKE");
		reservedWords.add("RIGHT");
		reservedWords.add("ROLLBACK");
		reservedWords.add("ROWCOUNT");
		reservedWords.add("ROWGUIDCOL");
		reservedWords.add("RULE");
		reservedWords.add("SAVE");
		reservedWords.add("SCHEMA");
		reservedWords.add("SECURITYAUDIT");
		reservedWords.add("SELECT");
		reservedWords.add("SEMANTICKEYPHRASETABLE");
		reservedWords.add("SEMANTICSIMILARITYDETAILSTABLE");
		reservedWords.add("SEMANTICSIMILARITYTABLE");
		reservedWords.add("SESSION_USER");
		reservedWords.add("SET");
		reservedWords.add("SETUSER");
		reservedWords.add("SHUTDOWN");
		reservedWords.add("SOME");
		reservedWords.add("STATISTICS");
		reservedWords.add("SYSTEM_USER");
		reservedWords.add("TABLE");
		reservedWords.add("TABLESAMPLE");
		reservedWords.add("TEXTSIZE");
		reservedWords.add("THEN");
		reservedWords.add("TO");
		reservedWords.add("TOP");
		reservedWords.add("TRAN");
		reservedWords.add("TRANSACTION");
		reservedWords.add("TRIGGER");
		reservedWords.add("TRUNCATE");
		reservedWords.add("TRY_CONVERT");
		reservedWords.add("TSEQUAL");
		reservedWords.add("UNION");
		reservedWords.add("UNIQUE");
		reservedWords.add("UNPIVOT");
		reservedWords.add("UPDATE");
		reservedWords.add("UPDATETEXT");
		reservedWords.add("USE");
		reservedWords.add("USER");
		reservedWords.add("VALUES");
		reservedWords.add("VARYING");
		reservedWords.add("VIEW");
		reservedWords.add("WAITFOR");
		reservedWords.add("WHEN");
		reservedWords.add("WHERE");
		reservedWords.add("WHILE");
		reservedWords.add("WITH");
		reservedWords.add("WITHINGROUP");
		reservedWords.add("WRITETEXT");
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
		String table = getSqlFieldName(myPref.getDatabaseName());

		// Verify is we insert an auto increment column
		Optional<FieldDefinition> optIncr = dbInfo2Write.stream().filter(FieldDefinition::isPrimaryKey).findAny();
		executeStatement(new StringBuilder("SET IDENTITY_INSERT ").append(table)
				.append(optIncr.isPresent() ? " ON" : " OFF").toString());

		int maxFields = dbInfo2Write.size();
		StringBuilder buf = new StringBuilder("INSERT INTO ");

		buf.append(table).append(" (");
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
