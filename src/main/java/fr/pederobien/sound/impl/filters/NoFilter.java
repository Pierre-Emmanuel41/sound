package fr.pederobien.sound.impl.filters;

import fr.pederobien.sound.interfaces.IFilter;

public class NoFilter implements IFilter {

	/**
	 * Creates a cleaner that does not do anything on the microphone stream.
	 */
	public NoFilter() {
		// Do nothing
	}

	@Override
	public void apply(short[] buffer) {
		// Do nothing
	}
}
