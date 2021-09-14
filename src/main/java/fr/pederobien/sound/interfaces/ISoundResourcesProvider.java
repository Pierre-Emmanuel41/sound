package fr.pederobien.sound.interfaces;

public interface ISoundResourcesProvider {

	/**
	 * @return The microphone associated to this management.
	 */
	IMicrophone getMicrophone();

	/**
	 * @return The speakers associated to this management.
	 */
	ISpeakers getSpeakers();

	/**
	 * @return The mixer associated to this management.
	 */
	IMixer getMixer();

	/**
	 * Creates a new encoder. This encoder has an internal state so until the audio source is the same there is no need to create a
	 * new encoder instance.
	 * 
	 * @return a new encoder.
	 */
	IEncoder newEncoder();

	/**
	 * Creates a new decoder. This decoder has an internal state so until the audio source is the same, there is no need to create a
	 * new decoder instance.
	 * 
	 * @return A new decoder.
	 */
	IDecoder newDecoder();
}
