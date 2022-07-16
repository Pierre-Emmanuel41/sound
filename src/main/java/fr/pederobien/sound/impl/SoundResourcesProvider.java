package fr.pederobien.sound.impl;

import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.sound.interfaces.ISpeakers;

public class SoundResourcesProvider {
	private static IMicrophone microphone;
	private static ISpeakers speakers;
	private static IMixer mixer;

	static {
		microphone = new Microphone();
		mixer = new Mixer();
		speakers = new Speakers((Mixer) mixer);
	}

	/**
	 * @return The microphone that read data from a {@link TargetDataLine}.
	 */
	public static IMicrophone getMicrophone() {
		return microphone;
	}

	/**
	 * @return The speakers that play data in a {@link SourceDataLine}.
	 */
	public ISpeakers getSpeakers() {
		return speakers;
	}

	/**
	 * @return A mixer that merge several audio streams.
	 */
	public IMixer getMixer() {
		return mixer;
	}
}
