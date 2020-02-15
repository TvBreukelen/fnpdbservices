package application.utils.gui;

import java.awt.GridBagConstraints;
import java.awt.Insets;

public class XGridBagConstraints extends GridBagConstraints {
	/**
	 * Klasse zur Erweiterung der <CODE>GridBagConstraints</CODE>. Diese Klasse
	 * stellt Methoden zum setzen der Parameter der <CODE>GridBagConstraints</CODE>
	 * zur Verf�gung. Diese Methoden setzen die �bergebenen Parameter; die �brigen
	 * Parameter erhalten Defaultwerte. Zus�tzlich liefern die Methoden das
	 * modifizierte Objekt als R�ckgabewert, so da� das Setzen der Parameter und die
	 * Verwendung des Objekts in einem Schritt erfolgen k�nnen. <BR>
	 * Eine genaue Beschreibung der Parameter findet sich in der Dokumentation der
	 * <CODE>GridBagConstraints</CODE>.
	 *
	 * @see java.awt.GridBagConstraints
	 * @author Hendrik W�rdehoff, sd&amp;m
	 */

	private static final long serialVersionUID = -4133978279158550012L;
	/**
	 * Konstante mit Defaultwert f�r Insets.
	 */
	protected static Insets DEFAULT_INSETS = new Insets(2, 3, 2, 3);
	/**
	 * Konstante f�r das Arbeiten ohne Insets.
	 */
	public static final Insets NO_INSETS = new Insets(0, 0, 0, 0);

	/**
	 * Insets f�r diese Instanz der <CODE>XGridBagConstraints</CODE>. Er kann mit
	 * {@link #setCommonInsets} ge�ndert werden.
	 */
	private Insets commonInsets;

	/**
	 * Vorgegebener Wert f�r {@link #anchor}. Der Wert wird mit dem Defaultwert
	 * {@link #WEST} vorbelegt. Er kann mit {@link #setCommonAnchor} ge�ndert
	 * werden.
	 */
	private int commonAnchor = WEST;

	/**
	 * Vorgegebener Wert f�r {@link #fill}. Der Wert wird mit dem Defaultwert
	 * {@link #BOTH} vorbelegt. Er kann mit {@link #setCommonFill} ge�ndert werden.
	 */
	private int commonFill = BOTH;

	/**
	 * Konstruktor mit Standardinsets.
	 */
	public XGridBagConstraints() {
		this(DEFAULT_INSETS);
	}

	/**
	 * Konstruktor mit expliziter Angabe der Insets.
	 *
	 * @param insets Vorgabe f�r Insets, sofern nicht angegeben
	 */
	public XGridBagConstraints(Insets insets) {
		commonInsets = insets;
	}

	/**
	 * Felder im Objekt auf die �bergebenen Werte setzen. Alle anderen Felder werden
	 * auf Defaultwerte gesetzt.
	 *
	 * @param gridx   Zeile im Gitter (ab Gitterindex 0)
	 * @param gridy   Spalte im Gitter (ab Gitterindex 0)
	 * @param weightx Gewicht der Zelle in horizontaler Richtung
	 * @param weighty Gewicht der Zelle in vertikaler Richtung
	 * @return das Objekt mit den eingetragenen Werten
	 */
	public GridBagConstraints gridCell(int gridx, int gridy, double weightx, double weighty) {
		return gridCell(gridx, gridy, weightx, weighty, commonFill);
	}

	/**
	 * Felder im Objekt auf die �bergebenen Werte setzen. Alle anderen Felder werden
	 * auf Defaultwerte gesetzt.
	 *
	 * @param gridx   Zeile im Gitter (ab Gitterindex 0)
	 * @param gridy   Spalte im Gitter (ab Gitterindex 0)
	 * @param weightx Gewicht der Zelle in horizontaler Richtung
	 * @param weighty Gewicht der Zelle in vertikaler Richtung
	 * @param fill    Beeinflussung des Inhalts durch die Zellengr��e
	 * @return das Objekt mit den eingetragenen Werten
	 */
	public GridBagConstraints gridCell(int gridx, int gridy, double weightx, double weighty, int fill) {
		return gridCell(gridx, gridy, weightx, weighty, 1, 1, fill);
	}

	/**
	 * Felder im Objekt auf die �bergebenen Werte setzen. Alle anderen Felder werden
	 * auf Defaultwerte gesetzt.
	 *
	 * @param gridx   Zeile im Gitter (ab Gitterindex 0)
	 * @param gridy   Spalte im Gitter (ab Gitterindex 0)
	 * @param weightx Gewicht der Zelle in horizontaler Richtung
	 * @param weighty Gewicht der Zelle in vertikaler Richtung
	 * @param fill    Beeinflussung des Inhalts durch die Zellengr��e
	 * @param anchor  Positionierung des Inhalts in der Zelle
	 * @return das Objekt mit den eingetragenen Werten
	 */
	public GridBagConstraints gridCell(int gridx, int gridy, double weightx, double weighty, int fill, int anchor) {
		return gridCell(gridx, gridy, weightx, weighty, 1, 1, fill, anchor);
	}

