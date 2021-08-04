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
}
