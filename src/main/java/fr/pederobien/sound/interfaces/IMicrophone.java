package fr.pederobien.sound.interfaces;

public interface IMicrophone {

	/**
	 * Opens the access to the microphone. If an error occurred while accessing the microphone, an exception shall be thrown.
	 */
	void open() throws Exception;

	/**
	 * Closes the access to the microphone. If an error occurred while closing the microphone, an exception shall be thrown.
	 */
	void close() throws Exception;

	/**
	 * Blocks until data are available to be sent to the remote. If the microphone is closed while waiting, the method shall returns a
	 * null bytes array.
	 * 
	 * @return The processed bytes array from the microphone.
	 */
	byte[] fetch() throws Exception;
}
