package fr.pederobien.sound.impl.effects;

import java.util.HashMap;

import fr.pederobien.sound.impl.EffectParametersHolder;
import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.sound.interfaces.IEffectParametersHolder;
import fr.pederobien.utils.event.Logger;

public class NoEffect implements IEffect {
	/**
	 * The name of this effect.
	 */
	public static final String NAME = "NO_EFFECT";

	/**
	 * @return A holder to update with new parameter values.
	 */
	public static IEffectParametersHolder holder() {
		return new EffectParametersHolder(NAME, new HashMap<String, Class<?>>());
	}

	private boolean isStopped;

	/**
	 * Creates an effect that do nothing.
	 */
	public NoEffect() {
		isStopped = true;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void start() {
		if (!isStopped)
			return;

		isStopped = false;
		debug("Starting effect");
	}

	@Override
	public void stop() {
		if (isStopped)
			return;

		isStopped = true;
		debug("Stopping effect");
	}

	@Override
	public boolean isStopped() {
		return isStopped;
	}

	@Override
	public void update(IEffectParametersHolder holder) {
		// Do nothing
	}

	@Override
	public void apply(short[] buffer, int length) {
		// Do nothing
	}

	@Override
	public boolean processTail(short[] buffer, int[] length) {
		return false;
	}

	private void debug(String format, Object... args) {
		Logger.debug(1, "[NoEffect] - %s", String.format(format, args));
	}
}
