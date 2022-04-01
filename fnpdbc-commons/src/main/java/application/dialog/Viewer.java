package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import application.model.ViewerModel;
import application.table.BooleanRenderer;
import application.table.CellEditor;
import application.table.ETable;
import application.table.ObjectRenderer;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;

public class Viewer extends BasicDialog {
	/**
	 * Title: ConfigXViewer Description: DBConverter Database PDA Viewer Copyright:
	 * (c) 2004-2014
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = -965247421599148461L;
	private JTable myTable;
	private JButton btReload;
	private ViewerModel myModel;
	private List<FieldDefinition> dbFields;
	private ActionListener funcRestoreValues;
	private boolean isModelSaved;

	public Viewer(List<FieldDefinition> dbFieldList) {
		super();
		myModel = new ViewerModel(dbFieldList);
		myTable = new ETable(myModel);
		dbFields = dbFieldList;
	}

	public void buildViewer() {
		funcRestoreValues = e -> {
			if (isModelSaved) {
				restoreOldValues();
			}
		};

		myTable.setDefaultRenderer(Object.class, new ObjectRenderer(dbFields));
		myTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
		myTable.setDefaultEditor(Object.class, new CellEditor(dbFields));

		init(GUIFactory.getTitle("xViewer"));
		setHelpFile("xviewer");
		buildDialog();
		pack();
	}

	@Override
	protected Component addToToolbar() {
		btReload = GUIFactory.createToolBarButton(GUIFactory.getToolTip("funcRestoreValues"), "Reload.png",
				funcRestoreValues);

		btSave.setVisible(false);
		return btReload;
	}

	@Override
	protected Component createCenterPanel() {
		isModelSaved = General.writeObjectToDisk(myModel.getDataListMap());
		JPanel result = new JPanel(new BorderLayout());
		btReload.setEnabled(isModelSaved);

		myTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		General.packColumns(myTable);
		result.add(new JScrollPane(myTable), BorderLayout.CENTER);
		result.setBorder(
				BorderFactory.createTitledBorder(GUIFactory.getTitle("noOfRecords") + " " + myModel.getRowCount()));
		return result;
	}

	@SuppressWarnings("unchecked")
	private void restoreOldValues() {
		myModel.setDataListMap((List<Map<String, Object>>) General.readObjectFromDisk());
		myModel.fireTableDataChanged();
		General.packColumns(myTable);
	}

	public ViewerModel getTableModel() {
		return myModel;
	}

	@Override
	protected void save() throws Exception {
		// Not used here
	}
}