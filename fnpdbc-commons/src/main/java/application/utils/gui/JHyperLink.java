package application.utils.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;

import application.utils.General;

public class JHyperLink extends JLabel {
	private static final long serialVersionUID = 1413934641540367114L;

	private static final Color LINKCOLOR = Color.blue;
	transient List<ActionListener> actionListenerList = new LinkedList<>();
	private boolean underline;
	private String actionCommand;

	transient MouseListener listener = new MouseAdapter() {
		@Override
		public void mouseEntered(MouseEvent me) {
			if (isEnabled()) {
				underline = true;
				repaint();
			}
		}

		@Override
		public void mouseExited(MouseEvent me) {
			if (isEnabled()) {
				underline = false;
				repaint();
			}
		}

		@Override
		public void mouseClicked(MouseEvent me) {
			if (isEnabled()) {
				fireActionEvent();
			}
		}
	};

	public JHyperLink() {
		this(General.EMPTY_STRING);
	}

	public void setActionCommand(String actionCmd) {
		actionCommand = actionCmd;
	}

	public JHyperLink(String text) {
		super(text);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setForeground(LINKCOLOR);
		addMouseListener(listener);
	}

	public void addActionListener(ActionListener l) {
		if (!actionListenerList.contains(l)) {
			actionListenerList.add(l);
		}
	}

	public void removeActionListener(ActionListener l) {
		actionListenerList.remove(l);
	}

	protected void fireActionEvent() {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				actionCommand == null ? getText() : actionCommand);
		for (ActionListener l : actionListenerList) {
			l.actionPerformed(e);
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (underline) {
			// really all this size stuff below only needs to be recalculated if font or
			// text changes
			Rectangle2D textBounds = getFontMetrics(getFont()).getStringBounds(getText(), g);

			// this layout stuff assumes the icon is to the left, or null
			int y = getHeight() / 2 + (int) (textBounds.getHeight() / 2);
			int w = (int) textBounds.getWidth();
			int x = getIcon() == null ? 0 : getIcon().getIconWidth() + getIconTextGap();

			g.setColor(getForeground());
			g.drawLine(0, y, x + w, y);
		}
	}
}
