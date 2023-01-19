package dbconvert.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
