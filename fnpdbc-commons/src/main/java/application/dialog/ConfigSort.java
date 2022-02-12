package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseFactory;
import application.model.SortData;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ConfigSort extends BasicDialog {
	/**
	 * Title: ScFieldSort Description: FNProgramvare Software Sort Configuration
	 *
	 * @author Tom van Breukelen
	 * @version 4.5+
	 */
	private static final long serialVersionUID = -1957596896864461526L;

	private JComboBox<String> jcCategoryField;
	private JComboBox<String>[] jcSortField;
	private JTextField[] txGroupingField;
	private JTextField txRemainField;
	private JCheckBox[] cbGroupField;
	private final int numSort;

	private String[] dbFilterFields;
	private ExportFile myExportFile;

	private boolean isGroupBy;
	private boolean hasGrouping;

	private transient SortData pdaSettings;

	@SuppressWarnings("unchecked")
	public ConfigSort(IDatabaseFactory dbFactory, SortData data) {
		myExportFile = dbFactory.getExportFile();
		pdaSettings = data;
		dbFilterFields = dbFactory.getDbFilterFields();
		numSort = myExportFile.getMaxSortFields();
		jcSortField = new JComboBox[numSort];
		cbGroupField = new JCheckBox[numSort];
		txGroupingField = new JTextField[numSort];
		txRemainField = GUIFactory.getJTextField("remainderGroup", pdaSettings.getRemainingField());
		init();
	}

	private void init() {
		init(myExportFile.getName() + " " + GUIFactory.getText("menuSortOrder"));

		switch (myExportFile) {
		case LIST:
			setHelpFile("sort_list");
			break;
		case JSON:
			setHelpFile("sort_json");
			break;
		case YAML:
			setHelpFile("sort_yaml");
			break;
		case XML:
			setHelpFile("sort_xml");
			break;
		default:
			setHelpFile("sort_order");
		}

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
	protected Component addToToolbar() {
		return General.createToolBarButton(GUIFactory.getToolTip("funcRemoveSort"), "Delete.png",
				e -> Arrays.stream(jcSortField).forEach(cb -> cb.setSelectedItem("")));
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		int index = 0;

		XGridBagConstraints c = new XGridBagConstraints();

		String guiText = "sortField";
		isGroupBy = myExportFile.isSpecialFieldSort();
		boolean hasCategories = myExportFile == ExportFile.LIST;
		hasGrouping = isGroupBy;

		switch (myExportFile) {
		case LIST:
			guiText = "sortFieldList";
			isGroupBy = false;
			hasGrouping = false;
			break;
		case XML:
			guiText = "sortFieldXml";
			hasGrouping = false;
			break;
		case JSON:
		case YAML:
			guiText = "sortFieldJson";
			break;
		default:
			break;
		}

		if (hasCategories) {
			jcCategoryField = new JComboBox<>(dbFilterFields);
			jcCategoryField.setSelectedItem(findFilterField(pdaSettings.getCategoryField()));
			result.add(GUIFactory.getJLabel("category"), c.gridCell(1, index, 0, 0));
			result.add(jcCategoryField, c.gridCell(2, index++, 2, 0));
		} else {
			result.add(GUIFactory.getJLabel("sortby", new Font("serif", Font.BOLD, 14)), c.gridCell(2, index, 2, 0));
			if (isGroupBy) {
				result.add(GUIFactory.getJLabel("groupby", new Font("serif", Font.BOLD, 14)),
						c.gridmultipleCell(3, index, 0, 0, 2, 1));
			}
			index++;
		}

		for (int i = 0; i < numSort; i++) {
			String sortField = findFilterField(pdaSettings.getSortField(i));
			jcSortField[i] = new JComboBox<>(dbFilterFields);
			jcSortField[i].setSelectedItem(sortField);
			jcSortField[i].addActionListener(e -> activateComponents());

			cbGroupField[i] = new JCheckBox();
			cbGroupField[i].setVisible(isGroupBy);
			cbGroupField[i].addActionListener(e -> activateComponents());

			txGroupingField[i] = GUIFactory.getJTextField("groupby", "");
			txGroupingField[i].setVisible(hasGrouping);

			if (isGroupBy && !sortField.isEmpty()) {
				for (int j = 0; j < 4; j++) {
					if (pdaSettings.getGroupField(j).equals(sortField)) {
						cbGroupField[i].setSelected(true);
						txGroupingField[i].setText(pdaSettings.getGroupingField(j));
						break;
					}
				}
			}

			result.add(GUIFactory.getJLabel(guiText + i), c.gridCell(1, index, 0, 0));
			result.add(jcSortField[i], c.gridCell(2, index, 2, 0));
			result.add(cbGroupField[i], c.gridCell(3, index, 1, 0));
			result.add(txGroupingField[i], c.gridCell(4, index, 3, 0));
			result.add(Box.createHorizontalGlue(), c.gridCell(5, index++, 3, 0));
		}

		if (hasGrouping) {
			Box box = Box.createHorizontalBox();
			box.add(GUIFactory.getJLabel("remainderGroup", new Font("serif", Font.BOLD, 14)));
			box.add(Box.createHorizontalStrut(5));
			box.add(Box.createHorizontalGlue());
			box.add(txRemainField);
			result.add(box, c.gridmultipleCell(2, index, 0, 0, 2, 1));
		}

		result.setBorder(BorderFactory.createTitledBorder(myExportFile.getName() + " "
				+ GUIFactory.getTitle(myExportFile.isSpecialFieldSort() ? "fieldDefinition" : "sortOrder")));
		return result;
	}

	private Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(GUIFactory.getJButton("apply", funcSave));
		return panel;
	}

	private String findFilterField(String field) {
		for (String filter : dbFilterFields) {
			if (filter.equals(field)) {
				return filter;
			}
		}
		return "";
	}

	@Override
	protected void save() throws Exception {
		Set<String> map = new HashSet<>();
		int sortIndex = 0;
		int groupIndex = 0;

		pdaSettings.clearSortFields();

		for (int i = 0; i < numSort; i++) {
			if (jcSortField[i].getSelectedIndex() > 0) {
				String sortValue = jcSortField[i].getSelectedItem().toString();
				if (!map.contains(sortValue)) {
					map.add(sortValue);
					if (cbGroupField[i].isSelected()) {
						if (!txGroupingField[i].getText().isEmpty()) {
							pdaSettings.setGroupingField(groupIndex, txGroupingField[i].getText());
						}

						pdaSettings.setGroupField(groupIndex++, sortValue);
					}
					pdaSettings.setSortField(sortIndex++, sortValue);
				}
			}
		}

		pdaSettings.setCategoryField(jcCategoryField != null ? jcCategoryField.getSelectedItem().toString() : "");
		pdaSettings
				.setRemainingField(txRemainField != null && txRemainField.isEnabled() ? txRemainField.getText() : "");
	}

	@Override
	public void activateComponents() {
		if (!isGroupBy) {
			return;
		}

		boolean isShowRemainder = false;
		for (int i = 0; i < numSort; i++) {
			if (jcSortField[i].getSelectedIndex() < 1) {
				cbGroupField[i].setSelected(false);
				cbGroupField[i].setEnabled(false);
				if (hasGrouping) {
					txGroupingField[i].setText("");
					txGroupingField[i].setEnabled(false);
				}
			} else {
				cbGroupField[i].setEnabled(true);
				if (hasGrouping) {
					txGroupingField[i].setEnabled(cbGroupField[i].isSelected());
					if (txGroupingField[i].isEnabled()) {
						isShowRemainder = true;
					}
				}
			}
		}

		if (hasGrouping) {
			txRemainField.setEnabled(isShowRemainder);
		}
	}
}
