package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;

import application.interfaces.FieldTypes;
import application.interfaces.FilterOperator;
import application.interfaces.IDatabaseFactory;
import application.interfaces.TvBSoftware;
import application.model.FilterData;
import application.table.CellRenderer;
import application.utils.BasisField;
import application.utils.FieldDefinition;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.ScrollComboBoxImpl;
import application.utils.gui.XGridBagConstraints;

public class ConfigFilter extends BasicDialog {
	/**
	 * Title: ScFilter Description: FNProgramvare Software Filter Configuration
	 * parms Cpyright (c) 2011-2020
	 *
	 * @author Tom van Breukelen
	 * @version 8.0
	 */
	private static final long serialVersionUID = 5688764743046506542L;

	@SuppressWarnings("unchecked")
	private JComboBox<String>[] cbFilter = new JComboBox[2];
	@SuppressWarnings("unchecked")
	private JComboBox<String>[] cbFilterField = new JComboBox[2];
	@SuppressWarnings("unchecked")
	private JList<Object>[] list = new JList[2];
	private JDialog[] dialog = new JDialog[2];

	private JFormattedTextField[] formattedText = new JFormattedTextField[2];
	private DatePicker datePicker;
	private DateTimePicker dateTimePicker;

	private Box[] filterPanel = new Box[2];
	private String[] filterValue = new String[2];

	private ScrollComboBoxImpl cbContentsBook = new ScrollComboBoxImpl();
	private ScrollComboBoxImpl cbKeywordFilter = new ScrollComboBoxImpl();

	private ActionListener funcFieldSelect;
	private ActionListener funcRemoveFilter;
	private ActionListener funcBoolFilter;
	private ActionListener funcListFilter;
	private ActionListener funcTextFilter;
	private ActionListener funcSelectPressed;
	private ActionListener funcSubSelectPressed;

	private JRadioButton btFilterAnd = GUIFactory.getJRadioButton("and");
	private JButton btErase[] = new JButton[2];

	private Box contentsPanel;
	private Box keywordPanel;
	private Map<String, FieldDefinition> dbFieldDefinition;

	private boolean isKeywordFilter = false;
	private boolean isContentsFilter = false;

	private FilterData pdaSettings;
	private IDatabaseFactory dbFactory;

	private final String[] option1 = GUIFactory.getArray("filter");
	private final String[] optionEnglish = FilterOperator.getFilterOperators();

	public ConfigFilter(IDatabaseFactory database, FilterData data) {
		super();
		pdaSettings = data;
		dbFactory = database;
		init();
	}

