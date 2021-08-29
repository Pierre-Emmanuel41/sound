package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneStartPostEvent extends MicrophoneEvent {

	/**
	 * Creates a event when a microphone has started.
	 * 
	 * @param microphone The started microphone.
	 */
	public MicrophoneStartPostEvent(IMicrophone microphone) {
		super(microphone);
	}
}
