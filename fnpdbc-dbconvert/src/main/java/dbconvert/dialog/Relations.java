package dbconvert.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.VerticalLayout;

import application.dialog.BasicDialog;
import application.interfaces.ExportFile;
import application.interfaces.IDatabaseFactory;
import application.utils.GUIFactory;
import application.utils.gui.XGridBagConstraints;
import dbconvert.model.RelationData;
import dbengine.SqlDB;
import dbengine.utils.ForeignKey;
import dbengine.utils.SqlTable;

public class Relations extends BasicDialog {
	private static final long serialVersionUID = -4009779144998771769L;
	private SqlDB sqlDb;
	private SqlTable sqlTable;
	private RelationData pdaSettings;

	private Map<String, ForeignKey> mapFk = new HashMap<>();

	public Relations(IDatabaseFactory dbFactory, RelationData data) {
		sqlDb = (SqlDB) dbFactory.getInputFile();
		sqlTable = sqlDb.getSqlTable();
		pdaSettings = data;
		init();
	}

	private void init() {
		init(GUIFactory.getText("table") + " " + sqlTable.getName() + " - " + GUIFactory.getTitle("relationships"));
		buildDialog();
		activateComponents();
		pack();
	}

	@Override
	protected void buildDialog() {
		btSave.setVisible(false);
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		getContentPane().add(createCenterPanel(), BorderLayout.CENTER);
		getContentPane().add(createBottomPanel(), BorderLayout.SOUTH);
		setMinimumSize(new Dimension(400, 200));
	}

	@Override
	protected void save() throws Exception {
		pdaSettings.setForeignKeys(mapFk.values());
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new VerticalLayout(5));

		// Foreign keys from table design
		sqlTable.getFkList().values().forEach(fk -> result.add(createForeignKeyPanel(fk)));

		// Foreign keys which were manually entered
		pdaSettings.getForeignKeys().forEach(fk -> result.add(createForeignKeyPanel(fk)));

		result.setBorder(BorderFactory.createEtchedBorder());
		return result;
	}

	private JPanel createForeignKeyPanel(ForeignKey fk) {
		JPanel panel = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();
		mapFk.put(fk.getTableTo(), fk);

		JComboBox<String> cb = getJoins(fk.getTableTo());
		cb.setSelectedItem(pdaSettings.getForeignKey(fk.getTableTo()).getJoin());
		pdaSettings.deleteForeignKey(fk.getTableTo());

		panel.add(new JLabel(GUIFactory.getText("FromTable")), c.gridCell(0, 0, 0, 0));
		panel.add(getTableOrField(sqlTable.getName()), c.gridCell(1, 0, 0, 0));
		panel.add(new JLabel(GUIFactory.getText("fromField")), c.gridCell(0, 1, 0, 0));

		int row = 1;
		for (String col : fk.getColumnFrom()) {
			panel.add(getTableOrField(col), c.gridCell(1, row++, 0, 0));
		}

		panel.add(cb, c.gridCell(2, 0, 0, 0));
		panel.add(new JLabel(GUIFactory.getText("toTable")), c.gridCell(3, 0, 0, 0));
		panel.add(getTableOrField(fk.getTableTo()), c.gridCell(4, 0, 0, 0));
		panel.add(new JLabel(GUIFactory.getText("toField")), c.gridCell(3, 1, 0, 0));

		row = 1;
		for (String col : fk.getColumnTo()) {
			panel.add(getTableOrField(col), c.gridCell(4, row++, 0, 0));
		}

		panel.setBorder(BorderFactory.createEtchedBorder());
		return panel;
	}

	private JTextField getTableOrField(String value) {
		JTextField result = new JTextField(value, 10);
		result.setEditable(false);
		return result;
	}

	@SuppressWarnings("unchecked")
	private JComboBox<String> getJoins(String toTable) {
		JComboBox<String> result = new JComboBox<>();
		result.setPreferredSize(new Dimension(100, 25));
		result.addItem("Left Join");
		result.addItem("Right Join");
		result.addItem("Inner join");

		if (sqlDb.getImportFile() != ExportFile.SQLITE) {
			result.addItem("Full Join");
		}

		result.setActionCommand(toTable);
		result.addActionListener(e -> {
			JComboBox<String> jc = (JComboBox<String>) e.getSource();
			mapFk.get(e.getActionCommand()).setJoin(jc.getSelectedItem().toString());
		});
		return result;
	}

	private Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(GUIFactory.getJButton("apply", funcSave));
		return panel;
	}
}
