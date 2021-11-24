package fr.pederobien.sound.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import fr.pederobien.sound.interfaces.IMixer;

public class Mixer implements IMixer {
	private static final int BUFFERED_SAMPLES_SIZE = 5;
	private Map<String, AudioStream> streams;
	private double globalVolume;
	private ReentrantLock lock;
	private Condition notEmpty;

	public Mixer() {
		streams = new HashMap<String, AudioStream>();
		globalVolume = 1.0;
		lock = new ReentrantLock(true);
		notEmpty = lock.newCondition();
	}

	@Override
	public double getGlobalVolume() {
		return globalVolume;
	}

	@Override
	public void setGlobalVolume(double globalVolume) {
		this.globalVolume = globalVolume;
	}

	@Override
	public void put(AudioPacket packet) {
		AudioStream stream = getStream(packet.getKey());
		if (stream == null) {
			stream = new AudioStream(packet.getKey());
			putStream(packet.getKey(), stream);
		}

		stream.extract(packet);
		if (stream.size() > BUFFERED_SAMPLES_SIZE) {
			lock.lock();
			try {
				notEmpty.signal();
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Read bytes from this Mixer. This method blocks when at least one of the two conditions is verified :
	 * <p>
	 * There is no registered streams. </br>
	 * For each registered stream, all of the audio samples have been read.</br>
	 * 
	 * @param data   The buffer to read the bytes into.
	 * @param offset The start index to read bytes into.
	 * @param length The maximum number of bytes that should be read.
	 * 
	 * @return The number of bytes read into buffer.
	 */
	protected int read(byte[] data, int offset, int length) {
		List<int[]> streamBuffers = new ArrayList<int[]>();
		int[] read = new int[1];

		// Needs to wait for a stream to be registered.
		if (streams.isEmpty() && sleep())
			return 0;

		// Step 1: reading n bytes from each registered stream.
		streamBuffers = readStreams(length);

		// Step 2: Merging buffers.
		if (!mergeStreams(data, offset, length, streamBuffers, read)) {
			if (sleep())
				return 0;
			else
				readAndMergeStreams(data, offset, length);
		}
		return read[0];
	}

	/**
	 * Read bytes from each registered streams.
	 * 
	 * @param length The number of bytes to read.
	 * 
	 * @return A list that contains the read array associated to each stream.
	 */
	private List<int[]> readStreams(int length) {
		List<int[]> streamBuffers = new ArrayList<int[]>();

		Iterator<AudioStream> iterator;
		lock.lock();
		try {
			iterator = new ArrayList<AudioStream>(streams.values()).iterator();
		} finally {
			lock.unlock();
		}

		// Iterating over a copy of the stream collection in order to put samples in a stream while reading
		while (iterator.hasNext()) {
			// Two bytes for one integer.
			int[] buffer = new int[length / 2];
			int size = iterator.next().read(buffer, buffer.length);

			if (size == length) {
				streamBuffers.add(buffer);
				continue;
			}

			// Fitting the array with the number of read bytes.
			int[] streamBuffer = new int[size];
			System.arraycopy(buffer, 0, streamBuffer, 0, size);
			streamBuffers.add(streamBuffer);
		}

		return streamBuffers;
	}

	/**
	 * Merge each integers array registered in the <code>streamBuffers</code> list in order to create one bytes array that contains
	 * the sum of each registered stream.
	 * 
	 * @param data          The buffer to read the bytes into.
	 * @param offset        The start index to read bytes into.
	 * @param length        The maximum number of bytes that should be read.
	 * @param streamBuffers The list that contains the data of each registered stream.
	 * @param read          An integer array that contains only one value which is the number of read bytes.
	 * 
	 * @return True if bytes has been read, false otherwise.
	 */
	private boolean mergeStreams(byte[] data, int offset, int length, List<int[]> streamBuffers, int[] read) {
		int currentLeft, currentRight, bufferIndex = 0;
		boolean bytesRead = false;
		for (int index = offset; index < length; index += 4) {
			// New temporal step so initialization
			currentLeft = 0;
			currentRight = 0;
			// Summing the value from each stream
			for (int[] buffer : streamBuffers) {
				try {
					currentLeft += buffer[bufferIndex] * globalVolume;
					currentRight += buffer[bufferIndex + 1] * globalVolume;
					bytesRead = true;
				} catch (IndexOutOfBoundsException e) {
					// Exception thrown when there was not enough registered bytes in the stream.
					// No need to keep reading the current buffer.
					continue;
				}
			}

			// If no bytes has been read, then no need to go further.
			if (!bytesRead)
				return false;

			// Clipping
			currentLeft = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, currentLeft));
			currentRight = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, currentRight));

			// left channel bytes
			data[index + 1] = (byte) ((currentLeft >> 8) & 0xFF); // MSB
			data[index] = (byte) (currentLeft & 0xFF); // LSB
			// then right channel bytes
			data[index + 3] = (byte) ((currentRight >> 8) & 0xFF); // MSB
			data[index + 2] = (byte) (currentRight & 0xFF); // LSB

			// Updating the number of read bytes
			read[0] += 4;

			bufferIndex += 2;
		}

		return true;
	}

	/**
	 * Read n bytes with n equals <code>length</code> and update the given <code>data</code> array. This method does not blocks. It is
	 * assumed that calling this methods means there are registered streams and at least one stream contains data to read.
	 * 
	 * @param data   The buffer to read the bytes into.
	 * @param offset The start index to read bytes into.
	 * @param length The maximum number of bytes that should be read.
	 * 
	 * @return The number of bytes read into buffer.
	 */
	private int readAndMergeStreams(byte[] data, int offset, int length) {
		List<int[]> streamBuffers = readStreams(length);
		int[] read = new int[1];
		mergeStreams(data, offset, length, streamBuffers, read);
		return read[0];
	}

	/**
	 * Sleep until the condition <code>empty</code> is notified.
	 * 
	 * @return true If an {@link InterruptedException} is thrown while waiting, false otherwise.
	 */
	private boolean sleep() {
		lock.lock();
		try {
			notEmpty.await();
			return false;
		} catch (InterruptedException e) {
			return true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Thread safe operation in order to get the stream associated to the given key.
	 * 
	 * @param key The key used to get the associated audio stream.
	 * 
	 * @return The audio stream if registered, null otherwise.
	 */
	private AudioStream getStream(String key) {
		lock.lock();
		try {
			return streams.get(key);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Register the given audio stream with the specified key.
	 * 
	 * @param key    The key used to retrieve the audio stream.
	 * @param stream The stream associated to the key.
	 */
	private void putStream(String key, AudioStream stream) {
		lock.lock();
		try {
			streams.put(key, stream);
		} finally {
			lock.unlock();
		}
	}
}