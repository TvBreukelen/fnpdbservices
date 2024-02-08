package dbconvert.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import application.dialog.BasicDialog;
import application.dialog.ScFieldSelect;
import application.interfaces.ExportFile;
import application.interfaces.IDatabaseFactory;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbconvert.software.XConverter;
import dbengine.SqlDB;
import dbengine.utils.ForeignKey;
import dbengine.utils.RelationData;
import dbengine.utils.SqlTable;

public class Relations extends BasicDialog {
	private static final long serialVersionUID = -4009779144998771769L;
	private SqlDB sqlDb;
	private SqlTable sqlTable;
	private RelationData pdaSettings;
	private XConverter dbFactory;
	private ScFieldSelect fieldSelect;

	private Map<String, ForeignKey> mapFk = new LinkedHashMap<>();
	private Map<String, JPanel> mapPanel = new HashMap<>();

	private JPanel centerPanel;
	private JButton btApply;

	public Relations(IDatabaseFactory dbFactory, ScFieldSelect fieldSelect, RelationData data) {
		this.dbFactory = (XConverter) dbFactory;
		this.fieldSelect = fieldSelect;

		sqlDb = (SqlDB) dbFactory.getInputFile();
		sqlTable = sqlDb.getSqlTable();
		pdaSettings = data;
		init();
	}

	private void init() {
		init(GUIFactory.getText("table") + General.SPACE + sqlTable.getName() + " - "
				+ GUIFactory.getTitle("relationships"));
		setHelpFile("relationships");
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
	protected void save() throws Exception {
		pdaSettings.setForeignKeys(mapFk.values());
		sqlTable.setFkList(mapFk);
		dbFactory.setupDBTranslation(false);
		fieldSelect.loadFieldPanel(dbFactory.getDbUserFields(), false);
	}

	@Override
	protected Component createCenterPanel() {
		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

		if (sqlTable.getFkList().isEmpty()) {
			centerPanel.add(createNoForeignKeyScreen());
		} else {
			sqlTable.getFkList().values().forEach(fk -> centerPanel.add(createForeignKeyPanel(fk)));
		}

		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		return centerPanel;
	}

	@Override
	protected Component addToToolbar() {
		return GUIFactory.createToolBarButton(GUIFactory.getToolTip("funcNewRelation"), "new_relationship.png", e -> {
			ForeignKey key = new ForeignKey();
			ManualRelation dialog = new ManualRelation(sqlDb, mapFk.values(), key);
			dialog.setVisible(true);

			if (key.isUserDefined()) {
				pdaSettings.addForeignKey(key);
				centerPanel.add(createForeignKeyPanel(key));
				addOrRemoveForeignKey(key.getTableTo(), false);
			}
		});
	}

	private Component createNoForeignKeyScreen() {
		JEditorPane result = new JEditorPane();
		result.setContentType("text/html");
		result.setText(GUIFactory.getText("noForeignkeyTable"));
		result.setEditable(false);
		return result;
	}

	private JPanel createForeignKeyPanel(ForeignKey fk) {
		JPanel panel = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		if (mapFk.isEmpty()) {
			centerPanel.removeAll();
		}

		mapFk.put(fk.getTableTo(), fk);

		JComboBox<String> cb = getJoins(fk.getTableTo());
		cb.setSelectedItem(pdaSettings.getForeignKey(fk.getTableTo()).getJoin());

		panel.add(new JLabel(GUIFactory.getText("fromTable")), c.gridCell(0, 0, 0, 0));
		panel.add(getTableOrField(fk.getTableFrom()), c.gridCell(1, 0, 0, 0));
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

		JButton bt = GUIFactory.getJButton("remove", fk.getTableTo(),
				e -> addOrRemoveForeignKey(fk.getTableTo(), true));

		if (fk.isUserDefined()) {
			mapPanel.put(fk.getTableTo(), panel);
			panel.add(bt, c.gridCell(5, 1, 0, 0));
		} else {
			panel.add(Box.createRigidArea(bt.getPreferredSize()), c.gridCell(5, 1, 0, 0));
		}

		panel.setBorder(BorderFactory.createEtchedBorder());
		return panel;
	}

	private void addOrRemoveForeignKey(String table, boolean isRemove) {
		if (isRemove) {
			boolean isInitial = true;
			while (true) {
				final String toTable = table;
				List<String> tables = mapFk.values().stream()
						.filter(v -> v.isUserDefined() && v.getTableFrom().equals(toTable)).map(ForeignKey::getTableTo)
						.collect(Collectors.toList());

				if (isInitial && !tables.isEmpty()
						&& !General.showConfirmMessage(centerPanel,
								GUIFactory.getMessage("removeRelationships", toTable, tables.get(0)),
								GUIFactory.getText("remove"))) {
					break;
				}

				isInitial = false;
				tables.add(toTable);

				tables.forEach(t -> {
					if (mapFk.containsKey(t)) {
						centerPanel.remove(mapPanel.get(t));
						mapPanel.remove(t);
						mapFk.remove(t);
					}
				});

				if (tables.size() == 1) {
					break;
				}

				table = tables.get(0);
			}

			if (mapFk.isEmpty()) {
				centerPanel.removeAll();
				centerPanel.add(createNoForeignKeyScreen());
			}
		}

		pack();
		btApply.setEnabled(true);
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
			btApply.setEnabled(true);
		});
		return result;
	}

	private Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(btApply);
		btApply.setEnabled(false);
		return panel;
	}
}
