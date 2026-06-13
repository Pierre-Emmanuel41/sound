package fr.pederobien.sound.impl;

import java.util.ArrayDeque;
import java.util.Queue;

public class AudioStream {
	private static final int MIN_SIZE = 26460;
	private final Mixer mixer;
	private final Queue<Short> queue;
	private final Object lock;
	private float leftVolume;
	private float rightVolume;
	private float globalVolume;
	private float offset;

	public AudioStream(Mixer mixer) {
		this.mixer = mixer;
		queue = new ArrayDeque<Short>(4095);
		lock = new Object();
		leftVolume = 1;
		rightVolume = 1;
		globalVolume = 1;
		offset = 0;
	}

	/**
	 * Set the volume in the left channel.
	 * 
	 * @param leftVolume The volume in the left channel.
	 */
	public void setLeftVolume(float leftVolume) {
		this.leftVolume = leftVolume;
	}

	/**
	 * Set the volume in the right channel.
	 * 
	 * @param rightVolume The volume in the right channel.
	 */
	public void setRightVolume(float rightVolume) {
		this.rightVolume = rightVolume;
	}

	/**
	 * Set the volume in both left and right channel.
	 * 
	 * @param globalVolume The volume in both left and right channel.
	 */
	public void setGlobalVolume(float globalVolume) {
		this.globalVolume = globalVolume;
	}

	/**
	 * Set the offset to apply to the global volume to modify the left and right volumes.
	 * 
	 * @param offset The value to apply on the global volume.
	 */
	public void setOffset(float offset) {
		this.offset = offset;
	}

	/**
	 * Add the given bytes array to the underlying queue of the stream.
	 * 
	 * @param data The bytes array that contains the audio sample to add to a stream.
	 */
	public void write(byte[] data) {
		if (data == null)
			return;

		synchronized (lock) {
			for (int i = 0; i < data.length; i += 2)
				queue.add((short) ((data[i + 1] & 0xFF) << 8 | (data[i] & 0xFF)));
		}

		// At least 3 frames to play
		if (queue.size() > MIN_SIZE)
			mixer.notifyOneStreamHasBeenFilled();
	}

	/**
	 * Read two bytes from the underlying queue, and update the left / right byte array with the correct samples value.
	 * 
	 * @param left  The sample for the left channel (left and global volume applied)
	 * @param right The sample for the right channel (right and global volume applied)
	 * @return True if data could be read, false otherwise.
	 */
	public boolean read(short[] left, short[] right) {
		if (queue.isEmpty())
			return false;

		short value;
		synchronized (lock) {
			value = queue.poll();
		}

		left[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * leftVolume * (globalVolume + offset)));
		right[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * rightVolume * (globalVolume + offset)));
		return true;
	}

	/**
	 * Clear the content of this audio stream so that the next call to the read method returns 0.
	 */
	public void flush() {
		synchronized (lock) {
			queue.clear();
		}
	}
}
