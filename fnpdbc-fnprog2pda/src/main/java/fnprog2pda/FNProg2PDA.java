package fnprog2pda;

import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import application.preferences.GeneralSettings;
import application.utils.GUIFactory;
import application.utils.General;
import fnprog2pda.dialog.ConfigFNProg;
import fnprog2pda.utils.ConvertOldVersion;

/**
 * Title: FNProg2PDA Description: Program to export FNProgramvare data (BookCAT,
 * CATraxx, CATVids, SoftCAT and StampCAT) to a PDA database Copyright: (c)
 * 2003-2019
 *
 * @author Tom van Breukelen
 * @version 8.5
 */

public class FNProg2PDA {
	private JFrame frame;

	private void createAndShowGUI() throws Exception {
		GeneralSettings settings = GeneralSettings.getInstance();

		Map<String, String> lfMap = General.getLookAndFeels();
		String lfName = settings.getLookAndFeel();
		String lfClassName = lfMap.get(lfName);

		if (lfClassName == null) {
			settings.setLookAndFeel("System");
			lfClassName = lfMap.get("System");
		}

		try {
			UIManager.setLookAndFeel(lfClassName);
		} catch (Exception e) {
			// No Gain, no Pain...
		}

		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);

		frame = new ConfigFNProg(true);
		frame.setIconImage(General.createImageIcon("PDA.png").getImage());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();

		frame.setVisible(true);
	}

	public static void main(String[] args) {
		GUIFactory.refresh();

		// Check if we are running in batch mode
		if (args.length > 0) {
			new FNProg2PDA_NoGUI(args);
			System.exit(0);
		}

		new FNProg2PDA().start();
	}

	public void start() {
		if (frame != null) {
			frame.setVisible(false);
			frame.dispose();
		}

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(() -> {
			try {
				ConvertOldVersion.convert();
				createAndShowGUI();
			} catch (Exception e) {
				General.errorMessage(frame, e, "System Error", null);
				System.exit(0);
			}
		});
	}
}