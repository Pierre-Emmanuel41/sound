package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersEvent extends SoundEvent {
	private ISpeakers speakers;

	/**
	 * Creates a speakers event.
	 * 
	 * @param speakers The speakers source involved in this event.
	 */
	public SpeakersEvent(ISpeakers speakers) {
		this.speakers = speakers;
	}

	/**
	 * @return The speakers involved in this event.
	 */
	public ISpeakers getSpeakers() {
		return speakers;
	}
}
