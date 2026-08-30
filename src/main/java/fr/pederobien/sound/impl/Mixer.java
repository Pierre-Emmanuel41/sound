package fr.pederobien.sound.impl;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.sound.interfaces.IEffectParametersHolder;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.utils.Disposable;
import fr.pederobien.utils.IDisposable;
import fr.pederobien.utils.event.Logger;

public class Mixer implements IMixer {
	private final float sampleRate;
	private final StreamMap streams;
	private final IDisposable disposable;
	private TargetDataLine microphoneLine;
	private SourceDataLine speakersLine;
	private boolean initialized;
	private long silenceStartTime;
	private int bufferSize;
	private short[] left;
	private short[] right;
	private int frameDuration;

	/**
	 * Creates a mixer used to play several audio stream at the same time.
	 * 
	 * @param sampleRate The sample rate to use for the microphone and speakers.
	 */
	public Mixer(float sampleRate) {
		this.sampleRate = sampleRate;

		streams = new StreamMap(sampleRate);
		disposable = new Disposable();
		initialized = false;
		silenceStartTime = 0;
		bufferSize = 0;
		frameDuration = 0;
	}

	/**
	 * Creates a mixer with default sample rate 44100Hz.
	 */
	public Mixer() {
		this(48000);
	}

	@Override
	public void initialize() throws Exception {
		disposable.checkDisposed();

		if (initialized)
			return;

		microphoneLine = AudioSystem.getTargetDataLine(new AudioFormat(sampleRate, 16, 1, true, false));
		speakersLine = AudioSystem.getSourceDataLine(new AudioFormat(sampleRate, 16, 2, true, false));

		initialized = true;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void dispose() {
		if (!disposable.dispose())
			return;

		// Closing microphone line
		if (microphoneLine != null && microphoneLine.isOpen()) {
			microphoneLine.stop();
			microphoneLine.close();
		}

		// Closing speakers line
		if (speakersLine != null && speakersLine.isOpen()) {
			speakersLine.drain();
			speakersLine.stop();
			speakersLine.close();
		}
	}

	@Override
	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public TargetDataLine getMicrophoneLine() {
		return microphoneLine;
	}

	@Override
	public SourceDataLine getSpeakersLine() {
		return speakersLine;
	}

	@Override
	public void write(String name, byte[] data) {
		disposable.checkDisposed();
		streams.getOrCreateStream(name).write(data);
	}

	@Override
	public void setOffset(String name, float offset) {
		disposable.checkDisposed();
		streams.getOrCreateStream(name).setOffset(offset);
	}

	@Override
	public void setVolumes(String name, float left, float right, float global) {
		disposable.checkDisposed();
		streams.setVolumes(name, left, right, global);
	}

	@Override
	public void addEffect(String name, int index, IEffect effect) {
		disposable.checkDisposed();
		info("Adding an effect on %s's audio stream: %s", name, effect);
		streams.getOrCreateStream(name).addEffect(index, effect);
	}

	@Override
	public void removeEffect(String name, String effectName) {
		disposable.checkDisposed();
		info("Removing %s from %s's audio stream", effectName, name);
		streams.getOrCreateStream(name).removeEffect(effectName);
	}

	@Override
	public void updateEffect(String name, IEffectParametersHolder holder) {
		disposable.checkDisposed();
		info("Updating an effect on %s's audio stream %s", name, holder);
		streams.getOrCreateStream(name).updateEffect(holder);
	}

	@Override
	public boolean exist(String name) {
		return streams.exist(name);
	}

	@Override
	public void resetVolumes() {
		info("Reseting left, right and global volumes of each registered streams");
		streams.resetVolumes();
	}

	@Override
	public int read(byte[] data) {
		if (disposable.isDisposed())
			return 0;

		int read = readAndMergeStreams(data);

		// All streams were empty
		if (read == 0) {
			if (silenceStartTime == 0)
				silenceStartTime = System.currentTimeMillis();

			long now = System.currentTimeMillis();

			if (now - silenceStartTime < 3 * frameDuration)
				return sleep(frameDuration) ? read(data) : -1;

			silenceStartTime = 0;
			Arrays.fill(data, 0, data.length, (byte) 0);
			return sleep(frameDuration) ? data.length : -1;
		}

		silenceStartTime = 0;
		return read;
	}

	@Override
	public void flush() {
		disposable.checkDisposed();
		streams.flush();
	}

	/**
	 * Read each stream from the underlying stream map until one of the following conditions is met:</br>
	 * All the streams are empty</br>
	 * The input bytes array is full.
	 * 
	 * @param data The bytes array to fill with the content of the each audio stream.
	 * @return The number of bytes written in the input bytes array.
	 */
	private int readAndMergeStreams(byte[] data) {
		// First call
		if (bufferSize == 0) {
			bufferSize = data.length / 4;
			left = new short[bufferSize];
			right = new short[bufferSize];
			frameDuration = (int) (data.length * 1000.0 / getSampleRate());
		}

		// Reinitializing content of left and right channel for next frame
		Arrays.fill(left, (short) 0);
		Arrays.fill(right, (short) 0);

		// Reading one frame of the left and right channel
		int read = streams.read(left, right, bufferSize);

		int offset;
		for (int i = 0; i < read; i++) {
			offset = i * 4;

			// Left channel
			data[offset] = (byte) (left[i] & 0xFF); // LSB
			data[offset + 1] = (byte) ((left[i] >> 8) & 0xFF); // MSB

			// Right channel
			data[offset + 2] = (byte) (right[i] & 0xFF); // LSB
			data[offset + 3] = (byte) ((right[i] >> 8) & 0xFF); // MSB
		}

		return read * 4;
	}

	/**
	 * Sleeps n milliseconds.
	 * 
	 * @param millis The number of milliseconds to sleep.
	 * @return True if no interrupt exception has been raised, false otherwise.
	 */
	private boolean sleep(int millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	private void info(String format, Object... args) {
		Logger.info("[Mixer] - %s", String.format(format, args));
	}
}