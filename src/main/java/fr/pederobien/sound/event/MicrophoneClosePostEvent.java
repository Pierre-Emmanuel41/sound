package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneClosePostEvent extends MicrophoneEvent {

	/**
	 * Creates an event thrown when a microphone has been paused.
	 * 
	 * @param microphone The paused microphone.
	 */
	public MicrophoneClosePostEvent(IMicrophone microphone) {
		super(microphone);
	}
}
