package fr.pederobien.sound.interfaces;

public interface IMixer {

	/**
	 * @return The global volume associated to the mixer.
	 */
	double getGlobalVolume();

	/**
	 * Set the global volume associated to the mixer.
	 * 
	 * @param globalVolume The global volume of the mixer.
	 */
	void setGlobalVolume(double globalVolume);

	/**
	 * Get or create an internal sound associated to the given key. This key is used to get a continuously sound when several sound
	 * need to be played at the same time. The byte array should correspond to a stereo signal.
	 * 
	 * @param key          The key used to get the associated sound
	 * @param data         The bytes array to extract for left and right channel.
	 * @param globalVolume The global volume associated to the sample.
	 */
	void put(String key, byte[] data, int globalVolume);
}
