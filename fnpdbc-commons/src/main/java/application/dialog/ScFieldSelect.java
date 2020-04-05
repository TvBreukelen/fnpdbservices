package application.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import application.interfaces.IDatabaseFactory;
import application.model.SelectionFieldModel;
import application.model.UserFieldModel;
import application.table.ETable;
import application.utils.BasisField;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class ScFieldSelect {
	/**
	 * Title: ScFieldSelect Description: Export Fields Selection Copyright: (c)
	 * 2004-2012
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 */
	private JButton btUp;
	private JButton btDown;
	private JButton btRemove;
	private JButton btClear;

	private JTable tbSelectedFields;
	private JTable tbAvailableFields;
	private JScrollPane scSelectedFields;

	private ActionListener funcAddFields;

	private UserFieldModel userModel;
	private SelectionFieldModel fieldModel;
	private IDatabaseFactory factory;

	public ScFieldSelect(IDatabaseFactory factory) {
		this.factory = factory;
		init();
	}

	private void init() {
		userModel = new UserFieldModel();
		fieldModel = new SelectionFieldModel();
		tbSelectedFields = new ETable(userModel);
		tbAvailableFields = new JTable(fieldModel);

		funcAddFields = e -> {
			List<BasisField> items = fieldModel.getSelectedItems(tbAvailableFields.getSelectedRows());
			int selectedRow = tbSelectedFields.getSelectedRow();
			int startRow = selectedRow > -1 && selectedRow < tbSelectedFields.getRowCount() - 1 ? selectedRow
					: userModel.getRowCount();
			selectedRow = startRow - 1;

			for (BasisField field : items) {
				userModel.addRecord(field, ++selectedRow);
			}

			scSelectedFields.getViewport().scrollRectToVisible(tbSelectedFields.getCellRect(selectedRow, 0, true));
			tbSelectedFields.setRowSelectionInterval(startRow, selectedRow);
			activateComponents();
		};

		ActionListener funcRemoveFields = e -> {
			int selectedRow = tbSelectedFields.getSelectedRow();
			userModel.removeRecords(tbSelectedFields.getSelectedRows());
			if (selectedRow > userModel.getRowCount() - 1) {
				selectedRow = userModel.getRowCount() - 1;
			}

			if (selectedRow > -1) {
				tbSelectedFields.setRowSelectionInterval(selectedRow, selectedRow);
			}
			activateComponents();
		};

		ActionListener funcMoveFieldsUp = e -> {
			int selectedRow = tbSelectedFields.getSelectedRow();
			if (userModel.moveRowUp(selectedRow)) {
				tbSelectedFields.setRowSelectionInterval(--selectedRow, selectedRow);
			}
		};

		ActionListener funcMoveFieldsDown = e -> {
			int selectedRow = tbSelectedFields.getSelectedRow();
			if (userModel.moveRowDown(selectedRow)) {
				tbSelectedFields.setRowSelectionInterval(++selectedRow, selectedRow);
			}
		};

		ActionListener funcClearFields = e -> {
			userModel.clear();
			activateComponents();
		};

		btRemove = createImageButton("ArrowL.png", GUIFactory.getToolTip("funcRemoveFields"), funcRemoveFields);
		btUp = createImageButton("ArrowU.png", GUIFactory.getToolTip("funcMoveFieldsUp"), funcMoveFieldsUp);
		btDown = createImageButton("ArrowD.png", GUIFactory.getToolTip("funcMoveFieldsDown"), funcMoveFieldsDown);
		btClear = createImageButton("Delete.png", GUIFactory.getToolTip("funcClearFields"), funcClearFields);
	}

	public void loadFieldPanel(List<BasisField> userFields) {
		fieldModel.setTableData(factory.getDbSelectFields());
		tbAvailableFields.getSelectionModel().setSelectionInterval(0, 0);
		userModel.setTableData(userFields);
		activateComponents();
	}

	public List<BasisField> getFieldList() {
		return userModel.getUserFields();
	}

	public Component createFieldPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		// Add Mouse listener event for double click
		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					funcAddFields.actionPerformed(null);
				}
			}
		};
		tbAvailableFields.addMouseListener(mouseListener);

		// Add scrollpane
		JScrollPane scroll = new JScrollPane(tbAvailableFields);
		scroll.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("availableFields")));

		// Create Panel for selected FieldTypes
		tbSelectedFields.setToolTipText(GUIFactory.getToolTip("selectedFields"));

		// add mouselistener
		mouseListener = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (!btUp.isEnabled()) {
					activateComponents();
				}
			}
		};
		tbSelectedFields.addMouseListener(mouseListener);
		scSelectedFields = new JScrollPane(tbSelectedFields);
		scSelectedFields.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("selectedFields")));

		Dimension dim = scSelectedFields.getPreferredSize();
		dim.setSize(scroll.getPreferredSize().getHeight(), dim.getWidth() * .7);
		scSelectedFields.setPreferredSize(dim);

		panel.add(scroll, c.gridCell(1, 0, 2, 1));
		panel.add(createButtonPanel(), c.gridCell(2, 0, 0, 0));
		panel.add(scSelectedFields, c.gridCell(3, 0, 2, 1));
		panel.setBorder(BorderFactory.createEtchedBorder());

		tbAvailableFields.getSelectionModel().setSelectionInterval(0, 0);
		return panel;
	}

	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(Box.createVerticalStrut(20));

		// Add "Add" Button
		JButton btAdd = createImageButton("ArrowR.png", GUIFactory.getToolTip("funcAddFields"), funcAddFields);
		panel.add(btAdd);

		panel.add(Box.createVerticalStrut(2));
		panel.add(btRemove);
		panel.add(Box.createVerticalStrut(40));
		panel.add(btUp);
		panel.add(Box.createVerticalStrut(2));
		panel.add(btDown);
		panel.add(Box.createVerticalStrut(40));
		panel.add(btClear);
		panel.add(Box.createVerticalGlue());
		return panel;
	}

	private JButton createImageButton(String image, String toolTip, ActionListener action) {
		JButton button = new JButton();
		button.setIcon(General.createImageIcon(image));
		button.setToolTipText(toolTip);
		if (action != null) {
			button.addActionListener(action);
		}
		return button;
	}

	public void activateComponents() {
		btUp.setEnabled(tbSelectedFields.getSelectedRow() != -1);
		btDown.setEnabled(btUp.isEnabled());
		btRemove.setEnabled(btUp.isEnabled());
		btClear.setEnabled(userModel.getRowCount() > 0);
	}
}
