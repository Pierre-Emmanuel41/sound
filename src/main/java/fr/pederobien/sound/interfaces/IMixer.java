package fr.pederobien.sound.interfaces;

import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public interface IMixer {

	/**
	 * Initialize resources to access the OS microphone and speakers.
	 */
	void initialize() throws Exception;

	/**
	 * Free resources used by the microphone and speakers. This method is called after closing the microphone and the speakers.
	 */
	void dispose();

	/**
	 * @return The line to use by the microphone.
	 */
	TargetDataLine getMicrophoneLine();

	/**
	 * @return The line to use for the speakers.
	 */
	SourceDataLine getSpeakersLine();

	/**
	 * Post process the given bytes array. The input data corresponds to the raw data retrieved from the microphone. If after
	 * processing the bytes array, there is no useful data, then an empty bytes array shall be returned.
	 * 
	 * @param data The bytes array that contains the microphone output.
	 * 
	 * @return the processed bytes array.
	 */
	byte[] processMicrophoneData(byte[] data);

	/**
	 * Creates an audio stream associated to the given name if none is registered.
	 * 
	 * @param name The name of the stream to get or create.
	 * @return The audio stream associated to the given name, or null if an error occurred.
	 */
	IAudioStream getOrCreateStream(String name);

	/**
	 * Read bytes from this Mixer. This method blocks when at least one of the two conditions is verified :
	 * <p>
	 * There is no registered streams</br>
	 * All the streams are empty</br>
	 * The input bytes array is full.
	 * 
	 * @param data   The buffer to read the bytes into.
	 * @param offset The start index to read bytes into.
	 * @param length The maximum number of bytes that should be read.
	 * 
	 * @return The number of bytes read into buffer.
	 */
	int read(byte[] data);

	/**
	 * Clears each audio stream registered in this mixer but leave the streams list unmodified.
	 */
	void flush();
}
