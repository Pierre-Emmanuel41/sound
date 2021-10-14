package fr.pederobien.sound.impl;

import javax.sound.sampled.AudioFormat;

public class SoundConstants {

	/**
	 * The sample rate, this value is the same for the microphone and for the speakers.
	 */
	public static final float SAMPLE_RATE = 48000.0f;

	/**
	 * The number of bits in each sample, this value is the same for the microphone and for the speakers.
	 */
	public static final int SAMPLE_SIZE = 16;

	/**
	 * The number of channel for the microphone audio format.
	 */
	public static final int MICRO_CHANNELS_NUMBER = 1;

	/**
	 * The number of channel for the microphone audio format.
	 */
	public static final int SPEAKERS_CHANNELS_NUMBER = 2;

	/**
	 * Indicates whether the data is signed or unsigned, this value is the same for the microphone and the speakers.
	 */
	public static final boolean SAMPLE_SIGNED = true;

	/**
	 * Indicates whether the data for a single sample is stored in big-endian byte order, this value is the same for the microphone
	 * and the speakers.
	 */
	public static final boolean BIG_ENDIAN = false;

	/**
	 * The length of an audio sample.
	 */
	public static final int CHUNK_LENGTH = 2880;

	/**
	 * The audio format associated to the microphone.
	 */
	public static final AudioFormat MICROPHONE_AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, MICRO_CHANNELS_NUMBER, SAMPLE_SIGNED, BIG_ENDIAN);

	/**
	 * The audio format associated to the speakers.
	 */
	public static final AudioFormat SPEAKERS_AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, SPEAKERS_CHANNELS_NUMBER, SAMPLE_SIGNED, BIG_ENDIAN);
}
