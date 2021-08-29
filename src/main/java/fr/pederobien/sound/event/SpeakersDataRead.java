package fr.pederobien.sound.event;

import javax.sound.sampled.SourceDataLine;

import fr.pederobien.sound.interfaces.ISpeakers;

public class SpeakersDataRead extends SpeakersEvent {
	private byte[] data;

	/**
	 * Creates an event thrown when data has been played by the underlying {@link SourceDataLine} of a speakers.
	 * 
	 * @param speakers The speakers that read data.
	 * @param data     The data reading by the speakers.
	 */
	public SpeakersDataRead(ISpeakers speakers, byte[] data) {
		super(speakers);
		this.data = data;
	}

	/**
	 * @return The buffer read by the {@link SourceDataLine}.
	 */
	public byte[] getData() {
		return data;
	}
}