	@Override
	protected void init() {
		init(pdaSettings.getProfileID() + " " + GUIFactory.getText("menuFilter"));
		setHelpFile("exportfilter");

		funcFieldSelect = e -> {
			int index = Integer.parseInt(e.getActionCommand());
			cbFilterField[index].removeActionListener(funcFieldSelect);
			String dbField = cbFilterField[index].getSelectedItem().toString();
			filterValue[index] = "";
			setFilterPanel(filterPanel[index], dbField, index);
			cbFilterField[index].addActionListener(funcFieldSelect);
			activateComponents();
		};

		funcRemoveFilter = e -> {
			if (!e.getActionCommand().isEmpty()) {
				int i = Integer.valueOf(e.getActionCommand());
				filterValue[i] = "";
				cbFilterField[i].setSelectedItem("");
				return;
			}
			filterValue[0] = "";
			filterValue[1] = "";
			cbFilterField[0].setSelectedItem("");
			cbFilterField[1].setSelectedItem("");
			cbContentsBook.setSelectedItem("");
			cbKeywordFilter.setSelectedItem("");
		};

		funcBoolFilter = e -> {
			switch (Integer.parseInt(e.getActionCommand())) {
			case 0:
				filterValue[0] = "true";
				break;
			case 1:
				filterValue[0] = "false";
				break;
			case 2:
				filterValue[1] = "true";
				break;
			case 3:
				filterValue[1] = "false";
				break;
			}
		};

		funcListFilter = e -> {
			String value = ((JComboBox<?>) e.getSource()).getSelectedItem().toString();
			filterValue[Integer.parseInt(e.getActionCommand())] = value;
		};

		funcTextFilter = e -> {
			Object value = ((JFormattedTextField) e.getSource()).getValue();
			filterValue[Integer.parseInt(e.getActionCommand())] = value.toString();
		};

		funcSubSelectPressed = e -> {
			int index = Integer.parseInt(e.getActionCommand());
			if (index < 3) {
				Object value = list[index].getSelectedValue();
				if (value == null) {
					return;
				}

				String dbField = cbFilterField[index].getSelectedItem().toString();
				BasisField dbDef = dbFieldDefinition.get(dbField);
				filterValue[index] = value.toString();

				switch (dbDef.getFieldType()) {
				case FLOAT:
				case NUMBER:
					formattedText[index].setValue(new Double(value.toString()));
					break;
				case DATE:
					datePicker.setDate(General.convertDB2Date(filterValue[index]));
					break;
				case TIMESTAMP:
					dateTimePicker.setDateTimeStrict(General.convertDB2Timestamp(filterValue[index]));
				default:
					break;
				}
			} else {
				index -= 3;
			}

			dialog[index].setVisible(false);
			dialog[index].dispose();
		};

		funcSelectPressed = e -> {
			final int index = Integer.parseInt(e.getActionCommand());
			String dbField = cbFilterField[index].getSelectedItem().toString();
			BasisField dbDef = dbFieldDefinition.get(dbField);

			ConfigFilter.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				refreshList(dbFactory.getDbFieldValues(dbField), dbDef, index);
			} catch (Exception ex) {
				General.errorMessage(ConfigFilter.this, ex, GUIFactory.getTitle("databaseProblem"),
						GUIFactory.getMessage("databaseReadProblem", dbFactory.getDatabaseFilename()));
				ConfigFilter.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			if (filterValue[index].isEmpty()) {
				list[index].setSelectedIndex(0);
			} else {
				list[index].setSelectedValue(filterValue[index], true);
			}

			ConfigFilter.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			JButton button1 = GUIFactory.getJButton("ok", funcSubSelectPressed);
			button1.setActionCommand(Integer.toString(index));

			JButton button2 = GUIFactory.getJButton("cancel", funcSubSelectPressed);
			button2.setActionCommand(Integer.toString(index + 3));

			JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			panel.add(button1);
			panel.add(button2);

			dialog[index] = new JDialog();
			dialog[index].add(new JScrollPane(list[index]), BorderLayout.CENTER);
			dialog[index].add(Box.createVerticalStrut(10), BorderLayout.NORTH);
			dialog[index].add(Box.createHorizontalStrut(10), BorderLayout.EAST);
			dialog[index].add(panel, BorderLayout.SOUTH);
			dialog[index].add(Box.createHorizontalStrut(10), BorderLayout.WEST);
			dialog[index].setTitle(dbField);
			dialog[index].setModal(true);
			dialog[index].setPreferredSize(new Dimension(200, 320));
			dialog[index].pack();
			dialog[index].setLocationRelativeTo(contentsPanel);
			dialog[index].setVisible(true);
		};

		cbKeywordFilter.setToolTipText(GUIFactory.getToolTip("keywordFilter"));
		cbKeywordFilter.setPreferredSize(new Dimension(190, 20));

		cbContentsBook.setToolTipText(GUIFactory.getToolTip("contentsBookFilter"));
		cbContentsBook.setPreferredSize(new Dimension(190, 20));

		// Create Filter Panels
		dbFieldDefinition = dbFactory.getDbFieldDefinition();
		if (pdaSettings.getTvBSoftware() == TvBSoftware.FNPROG2PDA) {
			isKeywordFilter = dbFieldDefinition.containsKey("Keyword");
			isContentsFilter = dbFieldDefinition.containsKey("ContentsType");
		}

		Vector<String> filterList = new Vector<>(dbFactory.getDbFilterFields());
		for (int i = 0; i < 2; i++) {
			filterPanel[i] = Box.createHorizontalBox();
			filterPanel[i].setToolTipText(GUIFactory.getToolTip("databaseFieldValue"));

			cbFilterField[i] = new JComboBox<>(new DefaultComboBoxModel<>(filterList));
			cbFilterField[i].setToolTipText(GUIFactory.getToolTip("databaseFields"));
			cbFilterField[i].setActionCommand(String.valueOf(i));

			cbFilter[i] = new JComboBox<>(option1);
			cbFilter[i].setToolTipText(GUIFactory.getToolTip("criteria"));
			filterValue[i] = pdaSettings.getFilterValue(i);
		}

		DatePickerSettings date1 = new DatePickerSettings(getLocale());
		date1.setFormatForDatesCommonEra(General.getDateFormat());
		DatePickerSettings date2 = new DatePickerSettings(getLocale());
		date2.setFormatForDatesCommonEra(General.getDateFormat());
		TimePickerSettings time = new TimePickerSettings(getLocale());
		time.setFormatForDisplayTime(General.sdInternalTime);
		time.setFormatForMenuTimes(General.sdInternalTime);

		dateTimePicker = new DateTimePicker(date1, time);
		dateTimePicker.addDateTimeChangeListener(e -> {
			filterValue[(Integer) dateTimePicker.getClientProperty("index")] = General
					.convertTimestamp2DB(dateTimePicker.getDateTimePermissive());
		});

		datePicker = new DatePicker(date2);
		datePicker.addDateChangeListener(e -> {
			filterValue[(Integer) datePicker.getClientProperty("index")] = General.convertDate2DB(datePicker.getDate());
		});

		buildDialog();
		activateComponents();
		pack();
	}

