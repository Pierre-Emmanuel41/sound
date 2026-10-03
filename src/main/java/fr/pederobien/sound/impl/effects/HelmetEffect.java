package fr.pederobien.sound.impl.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import fr.pederobien.sound.impl.EffectParametersHolder;
import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.sound.interfaces.IEffectParametersHolder;
import fr.pederobien.utils.event.Logger;

public class HelmetEffect implements IEffect {

	/**
	 * The name of the effect
	 */
	public static final String NAME = "HELMET_EFFECT";

	/**
	 * The name of the center frequency. The value shall be of type Float.
	 */
	public static final String FREQUENCY = "frequency";

	/**
	 * The name of the quality factor. The value shall be of type Float.
	 */
	public static final String QUALITY_FACTOR = "factor";

	/**
	 * @return A holder to update with new parameter values.
	 */
	public static IEffectParametersHolder holder() {
		Map<String, Class<?>> description = new HashMap<String, Class<?>>();
		description.put(FREQUENCY, Float.class);
		description.put(QUALITY_FACTOR, Float.class);
		return new EffectParametersHolder(NAME, description);
	}

	/**
	 * Creates a holder that contains parameter new values for a Biquad helmet effect.
	 * 
	 * @param frequency     Center frequency of the pass-band in Hz. Frequencies near this value pass through with minimal
	 *                      attenuation; frequencies far from it are strongly attenuated.
	 *                      <ul>
	 *                      <li><strong>Recommended range: 500 – 3000 Hz.</strong> ~800 Hz gives a very muffled, "underwater" feel;
	 *                      ~1500 Hz is a balanced helmet sound; ~2500–3000 Hz keeps more presence and intelligibility.</li>
	 *                      <li>Must be &gt; 0 and &lt; {@code sampleRate / 2} (Nyquist frequency).</li>
	 *                      <li>Lower values → more muffled / bass-heavy; higher values → brighter but less "enclosed".</li>
	 *                      </ul>
	 * @param qualityFactor Quality factor (dimensionless). Controls the <em>bandwidth</em> of the pass-band:
	 *                      {@code bandwidth = centerFreq / q}.
	 *                      <ul>
	 *                      <li><strong>Recommended range: 0.5 – 3.0.</strong>
	 *                      <ul>
	 *                      <li><strong>0.5 – 1.0</strong> — wide band, gentle muffle. Sounds like a thick helmet or a room with heavy
	 *                      curtains.</li>
	 *                      <li><strong>1.0 – 2.0</strong> — moderate narrowness. Typical "helmet on" feel.</li>
	 *                      <li><strong>2.0 – 3.0</strong> — narrow band, pronounced muffle. Sounds like being deep underwater or in a
	 *                      sealed container.</li>
	 *                      <li><strong>&gt; 5</strong> — very narrow; the sound becomes thin and resonant. Usually undesirable for a
	 *                      helmet effect.</li>
	 *                      </ul>
	 *                      </li>
	 *                      <li>Must be &gt; 0. Values &lt; 0.3 produce an extremely wide band that barely attenuates anything
	 *                      (effectively no filter).</li>
	 *                      <li>Higher Q → narrower pass-band → more muffled. Lower Q → wider pass-band → less muffled.</li>
	 *                      </ul>
	 *
	 *                      <p>
	 *                      <strong>Quick presets:</strong>
	 *                      <table border="1">
	 *                      <caption>Typical helmet/underwater presets</caption>
	 *                      <tr>
	 *                      <th>Feel</th>
	 *                      <th>centerFreq</th>
	 *                      <th>q</th>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Light helmet</td>
	 *                      <td>2000 Hz</td>
	 *                      <td>0.7</td>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Standard helmet</td>
	 *                      <td>1200 Hz</td>
	 *                      <td>1.0</td>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Heavy / sealed</td>
	 *                      <td>800 Hz</td>
	 *                      <td>1.5</td>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Underwater</td>
	 *                      <td>500 Hz</td>
	 *                      <td>2.0</td>
	 *                      </tr>
	 *                      </table>
	 * @return A holder that contains the new effect parameter values.
	 */
	public static IEffectParametersHolder holder(float frequency, float qualityFactor) {
		IEffectParametersHolder holder = holder();
		holder.setValue(FREQUENCY, frequency);
		holder.setValue(QUALITY_FACTOR, qualityFactor);
		return holder;
	}

