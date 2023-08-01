package dbengine;

import java.util.List;
import java.util.Map;

import application.BasicSoft;
import application.interfaces.ExportFile;
import application.utils.FieldDefinition;
import dbengine.utils.DatabaseHelper;

public interface IConvert {
	/**
	 * Title: IConvert Description: public interface for converting an input file
	 * with DBConvert Copyright: (c) 2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.6
	 */

	ExportFile getImportFile();

	void setSoftware(BasicSoft pSoft);

	void openFile(DatabaseHelper helper, boolean isInputFile) throws Exception;

	String getPdaDatabase();

	List<FieldDefinition> getTableModelFields() throws Exception;

	default List<FieldDefinition> getTableModelFields(boolean loadFromRegistry) throws Exception {
		return getTableModelFields();
	}

	List<String> getTableOrSheetNames();

	default List<Object> getDbFieldValues(String field) throws Exception {
		return null; // NOSONAR
	}

	int getTotalRecords() throws Exception;

	Map<String, Object> readRecord() throws Exception;

	void readTableContents() throws Exception;

	default void readInputFile() throws Exception {
		// Do Nothing
	}

	void closeFile();
}