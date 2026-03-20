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

public class MicrophoneDataCleaner {
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

	public MicrophoneDataCleaner() {
		lock = new ReentrantLock(true);
		microphoneStream = new MicrophoneStream();
		processedQueue = new ArrayDeque<Short>(10000);
		notEmpty = lock.newCondition();

		disposable = new Disposable();
		cleaner = new Thread(this::clean, "MicrophoneDataCleaner");
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
	 * Blocks until data are available to be sent to the remote. If the microphone is closed while waiting, the method shall return 1.
	 * 
	 * @param data The bytes array to update with the microphone audio stream.
	 * @return The number of bytes written in the array, 1 if an exception occurred while waiting.
	 */
	public int fetch(byte[] data) {
		// Queue is empty and an exception occurred while waiting
		if (processedQueue.isEmpty() && !waitNotEmpty())
			return -1;

		int queueSizeInBytes = processedQueue.size() * 2;
		int size = queueSizeInBytes < data.length ? queueSizeInBytes : data.length;
		int max = size / 2;
		int index = 0;

		// To avoid concurrent modification
		synchronized (lock) {
			for (int i = 0; i < max; i++) {
				Short value = processedQueue.poll();

				// LittleEndian format
				data[index] = (byte) (value & 0xFF); // LSB
				data[index + 1] = (byte) ((value >> 8) & 0xFF); // MSB
				index += 2;
			}
		}

		return size;
	}

	/**
	 * Stops the thread that improves the microphone audio quality.
	 */
	public void dispose() {
		// Value checked by the cleaner thread
		disposable.dispose();
	}

	/**
	 * Clean the raw stream of the microphone.
	 */
	private void clean() {
		int shortToRead = OUTPUT_SAMPLE_SIZE / 2;

		while (true) {
			// Case 1: Not enough data to perform analysis
			if (microphoneStream.size() < MIN_MIC_STREAM_SIZE_FOR_ANALYSIS) {
				write(microphoneStream.read(shortToRead));
			}

			// Perform analysis
			else {
				// TODO: Implementation of analysis
				write(microphoneStream.read(shortToRead));
				microphoneStream.resize();
			}

			// API has been closed
			if (disposable.isDisposed())
				return;
		}
	}

	/**
	 * Add all the short to the processed queue, notify the fetcher thread if it is waiting.
	 * 
	 * @param data The data to add to the processed queue.
	 */
	private void write(List<Short> data) {
		synchronized (lock) {
			processedQueue.addAll(data);
		}

		notifyNotEmpty();
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
	 * Notify that the queue that contains processed audio sample from the microphone has been filled.
	 * 
	 * @return True if the processed queue has been filled, false if an exception occurred.
	 */
	private boolean waitNotEmpty() {
		try {
			lock.lock();
			notEmpty.await();
			return true;
		} catch (Exception e) {
			// Do nothing
		} finally {
			lock.unlock();
		}

		return false;
	}

	private class MicrophoneStream {
		private final Lock lock;
		private final Condition notEmpty;
		private final List<Short> data;
		private int head;

		/**
		 * Creates a stream dedicated for the microphone.
		 */
		public MicrophoneStream() {
			this.lock = new ReentrantLock();
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
