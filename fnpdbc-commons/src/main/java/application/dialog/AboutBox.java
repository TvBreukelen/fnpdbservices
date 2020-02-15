package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import application.interfaces.TvBSoftware;
import application.utils.GUIFactory;
import application.utils.General;
import application.utils.gui.XGridBagConstraints;

public class AboutBox extends JDialog {
	/**
	 * About Box
	 */
	private static final long serialVersionUID = -1678574636675915817L;
	private TvBSoftware _software;
	private String _support;

	public AboutBox(TvBSoftware software, Component parent) {
		setModal(true);
		setTitle("About " + software.getName());

		_software = software;
		_support = software.getSupport();

		add(buildDialog());
		setLocationRelativeTo(parent);
		pack();
	}

	private JPanel buildDialog() {
		JPanel result = new JPanel(new BorderLayout());
		JLabel label = new JLabel(_software.getName());
		label.setFont(new Font("serif", Font.BOLD, 24));
		label.setIcon(General.createImageIcon("PDA.png"));
		label.setHorizontalTextPosition(SwingConstants.LEFT);

		Box box = Box.createHorizontalBox();
		box.add(new JLabel("Support: "));
		box.add(GUIFactory.getJHyperLink(_support, e -> {
			try {
				General.gotoWebsite(_support);
			} catch (Exception ex) {
				General.errorMessage(AboutBox.this, ex, GUIFactory.getTitle("connectionError"),
						GUIFactory.getMessage("connectionError", _support));
			}
		}));

		JPanel panel = new JPanel(new GridBagLayout());
		XGridBagConstraints c = new XGridBagConstraints();

		panel.add(label, c.gridCell(0, 0, 0, 0));
		panel.add(new JLabel("Version: " + _software.getVersion()), c.gridCell(0, 1, 0, 0));
		panel.add(new JLabel(_software.getCopyright()), c.gridCell(0, 2, 0, 0));
		panel.add(Box.createVerticalStrut(5), c.gridCell(0, 3, 0, 0));
		panel.add(new JLabel("Java version: " + System.getProperty("java.version", "")
				+ (General.IS_X64 ? " (64 bit)" : " (32 bit)")), c.gridCell(0, 4, 0, 0));
		panel.add(Box.createVerticalStrut(15), c.gridCell(0, 5, 0, 0));
		panel.add(box, c.gridCell(0, 6, 0, 0));
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JButton button = new JButton("OK");
		button.addActionListener(e -> close());
		JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel2.add(button);

		result.add(panel, BorderLayout.CENTER);
		result.add(panel2, BorderLayout.SOUTH);
		result.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		result.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		result.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		return result;
	}

	private void close() {
		setVisible(false);
		dispose();
	}
}
