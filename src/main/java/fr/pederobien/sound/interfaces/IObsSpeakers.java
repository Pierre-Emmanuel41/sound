package fr.pederobien.sound.interfaces;

import javax.sound.sampled.SourceDataLine;

public interface IObsSpeakers {

	/**
	 * Notify this observer that data has been read from the mixer.
	 * 
	 * @param data The byte array read from the mixer in order to be player by the underlying {@link SourceDataLine} of the speakers.
	 * @param size The number of byte read from the microphone.
	 */
	void onDataRead(byte[] data, int size);
}
