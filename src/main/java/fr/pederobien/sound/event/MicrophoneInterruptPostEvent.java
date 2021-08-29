package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneInterruptPostEvent extends MicrophoneEvent {

	/**
	 * Creates an event thrown when a microphone has been interrupted.
	 * 
	 * @param microphone The interrupted microphone.
	 */
	public MicrophoneInterruptPostEvent(IMicrophone microphone) {
		super(microphone);
	}
}
