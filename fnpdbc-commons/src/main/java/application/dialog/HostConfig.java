package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

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

	// SSH
	private JCheckBox ckUseSsh;
	private JTextField txSshHost;
	private JSpinner txSshPort;
	private JTextField txSshUser;
	private JPasswordField txSshPassword;
	private JTextField txKeyfile;
	private JButton btKeyfile;
	private JSpinner txLocalPort;

	// SSL
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
	private int port;

	private XGridBagConstraints c = new XGridBagConstraints();

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

		result.addTab(GUIFactory.getText("configuration"), createConnectionTab());
		result.addTab(GUIFactory.getText("sshTunnel"), createSshTab());
		result.addTab("SSL", createSslTab());

		Map<Integer, String> mapHelp = new HashMap<>();
		mapHelp.put(0, "export_hostdb");
		mapHelp.put(1, "export_ssh_hostdb");
		mapHelp.put(2, "export_ssl_hostdb");

		result.addChangeListener(e -> setHelpFile(mapHelp.get(result.getSelectedIndex())));
		return result;
	}

	private JPanel createConnectionTab() {
		port = dbInHelper.getPort();
		if (port == 0) {
			port = isMariaDB ? 3306 : 5432;
		}

		host = dbInHelper.getHost();
		if (host.isEmpty()) {
			host = "localhost";
		}

		String user = dbInHelper.getUser();
		if (user.isEmpty()) {
			user = isPostgreSQL ? "postgres" : "root";
		}

		JPanel panel = new JPanel(new GridBagLayout());
		txHost = GUIFactory.getJTextField("", host);
		txUser = GUIFactory.getJTextField("", user);
		txDatabase = GUIFactory.getJTextField("", dbInHelper.getDatabase());
		txPort = getPortSpinner("", port);
		txPassword = GUIFactory.getJPasswordField("", General.decryptPassword(dbInHelper.getPassword()));
		txHost.getDocument().addDocumentListener(funcDocumentChange);
		txUser.getDocument().addDocumentListener(funcDocumentChange);
		txDatabase.getDocument().addDocumentListener(funcDocumentChange);
		txHost.setPreferredSize(txPort.getPreferredSize());

		panel.add(GUIFactory.getJLabel("hostname"), c.gridCell(0, 0, 0, 0));
		panel.add(txHost, c.gridCell(1, 0, 2, 0));
		panel.add(GUIFactory.getJLabel("port"), c.gridCell(2, 0, 0, 0));
		panel.add(txPort, c.gridCell(3, 0, 2, 0));
		panel.add(GUIFactory.getJLabel("user"), c.gridCell(0, 1, 0, 0));
		panel.add(txUser, c.gridCell(1, 1, 2, 0));
		panel.add(GUIFactory.getJLabel("password"), c.gridCell(2, 1, 0, 0));
		panel.add(txPassword, c.gridCell(3, 1, 2, 0));
		panel.add(GUIFactory.getJLabel("database"), c.gridCell(0, 2, 0, 0));
		panel.add(txDatabase, c.gridCell(1, 2, 2, 0));
		return panel;
	}

	private JPanel createSshTab() {
		JPanel panel = new JPanel(new GridBagLayout());

		ckUseSsh = GUIFactory.getJCheckBox("useSsh", dbInHelper.isUseSsh(), e -> activateComponents());

		txSshHost = GUIFactory.getJTextField("sshHost",
				dbInHelper.getSshHost().isEmpty() ? host : dbInHelper.getSshHost());
		txSshPort = getPortSpinner("", dbInHelper.getSshPort() == 0 ? 22 : dbInHelper.getSshPort());
		txSshUser = GUIFactory.getJTextField("sshUser", dbInHelper.getSshUser());
		txSshPassword = GUIFactory.getJPasswordField("sshPassword",
				General.decryptPassword(dbInHelper.getSshPassword()));
		txKeyfile = GUIFactory.getJTextField("sshPrivateKey", dbInHelper.getPrivateKeyFile());
		btKeyfile = GUIFactory.getJButton("...", e -> General.getSelectedFile(this, txKeyfile, FileType.PPK, true));
		txLocalPort = getPortSpinner("sshLocalPort",
				dbInHelper.getLocalPort() == 0 ? port + 1 : dbInHelper.getLocalPort());

		panel.add(ckUseSsh, c.gridCell(1, 0, 0, 0));
		panel.add(GUIFactory.getJLabel("sshHost"), c.gridCell(0, 1, 0, 0));
		panel.add(txSshHost, c.gridCell(1, 1, 2, 0));
		panel.add(GUIFactory.getJLabel("port"), c.gridCell(2, 1, 0, 0));
		panel.add(txSshPort, c.gridCell(3, 1, 2, 0));
		panel.add(GUIFactory.getJLabel("sshUser"), c.gridCell(0, 2, 0, 0));
		panel.add(txSshUser, c.gridCell(1, 2, 2, 0));
		panel.add(GUIFactory.getJLabel("sshPassword"), c.gridCell(2, 2, 0, 0));
		panel.add(txSshPassword, c.gridCell(3, 2, 2, 0));
		panel.add(GUIFactory.getJLabel("sshPrivateKey"), c.gridCell(0, 3, 0, 0));
		panel.add(txKeyfile, c.gridCell(1, 3, 0, 0));
		panel.add(btKeyfile, c.gridCell(2, 3, 0, 0));
		panel.add(GUIFactory.getJLabel("sshLocalPort"), c.gridCell(0, 4, 0, 0));
		panel.add(txLocalPort, c.gridCell(1, 4, 2, 0));
		return panel;
	}

	private JPanel createSslTab() {
		JPanel panel = new JPanel(new GridBagLayout());

		String sslMode = dbInHelper.getSslMode();
		if (sslMode.isEmpty()) {
			sslMode = isPostgreSQL ? "prefer" : "trust";
		}

		ckUseSsl = GUIFactory.getJCheckBox("useSsl", dbInHelper.isUseSsl(), e -> activateComponents());
		cbMode = new JComboBox<>(isMariaDB ? new String[] { "trust", "verify-ca", "verify-full" }
				: new String[] { "allow", "prefer", "require", "verify-ca", "verify-full" });
		cbMode.setSelectedItem(sslMode);

		txKeyStore = GUIFactory.getJTextField("sslKeyStore", dbInHelper.getKeyStore());
		txKeyStorePassword = GUIFactory.getJPasswordField("",
				General.decryptPassword(dbInHelper.getKeyStorePassword()));
		txServerSslCert = GUIFactory.getJTextField("sslCertificate", dbInHelper.getServerSslCert());
		txServerSslCaCert = GUIFactory.getJTextField("sslCaCertificate", dbInHelper.getServerSslCaCert());

		btKeyStore = GUIFactory.getJButton("...",
				e -> General.getSelectedFile(this, txKeyStore, FileType.KEYSTORE, true));
		btServerSslCert = GUIFactory.getJButton("...",
				e -> General.getSelectedFile(this, txServerSslCert, FileType.TRUSTSTORE, true));
		btServerSslCaCert = GUIFactory.getJButton("...",
				e -> General.getSelectedFile(this, txServerSslCaCert, FileType.TRUSTSTORE, true));

		panel.add(ckUseSsl, c.gridCell(1, 0, 0, 0));
		panel.add(GUIFactory.getJLabel("sslMode"), c.gridCell(0, 1, 0, 0));
		panel.add(cbMode, c.gridCell(1, 1, 0, 0));
		panel.add(GUIFactory.getJLabel("sslKeyStore"), c.gridCell(0, 2, 0, 0));
		panel.add(txKeyStore, c.gridCell(1, 2, 2, 0));
		panel.add(btKeyStore, c.gridCell(2, 2, 0, 0));
		panel.add(GUIFactory.getJLabel("sslKeyStorePassword"), c.gridCell(0, 3, 0, 0));
		panel.add(txKeyStorePassword, c.gridCell(1, 3, 0, 0));
		panel.add(GUIFactory.getJLabel("sslCertificate"), c.gridCell(0, 4, 0, 0));
		panel.add(txServerSslCert, c.gridCell(1, 4, 0, 0));
		panel.add(btServerSslCert, c.gridCell(2, 4, 0, 0));

		if (isPostgreSQL) {
			panel.add(GUIFactory.getJLabel("sslCaCertificate"), c.gridCell(0, 5, 0, 0));
			panel.add(txServerSslCaCert, c.gridCell(1, 5, 0, 0));
			panel.add(btServerSslCaCert, c.gridCell(2, 5, 0, 0));
		}
		return panel;
	}

	private JSpinner getPortSpinner(String resourceID, int value) {
		JSpinner result = new JSpinner(new SpinnerNumberModel(1, 1, 65353, 1));
		JFormattedTextField txt = ((JSpinner.NumberEditor) result.getEditor()).getTextField();
		((NumberFormatter) txt.getFormatter()).setFormat(new DecimalFormat("#####"));

		result.setValue(value);
		result.setToolTipText(GUIFactory.getToolTip(resourceID));
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

		verify = new DatabaseHelper(txDatabase.getText(), dbInHelper.getDatabaseType());

		verify.setHost(txHost.getText().trim());
		verify.setPort((int) txPort.getValue());
		verify.setUser(txUser.getText().trim());
		verify.setPassword(General.encryptPassword(txPassword.getPassword()));

		// SSH
		verify.setUseSsh(ckUseSsh.isSelected());
		verify.setSshHost(txSshHost.getText().trim());
		verify.setSshPort((int) txSshPort.getValue());
		verify.setSshUser(txSshUser.getText().trim());
		verify.setSshPassword(General.encryptPassword(txSshPassword.getPassword()));
		verify.setPrivateKeyFile(txKeyfile.getText().trim());
		verify.setLocalPort((int) txLocalPort.getValue());

		// SSL
		verify.setUseSsl(ckUseSsl.isSelected());
		verify.setKeyStore(txKeyStore.getText().trim());
		verify.setKeyStorePassword(General.encryptPassword(txKeyStorePassword.getPassword()));
		verify.setServerSslCert(txServerSslCert.getText().trim());
		verify.setServerSslCaCert(txServerSslCaCert.getText().trim());
		verify.setSslMode(cbMode.getSelectedIndex() == -1 ? "" : cbMode.getSelectedItem().toString());

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

		// SSH
		txSshHost.setEnabled(ckUseSsh.isSelected());
		txSshPort.setEnabled(ckUseSsh.isSelected());
		txSshUser.setEnabled(ckUseSsh.isSelected());
		txSshPassword.setEnabled(ckUseSsh.isSelected());
		txKeyfile.setEnabled(ckUseSsh.isSelected());
		btKeyfile.setEnabled(ckUseSsh.isSelected());
		txLocalPort.setEnabled(ckUseSsh.isSelected());

		// SSL
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