package fr.pederobien.sound.interfaces;

public interface ISoundApi {

	/**
	 * Initialize Target and Source DataLine to get data from microphone and player data to the speakers. If an error occurred, an
	 * exception is raised.
	 */
	void initialize() throws Exception;

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
