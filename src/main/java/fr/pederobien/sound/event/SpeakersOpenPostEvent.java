package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersOpenPostEvent extends SpeakersEvent {

	/**
	 * Creates a event when speakers have started.
	 * 
	 * @param speakers The started speakers.
	 */
	public SpeakersOpenPostEvent(ISpeakers speakers) {
		super(speakers);
	}
}
