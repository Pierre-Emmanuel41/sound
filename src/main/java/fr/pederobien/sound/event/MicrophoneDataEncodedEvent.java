package fr.pederobien.sound.event;

import java.util.StringJoiner;

import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.interfaces.IMicrophone;

public class MicrophoneDataEncodedEvent extends MicrophoneEvent {
	private byte[] original, encoded;

	/**
	 * Creates an event thrown when data coming from the underlying {@link TargetDataLine} of a microphone has been encoded
	 * successfully.
	 * 
	 * @param microphone The microphone from which the data is coming.
	 * @param original   The raw data coming from the microphone.
	 * @param encoded    The encoded data.
	 */
	public MicrophoneDataEncodedEvent(IMicrophone microphone, byte[] original, byte[] encoded) {
		super(microphone);
		this.original = original;
		this.encoded = encoded;
	}

	/**
	 * @return The original bytes array that comes from the microphone.
	 */
	public byte[] getOriginal() {
		return original;
	}

	/**
	 * @return The encoded data associated to the original bytes array.
	 */
	public byte[] getEncoded() {
		return encoded;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(",", "{", "}");
		joiner.add("originalLength=" + getOriginal().length);
		joiner.add("encodedLength=" + getEncoded().length);
		return String.format("%s_%s", getName(), joiner);
	}
}
