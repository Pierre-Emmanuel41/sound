package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersRelaunchPostEvent extends SpeakersEvent {

	/**
	 * Creates an event thrown when the speakers have been relaunched.
	 * 
	 * @param speakers The relaunched speakers.
	 */
	public SpeakersRelaunchPostEvent(ISpeakers speakers) {
		super(speakers);
	}
}
