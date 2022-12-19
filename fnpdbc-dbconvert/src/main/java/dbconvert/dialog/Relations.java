package dbconvert.dialog;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import application.dialog.BasicDialog;
import application.interfaces.TvBSoftware;
import application.preferences.Databases;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbengine.SqlDB;
import dbengine.utils.ForeignKey;
import dbengine.utils.SqlTable;

public class Relations extends BasicDialog {
	private static final long serialVersionUID = -4009779144998771769L;
	private Databases dbSettings = Databases.getInstance(TvBSoftware.DBCONVERT);
	private SqlDB sqlDb;
	private SqlTable sqlTable;

	public Relations(SqlDB dbIn) {
		sqlDb = dbIn;
		sqlTable = sqlDb.getSqlTable();
		init();
	}

	private void init() {
		init(sqlTable.getName() + " - " + GUIFactory.getTitle("relationships"));
		buildDialog();
		activateComponents();
		pack();
	}

	@Override
	protected void save() throws Exception {
		dbSettings.getDatabase();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();
		int row = 0;

		for (Entry<String, ForeignKey> entry : sqlTable.getFkList().entrySet()) {
			result.add(new JLabel(sqlTable.getName()), c.gridCell(0, row, 0, 0));
			result.add(new JLabel(General.createImageIcon("ArrowR.png")), c.gridCell(1, row, 0, 0));
			result.add(new JLabel(entry.getKey()), c.gridCell(2, row++, 0, 0));
			result.add(new JLabel(entry.getValue().getColumnTo()), c.gridCell(0, row, 0, 0));
			result.add(new JLabel(entry.getValue().getColumnFrom()), c.gridCell(1, row++, 0, 0));
			result.add(Box.createHorizontalStrut(5), c.gridCell(0, row++, 0, 0));
		}

		result.setBorder(BorderFactory.createEtchedBorder());
		return result;
	}

}
