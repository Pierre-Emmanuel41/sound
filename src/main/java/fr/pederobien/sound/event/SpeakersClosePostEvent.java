package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersClosePostEvent extends SpeakersEvent {

	/**
	 * Creates an event thrown when the speakers have been paused.
	 * 
	 * @param speakers The paused speakers.
	 */
	public SpeakersClosePostEvent(ISpeakers speakers) {
		super(speakers);
	}
}
