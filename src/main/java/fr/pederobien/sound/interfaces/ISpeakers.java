package fr.pederobien.sound.interfaces;

public interface ISpeakers {

	/**
	 * Opens the access to the speakers. If an error occurred while accessing to the speakers, and exception shall be thrown.
	 */
	void open() throws Exception;

	/**
	 * Closes the access to the speakers. If an error occurred while closing the access to the speakers, an exception shall be thrown.
	 */
	void close() throws Exception;

	/**
	 * @return The mixer used to manager audio streams.
	 */
	IMixer getMixer();
}
