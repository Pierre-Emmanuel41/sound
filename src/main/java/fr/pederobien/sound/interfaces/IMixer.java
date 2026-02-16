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
	 * Get the audio stream associated to the given name, creates one if no one is registered for the given name, and appends the
	 * given audio sample to its main buffer.
	 * 
	 * @param name The name of the stream.
	 * @param data The bytes array that contains audio sample.
	 */
	void write(String name, byte[] data);

	/**
	 * Set the left, right and global volumes of an audio stream.
	 * 
	 * @param name   The name of the stream.
	 * @param left   The volume on the left side.
	 * @param right  The volume on the right side.
	 * @param global The global volume on both sides.
	 */
	void setVolumes(String name, float left, float right, float global);

	/**
	 * Read bytes from this Mixer. This method blocks when at least one of the two conditions is verified :
	 * <p>
	 * There is no registered streams</br>
	 * All the streams are empty</br>
	 * The input bytes array is full.
	 * 
	 * @param data The buffer to read the bytes into.
	 * 
	 * @return The number of bytes read into buffer.
	 */
	int read(byte[] data);

	/**
	 * Clears each audio stream registered in this mixer but leave the streams list unmodified.
	 */
	void flush();
}
