package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IEncoder;

public class EncoderEvent extends ProjectSoundEvent {
	private IEncoder encoder;

	/**
	 * Creates an encoder event.
	 * 
	 * @param encoder The encoder source involved in this event.
	 */
	public EncoderEvent(IEncoder encoder) {
		this.encoder = encoder;
	}

	/**
	 * @return The encoder involved in this event.
	 */
	public IEncoder getEncoder() {
		return encoder;
	}
}
