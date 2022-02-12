package dbengine;

import java.util.List;
import java.util.Map;

import application.BasicSoft;
import application.utils.FieldDefinition;
import dbengine.utils.DatabaseHelper;

public interface IConvert {
	/**
	 * Title: IConvert Description: public interface for converting a PDA database
	 * with DBConvert Copyright: (c) 2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.6
	 */
	void setSoftware(BasicSoft pSoft);

	void openFile(DatabaseHelper helper, boolean isInputFile) throws Exception;

	String getPdaDatabase();

	List<FieldDefinition> getTableModelFields() throws Exception;

	List<String> getTableOrSheetNames();

	int getTotalRecords();

	Map<String, Object> readRecord() throws Exception;

	void readTableContents() throws Exception;

	void closeFile();
}