	/**
	 * Felder im Objekt auf die �bergebenen Werte setzen. Alle anderen Felder werden
	 * auf Defaultwerte gesetzt.
	 *
	 * @param gridx      Zeile im Gitter (ab Gitterindex 0)
	 * @param gridy      Spalte im Gitter (ab Gitterindex 0)
	 * @param weightx    Gewicht der Zelle in horizontaler Richtung
	 * @param weighty    Gewicht der Zelle in vertikaler Richtung
	 * @param gridwidth  �berdeckte Gitterzellen in horizontaler Richtung
	 * @param gridheight �berdeckte Gitterzellen in vertikaler Richtung
	 * @param fill       Beeinflussung des Inhalts durch die Zellengr��e
	 * @return das Objekt mit den eingetragenen Werten
	 */
	public GridBagConstraints gridCell(int gridx, int gridy, double weightx, double weighty, int gridwidth,
			int gridheight, int fill) {
		return gridCell(gridx, gridy, weightx, weighty, gridwidth, gridheight, fill, commonAnchor);
	}

	/**
	 * Felder im Objekt auf die �bergebenen Werte setzen. Alle anderen Felder werden
	 * auf Defaultwerte gesetzt.
	 *
	 * @param gridx      Zeile im Gitter (ab Gitterindex 0)
	 * @param gridy      Spalte im Gitter (ab Gitterindex 0)
	 * @param weightx    Gewicht der Zelle in horizontaler Richtung
	 * @param weighty    Gewicht der Zelle in vertikaler Richtung
	 * @param gridwidth  �berdeckte Gitterzellen in horizontaler Richtung
	 * @param gridheight �berdeckte Gitterzellen in vertikaler Richtung
	 * @param fill       Beeinflussung des Inhalts durch die Zellengr��e
	 * @param anchor     Positionierung des Inhalts in der Zelle
	 * @return das Objekt mit den eingetragenen Werten
	 */
	public GridBagConstraints gridCell(int gridx, int gridy, double weightx, double weighty, int gridwidth,
			int gridheight, int fill, int anchor) {
		return gridCell(gridx, gridy, weightx, weighty, gridwidth, gridheight, fill, anchor, commonInsets);
	}

	/**
	 * Felder im Objekt auf die �bergebenen Werte setzen. Alle anderen Felder werden
	 * auf Defaultwerte gesetzt.
	 *
	 * @param gridx      Zeile im Gitter (ab Gitterindex 0)
	 * @param gridy      Spalte im Gitter (ab Gitterindex 0)
	 * @param weightx    Gewicht der Zelle in horizontaler Richtung
	 * @param weighty    Gewicht der Zelle in vertikaler Richtung
	 * @param gridwidth  �berdeckte Gitterzellen in horizontaler Richtung
	 * @param gridheight �berdeckte Gitterzellen in vertikaler Richtung
	 * @return das Objekt mit den eingetragenen Werten
	 */
	public GridBagConstraints gridmultipleCell(int gridx, int gridy, double weightx, double weighty, int gridwidth,
			int gridheight) {
		return gridCell(gridx, gridy, weightx, weighty, gridwidth, gridheight, commonFill, commonAnchor);
	}

	/**
	 * Felder im Objekt auf die �bergebenen Werte setzen. Alle anderen Felder werden
	 * auf Defaultwerte gesetzt.
	 *
	 * @param gridx      Zeile im Gitter (ab Gitterindex 0)
	 * @param gridy      Spalte im Gitter (ab Gitterindex 0)
	 * @param weightx    Gewicht der Zelle in horizontaler Richtung
	 * @param weighty    Gewicht der Zelle in vertikaler Richtung
	 * @param gridwidth  �berdeckte Gitterzellen in horizontaler Richtung
	 * @param gridheight �berdeckte Gitterzellen in vertikaler Richtung
	 * @param fill       Beeinflussung des Inhalts durch die Zellengr��e
	 * @param anchor     Positionierung des Inhalts in der Zelle
	 * @param insets     Abstand zwischen Zelle und Inhalt
	 * @return das Objekt mit den eingetragenen Werten
	 */
	public GridBagConstraints gridCell(int gridx, int gridy, double weightx, double weighty, int gridwidth,
			int gridheight, int fill, int anchor, Insets insets) {
		this.gridx = gridx;
		this.gridy = gridy;
		this.gridwidth = gridwidth;
		this.gridheight = gridheight;
		this.weightx = weightx;
		this.weighty = weighty;
		this.anchor = anchor;
		this.fill = fill;
		this.insets = insets;
		ipadx = 0;
		ipady = 0;
		return this;
	}
}