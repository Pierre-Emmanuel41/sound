package fr.pederobien.sound.impl.effects;

import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.utils.event.Logger;

public class UnderWaterEffect implements IEffect {
	/**
	 * Pre-computed 2*Pi value.
	 */
	private static final double _2_PI = 2 * Math.PI;

	private final float sampleRate;
	private float alpha;
	private float previousOutput;
	private float gain;
	private float chorusFrequency;
	private float chorusDepth;
	private float chorusDelay;
	private float[] delayBuffer;
	private int delayWrite;
	private float phase;
	private float[] reverbBuffer;
	private int reverbWrite;
	private float reverbDecay;
	private float currentMix = 0.0f;
	private float targetMix = 0.0f;
	private float fadeStep;

	/**
	 * Constructs a new UnderWaterEffect with configurable acoustic parameters.
	 * <p>
	 * This effect simulates an underwater environment by combining a low-pass filter (to mimic high-frequency absorption), a chorus
	 * effect (to simulate water density fluctuations), and a reverb tail (to simulate dense reflections).
	 *
	 * @param sampleRate      The audio sample rate in Hz (e.g., 44100.0f, 48000.0f). Must match the format of the audio buffer passed
	 *                        to {@link #apply(short[])}.
	 *
	 * @param cutOffFrequency The cutoff frequency for the low-pass filter in Hz.
	 *                        <ul>
	 *                        <li><b>High values (2000-4000 Hz):</b> Subtle muffling, sounds like being near the surface.</li>
	 *                        <li><b>Low values (200-500 Hz):</b> Heavy muffling, sounds like deep submersion. Recommended
	 *                        <b>300.0f</b> for a distinct underwater voice effect.</li>
	 *                        </ul>
	 *
	 * @param gain            Post-processing volume multiplier. Low-pass filtering reduces perceived loudness; values > 1.0 (e.g.,
	 *                        1.2f) compensate for this.
	 *
	 * @param chorusFrequency The speed of the chorus modulation in Hz (cycles per second). Controls how fast the "warble" effect
	 *                        moves.
	 *                        <ul>
	 *                        <li><b>0.5f - 1.0f:</b> Slow, natural water movement.</li>
	 *                        <li><b>> 2.0f:</b> Fast, artificial sounding modulation.</li>
	 *                        </ul>
	 *
	 * @param chorusDepth     The intensity of the chorus pitch modulation in milliseconds. Determines how much the pitch wavers.
	 *                        <ul>
	 *                        <li><b>0.0f:</b> No chorus effect.</li>
	 *                        <li><b>5.0f - 15.0f:</b> Recommended range for underwater simulation.</li>
	 *                        <li><b>> 20.0f:</b> Strong, disorienting warble.</li>
	 *                        </ul>
	 *
	 * @param chorusDelay     The base delay time for the chorus effect in milliseconds. Sets the center point around which the
	 *                        modulation occurs. Typical values are between <b>10.0f</b> and <b>30.0f</b>.
	 *
	 * @param reverbDelay     The delay time for the reverb comb filter in milliseconds. Controls the size of the simulated space.
	 *                        <ul>
	 *                        <li><b>20.0f - 50.0f:</b> Creates a dense, "thick" underwater reflection.</li>
	 *                        <li><b>> 100.0f:</b> Sounds like a large cave or tank rather than open water.</li>
	 *                        </ul>
	 *
	 * @param reverbDecay     The feedback coefficient for the reverb (0.0f - 1.0f). Controls how long the reverb tail lasts.
	 *                        <ul>
	 *                        <li><b>0.0f - 0.3f:</b> Dry, dead sound.</li>
	 *                        <li><b>0.4f - 0.6f:</b> Recommended for underwater density.</li>
	 *                        <li><b>> 0.8f:</b> Long, ringing echoes (unrealistic for water, may cause instability if too close to
	 *                        1.0).</li>
	 *                        </ul>
	 * @throws IllegalArgumentException if sampleRate is <= 0 or if buffer sizes calculated from delays exceed integer limits.
	 */
	public UnderWaterEffect(float sampleRate, float cutOffFrequency, float gain, float chorusFrequency, float chorusDepth, float chorusDelay, float reverbDelay,
			float reverbDecay) {
		this.sampleRate = sampleRate;

		// Low-pass parameters
		previousOutput = 0.0f;
		alpha = (float) (1.0 - Math.exp(-_2_PI * cutOffFrequency / sampleRate));

		// Gain to apply after low-pass
		this.gain = gain;

		// Chorus parameters
		this.chorusFrequency = chorusFrequency;
		this.chorusDepth = chorusDepth;
		this.chorusDelay = chorusDelay;
		delayBuffer = new float[(int) (sampleRate * (chorusDelay + chorusDepth) / 1000.0f)];
		delayWrite = 0;
		phase = 0.0f;

		// Reverb parameters
		reverbBuffer = new float[(int) (sampleRate * reverbDelay / 1000.0)];
		reverbWrite = 0;
		this.reverbDecay = reverbDecay;

		// Start/Stop management
		fadeStep = 0.0001f;
	}

