package fr.pederobien.sound.interfaces;

public interface IAudioStream {

	/**
	 * Set the volume in the left channel.
	 * 
	 * @param leftVolume The volume in the left channel.
	 */
	void setLeftVolume(float leftVolume);

	/**
	 * Set the volume in the right channel.
	 * 
	 * @param rightVolume The volume in the right channel.
	 */
	void setRightVolume(float rightVolume);

	/**
	 * Set the volume in both left and right channel.
	 * 
	 * @param globalVolume The volume in both left and right channel.
	 */
	void setGlobalVolume(float globalVolume);

	/**
	 * Set the offset to apply to the global volume to modify the left and right volumes.
	 * 
	 * @param offset The value to apply on the global volume.
	 */
	void setOffset(float offset);

	/**
	 * Add the given bytes array to the underlying queue of the stream.
	 * 
	 * @param data The bytes array that contains the audio sample to add to a stream.
	 */
	void put(byte[] data);

	/**
	 * Read two bytes from the underlying queue, and update the left / right byte array with the correct samples value.
	 * 
	 * @param left  The sample for the left channel (left and global volume applied)
	 * @param right The sample for the right channel (right and global volume applied)
	 * @return True if data could be read, false otherwise.
	 */
	boolean read(short[] left, short[] right);

	/**
	 * Clear the content of this audio stream so that the next call to the read method returns 0.
	 */
	void flush();
}
