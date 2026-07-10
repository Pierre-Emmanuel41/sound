package fr.pederobien.sound.impl.filters;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fr.pederobien.utils.Disposable;
import fr.pederobien.utils.IDisposable;

public class MicrophoneStream {
	private final Lock lock;
	private final Condition notEmpty;
	private final IDisposable disposable;

	private final short[] stream;
	private final int capacity;
	private int head;
	private int tail;
	private int count;

	/**
	 * Creates a stream dedicated for the microphone.
	 */
	public MicrophoneStream() {
		this.lock = new ReentrantLock();
		notEmpty = lock.newCondition();
		disposable = new Disposable();

		capacity = 40000;
		stream = new short[capacity];

		head = 0;
		tail = 0;
		count = 0;
	}

	/**
	 * Write the given buffer in the underlying stream.
	 * 
	 * @param buffer The short array that contains data to write.
	 * @param length The number of short to write.
	 */
	public void write(short[] buffer, int length) {
		// Safety: Ensure we don't read past the end of the input buffer
		int samplesToWrite = Math.max(0, Math.min(length, buffer.length));

		if (samplesToWrite == 0)
			return;

		lock.lock();
		try {
			int availableSpace = capacity - count;

			// Case 1: Buffer has enough space for all new data
			if (samplesToWrite <= availableSpace) {
				if (head + samplesToWrite <= capacity) {
					// Simple contiguous write
					System.arraycopy(buffer, 0, stream, head, samplesToWrite);
					head = (head + samplesToWrite) % capacity;
				} else {
					// Wrap-around write
					int firstPart = capacity - head;
					System.arraycopy(buffer, 0, stream, head, firstPart);
					System.arraycopy(buffer, firstPart, stream, 0, samplesToWrite - firstPart);
					head = (samplesToWrite - firstPart);
				}
				count += samplesToWrite;
			}
			// Case 2: Buffer overflow (overwrite oldest data)
			else {
				// Calculate new tail position (discard oldest samples)
				int samplesToDiscard = samplesToWrite - availableSpace;
				tail = (tail + samplesToDiscard) % capacity;
				count = capacity; // Buffer is now full

				// Write new data (will overwrite from current head)
				if (head + samplesToWrite <= capacity) {
					System.arraycopy(buffer, samplesToDiscard, stream, head, availableSpace);
					head = (head + availableSpace) % capacity;
				} else {
					int firstPart = capacity - head;
					System.arraycopy(buffer, samplesToDiscard, stream, head, firstPart);
					System.arraycopy(buffer, samplesToDiscard + firstPart, stream, 0, availableSpace - firstPart);
					head = (availableSpace - firstPart);
				}
			}

			notEmpty.signal();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Removes n shorts, with n equals length, from the underlying stream and return the result.
	 * 
	 * @param buffer The array that contains read data.
	 * @param length The number of bytes to removes and get.
	 * @return The number of samples written in the given buffer.
	 */
	public int read(short[] buffer, int length) {
		lock.lock();

		try {
			// Waiting for data to be written
			if (count == 0) {
				try {
					notEmpty.await();

					if (disposable.isDisposed())
						return -1;

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return -1;
				}
			}

			int size = Math.min(length, count);

			// Handle wrap-around case
			if (tail + size <= capacity) {
				// Contiguous block: simple arraycopy
				System.arraycopy(stream, tail, buffer, 0, size);
			} else {
				// Wrap-around: copy in two parts
				int firstPart = capacity - tail;
				System.arraycopy(stream, tail, buffer, 0, firstPart);
				System.arraycopy(stream, 0, buffer, firstPart, size - firstPart);
			}

			// Update read position and count
			tail = (tail + size) % capacity;
			count -= size;

			return size;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Dispose this stream. If a thread was waiting for reading data, it will be awake and the read method will return -1.
	 */
	public void dispose() {
		if (!disposable.dispose())
			return;

		notEmpty.signal();
	}
}