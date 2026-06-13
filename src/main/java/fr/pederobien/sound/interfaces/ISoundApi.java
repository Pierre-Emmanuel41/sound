package fr.pederobien.sound.interfaces;

public interface ISoundApi {

	/**
	 * Free resources used by the sound API.
	 */
	void dispose();

	/**
	 * @return Null if the initialize method has not been called or if an error occurred during the initialization. An initialized
	 *         microphone otherwise.
	 */
	IMicrophone getMicrophone();

	/**
	 * @return Null if the initialize method has not been called or if an error occurred during the initialization. An initialized
	 *         speakers otherwise.
	 */
	ISpeakers getSpeakers();

	/**
	 * @return The mixing table to play several streams at the same time.
	 */
	IMixer getMixer();
}
