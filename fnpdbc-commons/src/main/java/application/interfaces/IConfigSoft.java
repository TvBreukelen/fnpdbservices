package application.interfaces;

public interface IConfigSoft {
	void setVisible(boolean isVisable);

	void pack();

	default void verifyDatabase() {
		// used only by DBConvert
	}

	void activateComponents();
}
