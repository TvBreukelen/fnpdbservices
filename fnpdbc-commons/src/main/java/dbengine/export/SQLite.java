package dbengine.export;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;

import application.preferences.Profiles;
import application.utils.FNProgException;
import dbengine.IConvert;
import dbengine.SqlDB;

public class SQLite extends SqlDB implements IConvert {

	public SQLite(Profiles pref) {
		super(pref);
	}

	@Override
	protected void openFile(boolean isInputFile) throws Exception {
		if (isInputFile) {
			verifyHeader();
		}
		super.openFile(isInputFile);
	}

	private void verifyHeader() throws Exception {
		String header = null;

		try (RandomAccessFile raf = new RandomAccessFile(myDatabase, "r")) {
			FileChannel channel = raf.getChannel();
			int len = myImportFile.getDbType().length();

			if (channel.size() > len) {
				byte[] byteArray = new byte[len];
				raf.read(byteArray);
				header = new String(byteArray);
			}

			channel.close();

			if (header == null || !header.equals(myImportFile.getDbType())) {
				throw FNProgException.getException("invalidDatabaseID", myDatabase, myImportFile.getName(),
						myImportFile.getDbType(), header);
			}
		}
	}

	@Override
	protected Object getObject(int colType, int colNo, ResultSet rs) throws Exception {
		return rs.getString(colNo);
	}
}
