package fr.pederobien.sound.impl.effects;

import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.utils.event.Logger;

public class EchoEffect implements IEffect {
	/**
	 * The maximum delay in ms of the echo.
	 */
	private static final int MAX_DELAY_MS = 2000;
	private final float sampleRate;
	private final short[] delayBuffer;
	private int bufferIndex;
	private int currentBufferLength;
	private int targetBufferLength;
	private float feedback;
	private float currentFeedback;
	private float targetFeedback;
	private float gain;
	private float currentGain;
	private float targetGain;
	private float fadeStep;
	private volatile boolean writing;

	/**
	 * Creates an echo effect. The feedback and gain parameters modifies directly how the echo is done:<br>
	 * <br>
	 * Feedback:<br>
	 * 0.0: No repeats. You hear only the first echo (controlled by Gain), then silence.<br>
	 * 0.1 -> 0.4: A quick decay (2–3 repeats). Good for small room simulations.<br>
	 * 0.5 -> 0.7: A standard echo (4–8 repeats). The volume halves roughly every repeat.<br>
	 * 0.8 -> 0.9: A long, trailing echo (many repeats).<br>
	 * 1.0: Infinite sustain. The echo repeats forever at the same volume.<br>
	 * > 1.0: Runaway Feedback. The signal amplifies exponentially on every loop quickly hitting the maximum limit (Short.MAX_VALUE)
	 * and creating loud digital noise/static.<br>
	 * <br>
	 * gain:<br>
	 * 0.0: No echo is heard (the delay line still works, but the output is muted).<br>
	 * 0.1 -> 0.5: A subtle, background echo.<br>
	 * 0.6 -> 0.9: A prominent, distinct echo.<br>
	 * 1.0: The first echo is as loud as the original sound.<br>
	 * > 1.0: The first echo is louder than the original (can cause immediate clipping if the original signal is already loud).<br>
	 * <br>
	 * Note: The setParameters method expects a EchoEffect.Parameter argument.<br>
	 * 
	 * @param sampleRate The sample rate of the audio stream.
	 * @param maxDelay   The maximum delay this effect allows.
	 * @param delay      The time, in ms, before repeating previous sample. It shall be in range [0, 2000].
	 * @param feedback   Controls how much of the delayed signal is sent back into the delay line to create subsequent repetitions. It
	 *                   determines the number of repeats and the decay rate.
	 * @param gain       Controls the volume of the first echo repetition relative to the original (dry) sound. It determines how loud
	 *                   the echo is when it first becomes audible.
	 */
	public EchoEffect(float sampleRate, int delay, float feedback, float gain) {
		delay = Math.max(0, Math.min(MAX_DELAY_MS, delay));
		this.sampleRate = sampleRate;

		delayBuffer = new short[(int) sampleRate * MAX_DELAY_MS / 1000];
		bufferIndex = 0;

		// Initialize delay
		int size = (int) (sampleRate * delay / 1000);
		this.currentBufferLength = size;
		this.targetBufferLength = size;

		this.feedback = feedback;
		currentGain = 0.0f;
		targetGain = 0.0f;

		this.gain = gain;
		currentFeedback = 0.0f;
		targetFeedback = 0.0f;

		fadeStep = 0.0001f;
		writing = false;
	}

	@Override
	public void start() {
		if (targetFeedback == feedback)
			return;

		targetFeedback = feedback;
		targetGain = gain;

		int delay = (int) (targetBufferLength * 1000 / sampleRate);
		debug("Starting effect (delay=%s, feedback=%s, gain=%s)", delay, targetFeedback, targetGain);
	}

	@Override
	public void stop() {
		if (targetFeedback == 0.0f)
			return;

		targetFeedback = 0.0f;
		targetGain = 0.0f;
		debug("Stopping effect");
	}

	@Override
	public boolean isStopped() {
		return targetFeedback == 0.0f && currentFeedback == targetFeedback;
	}

