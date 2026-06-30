package fr.pederobien.sound.impl.filters;

import fr.pederobien.sound.interfaces.IFilter;

public class BiquadBandPassFilter implements IFilter {
	private final double b0, b1, b2, a1, a2;
	private double in1 = 0, in2 = 0;
	private double out1 = 0, out2 = 0;

	/**
	 * Creates a 2nd order filter.
	 * 
	 * @param frequency     The frequency used as center to filter microphone's stream.
	 * @param sampleRate    The sample rate used to fetch microphone's audio stream.
	 * @param qualityFactor The factor that determines the "sharpness" or bandwidth of the filter. The value should be in range [0.5,
	 *                      2] but it is recommended to use 0.707.
	 */
	public BiquadBandPassFilter(double frequency, double sampleRate, double qualityFactor) {
		// Initialize filter state variables
		in1 = 0;
		in2 = 0;
		out1 = 0;
		out2 = 0;

		// Pre-calculate normalized coefficients
		double omega = 2.0 * Math.PI * frequency / sampleRate;
		double sinW = Math.sin(omega);
		double cosW = Math.cos(omega);
		double alpha = sinW / (2.0 * qualityFactor);
		double a0 = 1.0 + alpha;

		b0 = alpha / a0;
		b1 = 0;
		b2 = -alpha / a0;
		a1 = -2.0 * cosW / a0;
		a2 = (1.0 - alpha) / a0;
	}

	@Override
	public void apply(short[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			double normalized = buffer[i] / SHORT_MAX_VALUE;

			// 1. Apply Filter
			// Formula: y[n]=(b0​ * x[n] + b1 * x[n-1] + b2 * x[n-2]​) - (a1 * y[n-1 + a2 * y[n-2]])
			double filtered = b0 * normalized + b1 * in1 + b2 * in2 - a1 * out1 - a2 * out2;

			// 2. Clip
			double clipped = Math.max(-1.0, Math.min(1.0, filtered));
			buffer[i] = (short) (clipped * SHORT_MAX_VALUE);

			// 3. Update State Variables FOR THE NEXT SAMPLE
			in2 = in1;
			in1 = normalized;
			out2 = out1;
			out1 = filtered;
		}
	}
}
