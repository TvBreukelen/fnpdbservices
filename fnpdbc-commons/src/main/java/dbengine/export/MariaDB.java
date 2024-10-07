package dbengine.export;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.ExportFile;
import application.preferences.Profiles;
import application.utils.FieldDefinition;
import application.utils.General;
import dbengine.SqlRemote;
import dbengine.utils.DatabaseHelper;

public class MariaDB extends SqlRemote {
	private Properties info;

	public MariaDB(Profiles pref) {
		super(pref);
		myHelper = new DatabaseHelper(General.EMPTY_STRING, ExportFile.MARIADB);
	}

	static {
		reservedWords.clear();
		reservedWords.add("ACCESSIBLE");
		reservedWords.add("ADD");
		reservedWords.add("ALL");
		reservedWords.add("ALTER");
		reservedWords.add("ANALYZE");
		reservedWords.add("AND");
		reservedWords.add("AS");
		reservedWords.add("ASC");
		reservedWords.add("ASENSITIVE");
		reservedWords.add("BEFORE");
		reservedWords.add("BETWEEN");
		reservedWords.add("BIGINT");
		reservedWords.add("BINARY");
		reservedWords.add("BLOB");
		reservedWords.add("BOTH");
		reservedWords.add("BY");
		reservedWords.add("CALL");
		reservedWords.add("CASCADE");
		reservedWords.add("CASE");
		reservedWords.add("CHANGE");
		reservedWords.add("CHAR");
		reservedWords.add("CHARACTER");
		reservedWords.add("CHECK");
		reservedWords.add("COLLATE");
		reservedWords.add("COLUMN");
		reservedWords.add("CONDITION");
		reservedWords.add("CONSTRAINT");
		reservedWords.add("CONTINUE");
		reservedWords.add("CONVERT");
		reservedWords.add("CREATE");
		reservedWords.add("CROSS");
		reservedWords.add("CURRENT_DATE");
		reservedWords.add("CURRENT_ROLE");
		reservedWords.add("CURRENT_TIME");
		reservedWords.add("CURRENT_TIMESTAMP");
		reservedWords.add("CURRENT_USER");
		reservedWords.add("CURSOR");
		reservedWords.add("DATABASE");
		reservedWords.add("DATABASES");
		reservedWords.add("DAY_HOUR");
		reservedWords.add("DAY_MICROSECOND");
		reservedWords.add("DAY_MINUTE");
		reservedWords.add("DAY_SECOND");
		reservedWords.add("DEC");
		reservedWords.add("DECIMAL");
		reservedWords.add("DECLARE");
		reservedWords.add("DEFAULT");
		reservedWords.add("DELAYED");
		reservedWords.add("DELETE");
		reservedWords.add("DELETE_DOMAIN_ID");
		reservedWords.add("DESC");
		reservedWords.add("DESCRIBE");
		reservedWords.add("DETERMINISTIC");
		reservedWords.add("DISTINCT");
		reservedWords.add("DISTINCTROW");
		reservedWords.add("DIV");
		reservedWords.add("DO_DOMAIN_IDS");
		reservedWords.add("DOUBLE");
		reservedWords.add("DROP");
		reservedWords.add("DUAL");
		reservedWords.add("EACH");
		reservedWords.add("ELSE");
		reservedWords.add("ELSEIF");
		reservedWords.add("ENCLOSED");
		reservedWords.add("ESCAPED");
		reservedWords.add("EXCEPT");
		reservedWords.add("EXISTS");
		reservedWords.add("EXIT");
		reservedWords.add("EXPLAIN");
		reservedWords.add("FALSE");
		reservedWords.add("FETCH");
		reservedWords.add("FLOAT");
		reservedWords.add("FLOAT4");
		reservedWords.add("FLOAT8");
		reservedWords.add("FOR");
		reservedWords.add("FORCE");
		reservedWords.add("FOREIGN");
		reservedWords.add("FROM");
		reservedWords.add("FULLTEXT");
		reservedWords.add("GENERAL");
		reservedWords.add("GRANT");
		reservedWords.add("GROUP");
		reservedWords.add("HAVING");
		reservedWords.add("HIGH_PRIORITY");
		reservedWords.add("HOUR_MICROSECOND");
		reservedWords.add("HOUR_MINUTE");
		reservedWords.add("HOUR_SECOND");
		reservedWords.add("IF");
		reservedWords.add("IGNORE");
		reservedWords.add("IGNORE_DOMAIN_IDS");
		reservedWords.add("IGNORE_SERVER_IDS");
		reservedWords.add("IN");
		reservedWords.add("INDEX");
		reservedWords.add("INFILE");
		reservedWords.add("INNER");
		reservedWords.add("INOUT");
		reservedWords.add("INSENSITIVE");
		reservedWords.add("INSERT");
		reservedWords.add("INT");
		reservedWords.add("INT1");
		reservedWords.add("INT2");
		reservedWords.add("INT3");
		reservedWords.add("INT4");
		reservedWords.add("INT8");
		reservedWords.add("INTEGER");
		reservedWords.add("INTERSECT");
		reservedWords.add("INTERVAL");
		reservedWords.add("INTO");
		reservedWords.add("IS");
		reservedWords.add("ITERATE");
		reservedWords.add("JOIN");
		reservedWords.add("KEY");
		reservedWords.add("KEYS");
		reservedWords.add("KILL");
		reservedWords.add("LEADING");
		reservedWords.add("LEAVE");
		reservedWords.add("LEFT");
		reservedWords.add("LIKE");
		reservedWords.add("LIMIT");
		reservedWords.add("LINEAR");
		reservedWords.add("LINES");
		reservedWords.add("LOAD");
		reservedWords.add("LOCALTIME");
		reservedWords.add("LOCALTIMESTAMP");
		reservedWords.add("LOCK");
		reservedWords.add("LONG");
		reservedWords.add("LONGBLOB");
		reservedWords.add("LONGTEXT");
		reservedWords.add("LOOP");
		reservedWords.add("LOW_PRIORITY");
		reservedWords.add("MASTER_HEARTBEAT_PERIOD");
		reservedWords.add("MASTER_SSL_VERIFY_SERVER_CERT");
		reservedWords.add("MATCH");
		reservedWords.add("MAXVALUE");
		reservedWords.add("MEDIUMBLOB");
		reservedWords.add("MEDIUMINT");
		reservedWords.add("MEDIUMTEXT");
		reservedWords.add("MIDDLEINT");
		reservedWords.add("MINUTE_MICROSECOND");
		reservedWords.add("MINUTE_SECOND");
		reservedWords.add("MOD");
		reservedWords.add("MODIFIES");
		reservedWords.add("NATURAL");
		reservedWords.add("NOT");
		reservedWords.add("NO_WRITE_TO_BINLOG");
		reservedWords.add("NULL");
		reservedWords.add("NUMERIC");
		reservedWords.add("OFFSET");
		reservedWords.add("ON");
		reservedWords.add("OPTIMIZE");
		reservedWords.add("OPTION");
		reservedWords.add("OPTIONALLY");
		reservedWords.add("OR");
		reservedWords.add("ORDER");
		reservedWords.add("OUT");
		reservedWords.add("OUTER");
		reservedWords.add("OUTFILE");
		reservedWords.add("OVER");
		reservedWords.add("PAGE_CHECKSUM");
		reservedWords.add("PARSE_VCOL_EXPR");
		reservedWords.add("PARTITION");
		reservedWords.add("PRECISION");
		reservedWords.add("PRIMARY");
		reservedWords.add("PROCEDURE");
		reservedWords.add("PURGE");
		reservedWords.add("RANGE");
		reservedWords.add("READ");
		reservedWords.add("READS");
		reservedWords.add("READ_WRITE");
		reservedWords.add("REAL");
		reservedWords.add("RECURSIVE");
		reservedWords.add("REF_SYSTEM_ID");
		reservedWords.add("REFERENCES");
		reservedWords.add("REGEXP");
		reservedWords.add("RELEASE");
		reservedWords.add("RENAME");
		reservedWords.add("REPEAT");
		reservedWords.add("REPLACE");
		reservedWords.add("REQUIRE");
		reservedWords.add("RESIGNAL");
		reservedWords.add("RESTRICT");
		reservedWords.add("RETURN");
		reservedWords.add("RETURNING");
		reservedWords.add("REVOKE");
		reservedWords.add("RIGHT");
		reservedWords.add("RLIKE");
		reservedWords.add("ROW_NUMBER");
		reservedWords.add("ROWS");
		reservedWords.add("SCHEMA");
		reservedWords.add("SCHEMAS");
		reservedWords.add("SECOND_MICROSECOND");
		reservedWords.add("SELECT");
		reservedWords.add("SENSITIVE");
		reservedWords.add("SEPARATOR");
		reservedWords.add("SET");
		reservedWords.add("SHOW");
		reservedWords.add("SIGNAL");
		reservedWords.add("SLOW");
		reservedWords.add("SMALLINT");
		reservedWords.add("SPATIAL");
		reservedWords.add("SPECIFIC");
		reservedWords.add("SQL");
		reservedWords.add("SQLEXCEPTION");
		reservedWords.add("SQLSTATE");
		reservedWords.add("SQLWARNING");
		reservedWords.add("SQL_BIG_RESULT");
		reservedWords.add("SQL_CALC_FOUND_ROWS");
		reservedWords.add("SQL_SMALL_RESULT");
		reservedWords.add("SSL");
		reservedWords.add("STARTING");
		reservedWords.add("STATS_AUTO_RECALC");
		reservedWords.add("STATS_PERSISTENT");
		reservedWords.add("STATS_SAMPLE_PAGES");
		reservedWords.add("STRAIGHT_JOIN");
		reservedWords.add("TABLE");
		reservedWords.add("TERMINATED");
		reservedWords.add("THEN");
		reservedWords.add("TINYBLOB");
		reservedWords.add("TINYINT");
		reservedWords.add("TINYTEXT");
		reservedWords.add("TO");
		reservedWords.add("TRAILING");
		reservedWords.add("TRIGGER");
		reservedWords.add("TRUE");
		reservedWords.add("UNDO");
		reservedWords.add("UNION");
		reservedWords.add("UNIQUE");
		reservedWords.add("UNLOCK");
		reservedWords.add("UNSIGNED");
		reservedWords.add("UPDATE");
		reservedWords.add("USAGE");
		reservedWords.add("USE");
		reservedWords.add("USING");
		reservedWords.add("UTC_DATE");
		reservedWords.add("UTC_TIME");
		reservedWords.add("UTC_TIMESTAMP");
		reservedWords.add("VALUES");
		reservedWords.add("VARBINARY");
		reservedWords.add("VARCHAR");
		reservedWords.add("VARCHARACTER");
		reservedWords.add("VARYING");
		reservedWords.add("WHEN");
		reservedWords.add("WHERE");
		reservedWords.add("WHILE");
		reservedWords.add("WITH");
		reservedWords.add("WRITE");
		reservedWords.add("XOR");
		reservedWords.add("YEAR_MONTH");
		reservedWords.add("ZEROFILL");
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		// Close any existing database connection
		if (isConnected) {
			closeFile();
		}

		if (myHelper.isUseSsh()) {
			getSshSession();
		}

		// Try to obtain a new database connection
		info = new Properties();
		info.put("user", myHelper.getUser());
		info.put("password", General.decryptPassword(myHelper.getPassword()));

		if (myHelper.isUseSsl()) {
			info.put("ssl", "true");
			addToProperies("sslMode", myHelper.getSslMode());
			addToProperies("serverSslCert", myHelper.getServerSslCert());
			addToProperies("keyStore", myHelper.getKeyStore());
			addToProperies("keyStorePassword", General.decryptPassword(myHelper.getKeyStorePassword()));
			addToProperies("keyStoreType", getKeyStoreType());
		}

		StringBuilder url = new StringBuilder("jdbc:mariadb://").append(myHelper.getHost()).append(":")
				.append(myHelper.isUseSsh() ? assignedPort : myHelper.getPort()).append("/")
				.append(myHelper.getDatabase());

		connection = DriverManager.getConnection(url.toString(), info);
		isConnected = true;
	}

