package fr.pederobien.sound.impl;

import fr.pederobien.sound.interfaces.IDecoder;
import fr.pederobien.sound.interfaces.IEncoder;
import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.sound.interfaces.ISoundResourcesProvider;
import fr.pederobien.sound.interfaces.ISpeakers;

public class SoundResourcesProvider implements ISoundResourcesProvider {
	private IMicrophone microphone;
	private ISpeakers speakers;
	private IMixer mixer;

	/**
	 * Creates a provider that gather all needed resources to get data coming from the microphone, to play data using a mixer, to
	 * compress and decompress data.
	 * 
	 * @param asynchronousEncoder True if the encoder is asynchronous, false otherwise.
	 * @param lowpassRate         low-pass rate (Hz) for the low-pass filter associated to the encoder.
	 * @param highpassRate        high-pass rate (Hz) for the high-pass filter associated to the encoder.
	 */
	public SoundResourcesProvider() {
		microphone = new Microphone();
		mixer = new Mixer();
		speakers = new Speakers((Mixer) mixer);
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

	@Override
	public IEncoder newEncoder() {
		return new Encoder();
	}

	@Override
	public IDecoder newDecoder() {
		return new Decoder();
	}
}
