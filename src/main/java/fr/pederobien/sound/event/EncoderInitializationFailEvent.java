package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IEncoder;

public class EncoderInitializationFailEvent extends EncoderEvent {

	/**
	 * Creates an event thrown when the given encoder fails to initialized itself.
	 * 
	 * @param encoder the encoder that fails to initialize.
	 */
	public EncoderInitializationFailEvent(IEncoder encoder) {
		super(encoder);
	}
}
