package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseFactory;
import application.model.SortData;
import application.utils.GUIFactory;
import application.utils.gui.XGridBagConstraints;

public class ConfigSort extends BasicDialog {
	/**
	 * Title: ScFieldSort Description: FNProgramvare Software Sort Configuration
	 * parms Copyright: (c) 2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = -1957596896864461526L;

	private JComboBox<String> cbCategoryField;
	private JComboBox<String>[] cbSortField;
	private JLabel[] lbSortField;
	private JCheckBox ckSort;
	private final int numSort;

	private boolean isDbConvert;

	private String[] dbFilterFields;
	private ExportFile myExportFile;

	private SortData pdaSettings;

	@SuppressWarnings("unchecked")
	public ConfigSort(IDatabaseFactory dbFactory, SortData data) {
		isDbConvert = dbFactory.isDbConvert();
		myExportFile = dbFactory.getExportFile();
		pdaSettings = data;
		dbFilterFields = dbFactory.getDbFilterFields();
		numSort = myExportFile.getMaxSortFields();
		lbSortField = new JLabel[numSort];
		cbSortField = new JComboBox[numSort];
		init();
	}

	@Override
	protected void init() {
		init(myExportFile.getName() + " " + GUIFactory.getText("menuSortOrder"));

		switch (myExportFile) {
		case LIST:
			setHelpFile("field_definition_list");
			break;
		case REFERENCER:
			setHelpFile("field_definition_referencer");
			break;
		case XML:
			setHelpFile("field_definition_xml");
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
	}
	
	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		int index = 0;

		XGridBagConstraints c = new XGridBagConstraints();

		JLabel lbCategory = GUIFactory.getJLabel("category");
		String guiText = "sortField";

		switch (myExportFile) {
		case LIST:
			ckSort = GUIFactory.getJCheckBox("reSortList", pdaSettings.isForceSort());
			guiText = "sortFieldList";
			break;
		case REFERENCER:
			ckSort = GUIFactory.getJCheckBox("reSortReferencer", pdaSettings.isForceSort());
			guiText = "sortFieldReferencer";
			break;
		case XML:
			guiText = "sortFieldXml";
			ckSort = GUIFactory.getJCheckBox("reSortXml", pdaSettings.isForceSort());
			break;
		default:
			ckSort = GUIFactory.getJCheckBox("reSortReferencer", pdaSettings.isForceSort());
		}

		result.add(ckSort, c.gridCell(0, index++, 0, 0));
		ckSort.setVisible(isDbConvert && myExportFile.isSpecialFieldSort());

		cbCategoryField = new JComboBox<>(dbFilterFields);
		cbCategoryField.setSelectedItem(findFilterField(pdaSettings.getCategoryField()));

		result.add(lbCategory, c.gridCell(0, index, 0, 0));
		result.add(cbCategoryField, c.gridCell(1, index, 2, 0));

		for (int i = 0; i < numSort; i++) {
			lbSortField[i] = GUIFactory.getJLabel(guiText + i);
			cbSortField[i] = new JComboBox<>(dbFilterFields);
			cbSortField[i].setSelectedItem(findFilterField(pdaSettings.getSortField(i)));
			cbSortField[i].addActionListener(e -> activateComponents());

			index++;
			result.add(lbSortField[i], c.gridCell(0, index, 0, 0));
			result.add(cbSortField[i], c.gridCell(1, index, 2, 0));
			result.add(Box.createHorizontalGlue(), c.gridCell(2, index, 3, 0));
		}

		lbCategory.setVisible(myExportFile.hasCategories());
		cbCategoryField.setVisible(myExportFile.hasCategories());
		result.setBorder(BorderFactory.createTitledBorder(myExportFile.getName() + " "
				+ GUIFactory.getTitle(myExportFile.isSpecialFieldSort() ? "fieldDefinition" : "sortOrder")));
		return result;
	}

	protected Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(GUIFactory.getJButton("apply", funcSave));
		return panel;
	}

	private String findFilterField(String field) {
		for(String filter : dbFilterFields) {
			if(filter.equals(field)) {
				return filter;
			}
		}
		return "";
	}
	
	
	@Override
	protected void save() throws Exception {
		boolean isSortSelected = false;
		HashSet<String> map = new HashSet<>();
		int index = 0;

		pdaSettings.clearSortFields();

		for (int i = 0; i < numSort; i++) {
			if (cbSortField[i].getSelectedIndex() > 0) {
				String sortValue = cbSortField[i].getSelectedItem().toString();
				if (!map.contains(sortValue)) {
					map.add(sortValue);
					pdaSettings.setSortField(myExportFile == ExportFile.REFERENCER ? i : index++, sortValue);
					isSortSelected = true;
				}
			}
		}

		if (myExportFile.isSpecialFieldSort()) {
			pdaSettings.setForceSort(ckSort.isVisible() ? isSortSelected && ckSort.isSelected() : isSortSelected);
		} else {
			pdaSettings.setForceSort(isSortSelected);
		}

		pdaSettings.setCategoryField(cbCategoryField.isVisible() ? cbCategoryField.getSelectedItem().toString() : "");
	}

	@Override
	public void activateComponents() {
		if (!ckSort.isVisible()) {
			return;
		}

		for (int i = 0; i < numSort; i++) {
			if (cbSortField[i].getSelectedIndex() > 0) {
				ckSort.setEnabled(true);
				return;
			}
		}

		ckSort.setEnabled(false);
	}
}
