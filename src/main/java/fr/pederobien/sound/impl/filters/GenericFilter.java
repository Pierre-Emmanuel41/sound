package fr.pederobien.sound.impl.filters;

import javax.sound.sampled.AudioFormat;

import fr.pederobien.sound.impl.Buffer;
import fr.pederobien.sound.interfaces.IFilter;
import fr.pederobien.utils.Disposable;
import fr.pederobien.utils.IDisposable;
import fr.pederobien.utils.event.Logger;

public class GenericFilter {
	/**
	 * Filtering 20ms in one go.
	 */
	private static final int SIZE_IN_MS = 100;

	private final int bufferSize;
	private IFilter impl;
	private Buffer input;
	private Buffer ouput;
	private IDisposable disposable;
	private Thread filterThread;
	private boolean isEnabled;

	/**
	 * Creates a generic filter. If there is no implementation defined, the filter does not apply any modification on the raw
	 * microphone output.
	 * 
	 * @param format The microphone's audio format.
	 */
	public GenericFilter(AudioFormat format) {
		float sampleRate = format.getSampleRate();
		int bitDepth = format.getSampleSizeInBits();
		int channels = format.getChannels();
		bufferSize = (int) ((sampleRate * (bitDepth / 16) * channels * (SIZE_IN_MS / 1000.0)));

		// Create streams and start cleaner thread
		impl = new NoFilter();
		input = new Buffer(true);
		ouput = new Buffer(true);
		disposable = new Disposable();

		filterThread = new Thread(this::filter, "MicrophoneFilter");
		filterThread.setDaemon(true);
		filterThread.start();

		isEnabled = false;
	}

	/**
	 * Set the implementation of this generic filter. If the filter is null, then not filter is applied.
	 * 
	 * @param impl The implementation to use to filter microphone's audio stream.
	 */
	public void setImpl(IFilter impl) {
		if (this.impl == impl)
			return;

		this.impl = impl == null ? new NoFilter() : impl;
	}

	/**
	 * Write a bytes array that contains a raw audio sample retrieved from the OS microphone.
	 * 
	 * @param buffer The bytes array containing the audio sample.
	 */
	public void write(byte[] buffer) {
		input.write(buffer);
	}

	/**
	 * Update the given bytes array with clean audio samples.
	 * 
	 * @param data The buffer to update.
	 * @return The number of bytes written in the given buffer, -1 if writing failed.
	 */
	public int read(byte[] data) {
		short[] values = new short[data.length / 2];
		int written = ouput.read(values, Math.min(data.length / 2, values.length));
		int index = 0;

		// Converting short to byte
		for (int i = 0; i < written; i++) {
			data[index++] = (byte) (values[i] & 0xFF);
			data[index++] = (byte) ((values[i] >> 8) & 0xFF);
		}

		return index;
	}

	public void setEnabled(boolean isEnabled) {
		if (this.isEnabled == isEnabled)
			return;

		this.isEnabled = isEnabled;
		debug("Filter %s", isEnabled ? "Enabled" : "Disabled");
	}

	public void dispose() {
		if (!disposable.dispose())
			return;

		try {
			// Force the cleaner thread to exit the loop
			input.dispose();

			// Waiting for the cleaner thread to finish the last execution
			filterThread.join(100);

			// Force the thread waiting for processed data to exit the loop
			ouput.dispose();

			// Garbage Collector
			filterThread = null;
			input = null;
			ouput = null;
		} catch (InterruptedException e) {
			// Do nothing
		}
	}

	/**
	 * Infinite loop, until thread is interrupted, to apply asynchronously filters on the raw microphone stream.
	 */
	private void filter() {
		while (!Thread.currentThread().isInterrupted()) {
			short[] values = new short[bufferSize];

			// Reading short values from rawStream
			int read = input.read(values, values.length);

			// Interrupted thread, no need to go further
			if (read == -1)
				break;

			if (read != values.length) {
				short[] val = new short[read];
				System.arraycopy(values, 0, val, 0, read);
				values = val;
			}

			if (isEnabled)
				impl.apply(values);

			ouput.write(values, read);
		}
	}

	private void debug(String format, Object... args) {
		Logger.debug(1, "[%s] - %s", impl.getName(), String.format(format, args));
	}
}
