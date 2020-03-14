package application.interfaces;

public enum FilterOperator {
	ISEQUALTO("Is equal to"), ISNOTEQUALTO("Is not equal to"), ISGREATERTHAN("Is greater than"),
	ISGREATERTHANOREQUALTO("Is greater than or equal to"), ISLESSTHAN("Is less than"),
	ISLESSTHANOREQULALTO("Is less than or equal to");

	private String value;

	private FilterOperator(String value) {
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
		return FilterOperator.ISEQUALTO;
	}
}
