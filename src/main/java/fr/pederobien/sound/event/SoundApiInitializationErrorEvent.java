package fr.pederobien.sound.event;

import java.util.StringJoiner;

import fr.pederobien.utils.event.Event;

public class SoundApiInitializationErrorEvent extends Event {
	private final Exception exception;

	/**
	 * Event thrown when the sound API could not be initialized successfully.
	 * 
	 * @param client The client associated to the sound API.
	 */
	public SoundApiInitializationErrorEvent(Exception exception) {
		this.exception = exception;
	}

	/**
	 * @return The exception thrown at the sound API initialization.
	 */
	public Exception getException() {
		return exception;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(",", "{", "}");
		joiner.add("message=" + getException().getMessage());
		return super.toString();
	}
}
