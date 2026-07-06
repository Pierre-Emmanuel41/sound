package fr.pederobien.sound.interfaces;

import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public interface IMixer {

	/**
	 * Initialize resources to access the OS microphone and speakers.
	 */
	void initialize() throws Exception;

	/**
	 * @return True if this mixer has been initialized successfully.
	 */
	boolean isInitialized();

	/**
	 * Free resources used by the microphone and speakers. This method is called after closing the microphone and the speakers.
	 */
	void dispose();

	/**
	 * @return The sampleRate to use for the microphone and speakers.
	 */
	float getSampleRate();

	/**
	 * @return The line to use by the microphone.
	 */
	TargetDataLine getMicrophoneLine();

	/**
	 * @return The line to use for the speakers.
	 */
	SourceDataLine getSpeakersLine();

	/**
	 * Get the audio stream associated to the given name, creates one if no one is registered for the given name, and appends the
	 * given audio sample to its main buffer.
	 * 
	 * @param name The name of the stream.
	 * @param data The bytes array that contains audio sample.
	 */
	void write(String name, byte[] data);

	/**
	 * Set the volume offset to apply on an audio stream. The average volume of an audio stream my be too low compared to others. This
	 * method applies an offset on the global volume. The method checks if the offset value is in range [0, 1].
	 * 
	 * @param name   The name of the audio stream to modify.
	 * @param offset The volume offset to apply.
	 */
	void setOffset(String name, float offset);

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
	 * Set to 1.0 the left, right and global volumes of each registered stream.
	 */
	void resetVolumes();

	/**
	 * Set the effect to apply on a stream. If an effect is already applied on the stream, the stop method is called and the given
	 * effect is queued until the previous effect finish its transition to no modification. The effect's start method is automatically
	 * called.
	 * 
	 * @param name   The name of the stream on which an effect shall be applied.
	 * @param effect The effect to apply.
	 */
	void setEffect(String name, IEffect effect);

	/**
	 * Update parameters of an effect. The parameters defines how the effect modifies the audio stream.
	 * 
	 * @param name   The name of the stream on which an effect shall be modified.
	 * @param params An list of values of parameter.
	 */
	void setEffectValues(String name, Object... params);

	/**
	 * Call the stop method of the current effect of the stream associated to the given name.
	 * 
	 * @param name The name of the audio stream to update.
	 */
	void removeEffect(String name);

	/**
	 * Read bytes from this Mixer. This method blocks when at least one of the two conditions is verified :
	 * <p>
	 * There is no registered streams</br>
	 * All the streams are empty</br>
	 * 
	 * @param data The buffer to read the bytes into.
	 * 
	 * @return The number of bytes read into buffer, or -1 if the thread was waiting and has been interrupted.
	 */
	int read(byte[] data);

	/**
	 * Clears each audio stream registered in this mixer but leave the streams list unmodified.
	 */
	void flush();
}