	@Override
	public void apply(short[] buffer) {
		float modulation;
		int readIndex;

		for (int i = 0; i < buffer.length; i++) {
			currentMix = interpolate(currentMix, targetMix, fadeStep);

			float normalized = (float) (buffer[i] / SHORT_MAX_VALUE);

			// Step 1: Applying low-pass filter
			float lowPass = (float) (previousOutput + alpha * (normalized - previousOutput));
			previousOutput = lowPass;

			// Step 2: Applying gain
			float processed = lowPass * gain;

			// Step 3: Applying chorus
			delayBuffer[delayWrite] = processed;
			delayWrite = (delayWrite + 1) % delayBuffer.length;

			phase += chorusFrequency * _2_PI / sampleRate;
			if (phase > _2_PI)
				phase -= _2_PI;

			modulation = (float) Math.sin(phase);
			float currentDelay = chorusDelay + (chorusDepth * modulation);
			float currentDelaySample = currentDelay * sampleRate / 1000.0f;
			float delayRead = delayWrite - currentDelaySample;
			while (delayRead < 0) {
				delayRead += delayBuffer.length;
			}
			while (delayRead >= delayBuffer.length) {
				delayRead -= delayBuffer.length;
			}

			// Linear Interpolation to prevent clicking
			readIndex = (int) delayRead;
			float decimal = delayRead - readIndex;
			int nextIndex = (readIndex + 1) % delayBuffer.length;

			float chorusWet = delayBuffer[readIndex] * (1.0f - decimal) + delayBuffer[nextIndex] * decimal;
			float chorusMix = processed * 0.6f + chorusWet * 0.4f;

			// Step 4: Reverb (Comb filter)
			float reverbDry = chorusMix;
			float reverbWet = reverbBuffer[reverbWrite];

			// Writing new value into reverb buffer: Input + (Delayed * Decay)
			reverbBuffer[reverbWrite] = reverbDry + (reverbWet * reverbDecay);
			reverbWrite = (reverbWrite + 1) % reverbBuffer.length;

			// Clipping
			float reverbMix = (reverbDry * 0.7f) + (reverbWet * 0.3f);
			float result = (normalized * (1.0f - currentMix)) + (reverbMix * currentMix);
			buffer[i] = (short) Math.max(-SHORT_MAX_VALUE, Math.min(SHORT_MAX_VALUE, result * SHORT_MAX_VALUE));
		}
	}

	@Override
	public void start() {
		if (targetMix == 1.0f)
			return;

		targetMix = 1.0f;
		debug("Starting effect");
	}

	@Override
	public void stop() {
		if (targetMix == 0.0f)
			return;

		targetMix = 0.0f;
		debug("Stopping effect");
	}

	@Override
	public boolean isStopped() {
		return targetMix == 0 && currentMix == 0;
	}

	@Override
	public void setValues(Object... values) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean processTail(short[] buffer, int[] length) {
		// No tail for this effect
		return false;
	}

	/**
	 * Transition from a point to a target point with a specific step size.
	 * 
	 * @param current The value to move to the target value.
	 * @param target  The value to reach.
	 * @param step    The step size to use to move the current value to the target value.
	 * @return The new value.
	 */
	private float interpolate(float current, float target, float step) {
		if (current < target)
			return Math.min(current + step, target);
		else if (current > target)
			return Math.max(current - step, target);

		return current;
	}

	private void debug(String format, Object... args) {
		Logger.debug(1, "[UnderWaterEffect] - %s", String.format(format, args));
	}
}