	private void refreshList(List<Object> values, BasisField field, int index) {
		final int idx = index;
		list[index] = new JList<>(values.toArray());
		list[index].setCellRenderer(new CellRenderer(field.getFieldType()));

		list[index].addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					funcSubSelectPressed.actionPerformed(
							new ActionEvent(list[idx], ActionEvent.ACTION_PERFORMED, Integer.toString(idx)));
				}
			}
		});
	}

	@Override
	protected Component addToToolbar() {
		return General.createToolBarButton(GUIFactory.getToolTip("funcRemoveFilter"), "Delete.png", funcRemoveFilter);
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		// Create RadioButtonPanel
		Box radioPanel = Box.createHorizontalBox();
		JRadioButton btFilterOr = GUIFactory.getJRadioButton("or");
		ButtonGroup bGroup = new ButtonGroup();
		bGroup.add(btFilterAnd);
		bGroup.add(btFilterOr);

		String s = pdaSettings.getFilterCondition();
		btFilterAnd.setSelected(s.equals("AND"));
		btFilterOr.setSelected(s.equals("OR"));

		radioPanel.add(btFilterAnd);
		radioPanel.add(btFilterOr);

		// Create Keyword panel
		keywordPanel = Box.createHorizontalBox();
		keywordPanel.add(cbKeywordFilter);
		keywordPanel.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("keywordFilter")));
		keywordPanel.setVisible(pdaSettings.getTvBSoftware() == TvBSoftware.FNPROG2PDA);

		// Create ContentsType panel
		contentsPanel = Box.createHorizontalBox();
		contentsPanel.add(cbContentsBook);
		contentsPanel.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("contentsBookFilter")));
		contentsPanel.setVisible(keywordPanel.isVisible());

		for (int i = 0; i < btErase.length; i++) {
			btErase[i] = General.createToolBarButton(null, "Clear.png", funcRemoveFilter);
			btErase[i].setBorder(BorderFactory.createEmptyBorder());
			btErase[i].setContentAreaFilled(false);
			btErase[i].setActionCommand(Integer.toString(i));
		}

		result.add(Box.createVerticalStrut(5), c.gridCell(0, 0, 0, 0));
		result.add(cbFilterField[0], c.gridCell(0, 1, 0, 0));
		result.add(cbFilter[0], c.gridCell(1, 1, 0, 0));
		result.add(btErase[0], c.gridCell(2, 1, 0, 0));
		result.add(filterPanel[0], c.gridmultipleCell(0, 2, 0, 0, 2, 1));
		result.add(Box.createVerticalStrut(10), c.gridCell(0, 3, 0, 0));

		result.add(new JSeparator(), c.gridmultipleCell(0, 5, 0, 0, 2, 1));
		result.add(radioPanel, c.gridCell(0, 6, 0, 0));
		result.add(new JSeparator(), c.gridmultipleCell(0, 7, 0, 0, 2, 1));

		result.add(Box.createVerticalStrut(10), c.gridCell(0, 8, 0, 0));
		result.add(cbFilterField[1], c.gridCell(0, 9, 0, 0));
		result.add(cbFilter[1], c.gridCell(1, 9, 0, 0));
		result.add(btErase[1], c.gridCell(2, 9, 0, 0));
		result.add(filterPanel[1], c.gridmultipleCell(0, 10, 0, 0, 2, 1));
		result.add(Box.createVerticalStrut(10), c.gridCell(0, 11, 0, 0));
		result.add(keywordPanel, c.gridCell(0, 12, 0, 0));
		result.add(contentsPanel, c.gridCell(1, 12, 0, 0));

		result.setBorder(BorderFactory.createRaisedBevelBorder());
		refreshFilter();
		return result;
	}

	private void refreshFilter() {
		boolean isValid = true;

		// Refresh Export Filter Selection
		for (int i = 0; i < 2; i++) {
			setFilterPanel(filterPanel[i], pdaSettings.getFilterField(i), i);
			cbFilter[i].setSelectedIndex(General.getID(pdaSettings.getFilterOperator(i).getValue(), optionEnglish));
			cbFilterField[i].addActionListener(funcFieldSelect);
		}

		// Refresh Contents Type and Keyword filter Selection
		List<Object> a = null;
		if (isKeywordFilter) {
			try {
				a = dbFactory.getDbFieldValues("Keyword");
				cbKeywordFilter.addItem("");
				for (Object obj : a) {
					cbKeywordFilter.addItem(obj.toString());
				}
			} catch (Exception e) {
				isValid = false;
				cbKeywordFilter.addItem(pdaSettings.getKeywordFilter());
			}
			cbKeywordFilter.setSelectedItem(pdaSettings.getKeywordFilter());
		}

		if (isContentsFilter) {
			if (isValid) {
				try {
					a = dbFactory.getDbFieldValues("ContentsType");
				} catch (Exception e) {
					// Should never occur
				}

				cbContentsBook.addItem("");
				for (Object obj : a) {
					cbContentsBook.addItem(obj.toString());
				}
			} else {
				cbContentsBook.addItem(pdaSettings.getContentsFilter());
			}
			cbContentsBook.setSelectedItem(pdaSettings.getContentsFilter());
		}
	}

	private void setFilterPanel(JComponent comp, String dbField, int index) {
		comp.removeAll();
		cbFilterField[index].setSelectedItem(dbField);
		BasisField dbDef = dbFieldDefinition.get(dbField);

		JButton button = GUIFactory.getJButton("select", funcSelectPressed);
		button.setActionCommand(Integer.toString(index));

		boolean isFloat = false;

		if (dbDef == null) {
			JComboBox<?> jc = new JComboBox<>();
			cbFilter[index].setEnabled(false);
			btErase[index].setEnabled(false);
			jc.setEnabled(false);
			comp.add(jc);
			pack();
			return;
		}

		btErase[index].setEnabled(true);

		if (dbDef.getFieldType() == FieldTypes.BOOLEAN) {
			if (cbFilter[index].getItemCount() == option1.length) {
				cbFilter[index].removeAllItems();
				cbFilter[index].addItem(option1[0]);
				cbFilter[index].setSelectedIndex(0);
			}
			cbFilter[index].setEnabled(false);
		} else {
			if (cbFilter[index].getItemCount() != option1.length) {
				cbFilter[index].removeAllItems();
				for (String element : option1) {
					cbFilter[index].addItem(element);
				}
				cbFilter[index].setSelectedIndex(0);
			}
			cbFilter[index].setEnabled(true);
		}

		switch (dbDef.getFieldType()) {
		case BOOLEAN:
			Box box = Box.createHorizontalBox();
			JRadioButton button1 = GUIFactory.getJRadioButton("yes");
			JRadioButton button2 = GUIFactory.getJRadioButton("no");
			button1.setActionCommand(index == 0 ? "0" : "2");
			button2.setActionCommand(index == 0 ? "1" : "3");

			ButtonGroup group = new ButtonGroup();
			group.add(button1);
			group.add(button2);

			button1.addActionListener(funcBoolFilter);
			button2.addActionListener(funcBoolFilter);
			button2.setSelected(filterValue[index].equals("false"));
			button1.setSelected(!button2.isSelected());
			filterValue[index] = button1.isSelected() ? "true" : "false";

			box.add(button1);
			box.add(button2);
			comp.add(box);
			break;
		case FLOAT:
			isFloat = true;
		case NUMBER:
			formattedText[index] = isFloat ? GUIFactory.getDoubleTextField("", 0, "0")
					: GUIFactory.getIntegerTextField("", 0, Integer.MAX_VALUE, "0");
			formattedText[index].addActionListener(funcTextFilter);
			formattedText[index].setActionCommand(Integer.toString(index));

			try {
				formattedText[index].setValue(new Double(filterValue[index]));
			} catch (Exception e) {
				formattedText[index].setValue(0);
			}

			comp.add(formattedText[index]);
			comp.add(Box.createHorizontalStrut(10));
			comp.add(button);
			break;
		case TIMESTAMP:
			dateTimePicker.putClientProperty("index", index);
			dateTimePicker.setDateTimePermissive(General.convertDB2Timestamp(filterValue[index]));
			comp.add(dateTimePicker);
			comp.add(Box.createHorizontalStrut(10));
			comp.add(button);
			break;
		case DATE:
			datePicker.putClientProperty("index", index);
			datePicker.setDate(General.convertDB2Date(filterValue[index]));
			comp.add(datePicker);
			comp.add(Box.createHorizontalStrut(10));
			comp.add(button);
			break;
		default:
			ScrollComboBoxImpl jc = new ScrollComboBoxImpl();
			jc.setActionCommand(Integer.toString(index));
			jc.setRenderer(new CellRenderer(dbDef.getFieldType()));

			try {
				List<Object> v = dbFactory.getDbFieldValues(dbField);
				for (Object obj : v) {
					jc.addItem(obj.toString());
				}

				jc.setSelectedItem(filterValue[index]);
				jc.addActionListener(funcListFilter);
				if (jc.getItemCount() > 0 && !jc.getSelectedItem().equals(filterValue[index])) {
					jc.setSelectedIndex(0);
				}

			} catch (Exception ex) {
				General.errorMessage(this, ex, GUIFactory.getTitle("databaseProblem"),
						GUIFactory.getMessage("databaseReadError", dbFactory.getDatabaseFilename()));
			}

			Dimension size = jc.getPreferredSize();
			if (size.width > 500) {
				jc.setPreferredSize(new Dimension(500, size.height));
			}

			comp.add(jc);
		}
		pack();
	}

	@Override
	protected void save() throws Exception {
		int counter = -1;
		pdaSettings.clearFilterFields();

		for (int i = 0; i < 2; i++) {
			String value = cbFilterField[i].getSelectedItem().toString();
			if (!value.isEmpty()) {
				counter++;
				pdaSettings.setFilterField(counter, value);
				pdaSettings.setFilterOperator(counter,
						FilterOperator.getFilterOperator(optionEnglish[cbFilter[i].getSelectedIndex()]));
				pdaSettings.setFilterValue(counter, filterValue[i]);
			}
		}

		if (counter > 0) {
			pdaSettings.setFilterCondition(btFilterAnd.isSelected() ? "AND" : "OR");
		} else {
			pdaSettings.setFilterCondition("AND");
		}

		if (cbContentsBook.isEnabled()) {
			pdaSettings.setContentsFilter(
					cbContentsBook.getSelectedIndex() < 1 ? "" : cbContentsBook.getSelectedItem().toString());
		}

		if (cbKeywordFilter.isEnabled()) {
			pdaSettings.setKeywordFilter(
					cbKeywordFilter.getSelectedIndex() < 1 ? "" : cbKeywordFilter.getSelectedItem().toString());
		}
	}

	@Override
	public void activateComponents() {
		cbKeywordFilter.setEnabled(isKeywordFilter);
		cbContentsBook.setEnabled(isContentsFilter);
	}
}
