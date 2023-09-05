package application.interfaces;

import application.dialog.ConfigTextFile.BuddyExport;

public interface IConfigSoft {
	void setVisible(boolean isVisable);

	void pack();

	default void verifyDatabase() {
		// used only by DBConvert
	}

	BuddyExport getBuddyExport();

	void activateComponents();
}
