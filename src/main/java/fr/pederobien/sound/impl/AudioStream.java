package fr.pederobien.sound.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fr.pederobien.sound.event.DecoderFailToDecodeEvent;
import fr.pederobien.sound.interfaces.IDecoder;
import fr.pederobien.utils.BlockingQueueTask;
import fr.pederobien.utils.event.EventHandler;
import fr.pederobien.utils.event.EventManager;
import fr.pederobien.utils.event.IEventListener;

public class AudioStream implements IEventListener {
	private String key;
	private IDecoder decoder;
	private List<AudioSample> samples, finished;
	private Lock lock;
	private int size;
	private BlockingQueueTask<AudioPacket> extractor;
	private float volume;

	public AudioStream(String key) {
		this.key = key;
		decoder = new Decoder();
		samples = new ArrayList<AudioSample>();
		finished = new ArrayList<AudioSample>();
		lock = new ReentrantLock(true);
		extractor = new BlockingQueueTask<AudioPacket>(String.format("%s_Extractor", key), packet -> extractPacket(packet));
		volume = 1;
		extractor.start();
		EventManager.registerListener(this);
	}

	/**
	 * @return The key to which this stream is associated.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key associated to this stream.
	 * 
	 * @param key The key stream.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return The global volume associated to this audio stream.
	 */
	public double getVolume() {
		return volume;
	}

	/**
	 * Set the global sound volume associated to this stream. The new volume should be in range [0,2].
	 * 
	 * @param volume The new volume of this stream.
	 */
	public void setVolume(float volume) {
		this.volume = volume < 0 ? 0 : volume > 2 ? 2 : volume;
	}

	/**
	 * Extract asynchronously the audio sample associated to the given audio packet.
	 * 
	 * @param packet The audio packet that contains audio sample data.
	 */
	public void extract(AudioPacket packet) {
		extractor.add(packet);
	}

	/**
	 * Reads audio data from the audio stream buffer. The requested number of bytes is read into the specified array.
	 * 
	 * @param buffer A bytes array that will contain the requested input data when this method returns.
	 * @param length The requested number of bytes to read.
	 * 
	 * @return The number of bytes actually read.
	 */
	public int read(int[] buffer, int length) {
		if (samples.isEmpty())
			return 0;

		int read = 0, index = 0;
		// Iterating on samples in order to read the right number of bytes if it is possible.
		while (read < length) {
			try {
				AudioSample sample = getSample(index++);
				read += sample.read(buffer, read, length - read);

				// Checking if the sample need to be removed at the end of the iteration.
				if (sample.isRead())
					finished.add(sample);
			} catch (IndexOutOfBoundsException e) {
				// Exception thrown when trying to read the next sample whereas there is no not read registered sample.
			}
		}

		// Deleting the already read samples.
		removeReadSamples();

		finished.clear();
		return read;
	}

	/**
	 * @return The number of not read registered samples.
	 */
	public int size() {
		return size;
	}

	/**
	 * Removes all audio samples registered for this audio stream.
	 */
	public void clear(boolean full) {
		lock.lock();
		try {
			samples.clear();

			if (full)
				extractor.dispose();
		} finally {
			lock.unlock();
		}
	}

	@EventHandler
	private void onDecodeFail(DecoderFailToDecodeEvent event) {
		System.err.println("[AudioStream] Fail to decode bytes array");
	}

	/**
	 * Transform the given bytes array that represent a mono signal to a stereo signal.
	 * 
	 * @param mono The bytes array that represents a mono signal.
	 * 
	 * @return The duplicated bytes array in order to correspond to a stereo signal.
	 */
	private byte[] toStereo(byte[] mono) {
		byte[] data = new byte[mono.length * 2];
		int index = 0;
		for (int i = 0; i < mono.length; i += 2) {
			short initialShort = (short) ((mono[i + 1] & 0xff) << 8 | mono[i] & 0xff);
			int withVolume = (int) (initialShort * volume);
			initialShort = withVolume > Short.MAX_VALUE ? Short.MAX_VALUE : withVolume < Short.MIN_VALUE ? Short.MIN_VALUE : (short) withVolume;

			data[index] = (byte) initialShort;
			data[index + 1] = (byte) (initialShort >> 8);
			data[index + 2] = data[index];
			data[index + 3] = data[index + 1];
			index += 4;
		}

		return data;
	}

	/**
	 * Thread safe operation to register a new sample for this audio stream.
	 * 
	 * @param sample The audio sample to register.
	 */
	private void addSample(AudioSample sample) {
		lock.lock();
		try {
			if (!samples.isEmpty())
				samples.get(samples.size() - 1).setNext(sample);
			samples.add(sample);
			size++;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Thread safe operation to get the sample associated to the given index.
	 * 
	 * @param index The index of the sample to retrieve.
	 * 
	 * @return The audio sample registered at the given position if registered.
	 */
	private AudioSample getSample(int index) {
		lock.lock();
		try {
			return samples.get(index);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Thread safe operation in order to remove all read samples from the samples map.
	 */
	private void removeReadSamples() {
		if (finished.size() <= 0)
			return;

		lock.lock();
		try {
			samples.removeAll(finished);
			size -= finished.size();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Extract the audio sample associated to the given audio packet.
	 * 
	 * @param packet The audio packet that contains audio sample data.
	 */
	public void extractPacket(AudioPacket packet) {
		byte[] data = packet.getData();

		if (packet.isEncoded())
			data = decoder.decode(data);

		if (packet.isMono())
			data = toStereo(data);

		addSample(new AudioSample(data, packet));
	}
}
