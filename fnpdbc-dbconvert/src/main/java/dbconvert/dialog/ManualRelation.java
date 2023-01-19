package dbconvert.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.VerticalLayout;

import application.dialog.BasicDialog;
import application.interfaces.ExportFile;
import application.utils.GUIFactory;
import application.utils.gui.XGridBagConstraints;
import dbengine.SqlDB;
import dbengine.utils.ForeignKey;
import dbengine.utils.SqlTable;

public class ManualRelation extends BasicDialog {
	private static final long serialVersionUID = -8458901845122803271L;

	private SqlDB sqlDb;
	private SqlTable sqlTable;
	private ForeignKey pdaSettings;
	private Collection<ForeignKey> foreignKeys;
	private JComboBox<String> cbFromTable;
	private JComboBox<String> cbToTable;
	private JComboBox<String> cbFromColumn;
	private JComboBox<String> cbToColumn;
	private JComboBox<String> cbJoins;

	public ManualRelation(SqlDB db, Collection<ForeignKey> keys, ForeignKey key) {
		sqlDb = db;
		sqlTable = sqlDb.getSqlTable();
		foreignKeys = keys;
		pdaSettings = key;
		init();
	}

	private void init() {
		init(GUIFactory.getTitle("funcNewRelation"));
		buildDialog();
		pack();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel panel = new JPanel(new VerticalLayout(5));

		getTablesComboxBox();
		getJoinsCombobox();

		panel.add(createForeignKeyPanel());
		panel.setBorder(BorderFactory.createEtchedBorder());
		return panel;
	}

	private void getJoinsCombobox() {
		cbJoins = new JComboBox<>();
		cbJoins.setPreferredSize(new Dimension(100, 25));
		cbJoins.addItem("Left Join");
		cbJoins.addItem("Right Join");
		cbJoins.addItem("Inner join");

		if (sqlDb.getImportFile() != ExportFile.SQLITE) {
			cbJoins.addItem("Full Join");
		}

		cbJoins.setSelectedItem("Left Join");
	}

	private void getTablesComboxBox() {
		cbFromTable = new JComboBox<>();
		cbFromColumn = new JComboBox<>();
		cbToTable = new JComboBox<>();
		cbToColumn = new JComboBox<>();

		cbFromTable.addItem(sqlTable.getName());
		sqlDb.getTableOrSheetNames().forEach(cbToTable::addItem);
		cbToTable.removeItem(sqlTable.getName());

		foreignKeys.forEach(fk -> {
			if (!fk.getTableTo().equals(sqlTable.getName())) {
				cbFromTable.addItem(fk.getTableTo());
				cbToTable.removeItem(fk.getTableTo());
			}
		});

		cbFromTable
				.addActionListener(e -> refreshColumnCombobox(cbFromColumn, cbFromTable.getSelectedItem().toString()));
		cbToTable.addActionListener(e -> refreshColumnCombobox(cbToColumn, cbToTable.getSelectedItem().toString()));

		cbFromTable.setSelectedIndex(0);
		cbToTable.setSelectedIndex(0);
	}

	private JPanel createForeignKeyPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		panel.add(new JLabel(GUIFactory.getText("fromTable")), c.gridCell(0, 0, 0, 0));
		panel.add(cbFromTable, c.gridCell(1, 0, 0, 0));
		panel.add(new JLabel(GUIFactory.getText("fromField")), c.gridCell(0, 1, 0, 0));
		panel.add(cbFromColumn, c.gridCell(1, 1, 0, 0));
		panel.add(cbJoins, c.gridCell(2, 0, 0, 0));
		panel.add(new JLabel(GUIFactory.getText("toTable")), c.gridCell(3, 0, 0, 0));
		panel.add(cbToTable, c.gridCell(4, 0, 0, 0));
		panel.add(new JLabel(GUIFactory.getText("toField")), c.gridCell(3, 1, 0, 0));
		panel.add(cbToColumn, c.gridCell(4, 1, 0, 0));

		panel.setBorder(BorderFactory.createEtchedBorder());
		return panel;
	}

	private void refreshColumnCombobox(JComboBox<String> cb, String table) {
		SqlTable sTable = sqlDb.getSqlTable(table);
		cb.removeAllItems();
		sTable.getDbFields().forEach(field -> cb.addItem(field.getFieldAlias()));
		cb.setSelectedIndex(0);
	}

	@Override
	protected void save() {
		pdaSettings.setColumnFrom(cbFromColumn.getSelectedItem().toString());
		pdaSettings.setColumnTo(cbToColumn.getSelectedItem().toString());
		pdaSettings.setTableFrom(cbFromTable.getSelectedItem().toString());
		pdaSettings.setTableTo(cbToTable.getSelectedItem().toString());
		pdaSettings.setJoin(cbJoins.getSelectedItem().toString());
		pdaSettings.setUserDefined(true);
	}
}