	private void addToProperies(String key, String value) {
		if (StringUtils.isNotBlank(value)) {
			info.put(key, value);
		}
	}

	private String getKeyStoreType() {
		if (StringUtils.isNotBlank(myHelper.getKeyStore())) {
			return myHelper.getKeyStore().toLowerCase().endsWith(".jks") ? "JKS" : "PKCS12";
		}
		return General.EMPTY_STRING;
	}

	@Override
	public String buildTableString(String table, List<FieldDefinition> fields) {
		StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getSqlFieldName(table))
				.append(" (\n");
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
					buf.append(field.isAutoIncrement() ? " INT AUTO_INCREMENT" : " INT");
					break;
				case TEXT, MEMO, IMAGE, THUMBNAIL:
					buf.append(" TEXT");
					break;
				case TIME:
					buf.append(" TIME");
					break;
				case TIMESTAMP:
					buf.append(" TIMESTAMP");
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
		prepReplace.clear();
		int maxFields = dbInfo2Write.size();
		boolean isIgnore = myPref.getOnConflict() == Profiles.ON_CONFLICT_IGNORE;
		boolean isReplace = myPref.getOnConflict() == Profiles.ON_CONFLICT_REPLACE;

		StringBuilder buf = new StringBuilder("INSERT ");
		if (isIgnore) {
			buf.append("IGNORE ");
			isReplace = false;
		}

		buf.append("INTO ").append(myPref.getDatabaseName()).append(" (");
		dbInfo2Write.forEach(field -> buf.append(getSqlFieldName(field.getFieldHeader())).append(","));

		buf.deleteCharAt(buf.length() - 1);
		buf.append(")\n");
		buf.append("VALUES (");

		for (int i = 0; i < maxFields; i++) {
			buf.append("?, ");
		}
		buf.delete(buf.lastIndexOf(","), buf.length());
		buf.append(")");

		// Check for a primary key
		Optional<FieldDefinition> pkOpt = dbInfo2Write.stream().filter(FieldDefinition::isPrimaryKey).findFirst();
		if (isReplace && pkOpt.isPresent()) {
			String pk = pkOpt.get().getFieldHeader();
			buf.append(" ON DUPLICATE KEY UPDATE\n");
			dbInfo2Write.forEach(field -> {
				if (!pk.equals(field.getFieldHeader())) {
					prepReplace.add(field);
					String sqlColumn = getSqlFieldName(field.getFieldHeader());
					buf.append(sqlColumn).append(" = ?").append(",\n");
				}
			});
			buf.delete(buf.length() - 2, buf.length());
			buf.append(";");
		}
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