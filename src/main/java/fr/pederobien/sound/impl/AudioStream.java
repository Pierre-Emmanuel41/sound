package fr.pederobien.sound.impl;

import java.util.ArrayDeque;
import java.util.Queue;

import fr.pederobien.sound.interfaces.IAudioStream;

public class AudioStream implements IAudioStream {
	private final Mixer mixer;
	private final Queue<Short> queue;
	private final Object lock;
	private float leftVolume;
	private float rightVolume;
	private float globalVolume;
	private float offset;

	public AudioStream(Mixer mixer) {
		this.mixer = mixer;
		queue = new ArrayDeque<Short>(4095);
		lock = new Object();
		leftVolume = 1;
		rightVolume = 1;
		globalVolume = 1;
		offset = 0;
	}

	/**
	 * Adds the bytes array to the underlying queue. Thread safe operation.
	 * 
	 * @param sample The sample that contains the bytes array to add.
	 */
	@Override
	public void put(byte[] data) {
		if (data == null)
			return;

		synchronized (lock) {
			for (int i = 0; i < data.length; i += 2)
				queue.add((short) ((data[i + 1] & 0xFF) << 8 | (data[i] & 0xFF)));
		}

		mixer.notifyOneStreamHasBeenFilled();
	}

	@Override
	public void setLeftVolume(float leftVolume) {
		this.leftVolume = leftVolume;
	}

	@Override
	public void setRightVolume(float rightVolume) {
		this.rightVolume = rightVolume;
	}

	@Override
	public void setGlobalVolume(float globalVolume) {
		this.globalVolume = globalVolume;
	}

	@Override
	public void setOffset(float offset) {
		this.offset = offset;
	}

	@Override
	public boolean read(short[] left, short[] right) {
		if (queue.isEmpty())
			return false;

		short value;
		synchronized (lock) {
			value = queue.poll();
		}

		left[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * leftVolume * (globalVolume + offset)));
		right[0] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * rightVolume * (globalVolume + offset)));
		return true;
	}

	@Override
	public void flush() {
		synchronized (lock) {
			queue.clear();
		}
	}
}
