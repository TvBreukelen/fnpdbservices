package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.NumberFormatter;

import org.apache.commons.lang3.StringUtils;

import application.interfaces.ExportFile;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbengine.export.MariaDB;
import dbengine.utils.DatabaseHelper;

public class HostConfig extends BasicDialog {
	private static final long serialVersionUID = -4570103476841905520L;

	private JTextField txHost;
	private JSpinner txPort;
	private JTextField txUser;
	private JPasswordField txPassword;
	private JTextField txDatabase;
	private JButton btTest;
	private JButton btApply;

	private DatabaseHelper dbInHelper;
	private DatabaseHelper verify;
	private String host = "127.0.0.1";
	private int port = 3306;
	private boolean isSaved;

	public HostConfig(DatabaseHelper helper) {
		dbInHelper = helper;

		init();
		buildDialog();
		activateComponents();
		pack();
	}

	private void init() {
		isSaved = false;
		init(dbInHelper.getDatabaseType().getName() + " " + GUIFactory.getText("configuration"));
		setHelpFile("export_hostdb");

		String database = dbInHelper.getDatabase();
		if (!database.isEmpty()) {
			int index = database.indexOf(":");
			host = database.substring(0, index++);
			String portNo = database.substring(index, database.indexOf("/"));
			port = Integer.valueOf(portNo);
			index += portNo.length() + 1;
			database = database.substring(index);
		}

		txHost = new JTextField();
		txHost.setText(host);

		txPort = new JSpinner(new SpinnerNumberModel(1, 1, 65353, 1));
		JFormattedTextField txt = ((JSpinner.NumberEditor) txPort.getEditor()).getTextField();
		((NumberFormatter) txt.getFormatter()).setFormat(new DecimalFormat("#####"));
		txPort.setValue(port);

		txUser = new JTextField();
		txUser.setText(dbInHelper.getUser());

		txPassword = new JPasswordField();
		txPassword.setText(General.decryptPassword(dbInHelper.getPassword()));

		txDatabase = new JTextField();
		txDatabase.setText(database);

		txHost.getDocument().addDocumentListener(funcDocumentChange);
		txUser.getDocument().addDocumentListener(funcDocumentChange);
		txDatabase.getDocument().addDocumentListener(funcDocumentChange);

		txHost.setPreferredSize(txPort.getPreferredSize());
	}

	@Override
	protected void save() throws Exception {
		dbInHelper.setDatabase(verify.getDatabase());
		dbInHelper.setPassword(verify.getPassword());
		dbInHelper.setUser(verify.getUser());
		dbInHelper.setDatabaseType(verify.getDatabaseType());
		isSaved = true;
	}

	public boolean isSaved() {
		return isSaved;
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
		result.setBorder(BorderFactory.createEtchedBorder());

		int index = 0;
		XGridBagConstraints c = new XGridBagConstraints();

		result.add(GUIFactory.getJLabel("hostname"), c.gridCell(0, index, 0, 0));
		result.add(txHost, c.gridCell(1, index, 2, 0));
		result.add(GUIFactory.getJLabel("port"), c.gridCell(2, index, 0, 0));
		result.add(txPort, c.gridCell(3, index++, 2, 0));
		result.add(GUIFactory.getJLabel("user"), c.gridCell(0, index, 0, 0));
		result.add(txUser, c.gridCell(1, index, 2, 0));
		result.add(GUIFactory.getJLabel("password"), c.gridCell(2, index, 0, 0));
		result.add(txPassword, c.gridCell(3, index++, 2, 0));
		result.add(GUIFactory.getJLabel("database"), c.gridCell(0, index, 0, 0));
		result.add(txDatabase, c.gridCell(1, index, 2, 0));

		return result;
	}

	private Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btTest = GUIFactory.getJButton("testConnection", e -> testConnection());
		btApply = GUIFactory.getJButton("apply", funcSave);
		btApply.setEnabled(false);

		panel.add(btTest);
		panel.add(Box.createHorizontalStrut(2));
		panel.add(btApply);
		return panel;
	}

	private void testConnection() {
		MariaDB db = new MariaDB(null);
		btTest.setEnabled(false);

		verify = new DatabaseHelper(txHost.getText() + ":" + txPort.getValue() + "/" + txDatabase.getText(),
				ExportFile.MARIADB);
		verify.setUser(txUser.getText());
		verify.setPassword(General.encryptPassword(txPassword.getPassword()));

		try {
			db.openFile(verify, true);
			db.closeFile();
			btApply.setEnabled(true);
			btTest.setEnabled(true);
			General.showMessage(this, GUIFactory.getText("testConnectionOK"), GUIFactory.getText("testConnection"),
					false);
		} catch (Exception ex) {
			General.errorMessage(HostConfig.this, ex, GUIFactory.getText("testConnection"), null);
		}
	}

	@Override
	public void activateComponents() {
		btTest.setEnabled(StringUtils.isNoneBlank(txHost.getText(), txUser.getText(), txDatabase.getText()));
		btApply.setEnabled(false);
	}
}