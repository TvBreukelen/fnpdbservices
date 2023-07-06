package application.utils;

public class FNProgException extends Exception {
	/**
	 * Title: FNProgException Description: FNProg2PDA Exception Class Copyright: (c)
	 * 2004-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 2632073785952240467L;

	public FNProgException(String arg0) {
		super(arg0);
	}

	public static FNProgException getException(String errorID, String... strings) {
		String errMesg = GUIFactory.getMessage(errorID, strings);
		return new FNProgException(errMesg);
	}

	public static Exception getFatalException(String errorID, String... strings) {
		String errMesg = GUIFactory.getMessage(errorID, strings);
		return new Exception(errMesg);
	}
}