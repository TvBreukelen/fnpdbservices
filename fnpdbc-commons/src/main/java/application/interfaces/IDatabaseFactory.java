package application.interfaces;

import java.util.List;
import java.util.Map;

import application.preferences.Profiles;
import application.utils.FieldDefinition;

public interface IDatabaseFactory {
	Map<String, FieldDefinition> getDbFieldDefinition();

	List<FieldDefinition> getDbSelectFields();

	String[] getDbFilterFields();

	List<Object> getFilterFieldValues(String pField) throws Exception;

	boolean isConnected();

	ExportFile getExportFile();

	Profiles getProfiles();

	String getDatabaseFilename();

	void close();
}
