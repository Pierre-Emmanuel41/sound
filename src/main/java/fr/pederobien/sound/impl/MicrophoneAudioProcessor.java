package fr.pederobien.sound.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fr.pederobien.utils.Disposable;
import fr.pederobien.utils.IDisposable;

public class MicrophoneAudioProcessor {
	/**
	 * 1s of sample to improve audio quality
	 */
	private static final int MIN_MIC_STREAM_SIZE_FOR_ANALYSIS = 44100;

	/**
	 * Maximum number of bytes to return by the fetch method.
	 */
	private static final int OUTPUT_SAMPLE_SIZE = 8820;
	private final Lock lock;
	private final MicrophoneStream microphoneStream;
	private final Queue<Short> processedQueue;
	private final Condition notEmpty;

	private IDisposable disposable;
	private Thread cleaner;

	public MicrophoneAudioProcessor() {
		lock = new ReentrantLock(true);
		microphoneStream = new MicrophoneStream(lock);
		processedQueue = new ArrayDeque<Short>(10000);
		notEmpty = lock.newCondition();

		disposable = new Disposable();
		cleaner = new Thread(this::clean, "MicrophoneCleaner");
	}

	/**
	 * Starts the thread that cleans raw microphone audio stream.
	 */
	public void initialize() {
		cleaner.setDaemon(true);
		cleaner.start();
	}

	/**
	 * Register a bytes array that contains raw data retrieved from the OS microphone.
	 * 
	 * @param raw The byte array that contains raw data.
	 */
	public void register(byte[] raw) {
		microphoneStream.register(raw);
	}

	/**
	 * @return A bytes array that contains cleaned audio sample from the microphone.
	 */
	public byte[] fetch() {
		if (processedQueue.isEmpty())
			waitNotEmpty();

		return getBytesFromQueue(processedQueue, OUTPUT_SAMPLE_SIZE);
	}

	/**
	 * Stops the thread that improves the microphone audio quality.
	 */
	public void dispose() {
		// Value checked by the cleaner thread
		disposable.dispose();
	}

	/**
	 * Get n bytes from the queue of shorts. If the queue does not contains length / 2 shorts then the returned bytes array will be
	 * shorter than the specified length.
	 * 
	 * @param queue  The that contains short to extract as bytes.
	 * @param length The number of bytes to extract from the queue.
	 * @return An array containing the value from the given queue but as byte.
	 */
	private byte[] getBytesFromQueue(Queue<Short> queue, int length) {
		int queueSizeInBytes = queue.size() * 2;
		int size = queueSizeInBytes < length ? queueSizeInBytes : length;
		byte[] bytes = new byte[size];
		int index = 0;

		int max = size / 2;

		// To avoid concurrent modification
		synchronized (lock) {
			for (int i = 0; i < max; i++) {
				Short value = queue.poll();

				// Little-Endian format
				bytes[index] = (byte) (value & 0xFF); // LSB
				bytes[index + 1] = (byte) ((value >> 8) & 0xFF); // MSB
				index += 2;
			}
		}

		return bytes;
	}

	/**
	 * Clean the raw stream of the microphone.
	 */
	private void clean() {
		int shortToRead = OUTPUT_SAMPLE_SIZE / 2;

		while (true) {

			// Case 1: Not enough data to perform analysis
			if (microphoneStream.size() < MIN_MIC_STREAM_SIZE_FOR_ANALYSIS) {
				List<Short> read = microphoneStream.read(shortToRead);

				synchronized (lock) {
					processedQueue.addAll(read);
				}

				notifyNotEmpty();
			}

			// Perform analysis
			else {
				// TODO: Implementation of analysis
				List<Short> read = microphoneStream.read(shortToRead);

				synchronized (lock) {
					processedQueue.addAll(read);
				}

				microphoneStream.resize();

				notifyNotEmpty();
			}

			// API has been closed
			if (disposable.isDisposed())
				return;
		}
	}

	/**
	 * Notify that the queue that contains raw audio sample from the microphone has been filled.
	 */
	private void notifyNotEmpty() {
		try {
			lock.lock();
			notEmpty.signal();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Notify that the queue that contains raw audio sample from the microphone has been filled.
	 */
	private void waitNotEmpty() {
		try {
			lock.lock();
			notEmpty.await();
		} catch (Exception e) {
			// Do nothing
		} finally {
			lock.unlock();
		}
	}

	private class MicrophoneStream {
		private final Lock lock;
		private final Condition notEmpty;
		private final List<Short> data;
		private int head;

		/**
		 * Creates a stream dedicated for the microphone.
		 */
		public MicrophoneStream(Lock lock) {
			this.lock = lock;
			notEmpty = lock.newCondition();

			data = new ArrayList<Short>();
			head = 0;
		}

		/**
		 * Register a bytes array that contains raw data retrieved from the OC microphone.
		 * 
		 * @param raw The byte array that contains raw data.
		 */
		public void register(byte[] raw) {
			synchronized (lock) {
				for (int i = 0; i < raw.length - 1; i += 2)
					data.add((short) ((raw[i + 1] & 0xFF) << 8 | (raw[i] & 0xFF)));
			}

			// Notifying the cleaner thread if it is waiting
			signalNotEmpty();
		}

		/**
		 * Read n short from the underlying stream and increase the head for the next read call. Blocks until there is short to read. Read
		 * data are not removed from the underlying stream.
		 * 
		 * @param length The number of short from the stream to read.
		 * @return A list containing up to length shorts.
		 */
		public List<Short> read(int length) {
			if (data.isEmpty() || head == data.size())
				waitNotEmpty();

			int remaining = data.size() - head;
			int size = remaining < length ? remaining : length;
			List<Short> read = new ArrayList<Short>();

			synchronized (lock) {
				for (int i = 0; i < size; i++)
					read.add(data.get(head + i));

				head += size;
			}

			return read;
		}

		/**
		 * Resize the underlying stream to keep only 1s of stream.
		 */
		public void resize() {
			int difference = size() - MIN_MIC_STREAM_SIZE_FOR_ANALYSIS;
			if (difference <= 0)
				return;

			synchronized (lock) {
				for (int i = 0; i < difference; i++)
					data.remove(0);

				head -= difference;
			}
		}

		/**
		 * @return The number of shorts in the underlying stream.
		 */
		public int size() {
			return data.size();
		}

		/**
		 * Notify that the queue that contains raw audio sample from the microphone has been filled.
		 */
		private void signalNotEmpty() {
			try {
				lock.lock();
				notEmpty.signal();
			} finally {
				lock.unlock();
			}
		}

		/**
		 * Notify that the queue that contains raw audio sample from the microphone has been filled.
		 */
		private void waitNotEmpty() {
			try {
				lock.lock();
				notEmpty.await();
			} catch (Exception e) {
				// Do nothing
			} finally {
				lock.unlock();
			}
		}
	}
}
