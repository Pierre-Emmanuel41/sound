package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersInterruptPostEvent extends SpeakersEvent {

	/**
	 * Creates an event thrown when the speakers have been interrupted.
	 * 
	 * @param speakers The interrupted speakers.
	 */
	public SpeakersInterruptPostEvent(ISpeakers speakers) {
		super(speakers);
	}
}