	@Override
	public void apply(short[] buffer) {
		if (isStopped())
			// Do nothing
			return;

		writing = true;
		for (int i = 0; i < buffer.length; i++) {
			// Step 1: Smoothly interpolating Gain and Feedback towards targets
			currentGain = interpolate(currentGain, targetGain, fadeStep);
			currentFeedback = interpolate(currentFeedback, targetFeedback, fadeStep);

			// Step 2: Smooth delay transition
			if (currentBufferLength < targetBufferLength)
				currentBufferLength++;
			else if (currentBufferLength > targetBufferLength)
				currentBufferLength--;

			// Step 3: Dynamic readIndex calculation, Read from (WriteIndex - Delay)
			// wrapping around the buffer
			int readIndex = bufferIndex - currentBufferLength;
			if (readIndex < 0)
				readIndex += delayBuffer.length;

			short currentSample = buffer[i];
			short delayedSample = delayBuffer[readIndex];

			// Step 4: Output = Current + Echo
			int mixedSample = currentSample + (int) (delayedSample * currentGain);

			// Step 5: Clamp to 16-bit range
			buffer[i] = (short) Math.max(-SHORT_MAX_VALUE, Math.min(SHORT_MAX_VALUE, mixedSample));

			// Step 6: Update delayBuffer with feedback
			// New Buffer Value = Current Dry Sample + (Delayed Sample * Feedback)
			// This creates the repeating effect.
			int feedbackSample = currentSample + (int) (delayedSample * currentFeedback);

			// Step 7: Clamp feedback value before storing to prevent buffer corruption
			delayBuffer[bufferIndex] = (short) Math.max(-SHORT_MAX_VALUE, Math.min(SHORT_MAX_VALUE, feedbackSample));

			// Step 8: Advance Circular Buffer
			bufferIndex = (bufferIndex + 1) % delayBuffer.length;
		}

		writing = false;
	}

	@Override
	public void setValues(Object... values) {
		int delay = (int) values[0];
		if (delay <= 0)
			targetBufferLength = 0;
		else if (MAX_DELAY_MS <= delay)
			targetBufferLength = delayBuffer.length;
		else
			targetBufferLength = (int) (sampleRate * delay / 1000);

		feedback = (float) values[1];
		targetFeedback = feedback;
		gain = (float) values[2];
		targetGain = gain;
	}

	@Override
	public boolean processTail(short[] buffer, int[] length) {
		short max = 0;
		length[0] = buffer.length;

		// Fill buffer with ONLY the echo decay (no dry input)
		for (int i = 0; i < buffer.length; i++) {

			// Step 1: Smooth parameters
			currentGain = interpolate(currentGain, targetGain, fadeStep);
			currentFeedback = interpolate(currentFeedback, targetFeedback, fadeStep);

			// Step 2: Read from delay buffer
			int readIndex = bufferIndex - currentBufferLength;
			if (readIndex < 0)
				readIndex += delayBuffer.length;

			short delayedSample = delayBuffer[readIndex];

			// Step 3: Output ONLY the delayed sample (scaled by gain/feedback)
			// No "currentSample" added because input is silence
			int outputSample = (int) (delayedSample * currentGain);

			// Step 4: Clamp
			buffer[i] = (short) Math.max(-SHORT_MAX_VALUE, Math.min(SHORT_MAX_VALUE, outputSample));

			// Step 5: Update delay buffer with feedback ONLY (no dry input)
			int feedbackSample = (int) (delayedSample * currentFeedback);
			delayBuffer[bufferIndex] = (short) Math.max(-SHORT_MAX_VALUE, Math.min(SHORT_MAX_VALUE, feedbackSample));

			// Step 6: Advance
			bufferIndex = (bufferIndex + 1) % delayBuffer.length;

			// Step 7: Tracking max value for silence detection
			short absVal = (short) Math.abs(buffer[i]);
			if (absVal > max)
				max = absVal;

			// Step 8: Dropping tail if new input data available
			if (writing) {
				length[0] = i;
				break;
			}
		}

		return max > 20;
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
		Logger.debug(1, "[EchoEffect] - %s", String.format(format, args));
	}
}
