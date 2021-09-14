package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IEncoder;

public class EncoderFailToEncodeEvent extends EncoderEvent {
	private byte[] data;

	/**
	 * Creates an event thrown when the given encoder fails to encode the specified byte array.
	 * 
	 * @param encoder The encoder that fail to encode data.
	 * @param data    The original data that should be encoded.
	 */
	public EncoderFailToEncodeEvent(IEncoder encoder, byte[] data) {
		super(encoder);
		this.data = data;
	}

	/**
	 * @return The original data that should be encoded.
	 */
	public byte[] getData() {
		return data;
	}
}
