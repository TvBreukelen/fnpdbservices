package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import application.model.ViewerModel;
import application.table.BooleanRenderer;
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
	private ViewerModel myModel;

	public Viewer(List<FieldDefinition> dbFieldList) {
		super();
		myModel = new ViewerModel(dbFieldList);
		myTable = new ETable(myModel, true);
		myTable.setDefaultRenderer(Object.class, new ObjectRenderer());
		myTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
	}

	public void buildViewer() {
		init(GUIFactory.getTitle("xViewer"));
		setHelpFile("xviewer");
		buildDialog();
		btSave.setVisible(false);

		pack();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new BorderLayout());
		myTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		General.packColumns(myTable);
		result.add(new JScrollPane(myTable), BorderLayout.CENTER);
		result.setBorder(
				BorderFactory.createTitledBorder(GUIFactory.getTitle("noOfRecords") + " " + myModel.getRowCount()));
		return result;
	}

	public ViewerModel getTableModel() {
		return myModel;
	}

	@Override
	protected void save() throws Exception {
		// Not used here
	}
}