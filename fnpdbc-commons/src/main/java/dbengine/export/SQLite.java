package dbengine.export;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;

import application.interfaces.FieldTypes;
import application.preferences.Profiles;
import application.utils.FNProgException;
import dbengine.IConvert;
import dbengine.SqlDB;

public class SQLite extends SqlDB implements IConvert {

	public SQLite(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean createBackup, boolean isInputFile) throws Exception {
		if (isInputFile) {
			verifyHeader();
		}
		super.openFile(createBackup, isInputFile);
	}

	private void verifyHeader() throws Exception {
		String header = null;

		try (RandomAccessFile raf = new RandomAccessFile(myFilename, "r")) {
			FileChannel channel = raf.getChannel();
			int len = myImportFile.getDbType().length();

			if (channel.size() > len) {
				byte[] byteArray = new byte[len];
				raf.read(byteArray);
				header = new String(byteArray);
			}

			channel.close();

			if (header == null || !header.equals(myImportFile.getDbType())) {
				throw FNProgException.getException("invalidDatabaseID", myFilename, myImportFile.getName(),
						myImportFile.getDbType(), header);
			}
		}
	}

	@Override
	protected String[] getConnectionStrings() {
		return new String[] { "org.sqlite.JDBC", "jdbc:sqlite:" + myFilename };
	}

	@Override
	protected Object getObject(int colType, int colNo, ResultSet rs) throws Exception {
		return rs.getString(colNo);
	}

	@Override
	protected FieldTypes getFieldType(int type) {
		return FieldTypes.TEXT;
	}
}
