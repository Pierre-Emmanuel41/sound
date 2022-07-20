package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMixer;

public class MixerEvent extends ProjectSoundEvent {
	private IMixer mixer;

	/**
	 * Creates a mixer event.
	 * 
	 * @param mixer The mixer source involved in this event.
	 */
	public MixerEvent(IMixer mixer) {
		this.mixer = mixer;
	}

	/**
	 * @return The mixer involved in this event.
	 */
	public IMixer getMixer() {
		return mixer;
	}
}
