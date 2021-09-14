package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IDecoder;

public class DecoderEvent extends SoundEvent {
	private IDecoder decoder;

	/**
	 * Creates a decoder event.
	 * 
	 * @param decoder The decoder source involved in this event.
	 */
	public DecoderEvent(IDecoder decoder) {
		this.decoder = decoder;
	}

	/**
	 * @return The decoder involved in this event.
	 */
	public IDecoder getDecoder() {
		return decoder;
	}
}
