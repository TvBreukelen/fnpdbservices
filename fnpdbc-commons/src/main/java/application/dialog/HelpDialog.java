package application.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

public class HelpDialog extends JDialog {
	/**
	 * Title: HelpDialog Description: HTML Help Dialog Class Copyright: (c)
	 * 2004-2005
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */
	private static final long serialVersionUID = 4320971031741162514L;
	transient ActionListener funcBack;
	transient ActionListener funcExit;
	private JEditorPane helpInfo;
	private Deque<URL> urlList;
	private JButton btBack;

	public HelpDialog(String title, String topic) {
		setModal(true);
		init(title, topic);
	}

	private void init(String title, String topic) {
		urlList = new ArrayDeque<>();

		final String HELPFILE = "/help/" + topic + ".html";
		URL url;
		try {
			url = getClass().getResource(HELPFILE);
			urlList.push(url);
			helpInfo = new JEditorPane(url);
		} catch (Exception e) {
			helpInfo = new JEditorPane();
			helpInfo.setText("Unable to find help file " + HELPFILE);
		}

		helpInfo.addHyperlinkListener(createHyperLinkListener());
		helpInfo.setEditable(false);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(helpInfo, BorderLayout.CENTER);
		scrollPane.setPreferredSize(new Dimension(700, 500));

		funcExit = e -> close();

		funcBack = e -> {
			// Previous Button has been pressed
			urlList.pop(); // remove current url from stack

			try {
				helpInfo.setPage(urlList.peek()); // read previous url from stack
				activateComponents();
			} catch (IOException ex) {
				// Should not occur
			}
		};

		setTitle(title == null ? "Help" : title);
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());
		pane.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		pane.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		pane.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		pane.add(createGlobalButtonPanel(), BorderLayout.SOUTH);
		pane.add(scrollPane, BorderLayout.CENTER);

		pack();
		activateComponents();
	}

	private JPanel createGlobalButtonPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btBack = new JButton("Previous");
		btBack.setToolTipText("Go back one paqe");
		btBack.addActionListener(funcBack);
		panel.add(btBack);

		JButton bt = new JButton("Exit");
		bt.setMnemonic(KeyEvent.VK_X);
		bt.setToolTipText("Exit Help");
		bt.addActionListener(funcExit);
		panel.add(bt);
		return panel;
	}

	private void activateComponents() {
		btBack.setEnabled(urlList.size() > 1);
	}

	private void close() {
		setVisible(false);
		dispose();
	}

	@Override
	public void setVisible(boolean b) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((dim.width - getSize().width) / 4, (dim.height - getSize().height) / 2);
		super.setVisible(b);
	}

	private HyperlinkListener createHyperLinkListener() {
		return e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				if (e instanceof HTMLFrameHyperlinkEvent) {
					((HTMLDocument) helpInfo.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
				} else {
					try {
						URL url = e.getURL();
						urlList.push(url);
						helpInfo.setPage(url);
						activateComponents();
					} catch (IOException ex) {
						// Should not occur
					}
				}
			}
		};
	}
}