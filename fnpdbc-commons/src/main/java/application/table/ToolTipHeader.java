package application.table;

import java.awt.event.MouseEvent;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import application.utils.General;

public class ToolTipHeader extends JTableHeader {
	private static final long serialVersionUID = 7679099173642805581L;
	private String[] toolTips;

	public ToolTipHeader(TableColumnModel model, String[] toolTips) {
		super(model);
		this.toolTips = toolTips;
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		int col = columnAtPoint(e.getPoint());
		int modelCol = getTable().convertColumnIndexToModel(col);
		String retStr;
		try {
			retStr = toolTips[modelCol];
		} catch (NullPointerException | ArrayIndexOutOfBoundsException ex) {
			retStr = General.EMPTY_STRING;
		}

		if (retStr.length() < 1) {
			retStr = super.getToolTipText(e);
		}
		return retStr;
	}
}