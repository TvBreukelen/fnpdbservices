package application.interfaces;

import application.dialog.ProgramDialog;
import application.preferences.Profiles;
import dbengine.GeneralDB;
import dbengine.export.Calc;
import dbengine.export.CsvFile;
import dbengine.export.DBaseFile;
import dbengine.export.Excel;
import dbengine.export.Firebird;
import dbengine.export.HanDBase;
import dbengine.export.JsonFile;
import dbengine.export.MariaDB;
import dbengine.export.PostgreSQL;
import dbengine.export.SQLServer;
import dbengine.export.SQLite;
import dbengine.export.XmlFile;
import dbengine.export.YamlFile;

public abstract class IExportProcess {
	public abstract void init(ProgramDialog pProgram, ExportStatus status);

	public GeneralDB getDatabase(ExportFile db, Profiles profile) {
		switch (db) {
		case CALC:
			return new Calc(profile);
		case DBASE:
			return new DBaseFile(profile);
		case EXCEL:
			return new Excel(profile);
		case FIREBIRD:
			return new Firebird(profile);
		case HANDBASE:
			return new HanDBase(profile);
		case JSON:
			return new JsonFile(profile);
		case MARIADB:
			return new MariaDB(profile);
		case POSTGRESQL:
			return new PostgreSQL(profile);
		case SQLITE:
			return new SQLite(profile);
		case SQLSERVER:
			return new SQLServer(profile);
		case TEXTFILE:
			return new CsvFile(profile);
		case XML:
			return new XmlFile(profile);
		case YAML:
			return new YamlFile(profile);
		default:
			return null;
		}

	}
}
