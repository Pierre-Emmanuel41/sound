package fr.pederobien.sound.impl;

import java.util.ArrayList;
import java.util.List;
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
	private TargetDataLine microphoneLine;
	private SourceDataLine speakersLine;
	private boolean waiting;

	public Mixer() {
		streams = new StreamMap(this);
		lock = new ReentrantLock(true);
		isEmpty = lock.newCondition();
		disposable = new Disposable();
		waiting = false;
	}

	@Override
	public void initialize() throws Exception {
		disposable.checkDisposed();
		microphoneLine = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, new AudioFormat(44100f, 16, 1, true, false)));
		speakersLine = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100f, 16, 2, true, false)));
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
	public TargetDataLine getMicrophoneLine() {
		return microphoneLine;
	}

	@Override
	public SourceDataLine getSpeakersLine() {
		return speakersLine;
	}

	@Override
	public byte[] processMicrophoneData(byte[] data) {
		// TODO: Post process input data
		return data;
	}

	@Override
	public void write(String name, byte[] data) {
		streams.getOrCreateStream(name).write(data);
	}

	@Override
	public void setVolumes(String name, float left, float right, float global) {
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
		if (read == 0) {
			waitForStreamsToBeFilled();
			return read(data);
		}

		return read;
	}

	@Override
	public void flush() {
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
	 */
	private void waitForStreamsToBeFilled() {
		try {
			lock.lock();
			waiting = true;
			isEmpty.await();
		} catch (InterruptedException e) {

		} finally {
			lock.unlock();
		}
	}

	private class StreamMap {

		private class Stream {
			private String name;
			private AudioStream audio;

			/**
			 * Creates a stream element based on the given name and audio stream.
			 * 
			 * @param name   The name of the stream
			 * @param stream
			 */
			private Stream(String name, AudioStream audio) {
				this.name = name;
				this.audio = audio;
			}

			/**
			 * @return The name of the audio stream.
			 */
			public String getName() {
				return name;
			}

			/**
			 * @return The audio stream associated to the name.
			 */
			public AudioStream getAudio() {
				return audio;
			}
		}

		private final Mixer mixer;
		private final List<Stream> streams;
		private final Object lock;

		private StreamMap(Mixer mixer) {
			this.mixer = mixer;
			streams = new ArrayList<Stream>();
			lock = new Object();
		}

		/**
		 * Get a stream associated to the given name if registered. If there is no stream associated to the given name, one is created.
		 * 
		 * @param name The name of the stream to retrieve.
		 * @return The stream associated to the given name.
		 */
		private AudioStream getOrCreateStream(String name) {
			synchronized (lock) {
				for (Stream stream : streams)
					if (stream.getName().equals(name))
						return stream.getAudio();
			}

			// Stream not found
			Stream stream = new Stream(name, new AudioStream(mixer));
			streams.add(stream);
			return stream.getAudio();
		}

		/**
		 * Read one sample from each stream registered in this map, sums the result, perform clipping checks.
		 * 
		 * @param left  The resulting sample for the left channel.
		 * @param right The resulting sample for the right channel.
		 * @return True if there was at least one non-empty stream, false otherwise.
		 */
		private boolean read(short[] left, short[] right) {
			int sumLeft = 0;
			int sumRight = 0;
			boolean read = false;

			synchronized (lock) {
				for (Stream stream : streams) {
					short[] sampleLeft = new short[1];
					short[] sampleRight = new short[1];

					// Getting left and right sample for the stream
					if (stream.getAudio().read(sampleLeft, sampleRight)) {
						sumLeft += sampleLeft[0];
						sumRight += sampleRight[0];
						read = true;
					}
				}
			}

			// All streams are empty
			if (!read)
				return false;

			// Clipping
			left[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sumLeft));
			right[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sumRight));

			return true;
		}

		/**
		 * Clears each audio stream registered in this mixer but leave the streams list unmodified.
		 */
		private void flush() {
			synchronized (lock) {
				for (Stream stream : streams) {
					stream.getAudio().flush();
				}
			}
		}
	}
}