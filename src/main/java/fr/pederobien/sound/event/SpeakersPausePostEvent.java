package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersPausePostEvent extends SpeakersEvent {

	/**
	 * Creates an event thrown when the speakers have been paused.
	 * 
	 * @param speakers The paused speakers.
	 */
	public SpeakersPausePostEvent(ISpeakers speakers) {
		super(speakers);
	}
}
