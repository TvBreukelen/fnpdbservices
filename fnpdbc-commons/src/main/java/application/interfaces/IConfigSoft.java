package application.interfaces;

import application.dialog.ConfigTextFile.BuddyExport;

public interface IConfigSoft {
	void setVisible(boolean isVisable);

	void pack();

	default void verifyDatabase() {
		// used only by DBConvert
	}

	default BuddyExport getBuddyExport() {
		return BuddyExport.None;
	}

	void activateComponents();
}
