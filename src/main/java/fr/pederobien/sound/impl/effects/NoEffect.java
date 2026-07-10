package fr.pederobien.sound.impl.effects;

import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.utils.event.Logger;

public class NoEffect implements IEffect {
	private boolean isStopped;

	/**
	 * Creates an effect that do nothing.
	 */
	public NoEffect() {
		isStopped = true;
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
	public void apply(short[] buffer) {
		// Do nothing
	}

	@Override
	public void setValues(Object... values) {
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
