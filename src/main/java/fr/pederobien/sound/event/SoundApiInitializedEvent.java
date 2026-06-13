package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISoundApi;
import fr.pederobien.utils.event.Event;

public class SoundApiInitializedEvent extends Event {
	private final ISoundApi soundApi;

	/**
	 * Creates an event thrown when the sound API has been initialized successfully
	 */
	public SoundApiInitializedEvent(ISoundApi soundApi) {
		this.soundApi = soundApi;
	}

	/**
	 * @return The initialized sound API.
	 */
	public ISoundApi getSoundApi() {
		return soundApi;
	}
}
