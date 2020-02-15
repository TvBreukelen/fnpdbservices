package application.interfaces;

/**
 * Title: IConfigDb Description: Database Config Selection Interface Copyright
 * (c) 2004-2020
 *
 * @author Tom van Breukelen
 * @version 4.5
 */
public interface IConfigDb {
	void setProperties() throws Exception;

	default void activateComponents() {
	}
}