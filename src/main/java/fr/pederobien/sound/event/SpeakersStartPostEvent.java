package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersStartPostEvent extends SpeakersEvent {

	/**
	 * Creates a event when speakers have started.
	 * 
	 * @param speakers The started speakers.
	 */
	public SpeakersStartPostEvent(ISpeakers speakers) {
		super(speakers);
	}
}
