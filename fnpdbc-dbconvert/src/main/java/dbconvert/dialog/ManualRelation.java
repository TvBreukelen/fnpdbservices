package dbconvert.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import application.dialog.BasicDialog;
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
	private JList<String> cbFromColumn;
	private JList<String> cbToColumn;
	private DefaultListModel<String> fromColModel;
	private DefaultListModel<String> toColModel;
	private JButton btApply;

	public ManualRelation(SqlDB db, Collection<ForeignKey> keys, ForeignKey key) {
		sqlDb = db;
		sqlTable = sqlDb.getSqlTable();
		foreignKeys = keys;
		pdaSettings = key;
		init();
	}

	private void init() {
		init(GUIFactory.getTitle("funcNewRelation"));
		setHelpFile("add_relationship");
		buildDialog();
		pack();
	}

	@Override
	protected void buildDialog() {
		btSave.setVisible(false);
		btApply = GUIFactory.getJButton("apply", funcSave);

		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		getContentPane().add(createCenterPanel(), BorderLayout.CENTER);
		getContentPane().add(createBottomPanel(), BorderLayout.SOUTH);
	}

	@Override
	protected Component createCenterPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		getTablesComboxBox();
		panel.add(createForeignKeyPanel());
		panel.setBorder(BorderFactory.createEtchedBorder());
		return panel;
	}

	private Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(btApply);
		return panel;
	}

	private void getTablesComboxBox() {
		cbFromTable = new JComboBox<>();
		cbFromColumn = new JList<>();
		cbToTable = new JComboBox<>();
		cbToColumn = new JList<>();
		fromColModel = new DefaultListModel<>();
		toColModel = new DefaultListModel<>();

		cbFromColumn.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		cbFromColumn.setModel(fromColModel);
		cbFromColumn.addListSelectionListener(e -> activateComponents());

		cbToColumn.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		cbToColumn.setModel(toColModel);
		cbToColumn.addListSelectionListener(e -> activateComponents());

		cbFromTable.addItem(sqlTable.getName());
		sqlDb.getTableOrSheetNames().forEach(cbToTable::addItem);
		cbToTable.removeItem(sqlTable.getName());

		foreignKeys.forEach(fk -> {
			if (!fk.getTableTo().equals(sqlTable.getName())) {
				cbFromTable.addItem(fk.getTableTo());
				cbToTable.removeItem(fk.getTableTo());
			}
		});

		cbFromTable.addActionListener(e -> refreshColumnList(cbFromTable.getSelectedItem().toString(), true));
		cbToTable.addActionListener(e -> refreshColumnList(cbToTable.getSelectedItem().toString(), false));

		cbFromTable.setSelectedIndex(0);
		cbToTable.setSelectedIndex(0);
	}

	private JPanel createForeignKeyPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();
		Font boldFont = new Font("serif", Font.BOLD, 14);

		JScrollPane scrollFrom = new JScrollPane(cbFromColumn);
		JScrollPane scrollTo = new JScrollPane(cbToColumn);
		scrollFrom.setPreferredSize(new Dimension(150, 200));
		scrollTo.setPreferredSize(new Dimension(150, 200));

		panel.add(GUIFactory.getJLabel("fromTable", boldFont), c.gridCell(0, 0, 0, 0));
		panel.add(GUIFactory.getJLabel("toTable", boldFont), c.gridCell(1, 0, 0, 0));
		panel.add(cbFromTable, c.gridCell(0, 1, 0, 0));
		panel.add(cbToTable, c.gridCell(1, 1, 0, 0));
		panel.add(Box.createVerticalStrut(5), c.gridCell(0, 2, 0, 0));

		panel.add(GUIFactory.getJLabel("fromField", boldFont), c.gridCell(0, 3, 0, 0));
		panel.add(GUIFactory.getJLabel("toField", boldFont), c.gridCell(1, 3, 0, 0));
		panel.add(scrollFrom, c.gridCell(0, 4, 0, 0));
		panel.add(scrollTo, c.gridCell(1, 4, 0, 0));

		panel.setBorder(BorderFactory.createEtchedBorder());
		return panel;
	}

	private void refreshColumnList(String table, boolean isFromTable) {
		SqlTable sTable = sqlDb.getSqlTable(table);
		DefaultListModel<String> model = isFromTable ? fromColModel : toColModel;
		JList<String> list = isFromTable ? cbFromColumn : cbToColumn;

		model.clear();
		sTable.getDbFields().forEach(field -> model.addElement(field.getFieldAlias()));
		list.setSelectedIndex(0);

		activateComponents();
	}

	@Override
	protected void save() {
		pdaSettings.setColumnFrom(cbFromColumn.getSelectedValuesList());
		pdaSettings.setColumnTo(cbToColumn.getSelectedValuesList());
		pdaSettings.setTableFrom(cbFromTable.getSelectedItem().toString());
		pdaSettings.setTableTo(cbToTable.getSelectedItem().toString());
		pdaSettings.setUserDefined(true);
	}

	@Override
	public void activateComponents() {
		btApply.setEnabled(cbFromColumn.getSelectedValuesList().size() == cbToColumn.getSelectedValuesList().size());
	}

}
