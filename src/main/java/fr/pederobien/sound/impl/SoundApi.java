package fr.pederobien.sound.impl;

import fr.pederobien.sound.event.SoundApiInitializationErrorEvent;
import fr.pederobien.sound.event.SoundApiInitializedEvent;
import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.sound.interfaces.ISoundApi;
import fr.pederobien.sound.interfaces.ISpeakers;
import fr.pederobien.utils.event.EventManager;
import fr.pederobien.utils.event.Logger;

public class SoundApi implements ISoundApi {
	private IMixer mixer;
	private Microphone microphone;
	private Speakers speakers;

	/**
	 * Creates a sound API to interact with OS microphone and speakers. The given mixer is initialized before instantiating a
	 * microphone and a speakers.
	 * 
	 * @param mixer The mixer to use to process microphone data and manage audio streams.
	 */
	public SoundApi(IMixer mixer) {
		this.mixer = mixer;

		try {
			mixer.initialize();
			microphone = new Microphone(mixer);
			speakers = new Speakers(mixer);

			Logger.info("Sound API initialized successfully");
			EventManager.callEvent(new SoundApiInitializedEvent(this));
		} catch (Exception e) {
			Logger.error("An issue occurred while initializing sound API: %s", e.getMessage());
			EventManager.callEvent(new SoundApiInitializationErrorEvent(e));
		}
	}

	/**
	 * Creates a sound API with default mixer.
	 */
	public SoundApi() {
		this(new Mixer());
	}

	@Override
	public void dispose() {
		try {
			microphone.close();
			speakers.close();
			mixer.dispose();

			Logger.info("Sound API disposed successfully");
		} catch (Exception e) {
			// Do nothing
		}
	}

	@Override
	public IMicrophone getMicrophone() {
		return microphone;
	}

	@Override
	public ISpeakers getSpeakers() {
		return speakers;
	}

	@Override
	public IMixer getMixer() {
		return mixer;
	}
}
