package fr.pederobien.sound.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.utils.event.IEventListener;

public class Mixer implements IMixer, IEventListener {
	private static final int BUFFERED_SAMPLES_SIZE = 5;
	private static final int EMPTY_CALL_THRESHOLD = 20;

	private Map<String, AudioStream> streams;
	private double globalVolume;
	private Lock lock;
	private Condition isEmpty;
	private int currentEmptyCall;

	public Mixer() {
		streams = new HashMap<String, AudioStream>();
		globalVolume = 1.0;
		lock = new ReentrantLock(true);
		isEmpty = lock.newCondition();
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
		lock.lock();
		try {
			AudioStream stream = streams.get(packet.getKey());
			if (stream == null)
				streams.put(packet.getKey(), stream = new AudioStream(packet.getKey()));

			stream.extract(packet);
			if (stream.size() > BUFFERED_SAMPLES_SIZE)
				signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean renameStream(String oldKey, String newKey) {
		lock.lock();
		try {
			AudioStream stream = streams.remove(oldKey);
			if (stream == null)
				return false;

			stream.setKey(newKey);
			streams.put(stream.getKey(), stream);
			return true;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean setStreamVolume(String key, float volume) {
		AudioStream stream = null;
		lock.lock();
		try {
			stream = streams.get(key);
		} finally {
			lock.unlock();
		}

		if (stream == null)
			return false;

		stream.setVolume(volume);
		return true;
	}

	@Override
	public void clear() {
		lock.lock();
		try {
			for (Map.Entry<String, AudioStream> entry : streams.entrySet())
				entry.getValue().clear();

			streams.clear();
		} finally {
			lock.unlock();
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
		int readBytes = mergeStreams(data, offset, length, readStreams(length));
		if (readBytes == 0) {
			currentEmptyCall++;
			if (currentEmptyCall == EMPTY_CALL_THRESHOLD) {
				await();
				currentEmptyCall = 0;
			}
		} else
			currentEmptyCall = 0;

		return readBytes;
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
	 * 
	 * @return the number of read bytes
	 */
	private int mergeStreams(byte[] data, int offset, int length, List<int[]> streamBuffers) {
		int currentLeft, currentRight, bufferIndex = 0;
		boolean bytesRead = false;
		int readBytes = 0;
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
				return 0;

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
			readBytes += 4;

			bufferIndex += 2;
		}

		return readBytes;
	}

	/**
	 * Wait for streams to be read.
	 */
	private void await() {
		lock.lock();
		try {
			isEmpty.await();
		} catch (InterruptedException e) {

		} finally {
			lock.unlock();
		}
	}

	/**
	 * Signal that there are streams to be read.
	 */
	private void signal() {
		lock.lock();
		try {
			isEmpty.signal();
		} finally {
			lock.unlock();
		}
	}
}