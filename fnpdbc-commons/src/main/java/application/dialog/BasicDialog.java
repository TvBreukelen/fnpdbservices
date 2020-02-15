package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import application.preferences.GeneralSettings;
import application.utils.FNProgException;
import application.utils.GUIFactory;
import application.utils.General;

public abstract class BasicDialog extends JDialog {
	/**
	 * Title: BasicDialog
	 *
	 * @description: Generic Abstract Class for Configuration Dialogs Copyright: (c)
	 *               2004-2010
	 *
	 * @author Tom van Breukelen
	 * @version 4.5
	 */

	protected JButton btExit;
	protected JButton btHelp;
	protected JButton btSave;

	protected DocumentListener funcDocumentChange;

	protected GeneralSettings generalSettings = GeneralSettings.getInstance();

	private String myHelpFile;
	private boolean isMainScreen;

	private static final long serialVersionUID = -4427269633857070104L;

	public BasicDialog() {
		setModal(true);
		isMainScreen = true;
	}

	public BasicDialog(Component dialog) {
		setModal(true);
		setLocationRelativeTo(dialog);
		isMainScreen = false;
	}

	protected void init(String title) {
		setTitle(title);

		funcDocumentChange = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateDocument(e);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateDocument(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// not fired by PlainDocument
			}

			public void updateDocument(DocumentEvent e) {
				activateComponents();
			}
		};

		btSave = General.createToolBarButton(GUIFactory.getToolTip("menuSave"), "Save.png", e -> {
			try {
				save();
				close();
			} catch (Exception ex) {
				General.errorMessage(BasicDialog.this, ex, GUIFactory.getTitle("configError"), null);
				if (!(ex instanceof FNProgException)) {
					close();
				}
			}
		});

		btHelp = General.createToolBarButton(GUIFactory.getToolTip("menuHelp"), "Help.png", e -> showHelp());
		btExit = General.createToolBarButton(GUIFactory.getToolTip("menuExitScr"), "Exit.png", e -> close());
	}

	protected void buildDialog() {
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		getContentPane().add(createCenterPanel(), BorderLayout.CENTER);
	}

	protected void close() {
		setVisible(false);
		dispose();
	}

	protected void showHelp() {
		HelpDialog help = new HelpDialog(GUIFactory.getText("menuHelp") + " - " + getTitle(), myHelpFile);
		help.setVisible(true);
	}

	public void setHelpFile(String helpFile) {
		myHelpFile = helpFile;
	}

	protected Component createToolBar() {
		Box result = Box.createHorizontalBox();
		result.add(Box.createHorizontalStrut(5));
		result.add(btSave);
		result.add(addToToolbar());
		result.add(Box.createRigidArea(new Dimension(5, 42)));
		result.add(Box.createHorizontalGlue());
		result.add(btHelp);
		result.add(Box.createHorizontalStrut(2));
		result.add(btExit);
		result.add(Box.createHorizontalStrut(5));
		result.setBorder(BorderFactory.createRaisedBevelBorder());
		return result;
	}

	@Override
	public void setVisible(boolean b) {
		if (isMainScreen) {
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			setLocation((dim.width - getSize().width) / 3, (dim.height - getSize().height) / 3);
		}
		super.setVisible(b);
	}

	public void activateComponents() {
		// Nothing to do here on this level
	}

	protected void save() throws Exception {
		// Nothing to do here on this level
	}

	protected Component addToToolbar() {
		return Box.createHorizontalGlue();
	}

	protected abstract Component createCenterPanel();

	protected abstract void init();
}