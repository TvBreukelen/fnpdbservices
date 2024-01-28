package dbengine.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import application.preferences.Profiles;

public class RelationData {
	private Map<String, ForeignKey> relationMap = new HashMap<>();

	public void loadProfile(Profiles profile) {
		profile.getRelations().forEach(e -> {
			ForeignKey key = ForeignKey.getFromString(e);
			relationMap.put(key.getTableTo(), key);
		});
	}

	public void saveProfile(Profiles profile) {
		profile.setRelations(
				relationMap.values().stream().filter(e -> e.isUserDefined() || !e.getJoin().equals("Left Join"))
						.map(ForeignKey::toString).collect(Collectors.toList()));
	}

	public ForeignKey getForeignKey(String toTable) {
		return relationMap.getOrDefault(toTable, new ForeignKey());
	}

	public void addForeignKey(ForeignKey key) {
		relationMap.put(key.getTableTo(), key);
	}

	public Map<String, ForeignKey> getRelationMap() {
		return relationMap;
	}

	public void setForeignKeys(Collection<ForeignKey> keys) {
		relationMap.clear();
		keys.forEach(fk -> relationMap.put(fk.getTableTo(), fk));
	}
}
