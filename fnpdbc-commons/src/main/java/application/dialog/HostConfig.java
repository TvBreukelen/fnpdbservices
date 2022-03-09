package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.NumberFormatter;

import org.apache.commons.lang3.StringUtils;

import application.FileType;
import application.interfaces.ExportFile;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;
import dbengine.GeneralDB;
import dbengine.utils.DatabaseHelper;

public class HostConfig extends BasicDialog {
	private static final long serialVersionUID = -4570103476841905520L;

	private JTextField txHost;
	private JTextField txUser;
	private JTextField txDatabase;
	private JSpinner txPort;
	private JPasswordField txPassword;
	private JButton btTest;
	private JButton btApply;

	private JCheckBox ckUseSsl;
	private JTextField txKeyStore;
	private JPasswordField txKeyStorePassword;
	private JTextField txServerSslCert;
	private JTextField txServerSslCaCert;
	private JComboBox<String> cbMode;
	private JButton btKeyStore;
	private JButton btServerSslCert;
	private JButton btServerSslCaCert;

	private DatabaseHelper dbInHelper;
	private DatabaseHelper verify;
	private String host = "localhost";

	private boolean isSaved;
	private boolean isMariaDB;
	private boolean isPostgreSQL;

	private static final String TEST_CONNECTION = "testConnection";

	public HostConfig(DatabaseHelper helper) {
		dbInHelper = helper;
		isMariaDB = dbInHelper.getDatabaseType() == ExportFile.MARIADB;
		isPostgreSQL = dbInHelper.getDatabaseType() == ExportFile.POSTGRESQL;

		init();
		buildDialog();
		activateComponents();
		pack();
	}

	private void init() {
		isSaved = false;
		init(dbInHelper.getDatabaseType().getName() + " " + GUIFactory.getText("configuration"));
		setHelpFile("export_hostdb");

		int port = isMariaDB ? 3306 : 5432;

		String database = dbInHelper.getDatabase();
		if (!database.isEmpty()) {
			int index = database.indexOf(":");
			host = database.substring(0, index++);
			String portNo = database.substring(index, database.indexOf("/"));
			port = Integer.valueOf(portNo);
			index += portNo.length() + 1;
			database = database.substring(index);
		}

		txHost = GUIFactory.getJTextField("", host);
		txUser = GUIFactory.getJTextField("", dbInHelper.getUser());
		txDatabase = GUIFactory.getJTextField("", database);
		txKeyStore = GUIFactory.getJTextField("sslKeyStore", dbInHelper.getKeyStore());
		txServerSslCert = GUIFactory.getJTextField("sslCertificate", dbInHelper.getServerSslCert());
		txServerSslCaCert = GUIFactory.getJTextField("sslCaCertificate", dbInHelper.getServerSslCaCert());
		ckUseSsl = GUIFactory.getJCheckBox("useSsl", dbInHelper.isUseSsl(), e -> activateComponents());

		txPort = new JSpinner(new SpinnerNumberModel(1, 1, 65353, 1));
		JFormattedTextField txt = ((JSpinner.NumberEditor) txPort.getEditor()).getTextField();
		((NumberFormatter) txt.getFormatter()).setFormat(new DecimalFormat("#####"));
		txPort.setValue(port);

		txPassword = new JPasswordField();
		txPassword.setText(General.decryptPassword(dbInHelper.getPassword()));
		txKeyStorePassword = new JPasswordField();
		txKeyStorePassword.setText(General.decryptPassword(dbInHelper.getKeyStorePassword()));

		txHost.getDocument().addDocumentListener(funcDocumentChange);
		txUser.getDocument().addDocumentListener(funcDocumentChange);
		txDatabase.getDocument().addDocumentListener(funcDocumentChange);
		txHost.setPreferredSize(txPort.getPreferredSize());

		btKeyStore = GUIFactory.getJButton("...",
				e -> General.getSelectedFile(this, txKeyStore, FileType.KEYSTORE, true));
		btServerSslCert = GUIFactory.getJButton("...",
				e -> General.getSelectedFile(this, txServerSslCert, FileType.TRUSTSTORE, true));
		btServerSslCaCert = GUIFactory.getJButton("...",
				e -> General.getSelectedFile(this, txServerSslCaCert, FileType.TRUSTSTORE, true));

		cbMode = new JComboBox<>(isMariaDB ? new String[] { "trust", "verify-ca", "verify-full" }
				: new String[] { "allow", "prefer", "require", "verify-ca", "verify-full" });
		cbMode.setSelectedItem(dbInHelper.getSslMode());
	}

