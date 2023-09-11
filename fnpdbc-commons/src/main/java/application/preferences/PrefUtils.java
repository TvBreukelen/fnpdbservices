package application.preferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.prefs.Preferences;

public class PrefUtils {

	private PrefUtils() {
		// Hide constructor
	}

	public static void writePref(Preferences pref, String key, boolean newValue, boolean oldValue,
			boolean defaultValue) {
		if (newValue != oldValue) {
			if (newValue == defaultValue) {
				pref.remove(key);
			} else {
				pref.putBoolean(key, newValue);
			}
		}
	}

	public static void writePref(Preferences pref, String key, String newValue, String oldValue, String defaultValue) {
		if (!newValue.equals(oldValue)) {
			if (newValue.equals(defaultValue)) {
				pref.remove(key);
			} else {
				pref.put(key, newValue);
			}
		}
	}

	public static void writePref(Preferences pref, String key, int newValue, int oldValue, int defaultValue) {
		if (newValue != oldValue) {
			if (newValue == defaultValue) {
				pref.remove(key);
			} else {
				pref.putInt(key, newValue);
			}
		}
	}

	public static void writePref(Preferences pref, String key, long newValue, long oldValue, long defaultValue) {
		if (newValue != oldValue) {
			if (newValue == defaultValue) {
				pref.remove(key);
			} else {
				pref.putLong(key, newValue);
			}
		}
	}

	public static void copyNode(Preferences copyFrom, Preferences copyTo, String profile) throws Exception {
		String[] keys = copyFrom.keys();
		Preferences p1 = copyTo.node(profile);
		final String userList = "userList";

		for (String key : keys) {
			p1.put(key, copyFrom.get(key, ""));
		}

		if (copyFrom.nodeExists(userList)) {
			Preferences p2 = copyFrom.node(userList);
			Preferences p3 = p1.node(userList);
			keys = p2.keys();
			for (String key : keys) {
				p3.put(key, p2.get(key, ""));
			}
		}

		copyTo.flush();
	}

	public static boolean deleteNode(Preferences pref, String key) {
		try {
			if (pref.nodeExists(key)) {
				Preferences p = pref.node(key);
				p.removeNode();
				pref.flush();
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public static void exportNode(Preferences pref, String filename) {
		try {
			// Export the node to a file
			pref.exportSubtree(new FileOutputStream(filename));
		} catch (Exception e) {
		}
	}

	public static void importNode(String filename) {
		try {
			// Import the node from a file
			Preferences.importPreferences(new FileInputStream(filename));
		} catch (Exception e) {
		}
	}
}
