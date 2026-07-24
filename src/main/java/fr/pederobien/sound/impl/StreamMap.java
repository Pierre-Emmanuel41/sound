package fr.pederobien.sound.impl;

import java.util.ArrayList;
import java.util.List;

public class StreamMap {
	private final Mixer mixer;
	private final List<Stream> streams;
	private final Object lock;

	public StreamMap(Mixer mixer) {
		this.mixer = mixer;
		streams = new ArrayList<Stream>();
		lock = new Object();
	}

	/**
	 * Get a stream associated to the given name if registered. If there is no stream associated to the given name, one is created.
	 * 
	 * @param name The name of the stream to retrieve.
	 * @return The stream associated to the given name.
	 */
	public AudioStream getOrCreateStream(String name) {
		synchronized (lock) {
			for (Stream stream : streams)
				if (stream.getName().equals(name))
					return stream.getAudio();
		}

		// Stream not found
		Stream stream = new Stream(name, new AudioStream(mixer));
		streams.add(stream);
		return stream.getAudio();
	}

	/**
	 * Check if there is an audio stream registered for the given audio stream name.
	 * 
	 * @param name The name of the audio stream.
	 * @return True if an audio stream is registered for the given name, false otherwise.
	 */
	public boolean exist(String name) {
		synchronized (lock) {
			for (Stream stream : streams)
				if (stream.getName().equals(name))
					return true;
		}

		return false;
	}

	/**
	 * Read one sample from each stream registered in this map, sums the result, performs clipping checks.
	 * 
	 * @param left  The resulting sample for the left channel.
	 * @param right The resulting sample for the right channel.
	 * @return True if there was at least one non-empty stream, false otherwise.
	 */
	public boolean read(short[] left, short[] right) {
		int sumLeft = 0;
		int sumRight = 0;
		boolean read = false;

		synchronized (lock) {
			for (Stream stream : streams) {
				short[] sampleLeft = new short[1];
				short[] sampleRight = new short[1];

				// Getting left and right sample for the stream
				if (stream.getAudio().read(sampleLeft, sampleRight)) {
					sumLeft += sampleLeft[0];
					sumRight += sampleRight[0];
					read = true;
				}
			}
		}

		// All streams are empty
		if (!read)
			return false;

		// Clipping
		left[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sumLeft));
		right[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sumRight));

		return true;
	}

	/**
	 * Clears each audio stream registered in this mixer but leave the streams list unmodified.
	 */
	public void flush() {
		synchronized (lock) {
			for (Stream stream : streams) {
				stream.getAudio().flush();
			}
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
	public void setVolumes(String name, float left, float right, float global) {
		setVolumes(getOrCreateStream(name), left, right, global);
	}

	/**
	 * Set to 1.0 the left, right and global volumes of each registered stream.
	 */
	public void resetVolumes() {
		synchronized (lock) {
			for (Stream stream : streams)
				setVolumes(stream.getAudio(), 1.0f, 1.0f, 1.0f);
		}
	}

	private void setVolumes(AudioStream stream, float left, float right, float global) {
		stream.setLeftVolume(left);
		stream.setRightVolume(right);
		stream.setGlobalVolume(global);
	}

	private class Stream {
		private String name;
		private AudioStream audio;

		/**
		 * Creates a stream element based on the given name and audio stream.
		 * 
		 * @param name   The name of the stream
		 * @param stream
		 */
		private Stream(String name, AudioStream audio) {
			this.name = name;
			this.audio = audio;
		}

		/**
		 * @return The name of the audio stream.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return The audio stream associated to the name.
		 */
		public AudioStream getAudio() {
			return audio;
		}
	}
}
