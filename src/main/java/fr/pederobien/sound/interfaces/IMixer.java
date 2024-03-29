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
	 * Get or create an internal audio stream associated to the given key. This key is used to get a continuous audio stream when
	 * several audio stream need to be played at the same time.
	 * 
	 * @param packet the packet that gather the properties of the audio sample to add.
	 */
	void put(AudioPacket packet);

	/**
	 * Rename the audio stream associated to the given oldKey.
	 * 
	 * @param oldKey The old stream name.
	 * @param newKey The new stream name.
	 * 
	 * @return True if a stream is associated to the oldKey and has been renamed, false otherwise.
	 */
	boolean renameStream(String oldKey, String newKey);

	/**
	 * Set the volume of the stream associated to the given key. The new volume should be in range [0,2].
	 * 
	 * @param key    The key used to retrieve the audio stream.
	 * @param volume The new volume of the audio stream.
	 * 
	 * @return True if a stream exists for the given key.
	 */
	boolean setStreamVolume(String key, float volume);

	/**
	 * Removes all samples associated to each audio streams.
	 * 
	 * @param full Set to true in order to remove also all streams.
	 */
	void clear(boolean full);
}
