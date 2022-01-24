package dbengine.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import application.preferences.Profiles;
import dbengine.JsonFile;

public class YamlFile extends JsonFile {
	public YamlFile(Profiles pref) {
		super(pref);
		mapper = new ObjectMapper(new YAMLFactory());
	}
}