	@Override
	protected void save() throws Exception {
		dbInHelper.update(verify);
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
		JTabbedPane result = new JTabbedPane();
		result.setBorder(BorderFactory.createEtchedBorder());

		JPanel panel1 = new JPanel(new GridBagLayout());
		JPanel panel2 = new JPanel(new GridBagLayout());

		int index = 0;
		XGridBagConstraints c = new XGridBagConstraints();

		panel1.add(GUIFactory.getJLabel("hostname"), c.gridCell(0, index, 0, 0));
		panel1.add(txHost, c.gridCell(1, index, 2, 0));
		panel1.add(GUIFactory.getJLabel("port"), c.gridCell(2, index, 0, 0));
		panel1.add(txPort, c.gridCell(3, index++, 2, 0));
		panel1.add(GUIFactory.getJLabel("user"), c.gridCell(0, index, 0, 0));
		panel1.add(txUser, c.gridCell(1, index, 2, 0));
		panel1.add(GUIFactory.getJLabel("password"), c.gridCell(2, index, 0, 0));
		panel1.add(txPassword, c.gridCell(3, index++, 2, 0));
		panel1.add(GUIFactory.getJLabel("database"), c.gridCell(0, index, 0, 0));
		panel1.add(txDatabase, c.gridCell(1, index, 2, 0));

		result.addTab(GUIFactory.getText("configuration"), panel1);

		panel2.add(ckUseSsl, c.gridCell(1, 0, 0, 0));
		panel2.add(GUIFactory.getJLabel("sslMode"), c.gridCell(0, 1, 0, 0));
		panel2.add(cbMode, c.gridCell(1, 1, 0, 0));
		panel2.add(GUIFactory.getJLabel("sslKeyStore"), c.gridCell(0, 2, 0, 0));
		panel2.add(txKeyStore, c.gridCell(1, 2, 2, 0));
		panel2.add(btKeyStore, c.gridCell(2, 2, 0, 0));
		panel2.add(GUIFactory.getJLabel("sslKeyStorePassword"), c.gridCell(0, 3, 0, 0));
		panel2.add(txKeyStorePassword, c.gridCell(1, 3, 0, 0));
		panel2.add(GUIFactory.getJLabel("sslCertificate"), c.gridCell(0, 4, 0, 0));
		panel2.add(txServerSslCert, c.gridCell(1, 4, 0, 0));
		panel2.add(btServerSslCert, c.gridCell(2, 4, 0, 0));

		if (isPostgreSQL) {
			panel2.add(GUIFactory.getJLabel("sslCaCertificate"), c.gridCell(0, 5, 0, 0));
			panel2.add(txServerSslCaCert, c.gridCell(1, 5, 0, 0));
			panel2.add(btServerSslCaCert, c.gridCell(2, 5, 0, 0));
		}

		result.addTab("SSL", panel2);
		result.addChangeListener(
				e -> setHelpFile(result.getSelectedIndex() == 0 ? "export_hostdb" : "export_ssl_hostdb"));
		return result;
	}

	private Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btTest = GUIFactory.getJButton(TEST_CONNECTION, e -> testConnection());
		btApply = GUIFactory.getJButton("apply", funcSave);
		btApply.setEnabled(false);

		panel.add(btTest);
		panel.add(Box.createHorizontalStrut(2));
		panel.add(btApply);
		return panel;
	}

	private void testConnection() {
		GeneralDB db = GeneralDB.getDatabase(dbInHelper.getDatabaseType(), null);
		btTest.setEnabled(false);

		verify = new DatabaseHelper(txHost.getText() + ":" + txPort.getValue() + "/" + txDatabase.getText(),
				dbInHelper.getDatabaseType());
		verify.setUser(txUser.getText().trim());
		verify.setPassword(General.encryptPassword(txPassword.getPassword()));
		verify.setKeyStore(txKeyStore.getText().trim());
		verify.setKeyStorePassword(General.encryptPassword(txKeyStorePassword.getPassword()));
		verify.setServerSslCert(txServerSslCert.getText().trim());
		verify.setServerSslCaCert(txServerSslCaCert.getText().trim());
		verify.setSslMode(cbMode.getSelectedIndex() == -1 ? "" : cbMode.getSelectedItem().toString());
		verify.setUseSsl(ckUseSsl.isSelected());

		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			db.openFile(verify, true);
			db.closeFile();
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			btApply.setEnabled(true);
			btTest.setEnabled(true);
			General.showMessage(this, GUIFactory.getText("testConnectionOK"), GUIFactory.getText(TEST_CONNECTION),
					false);
		} catch (Exception ex) {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			General.errorMessage(HostConfig.this, ex, GUIFactory.getText(TEST_CONNECTION), null);
			activateComponents();
		}
	}

	@Override
	public void activateComponents() {
		btTest.setEnabled(StringUtils.isNoneBlank(txHost.getText(), txUser.getText(), txDatabase.getText()));
		btApply.setEnabled(false);

		txKeyStore.setEnabled(ckUseSsl.isSelected());
		txKeyStorePassword.setEnabled(ckUseSsl.isSelected());
		txServerSslCert.setEnabled(ckUseSsl.isSelected());
		txServerSslCaCert.setEnabled(ckUseSsl.isSelected());
		btKeyStore.setEnabled(ckUseSsl.isSelected());
		btServerSslCert.setEnabled(ckUseSsl.isSelected());
		btServerSslCaCert.setEnabled(ckUseSsl.isSelected());
		cbMode.setEnabled(ckUseSsl.isSelected());
	}
}