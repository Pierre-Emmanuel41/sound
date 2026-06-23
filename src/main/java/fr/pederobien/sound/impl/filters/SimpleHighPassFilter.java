package fr.pederobien.sound.impl.filters;

import fr.pederobien.sound.interfaces.IFilter;
import fr.pederobien.utils.Disposable;
import fr.pederobien.utils.IDisposable;
import fr.pederobien.utils.event.Logger;

public class SimpleHighPassFilter implements IFilter {
	/**
	 * Value used to normalize the raw stream values.
	 */
	private static final double SHORT_MAX_VALUE = (double) Short.MAX_VALUE;

	/**
	 * Maximum number of bytes to filter in one iteration.
	 */
	private static final int BUFFER_SIZE = 8820;

	private final short[] rawBufferWrite;
	private final short[] rawBufferRead;
	private final short[] filterBufferWrite;
	private final short[] filterBufferRead;
	private double alpha;
	private MicrophoneStream rawStream;
	private MicrophoneStream filteredStream;
	private double previousOutput;
	private double previousInput;
	private boolean isEnabled;
	private IDisposable disposable;
	private Thread cleaner;

	/**
	 * Creates a simple high pass filter that asynchronously filters the microphone stream.
	 * 
	 * @param cutoffFrequency The cutoff frequency, the lowest frequency to keep.
	 * @param sampleRate      The rate at which the OS is sampling the microphone stream.
	 */
	public SimpleHighPassFilter(double cutoffFrequency, double sampleRate) {
		rawBufferWrite = new short[BUFFER_SIZE];
		rawBufferRead = new short[BUFFER_SIZE];
		filterBufferWrite = new short[BUFFER_SIZE];
		filterBufferRead = new short[BUFFER_SIZE];

		// Initialize filter state variables
		previousOutput = 0.0;
		previousInput = 0.0;

		// Calculate filter coefficients
		alpha = Math.exp(-2.0 * Math.PI * cutoffFrequency / sampleRate);

		// Create streams and start cleaner thread
		rawStream = new MicrophoneStream();
		filteredStream = new MicrophoneStream();
		disposable = new Disposable();

		cleaner = new Thread(this::clean, "SimpleHighPassFilter");
		cleaner.setDaemon(true);
		cleaner.start();

		isEnabled = false;
	}

	@Override
	public void write(byte[] buffer) {
		int index = 0;

		// Converting bytes array to short array
		for (int i = 0; i < buffer.length; i += 2)
			rawBufferWrite[index++] = (short) ((buffer[i + 1] & 0xFF) << 8 | (buffer[i] & 0xFF));

		rawStream.write(rawBufferWrite, index);
	}

	@Override
	public int read(byte[] data) {
		int written = filteredStream.read(filterBufferRead, Math.min(data.length / 2, filterBufferRead.length));
		int index = 0;

		// Converting short to byte
		for (int i = 0; i < written; i++) {
			data[index++] = (byte) (filterBufferRead[i] & 0xFF);
			data[index++] = (byte) ((filterBufferRead[i] >> 8) & 0xFF);
		}

		return index;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		if (this.isEnabled == isEnabled)
			return;

		this.isEnabled = isEnabled;
		debug("Filter %s", isEnabled ? "Enabled" : "Disabled");
	}

	@Override
	public void dispose() {
		if (!disposable.dispose())
			return;

		try {
			// Force the cleaner thread to exit the loop
			rawStream.dispose();

			// Waiting for the cleaner thread to finish the last execution
			cleaner.join(100);

			// Force the thread waiting for processed data to exit the loop
			filteredStream.dispose();

			// Garbage Collector
			cleaner = null;
			rawStream = null;
			filteredStream = null;
		} catch (InterruptedException e) {
			// Do nothing
		}
	}

	private void clean() {
		while (!Thread.currentThread().isInterrupted()) {

			// Reading short values from rawStream
			int read = rawStream.read(rawBufferRead, rawBufferRead.length);

			// Interrupted thread, no need to go further
			if (read == -1)
				break;

			filter(rawBufferRead, read);
		}
	}

	/**
	 * Applies the filter on each byte of the given buffer, if enabled. Otherwise fills the filtered stream with the content of the
	 * given buffer.
	 * 
	 * @param read The buffer on which the filter shall be applied.
	 */
	private void filter(short[] read, int length) {
		if (!isEnabled)
			filteredStream.write(read, length);

		else {
			for (int i = 0; i < length; i++) {
				double normalized = read[i] / SHORT_MAX_VALUE;

				// Applying filter
				previousOutput = alpha * (previousOutput + normalized - previousInput);

				// Clipping
				double filtered = Math.max(-1.0, Math.min(1.0, previousOutput));
				filterBufferWrite[i] = (short) (filtered * SHORT_MAX_VALUE);

				// Updating State Variables FOR THE NEXT SAMPLE
				previousInput = normalized;
			}

			filteredStream.write(filterBufferWrite, length);
		}
	}

	private void debug(String format, Object... args) {
		Logger.debug(1, "[SimpleHighPassFilter] - %s", String.format(format, args));
	}
}
