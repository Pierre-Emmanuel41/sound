package fr.pederobien.sound.event;

import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneDataReadEvent extends MicrophoneEvent {
	private byte[] data;

	/**
	 * Creates an event thrown when data has been read from the underlying {@link TargetDataLine} of a microphone.
	 * 
	 * @param microphone The microphone that read data.
	 * @param data       The data coming from the microphone.
	 */
	public MicrophoneDataReadEvent(IMicrophone microphone, byte[] data) {
		super(microphone);
		this.data = data;
	}

	/**
	 * @return The buffer filled by the {@link TargetDataLine}.
	 */
	public byte[] getData() {
		return data;
	}
}
