package dbengine.utils;

import java.util.ArrayList;
import java.util.List;

import application.utils.General;

public class ForeignKey {
	private List<String> columnFrom = new ArrayList<>();
	private List<String> columnTo = new ArrayList<>();
	private String tableFrom;
	private String tableTo;
	private String join = "Left Join";
	private boolean isUserDefined = false;

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

	public String getTableFrom() {
		return tableFrom;
	}

	public void setTableFrom(String tableFrom) {
		this.tableFrom = tableFrom;
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

	public boolean isUserDefined() {
		return isUserDefined;
	}

	public void setUserDefined(boolean isUserDefined) {
		this.isUserDefined = isUserDefined;
	}

	public static ForeignKey getFromString(String registry) {
		String[] element = registry.split(";");
		ForeignKey result = new ForeignKey();
		result.columnFrom = General.convertStringToList(element[0], ",");
		result.columnTo = General.convertStringToList(element[1], ",");
		result.setTableFrom(element[2]);
		result.setTableTo(element[3]);
		result.setJoin(element[4]);
		result.setUserDefined(Boolean.parseBoolean(element[5]));
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(General.convertListToString(getColumnFrom(), ",")).append(";")
				.append(General.convertListToString(getColumnTo(), ",")).append(";").append(getTableFrom()).append(";")
				.append(getTableTo()).append(";").append(join).append(";").append(isUserDefined);
		return sb.toString();
	}

	public ForeignKey copy() {
		ForeignKey result = new ForeignKey();
		result.setColumnFrom(getColumnFrom());
		result.setColumnTo(getColumnTo());
		result.setTableFrom(getTableFrom());
		result.setTableTo(getTableTo());
		result.setJoin(getJoin());
		result.setUserDefined(isUserDefined());
		return result;
	}
}
