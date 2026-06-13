package fr.pederobien.sound.interfaces;

public interface IFilter {

	/**
	 * Write a bytes array that contains a raw audio sample retrieved from the OS microphone.
	 * 
	 * @param buffer The bytes array containing the audio sample.
	 */
	void write(byte[] buffer);

	/**
	 * Update the given bytes array with clean audio samples.
	 * 
	 * @param data The buffer to update.
	 * @return The number of bytes written in the given buffer, -1 if writing failed.
	 */
	int read(byte[] data);

	/**
	 * Set if this filter is enabled. By default a filter should be disabled, the microphone enables it when opened and disable it
	 * when closed.
	 * 
	 * @param isEnabled True to filter the microphone stream, false to leave it as it is.
	 */
	void setEnabled(boolean isEnabled);

	/**
	 * Free resources, this filter cannot be used anymore.
	 */
	void dispose();
}