	private final float sampleRate;
	private float frequency;
	private float factor;
	private float b0, b1, b2, a1, a2;
	private float previousInput1 = 0;
	private float previousInput2 = 0;
	private float previousOutput1 = 0;
	private float previousOutput2 = 0;
	private float targetMix;
	private float currentMix;
	private float fadeStep;

	/**
	 * Creates a new helmet band-pass filter.
	 *
	 * <p>
	 * The filter is a 2nd-order IIR (biquad) band-pass that isolates a mid-frequency band, simulating the muffled, voice-focused
	 * sound heard when a player is wearing a helmet (or underwater).
	 *
	 * @param sampleRate    The audio sample rate in Hz. Must match the rate at which the {@code buffer} passed to {@link #apply} is
	 *                      produced. Typical values: <strong>44100</strong> or <strong>48000</strong>.
	 *                      <ul>
	 *                      <li>Must be &gt; 0.</li>
	 *                      <li>Must equal the hardware/device sample rate; a mismatch will shift the effective center frequency.</li>
	 *                      </ul>
	 *
	 * @param frequency     Center frequency of the pass-band in Hz. Frequencies near this value pass through with minimal
	 *                      attenuation; frequencies far from it are strongly attenuated.
	 *                      <ul>
	 *                      <li><strong>Recommended range: 500 – 3000 Hz.</strong> ~800 Hz gives a very muffled, "underwater" feel;
	 *                      ~1500 Hz is a balanced helmet sound; ~2500–3000 Hz keeps more presence and intelligibility.</li>
	 *                      <li>Must be &gt; 0 and &lt; {@code sampleRate / 2} (Nyquist frequency).</li>
	 *                      <li>Lower values → more muffled / bass-heavy; higher values → brighter but less "enclosed".</li>
	 *                      </ul>
	 *
	 * @param qualityFactor Quality factor (dimensionless). Controls the <em>bandwidth</em> of the pass-band:
	 *                      {@code bandwidth = centerFreq / q}.
	 *                      <ul>
	 *                      <li><strong>Recommended range: 0.5 – 3.0.</strong>
	 *                      <ul>
	 *                      <li><strong>0.5 – 1.0</strong> — wide band, gentle muffle. Sounds like a thick helmet or a room with heavy
	 *                      curtains.</li>
	 *                      <li><strong>1.0 – 2.0</strong> — moderate narrowness. Typical "helmet on" feel.</li>
	 *                      <li><strong>2.0 – 3.0</strong> — narrow band, pronounced muffle. Sounds like being deep underwater or in a
	 *                      sealed container.</li>
	 *                      <li><strong>&gt; 5</strong> — very narrow; the sound becomes thin and resonant. Usually undesirable for a
	 *                      helmet effect.</li>
	 *                      </ul>
	 *                      </li>
	 *                      <li>Must be &gt; 0. Values &lt; 0.3 produce an extremely wide band that barely attenuates anything
	 *                      (effectively no filter).</li>
	 *                      <li>Higher Q → narrower pass-band → more muffled. Lower Q → wider pass-band → less muffled.</li>
	 *                      </ul>
	 *
	 *                      <p>
	 *                      <strong>Quick presets:</strong>
	 *                      <table border="1">
	 *                      <caption>Typical helmet/underwater presets</caption>
	 *                      <tr>
	 *                      <th>Feel</th>
	 *                      <th>centerFreq</th>
	 *                      <th>q</th>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Light helmet</td>
	 *                      <td>2000 Hz</td>
	 *                      <td>0.7</td>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Standard helmet</td>
	 *                      <td>1200 Hz</td>
	 *                      <td>1.0</td>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Heavy / sealed</td>
	 *                      <td>800 Hz</td>
	 *                      <td>1.5</td>
	 *                      </tr>
	 *                      <tr>
	 *                      <td>Underwater</td>
	 *                      <td>500 Hz</td>
	 *                      <td>2.0</td>
	 *                      </tr>
	 *                      </table>
	 *
	 *                      <p>
	 *                      <strong>Example:</strong>
	 * 
	 *                      <pre>{@code
	 * // Standard helmet at 48 kHz
	 * HelmetEffect helmet = new HelmetEffect(48000, 1200, 1.0);
	 * }</pre>
	 */
	public HelmetEffect(float sampleRate, float frequency, float qualityFactor) {
		this.sampleRate = sampleRate;
		this.frequency = frequency;
		this.factor = qualityFactor;

		targetMix = 0.0f;
		currentMix = 0.0f;
		previousOutput1 = 0.0f;
		previousOutput2 = 0.0f;
		previousInput1 = 0.0f;
		previousInput2 = 0.0f;
		fadeStep = 1.0f / (sampleRate * 0.15f);

		update(frequency, qualityFactor);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void start() {
		if (targetMix == 1.0f)
			return;

		targetMix = 1.0f;
		debug("Starting effect (frequency=%s, factor=%s)", frequency, factor);
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
		return targetMix == 0.0f && currentMix == 0.0f;
	}

	@Override
	public void update(IEffectParametersHolder holder) {
		Object frequencyObj = holder.getValue(FREQUENCY);
		Object factorObj = holder.getValue(QUALITY_FACTOR);

		if (frequencyObj != null && factorObj != null) {
			frequency = (float) frequencyObj;
			factor = (float) factorObj;
			update(frequency, factor);
		}
	}

	@Override
	public void apply(short[] buffer, int length) {
		if (isStopped())
			return;

		for (int i = 0; i < length; i++) {
			currentMix = interpolate(currentMix, targetMix, fadeStep);

			// Step 1: Normalizing
			float normalized = (float) (buffer[i] / SHORT_MAX_VALUE);

			// Step 2: Applying filter
			float filtered = b0 * normalized + b1 * previousInput1 + b2 * previousInput2 - a1 * previousOutput1 - a2 * previousOutput2;

			// Step 3: Clipping
			filtered = Math.max(-1.0f, Math.min(1.0f, filtered));

			// Step 4: Denormalizing
			short wet = (short) (filtered * SHORT_MAX_VALUE);
			buffer[i] = (short) (buffer[i] * (1.0f - currentMix) + wet * currentMix);

			// Step 5: Updating state variable
			previousInput2 = previousInput1;
			previousInput1 = normalized;

			previousOutput2 = previousOutput1;
			previousOutput1 = filtered;
		}
	}

	@Override
	public boolean isTailActive() {
		return false;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(",", "{", "}");
		joiner.add("name=" + getName());
		joiner.add("frequency=" + frequency);
		joiner.add("qualityFactor=" + factor);
		return joiner.toString();
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

	/**
	 * Updates filter parameters based on the given frequency and quality factor.
	 * 
	 * @param frequency The center frequency of the filter.
	 * @param factor    The quality factor of the filter.
	 */
	private void update(float frequency, float factor) {
		float w0 = (float) (2.0 * Math.PI * frequency / sampleRate);
		float alpha = (float) (Math.sin(w0) / (2.0 * factor));
		float cosW0 = (float) Math.cos(w0);

		// Band-pass (0 dB peak gain)
		b0 = alpha;
		b1 = 0.0f;
		b2 = -alpha;
		a1 = -2.0f * cosW0;
		a2 = 1.0f - alpha;

		// Normalize so a0 = 1
		double a0 = 1.0 + alpha;
		b0 /= a0;
		b1 /= a0;
		b2 /= a0;
		a1 /= a0;
		a2 /= a0;
	}

	private void debug(String format, Object... args) {
		Logger.debug(1, "[BiquadHelmetEffect] - %s", String.format(format, args));
	}
}
