package dbengine;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import application.preferences.Profiles;
import application.utils.FNProgException;
import application.utils.General;

public abstract class SqlRemote extends SqlDB {
	private Session session;
	protected int assignedPort;

	protected SqlRemote(Profiles pref) {
		super(pref);
	}

	protected void getSshSession() throws FileNotFoundException, FNProgException, JSchException {
		// Remote host
		final String remoteHost = myHelper.getSshHost();

		boolean usePrivateKey = !myHelper.getPrivateKeyFile().isEmpty();
		boolean usePassword = !myHelper.getSshPassword().isEmpty();

		JSch jsch = new JSch();

		// Check if a private key is provided
		if (usePrivateKey) {
			verifyKeyFile(myHelper.getPrivateKeyFile());

			if (usePassword) {
				jsch.addIdentity(myHelper.getPrivateKeyFile(), General.decryptPassword(myHelper.getSshPassword()));
			} else {
				jsch.addIdentity(myHelper.getPrivateKeyFile());
			}
		}

		// Create SSH session. Port 22 is the default SSH port which is open in your
		// firewall setup.
		session = jsch.getSession(myHelper.getSshUser(), remoteHost, myHelper.getSshPort());

		if (usePassword && !usePrivateKey) {
			session.setPassword(General.decryptPassword(myHelper.getSshPassword()));
		}

		// Additional SSH options. See your ssh_config manual for more options. Set
		// options according to your requirements.
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no"); // Not really wanted..
		config.put("Compression", "yes");
		config.put("ConnectionAttempts", "2");

		session.setConfig(config);

		// Connect
		session.connect();

		// Create the tunnel through port forwarding. This is basically instructing jsch
		// session to send data received from local_port in the local machine to
		// remote_port of the remote_host assigned_port is the port assigned by jsch for
		// use, it may not always be the same as local_port.

		assignedPort = session.setPortForwardingL(0, remoteHost, myHelper.getPort());

		if (assignedPort == 0) {
			throw new JSchException("Port forwarding failed !");
		}
	}

	protected void verifyKeyFile(String keyFile) throws FileNotFoundException, FNProgException {
		if (!General.existFile(keyFile)) {
			// Should not occur unless file has been deleted
			throw FNProgException.getException("noDatabaseExists", keyFile);
		}

		boolean hasBeginTag = false;
		boolean hasEndTag = false;

		try (Scanner sc = new Scanner(new File(keyFile))) {
			while (sc.hasNext()) {
				String line = sc.nextLine();
				if (line.startsWith("-----BEGIN ")) {
					hasBeginTag = true;
				} else if (line.startsWith("-----END ")) {
					hasEndTag = true;
					break;
				}
			}
		}

		if (!(hasBeginTag && hasEndTag)) {
			throw FNProgException.getException("invalidKeyfile", keyFile);
		}
	}

	@Override
	public void closeFile() {
		try {
			// Verify if we have a SSH session open
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		} catch (Exception e) {
			// Should not occur
		}
		super.closeFile();
	}
}
