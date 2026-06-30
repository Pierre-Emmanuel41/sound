package fr.pederobien.sound.impl.filters;

import fr.pederobien.sound.interfaces.IFilter;

public class SimpleLowPassFilter implements IFilter {
	private double alpha;
	private Double previousOutput;

	/**
	 * Creates a simple low pass filter that asynchronously filters the microphone
	 * stream.
	 * 
	 * @param cutoffFrequency The cutoff frequency, the highest frequency to keep.
	 * @param sampleRate      The rate at which the OS is sampling the microphone
	 *                        stream.
	 */
	public SimpleLowPassFilter(double cutoffFrequency, double sampleRate) {
		// Initialize filter state variables
		previousOutput = 0.0;

		// Calculate filter coefficients
		alpha = 1.0 - Math.exp(-2.0 * Math.PI * cutoffFrequency / sampleRate);
	}

	@Override
	public void apply(short[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			double normalized = buffer[i] / SHORT_MAX_VALUE;

			// Applying filter
			previousOutput = previousOutput + alpha * (normalized - previousOutput);

			// Clipping
			double filtered = Math.max(-1.0, Math.min(1.0, previousOutput));
			buffer[i] = (short) (filtered * SHORT_MAX_VALUE);
		}
	}
}
