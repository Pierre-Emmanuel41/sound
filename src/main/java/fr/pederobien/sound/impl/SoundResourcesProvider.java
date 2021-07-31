package fr.pederobien.sound.impl;

import fr.pederobien.sound.interfaces.IEncoder;
import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.sound.interfaces.ISoundResourcesProvider;
import fr.pederobien.sound.interfaces.ISpeakers;

public class SoundResourcesProvider implements ISoundResourcesProvider {
	private IMicrophone microphone;
	private ISpeakers speakers;
	private IEncoder encoder;
	private IMixer mixer;

	/**
	 * Creates a provider that gather all needed resources to get data coming from the microphone, to play data using a mixer, to
	 * compress and decompress data.
	 * 
	 * @param asynchronousEncoder True if the encoder is asynchronous, false otherwise.
	 * @param lowpassRate         low-pass rate (Hz) for the low-pass filter associated to the encoder.
	 * @param highpassRate        high-pass rate (Hz) for the high-pass filter associated to the encoder.
	 */
	public SoundResourcesProvider(boolean asynchronousEncoder, double lowpassRate, double highpassRate) {
		microphone = new Microphone();
		mixer = new Mixer();
		speakers = new Speakers((Mixer) mixer);
		encoder = asynchronousEncoder ? new AsynchonousEncoder(lowpassRate, highpassRate) : new Encoder(lowpassRate, highpassRate);
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
	public IEncoder getEncoder() {
		return encoder;
	}

	@Override
	public IMixer getMixer() {
		return mixer;
	}
}
