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
	 * need to be played at the same time.
	 * 
	 * @param key          The key used to get the associated sound
	 * @param data         The bytes array to extract for left and right channel.
	 * @param globalVolume The global volume associated to the sample.
	 * @param isMono       Indicates id the bytes array correspond to a mono signal.
	 */
	void put(String key, byte[] data, double globalVolume, boolean isMono);
}
