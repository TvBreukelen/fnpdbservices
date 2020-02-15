package application.utils.gui;

import java.util.Vector;

public class ScrollComboBoxImpl extends ScrollableComboBox {
	private static final long serialVersionUID = 3204780205638724272L;

	public ScrollComboBoxImpl() {
		super();
		updateUI();
	}

	public ScrollComboBoxImpl(String[] objects) {
		super(objects);
		updateUI();
	}

	public ScrollComboBoxImpl(Vector<String> vec) {
		super(vec);
		updateUI();
	}

}
