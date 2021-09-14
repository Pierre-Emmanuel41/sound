package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IDecoder;

public class DecoderInitializationFailEvent extends DecoderEvent {

	/**
	 * Creates an event thrown when the given decoder fails to initialize itself.
	 * 
	 * @param decoder The decoder that fails to initialize.
	 */
	public DecoderInitializationFailEvent(IDecoder decoder) {
		super(decoder);
	}
}
