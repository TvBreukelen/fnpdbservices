package dbengine.utils;

public class ForeignKey {
	private String columnFrom;
	private String columnTo;

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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ColumnFrom=").append(columnFrom).append(", ColumnTo=").append(columnTo);
		return sb.toString();
	}
}
