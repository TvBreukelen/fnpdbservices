package dbengine.utils;

public class ForeignKey {
	private String columnFrom;
	private String tableTo;
	private String columnTo;
	private String join = "Left Join";

	public String getColumnFrom() {
		return columnFrom;
	}

	public void setColumnFrom(String pkColumn) {
		this.columnFrom = pkColumn;
	}

	public String getColumnTo() {
		return columnTo;
	}

	public void setColumnTo(String fkColumn) {
		this.columnTo = fkColumn;
	}

	public String getTableTo() {
		return tableTo;
	}

	public void setTableTo(String tableTo) {
		this.tableTo = tableTo;
	}

	public String getJoin() {
		return join;
	}

	public void setJoin(String join) {
		this.join = join;
	}

	public static ForeignKey getFromString(String registry) {
		String[] element = registry.split("; ");
		ForeignKey result = new ForeignKey();
		result.setColumnFrom(element[0]);
		result.setColumnTo(element[1]);
		result.setTableTo(element[2]);
		result.setJoin(element[3]);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getColumnFrom()).append("; ").append(getColumnTo()).append("; ").append(getTableTo()).append("; ")
				.append(getJoin());
		return sb.toString();
	}
}
