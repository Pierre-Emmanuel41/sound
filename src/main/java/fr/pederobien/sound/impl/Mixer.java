package fr.pederobien.sound.impl;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.utils.Disposable;
import fr.pederobien.utils.IDisposable;

public class Mixer implements IMixer {
	private final StreamMap streams;
	private final Lock lock;
	private final Condition isEmpty;
	private final IDisposable disposable;
	private final MicrophoneDataCleaner microphoneAudioProcessor;
	private TargetDataLine microphoneLine;
	private SourceDataLine speakersLine;
	private boolean waiting;

	public Mixer() {
		streams = new StreamMap(this);
		lock = new ReentrantLock(true);
		isEmpty = lock.newCondition();
		disposable = new Disposable();
		microphoneAudioProcessor = new MicrophoneDataCleaner();
		waiting = false;
	}

	@Override
	public void initialize() throws Exception {
		disposable.checkDisposed();
		microphoneLine = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, new AudioFormat(44100f, 16, 1, true, false)));
		speakersLine = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100f, 16, 2, true, false)));

		microphoneAudioProcessor.initialize();
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

		microphoneAudioProcessor.dispose();
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
	public void registerRawMicrophoneData(byte[] data) {
		disposable.checkDisposed();
		microphoneAudioProcessor.register(data);
	}

	@Override
	public int fetchProcessedMicrophoneData(byte[] data) {
		disposable.checkDisposed();
		return microphoneAudioProcessor.fetch(data);
	}

	@Override
	public void write(String name, byte[] data) {
		disposable.checkDisposed();
		streams.getOrCreateStream(name).write(data);
	}

	@Override
	public void setVolumes(String name, float left, float right, float global) {
		disposable.checkDisposed();
		AudioStream stream = streams.getOrCreateStream(name);
		stream.setLeftVolume(left);
		stream.setRightVolume(right);
		stream.setGlobalVolume(global);
	}

	@Override
	public int read(byte[] data) {
		if (disposable.isDisposed())
			return 0;

		int read = readAndMergeStreams(data);

		// All streams were empty
		if (read == 0)
			return waitForStreamsToBeFilled() ? read(data) : -1;

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
			isEmpty.await();
			return true;
		} catch (InterruptedException e) {
			return false;
		} finally {
			lock.unlock();
		}
	}
}