package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophonePausePostEvent extends MicrophoneEvent {

	/**
	 * Creates an event thrown when a microphone has been paused.
	 * 
	 * @param microphone The paused microphone.
	 */
	public MicrophonePausePostEvent(IMicrophone microphone) {
		super(microphone);
	}
}
