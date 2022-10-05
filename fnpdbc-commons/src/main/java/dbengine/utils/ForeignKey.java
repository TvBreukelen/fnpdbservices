package dbengine.utils;

public class ForeignKey {
	String fkColumn;
	String fkTable;

	public String getFkColumn() {
		return fkColumn;
	}

	public void setFkColumn(String fkColumn) {
		this.fkColumn = fkColumn;
	}

	public String getFkTable() {
		return fkTable;
	}

	public void setFkTable(String fkTable) {
		this.fkTable = fkTable;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("FKTable=").append(fkTable).append(", FKColumn=").append(fkColumn);
		return sb.toString();
	}

}
