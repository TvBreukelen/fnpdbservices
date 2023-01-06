package dbengine.utils;

import java.util.ArrayList;
import java.util.List;

import application.utils.General;

public class ForeignKey {
	private List<String> columnFrom = new ArrayList<>();
	private List<String> columnTo = new ArrayList<>();
	private String tableTo;
	private String join = "Left Join";

	public List<String> getColumnFrom() {
		return columnFrom;
	}

	public void setColumnFrom(String pkColumn) {
		columnFrom.add(pkColumn);
	}

	public void setColumnFrom(List<String> pkColumn) {
		columnFrom.clear();
		columnFrom.addAll(pkColumn);
	}

	public List<String> getColumnTo() {
		return columnTo;
	}

	public void setColumnTo(String fkColumn) {
		columnTo.add(fkColumn);
	}

	public void setColumnTo(List<String> fkColumn) {
		columnTo.clear();
		columnTo.addAll(fkColumn);
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
		result.columnFrom = General.convertStringToList(element[0], ", ");
		result.columnTo = General.convertStringToList(element[1], ", ");
		result.setTableTo(element[2]);
		result.setJoin(element[3]);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(General.convertListToString(getColumnFrom(), ", ")).append("; ")
				.append(General.convertListToString(getColumnTo(), ", ")).append("; ").append(getTableTo()).append("; ")
				.append(getJoin());
		return sb.toString();
	}

	public ForeignKey copy() {
		ForeignKey result = new ForeignKey();
		result.columnFrom.addAll(columnFrom);
		result.columnTo.addAll(columnTo);
		result.setJoin(getJoin());
		result.setTableTo(getTableTo());
		return result;
	}
}
