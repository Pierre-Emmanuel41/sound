package fr.pederobien.sound.impl.filters;

import fr.pederobien.sound.interfaces.IFilter;

public class SimpleBandPassFilter implements IFilter {
	private double alphaHighPass;
	private double alphaLowPass;
	private double previousLowPassOutput;
	private double previousHighPassOutput;
	private double previousHighPassInput;

	/**
	 * Creates a band pass filter, composed of one first order low/high pass filter.
	 * 
	 * @param cutoffHighPassFrequency The lowest frequency to accept.
	 * @param cutoffLowPassFrequency  The highest frequency to accept.
	 * @param sampleRate              The audio sample rate.
	 */
	public SimpleBandPassFilter(double cutoffHighPassFrequency, double cutoffLowPassFrequency, double sampleRate) {
		// Initialize filter state variables
		previousHighPassOutput = 0.0;
		previousHighPassInput = 0.0;
		previousLowPassOutput = 0.0;

		// Calculate filter coefficients
		alphaHighPass = Math.exp(-2.0 * Math.PI * cutoffHighPassFrequency / sampleRate);
		alphaLowPass = 1.0 - Math.exp(-2.0 * Math.PI * cutoffLowPassFrequency / sampleRate);
	}

	@Override
	public void apply(short[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			double normalized = buffer[i] / SHORT_MAX_VALUE;

			// 2. Apply High-Pass Filter
			// Formula: y[n] = alpha * (y[n-1] + x[n] - x[n-1])
			double highPassOutput = alphaHighPass * (previousHighPassOutput + normalized - previousHighPassInput);

			// 3. Apply Low-Pass Filter (on the High-Pass result)
			// Formula: y[n] = y[n-1] + alpha * (x[n] - y[n-1])
			double lowPassOutput = previousLowPassOutput + alphaLowPass * (highPassOutput - previousLowPassOutput);

			// 4. Clip
			double filtered = Math.max(-1.0, Math.min(1.0, lowPassOutput));
			buffer[i] = (short) (filtered * SHORT_MAX_VALUE);

			// 5. Update State Variables FOR THE NEXT SAMPLE
			previousHighPassOutput = highPassOutput;
			previousHighPassInput = normalized; // Store current input as "previous" for next loop
			previousLowPassOutput = lowPassOutput;
		}
	}
}
