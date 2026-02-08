package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneEvent extends SoundEvent {
	private IMicrophone microphone;

	/**
	 * Creates a microphone event.
	 * 
	 * @param microphone The microphone source involved in this event.
	 */
	public MicrophoneEvent(IMicrophone microphone) {
		this.microphone = microphone;
	}

	/**
	 * @return The microphone involved in this event.
	 */
	public IMicrophone getMicrophone() {
		return microphone;
	}
}
