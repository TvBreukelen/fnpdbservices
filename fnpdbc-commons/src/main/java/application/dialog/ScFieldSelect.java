package application.dialog;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import application.interfaces.ExportFile;
import application.interfaces.IDatabaseFactory;
import application.model.UserFieldModel;
import application.table.BooleanRenderer;
import application.table.ETable;
import application.table.ToolTipHeader;
import application.utils.BasisField;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;

public class ScFieldSelect implements PropertyChangeListener {
	/**
	 * Export Fields Selection
	 *
	 * @author Tom van Breukelen
	 * @version 8
	 * @since 2004
	 */
	private JButton btAdd;
	private JButton btUp;
	private JButton btDown;
	private JButton btRemove;
	private JButton btClear;

	private JList<BasisField> lstAvailableFields;
	private JTable table;
	private JScrollPane scSelectedFields;

	private ActionListener funcAddFields;

	private DefaultListModel<BasisField> availableModel;
	private UserFieldModel userModel;
	private IDatabaseFactory factory;
	private JPanel fieldPanel = new JPanel();

	public ScFieldSelect(IDatabaseFactory factory) {
		this.factory = factory;
		init();
	}

	private void init() {
		fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.X_AXIS));

		availableModel = new DefaultListModel<>();
		lstAvailableFields = new JList<>(availableModel);
		lstAvailableFields.setToolTipText(GUIFactory.getToolTip("availableFields"));

		userModel = new UserFieldModel(factory.getExportFile());
		table = new ETable(userModel, false);
		table.setDefaultRenderer(Boolean.class, new BooleanRenderer());
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

		ToolTipHeader toolTipHeader = new ToolTipHeader(table.getColumnModel(),
				GUIFactory.getArray("exportToolTipHeaders"));
		table.setTableHeader(toolTipHeader);

		funcAddFields = e -> {
			List<BasisField> items = lstAvailableFields.getSelectedValuesList();
			int selectedRow = table.getSelectedRow();
			int startRow = selectedRow > -1 && selectedRow < table.getRowCount() - 1 ? selectedRow
					: userModel.getRowCount();
			selectedRow = startRow - 1;

			for (BasisField field : items) {
				userModel.addRecord(field, ++selectedRow);
			}

			scSelectedFields.getViewport().scrollRectToVisible(table.getCellRect(selectedRow, 0, true));
			table.setRowSelectionInterval(startRow, selectedRow);
			activateComponents();
		};

		ActionListener funcRemoveFields = e -> {
			int selectedRow = table.getSelectedRow();
			userModel.removeRecords(table.getSelectedRows());
			if (selectedRow > userModel.getRowCount() - 1) {
				selectedRow = userModel.getRowCount() - 1;
			}

			if (selectedRow > -1) {
				table.setRowSelectionInterval(selectedRow, selectedRow);
			}
			activateComponents();
		};

		ActionListener funcMoveFieldsUp = e -> {
			int selectedRow = table.getSelectedRow();
			if (userModel.moveRowUp(selectedRow)) {
				table.setRowSelectionInterval(--selectedRow, selectedRow);
			}
		};

		ActionListener funcMoveFieldsDown = e -> {
			int selectedRow = table.getSelectedRow();
			if (userModel.moveRowDown(selectedRow)) {
				table.setRowSelectionInterval(++selectedRow, selectedRow);
			}
		};

		ActionListener funcClearFields = e -> {
			userModel.clear();
			activateComponents();
		};

		btAdd = createImageButton("ArrowR.png", GUIFactory.getToolTip("funcAddFields"), funcAddFields);
		btRemove = createImageButton("ArrowL.png", GUIFactory.getToolTip("funcRemoveFields"), funcRemoveFields);
		btUp = createImageButton("ArrowU.png", GUIFactory.getToolTip("funcMoveFieldsUp"), funcMoveFieldsUp);
		btDown = createImageButton("ArrowD.png", GUIFactory.getToolTip("funcMoveFieldsDown"), funcMoveFieldsDown);
		btClear = createImageButton("Delete.png", GUIFactory.getToolTip("funcClearFields"), funcClearFields);
	}

	private void setTableColumns() {
		General.packColumns(table);
		TableColumn col = table.getColumnModel().getColumn(UserFieldModel.COL_TYPE);
		col.setMaxWidth(80);

		int setColumn = UserFieldModel.COL_TEXT_EXPORT;
		if (userModel.getColumnCount() > setColumn) {
			if (userModel.isColumnVisible(setColumn)) {
				col = table.getColumnModel().getColumn(setColumn);
				col.setMaxWidth(80);
				setColumn++;
			}

			for (int i = setColumn; i < userModel.getColumnCount(); i++) {
				col = table.getColumnModel().getColumn(i);
				col.setMaxWidth(40);
			}
		}
	}

	public void loadFieldPanel(List<BasisField> userFields, boolean isUserfieldUpdate) {
		availableModel.clear();

		boolean hasTextExport = false;
		for (FieldDefinition field : factory.getDbSelectFields()) {
			availableModel.addElement(field);
			if (!hasTextExport) {
				switch (field.getFieldType()) {
				case BOOLEAN, DATE, DURATION, TIME, TIMESTAMP:
					hasTextExport = true;
					break;
				default:
					break;
				}
			}
		}

		lstAvailableFields.setSelectedIndex(0);
		if (isUserfieldUpdate) {
			userModel.setTableData(userFields, hasTextExport);
			setTableColumns();
		}

		activateComponents();
		fieldPanel.updateUI();
	}

	public List<BasisField> getFieldList() {
		return userModel.getUserFields();
	}

	public Component createFieldPanel() {
		// Add Mouse listener event for double click
		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					funcAddFields.actionPerformed(null);
				}
			}
		};
		lstAvailableFields.addMouseListener(mouseListener);

		// Add scrollpane
		JScrollPane scAvailableFields = new JScrollPane(lstAvailableFields);
		scAvailableFields.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("availableFields")));

		// Create Panel for selected FieldTypes
		table.setToolTipText(GUIFactory.getToolTip("selectedFields"));

		// add mouselistener
		mouseListener = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (!btUp.isEnabled()) {
					activateComponents();
				}
			}
		};

		table.addMouseListener(mouseListener);
		scSelectedFields = new JScrollPane(table);
		scSelectedFields.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("selectedFields")));

		fieldPanel.add(scAvailableFields);
		fieldPanel.add(createButtonPanel());
		fieldPanel.add(scSelectedFields);
		fieldPanel.setBorder(BorderFactory.createEtchedBorder());

		lstAvailableFields.setSelectedIndex(0);
		return fieldPanel;
	}

	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(Box.createVerticalStrut(20));

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
		btAdd.setEnabled(!availableModel.isEmpty());
		btUp.setEnabled(table.getSelectedRow() != -1);
		btDown.setEnabled(btUp.isEnabled());
		btRemove.setEnabled(btUp.isEnabled());
		btClear.setEnabled(userModel.getRowCount() > 0);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// Called from ScConfigDb after an export file change
		ExportFile newValue = (ExportFile) evt.getNewValue();
		ExportFile oldValue = (ExportFile) evt.getOldValue();

		if (newValue == oldValue) {
			return;
		}

		userModel.setInputFile(newValue);
		setTableColumns();
	}
}
