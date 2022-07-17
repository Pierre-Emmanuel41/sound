package fr.pederobien.sound.interfaces;

import fr.pederobien.sound.impl.AudioPacket;

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
	 * @param packet the packet that gather the properties of the audio sample to add.
	 */
	void put(AudioPacket packet);

	/**
	 * Removes all registered audio streams.
	 */
	void clear();
}
