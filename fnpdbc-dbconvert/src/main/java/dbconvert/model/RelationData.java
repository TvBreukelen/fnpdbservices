package dbconvert.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.preferences.Profiles;
import dbengine.utils.ForeignKey;

public class RelationData {
	private Map<String, ForeignKey> relationMap = new HashMap<>();

	public void loadProfile(Profiles profile) {
		profile.getRelations().forEach(e -> {
			ForeignKey key = ForeignKey.getFromString(e);
			relationMap.put(key.getTableTo(), key);
		});
	}

	public void saveProfile(Profiles profile) {
		List<String> relations = new ArrayList<>();
		relationMap.values().forEach(e -> relations.add(e.toString()));
		profile.setRelations(relations);
	}

	public ForeignKey getForeignKey(String toTable) {
		return relationMap.getOrDefault(toTable, new ForeignKey());
	}

	public void deleteForeignKey(String toTable) {
		relationMap.remove(toTable);
	}

	public List<ForeignKey> getForeignKeys() {
		return new ArrayList<>(relationMap.values());
	}

	public void setForeignKeys(Collection<ForeignKey> keys) {
		relationMap.clear();
		keys.forEach(fk -> relationMap.put(fk.getTableTo(), fk));
	}
}
