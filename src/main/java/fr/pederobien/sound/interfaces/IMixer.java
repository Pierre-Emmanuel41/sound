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
	 * method applies an offset on the global volume. The method checks if the offset value is in range [0, 5].
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
	 * Adds an effect to apply on an audio stream. The start method is automatically applied.
	 * 
	 * @param name   The name of the stream on which an effect shall be added.
	 * @param index  The index at which the effect shall be added. If the index is greater than the size of the list of effect then
	 *               the effect will be added to the end.
	 * @param effect The effect to add.
	 */
	void addEffect(String name, int index, IEffect effect);

	/**
	 * Stops the effect associated to the given effectName. The effect will transition smoothly from applied to not applied. Once
	 * stopped completely, the effect will be removed.
	 * 
	 * @param name       The name of the audio stream for which an effect shall be removed.
	 * @param effectName The name of the effect to remove.
	 */
	void removeEffect(String name, String effectName);

	/**
	 * Update the parameters of an effect. The parameters defines how the effect modifies the audio stream. If there is not effect
	 * registered for the given audio stream name then the method returns. If a parameter name is not supported, the value will be
	 * ignored. If the parameter's value has a wrong data type, the method throws an IllegalArgumentException.
	 * 
	 * @param name   The name of the audio stream on which an effect shall be modified.
	 * @param holder An holder that contains the effect name and gather parameter's name / parameter's value.
	 */
	void updateEffect(String name, IEffectParametersHolder holder);

	/**
	 * Check if there is an audio stream registered for the given audio stream name.
	 * 
	 * @param name The name of the audio stream.
	 * @return True if an audio stream is registered for the given name, false otherwise.
	 */
	boolean exist(String name);

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
