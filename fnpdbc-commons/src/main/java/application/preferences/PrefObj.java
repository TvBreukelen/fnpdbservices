// $Id$

package application.preferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PrefObj {
	// Max byte count is 3/4 max string length (see Preferences
	// documentation).
	private static final int PIECELENGTH = 3 * Preferences.MAX_VALUE_LENGTH / 4;

	private PrefObj() {
		// Hide constructor
	}
	
	private static byte[] object2Bytes(Object o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		return baos.toByteArray();
	}

	private static byte[][] breakIntoPieces(byte[] raw) {
		int numPieces = (raw.length + PIECELENGTH - 1) / PIECELENGTH;
		byte[][] pieces = new byte[numPieces][];
		for (int i = 0; i < numPieces; ++i) {
			int startByte = i * PIECELENGTH;
			int endByte = startByte + PIECELENGTH;
			if (endByte > raw.length) {
				endByte = raw.length;
			}
			int length = endByte - startByte;
			pieces[i] = new byte[length];
			System.arraycopy(raw, startByte, pieces[i], 0, length);
		}
		return pieces;
	}

	private static void writePieces(Preferences prefs, String key, byte[][] pieces) throws BackingStoreException {
		Preferences node = prefs.node(key);
		node.clear();
		for (int i = 0; i < pieces.length; ++i) {
			node.putByteArray("" + i, pieces[i]);
		}
	}

	private static byte[][] readPieces(Preferences prefs, String key) throws BackingStoreException {
		Preferences node = prefs.node(key);
		String[] keys = node.keys();
		int numPieces = keys.length;
		byte[][] pieces = new byte[numPieces][];
		for (int i = 0; i < numPieces; ++i) {
			pieces[i] = node.getByteArray("" + i, null);
		}
		return pieces;
	}

	private static byte[] combinePieces(byte[][] pieces) {
		int length = 0;
		for (byte[] element : pieces) {
			length += element.length;
		}
		byte[] raw = new byte[length];
		int cursor = 0;
		for (byte[] element : pieces) {
			System.arraycopy(element, 0, raw, cursor, element.length);
			cursor += element.length;
		}
		return raw;
	}

	private static Object bytes2Object(byte[] raw) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
		ObjectInputStream ois = new ObjectInputStream(bais);
		return ois.readObject();
	}

	public static void putObject(Preferences prefs, String key, Object o) throws IOException, BackingStoreException {
		byte[] raw = object2Bytes(o);
		byte[][] pieces = breakIntoPieces(raw);
		writePieces(prefs, key, pieces);
	}

	public static Object getObject(Preferences prefs, String key)
			throws IOException, BackingStoreException, ClassNotFoundException {
		byte[][] pieces = readPieces(prefs, key);
		byte[] raw = combinePieces(pieces);
		return bytes2Object(raw);
	}
}
