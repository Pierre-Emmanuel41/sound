package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IDecoder;

public class DecoderFailToDecodeEvent extends DecoderEvent {
	private byte[] data;

	/**
	 * Creates an event thrown when a decoder fails to decode a bytes array.
	 * 
	 * @param decoder The decoder that fails to decode a bytes array.
	 * @param data    The bytes array to decode.
	 */
	public DecoderFailToDecodeEvent(IDecoder decoder, byte[] data) {
		super(decoder);
		this.data = data;
	}

	/**
	 * @return The bytes array to decode.
	 */
	public byte[] getData() {
		return data;
	}
}
