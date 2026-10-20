package fr.pederobien.sound.impl;

import java.util.HashMap;
import java.util.Map;

public class StreamMap {
	private final float sampleRate;
	private final Map<String, AudioStream> streams;
	private final Object lock;
	private int bufferSize;
	private short[] tmpLeft;
	private short[] tmpRight;

	/**
	 * Creates a map that manages audio streams.
	 * 
	 * @param sampleRate The sample rate used for each audio stream.
	 */
	public StreamMap(float sampleRate) {
		this.sampleRate = sampleRate;
		streams = new HashMap<String, AudioStream>();
		lock = new Object();
	}

	/**
	 * Gets a stream associated to the given name if registered. If there is no stream associated to the given name, one is created.
	 * 
	 * @param name The name of the stream to retrieve.
	 * @return The stream associated to the given name.
	 */
	public AudioStream getOrCreateStream(String name) {
		synchronized (lock) {
			AudioStream stream = streams.get(name);
			if (stream == null) {
				stream = new AudioStream(name, sampleRate);
				streams.put(name, stream);
			}

			return stream;
		}
	}

	/**
	 * Gets a stream associated to the given name if registered.
	 * 
	 * @param name The name of the stream to retrieve.
	 * @return Null if there is no stream registered for the given name, the audio stream otherwise.
	 */
	public AudioStream getStream(String name) {
		synchronized (lock) {
			return streams.get(name);
		}
	}

	/**
	 * Check if there is an audio stream registered for the given audio stream name.
	 * 
	 * @param name The name of the audio stream.
	 * @return True if an audio stream is registered for the given name, false otherwise.
	 */
	public boolean exist(String name) {
		synchronized (lock) {
			return streams.get(name) != null;
		}
	}

	/**
	 * Read n samples from each stream registered in this map, sums the result, performs clipping checks.
	 * 
	 * @param left   The resulting sample for the left channel.
	 * @param right  The resulting sample for the right channel.
	 * @param length The number of shorts to read.
	 * @return The number of shorts that has been read.
	 */
	public int read(short[] left, short[] right, int length) {
		// First call
		if (bufferSize == 0) {
			bufferSize = length;
			tmpLeft = new short[bufferSize];
			tmpRight = new short[bufferSize];
		}
		int read = 0;

		synchronized (lock) {
			for (AudioStream stream : streams.values()) {

				// Getting left and right sample for the stream
				int tmpRead = stream.read(tmpLeft, tmpRight, length);

				if (tmpRead == 0)
					continue;

				int tmp;
				for (int i = 0; i < tmpRead; i++) {
					tmp = left[i] + tmpLeft[i];
					left[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, tmp));
					tmp = right[i] + tmpRight[i];
					right[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, tmp));
				}

				read = Math.max(read, tmpRead);
			}
		}

		return read;
	}

	/**
	 * Clears each audio stream registered in this mixer but leave the streams list unmodified.
	 */
	public void flush() {
		synchronized (lock) {
			for (AudioStream stream : streams.values())
				stream.flush();
		}
	}

	/**
	 * Set the left, right and global volumes of an audio stream.
	 * 
	 * @param name   The name of the stream.
	 * @param left   The volume on the left side.
	 * @param right  The volume on the right side.
	 * @param global The global volume on both sides.
	 */
	public void setVolumes(AudioStream stream, float left, float right, float global) {
		stream.setLeftVolume(left);
		stream.setRightVolume(right);
		stream.setGlobalVolume(global);
	}

	/**
	 * Set to 1.0 the left, right and global volumes of each registered stream.
	 */
	public void resetVolumes() {
		synchronized (lock) {
			for (AudioStream stream : streams.values())
				setVolumes(stream, 1.0f, 1.0f, 1.0f);
		}
	}
}
