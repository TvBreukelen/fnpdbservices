package application.interfaces;

public enum FilterOperator {
	IS_EQUAL_TO("Is equal to"), IS_NOT_EQUAL_TO("Is not equal to"), IS_GREATER_THAN("Is greater than"),
	IS_GREATER_THAN_OR_EQUAL_TO("Is greater than or equal to"), IS_LESS_THAN("Is less than"),
	IS_LESS_THAN_OR_EQUAL_TO("Is less than or equal to");

	private String value;

	FilterOperator(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static String[] getFilterOperators() {
		int maxOptions = values().length;
		String[] result = new String[maxOptions];
		for (int i = 0; i < maxOptions; i++) {
			result[i] = values()[i].value;
		}
		return result;
	}

	public static FilterOperator getFilterOperator(String option) {
		for (FilterOperator result : FilterOperator.values()) {
			if (result.value.equals(option)) {
				return result;
			}
		}
		return FilterOperator.IS_EQUAL_TO;
	}
}
