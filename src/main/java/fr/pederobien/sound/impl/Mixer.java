package fr.pederobien.sound.impl;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.utils.Disposable;
import fr.pederobien.utils.IDisposable;
import fr.pederobien.utils.event.Logger;

public class Mixer implements IMixer {
	private final float sampleRate;
	private final StreamMap streams;
	private final Lock lock;
	private final Condition isEmpty;
	private final IDisposable disposable;
	private TargetDataLine microphoneLine;
	private SourceDataLine speakersLine;
	private boolean waiting;
	private boolean initialized;
	private long silenceStartTime;

	/**
	 * Creates a mixer used to play several audio stream at the same time.
	 * 
	 * @param sampleRate The sample rate to use for the microphone and speakers.
	 */
	public Mixer(float sampleRate) {
		this.sampleRate = sampleRate;

		streams = new StreamMap(this);
		lock = new ReentrantLock(true);
		isEmpty = lock.newCondition();
		disposable = new Disposable();
		waiting = false;
		initialized = false;
		silenceStartTime = 0;
	}

	/**
	 * Creates a mixer with default sample rate 44100Hz.
	 */
	public Mixer() {
		this(44100);
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

		if (waiting)
			notifyOneStreamHasBeenFilled();

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
	public void setEffect(String name, IEffect effect) {
		disposable.checkDisposed();
		info("Setting effect \"%s\" for audio stream \"%s\"", effect.getName(), name);
		streams.getOrCreateStream(name).setEffect(effect);
	}

	@Override
	public void setEffectValues(String name, Object... params) {
		disposable.checkDisposed();
		info("Updating %s's audio stream effect: %s", name, params);
		streams.getOrCreateStream(name).getEffect().setValues(params);
	}

	@Override
	public void removeEffect(String name) {
		disposable.checkDisposed();
		info("Removing effect on audio stream \"%s\"", name);
		streams.getOrCreateStream(name).getEffect().stop();
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

			// For the last 800ms all the streams are empty
			if (now - silenceStartTime > 1000)
				return waitForStreamsToBeFilled() ? read(data) : -1;

			// For the last 200ms all the streams are empty
			long difference = now - silenceStartTime;
			if (difference > 200) {
				Arrays.fill(data, 0, data.length, (byte) 0);
				return sleep(50) ? data.length : -1;
			}

			return sleep(10) ? read(data) : -1;
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
		for (int i = 0; i < data.length; i += 4) {
			short[] left = new short[1];
			short[] right = new short[1];

			// Each stream is empty
			if (!streams.read(left, right))
				return i;

			// Left channel
			data[i] = (byte) (left[0] & 0xFF); // LSB
			data[i + 1] = (byte) ((left[0] >> 8) & 0xFF); // MSB

			// Right channel
			data[i + 2] = (byte) (right[0] & 0xFF); // LSB
			data[i + 3] = (byte) ((right[0] >> 8) & 0xFF); // MSB
		}

		return data.length;
	}

	/**
	 * Signal that there are streams to be read.
	 */
	protected void notifyOneStreamHasBeenFilled() {
		if (!waiting)
			return;

		try {
			lock.lock();
			waiting = false;
			isEmpty.signal();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Wait for streams to be read.
	 * 
	 * @return True if the thread shall read bytes, false if it should return.
	 */
	private boolean waitForStreamsToBeFilled() {
		try {
			lock.lock();
			waiting = true;
			debug("Waiting for streams to be filled");
			isEmpty.await();
			debug("At least one stream has been filled");
			return true;
		} catch (InterruptedException e) {
			return false;
		} finally {
			lock.unlock();
		}
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

	private void debug(String format, Object... args) {
		Logger.debug(1, "[Mixer] - %s", String.format(format, args));
	}

	private void info(String format, Object... args) {
		Logger.info("[Mixer] - %s", String.format(format, args));
	}
}