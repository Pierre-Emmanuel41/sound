package fr.pederobien.sound.impl.filters;

import fr.pederobien.sound.interfaces.IFilter;

public class SimpleHighPassFilter implements IFilter {
	private double alpha;
	private double previousOutput;
	private double previousInput;

	/**
	 * Creates a simple high pass filter that asynchronously filters the microphone
	 * stream.
	 * 
	 * @param cutoffFrequency The cutoff frequency, the lowest frequency to keep.
	 * @param sampleRate      The rate at which the OS is sampling the microphone
	 *                        stream.
	 */
	public SimpleHighPassFilter(double cutoffFrequency, double sampleRate) {
		// Initialize filter state variables
		previousOutput = 0.0;
		previousInput = 0.0;

		// Calculate filter coefficients
		alpha = Math.exp(-2.0 * Math.PI * cutoffFrequency / sampleRate);
	}

	@Override
	public void apply(short[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			double normalized = buffer[i] / SHORT_MAX_VALUE;

			// Applying filter
			previousOutput = alpha * (previousOutput + normalized - previousInput);

			// Clipping
			double filtered = Math.max(-1.0, Math.min(1.0, previousOutput));
			buffer[i] = (short) (filtered * SHORT_MAX_VALUE);

			// Updating State Variables FOR THE NEXT SAMPLE
			previousInput = normalized;
		}
	}
}
