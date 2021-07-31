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
	 * @return The encoder used to compress data coming from the microphone and decompress data coming from the compression.
	 */
	IEncoder getEncoder();

	/**
	 * @return The mixer associated to this management.
	 */
	IMixer getMixer();
}
