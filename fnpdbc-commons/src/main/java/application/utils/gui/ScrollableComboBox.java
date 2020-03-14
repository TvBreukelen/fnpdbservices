package application.utils.gui;

import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import application.utils.General;

public abstract class ScrollableComboBox extends JComboBox<String> {
	/**
	 * ComboBox with horizontal scrolling
	 */
	private static final long serialVersionUID = -4441602300838324297L;

	public ScrollableComboBox() {
		super();
		if (General.IS_MAC_OSX) {
			return;
		}
		setUI(new ScrollableComboUI());
	}

	public ScrollableComboBox(Vector<String> vec) {
		super(vec);
		if (General.IS_MAC_OSX) {
			return;
		}
		setUI(new ScrollableComboUI());
	}

	public ScrollableComboBox(String[] objects) {
		super(objects);
		setUI(new ScrollableComboUI());
	}

	public class ScrollableComboUI extends BasicComboBoxUI {
		@Override
		protected ComboPopup createPopup() {
			return new BasicComboPopup(comboBox) {
				private static final long serialVersionUID = 5353990132838871794L;

				@Override
				protected JScrollPane createScroller() {
					return new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
							ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				}
			};
		}
	}
}
