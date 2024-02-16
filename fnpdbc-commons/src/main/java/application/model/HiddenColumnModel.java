package application.model;

import java.util.Arrays;

import javax.swing.table.AbstractTableModel;

public abstract class HiddenColumnModel extends AbstractTableModel {
	private static final long serialVersionUID = 7096538035424503544L;
	protected boolean[] columnsVisible;
	protected int visibleColumns;
	protected int totalColumns;

	public HiddenColumnModel(int totalColumns) {
		this.totalColumns = totalColumns;
		columnsVisible = new boolean[totalColumns];
		visibleColumns = totalColumns;
		Arrays.fill(columnsVisible, true);
	}

	/**
	 * This function converts a column number in the table to the right number of
	 * the data.
	 */
	public int getNumber(int col) {
		int n = col; // right number to return
		int i = 0;
		do {
			if (!columnsVisible[i]) {
				n++;
			}
			i++;
		} while (i < n);
		// If we are on an invisible column,
		// we have to go one step further
		while (!columnsVisible[n]) {
			n++;
		}
		return n;
	}

	public boolean isColumnVisible(int col) {
		return columnsVisible[col];
	}

	@Override
	public int getColumnCount() {
		return visibleColumns;
	}
}
