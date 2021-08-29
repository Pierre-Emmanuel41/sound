package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneRelaunchPostEvent extends MicrophoneEvent {

	/**
	 * Creates an event thrown when a microphone has been relaunched.
	 * 
	 * @param microphone The relaunched microphone.
	 */
	public MicrophoneRelaunchPostEvent(IMicrophone microphone) {
		super(microphone);
	}
}
