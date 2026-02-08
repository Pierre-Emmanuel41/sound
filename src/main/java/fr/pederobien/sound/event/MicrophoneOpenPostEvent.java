package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneOpenPostEvent extends MicrophoneEvent {

	/**
	 * Creates a event when a microphone has started.
	 * 
	 * @param microphone The started microphone.
	 */
	public MicrophoneOpenPostEvent(IMicrophone microphone) {
		super(microphone);
	}
}
