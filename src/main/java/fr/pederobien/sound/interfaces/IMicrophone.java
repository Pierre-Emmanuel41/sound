package fr.pederobien.sound.interfaces;

public interface IMicrophone {

	/**
	 * Opens the access to the microphone. If an error occurred while accessing the
	 * microphone, an exception shall be thrown. The method initialize its filter if
	 * defined.
	 */
	void open() throws Exception;

	/**
	 * Closes the access to the microphone. If an error occurred while closing the
	 * microphone, an exception shall be thrown.
	 */
	void close() throws Exception;

	/**
	 * Blocks until data are available to be sent to the remote. If the microphone
	 * is closed while waiting, the method shall return -1.
	 * 
	 * @param data The bytes array to update with the microphone audio stream.
	 * @return The number of bytes written in the array, -1 if an exception occurred
	 *         while waiting.
	 */
	int read(byte[] data) throws Exception;

	/**
	 * Set the filter to use to improve microphone's audio quality. If setting the
	 * filter while the microphone is opened, the filter shall already be
	 * initialized. If the microphone is not yet opened, the filter shall not be
	 * initialized as the open method call the initialize method of the filter. The
	 * given filter is enabled, and the previous filter is disabled and disposed.
	 * Set to null will set the filter to new NoFilter().
	 * 
	 * @param impl The implementation to use to filter raw microphone's audio
	 *             stream.
	 */
	void setFilter(IFilter impl);

	/**
	 * Set if this filter is enabled. By default a filter should be disabled, the
	 * microphone enables it when opened and disable it when closed.
	 * 
	 * @param isEnabled True to filter the microphone stream, false to leave it as
	 *                  it is.
	 */
	void setFilterEnabled(boolean isEnabled);
}
