package application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;

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
	protected ActionListener funcSave;

	protected GeneralSettings generalSettings = GeneralSettings.getInstance();
	protected static final String CONFIG_ERROR = GUIFactory.getTitle("configError");

	private String myHelpFile;

	private static final long serialVersionUID = -4427269633857070104L;

	protected BasicDialog() {
		setModal(true);
	}

	protected void init(String title) {
		init(title, 3);
	}

	protected void init(String title, int pos) {
		setTitle(title);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((dim.width - getSize().width) / pos, (dim.height - getSize().height) / pos);

		funcDocumentChange = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateDocument();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateDocument();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// not fired by PlainDocument
			}

			public void updateDocument() {
				activateComponents();
			}
		};

		funcSave = e -> {
			try {
				save();
				close();
			} catch (FNProgException ex) {
				General.errorMessage(BasicDialog.this, ex, CONFIG_ERROR, null);
				close();
			} catch (Exception ex) {
				General.errorMessage(BasicDialog.this, ex, CONFIG_ERROR, null);
			}
		};

		btSave = GUIFactory.createToolBarButton(GUIFactory.getToolTip("menuSave"), "Save.png", funcSave);
		btHelp = GUIFactory.createToolBarButton(GUIFactory.getToolTip("menuHelp"), "Help.png", e -> showHelp());
		btExit = GUIFactory.createToolBarButton(GUIFactory.getToolTip("menuExitScr"), "Exit.png", e -> close());
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

	public void activateComponents() {
		// Nothing to do here on this level
	}

	protected abstract void save() throws Exception;

	protected Component addToToolbar() {
		return Box.createHorizontalGlue();
	}

	protected abstract Component createCenterPanel();
}