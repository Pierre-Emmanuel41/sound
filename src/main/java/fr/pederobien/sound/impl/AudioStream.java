package fr.pederobien.sound.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import fr.pederobien.sound.interfaces.IEffect;
import fr.pederobien.sound.interfaces.IEffectParametersHolder;

public class AudioStream {
	private final String name;
	private final float sampleRate;
	private final Buffer input;
	private final Buffer output;
	private final Thread effectThread;
	private final List<IEffect> effects;
	private final Queue<Short> queue;
	private final Object lock;
	private float leftVolume;
	private float rightVolume;
	private float globalVolume;
	private float factor;
	private int bufferSize;
	private int frameDuration;
	private short[] tmp;
	private long lastInputTime;

	/**
	 * Creates an audio stream.
	 * 
	 * @param name  The stream name.
	 * @param mixer The sampleRate of the audio stream.
	 */
	public AudioStream(String name, float sampleRate) {
		this.name = name;
		this.sampleRate = sampleRate;

		input = new Buffer(false);
		output = new Buffer(false);
		effectThread = new Thread(this::process, String.format("[%s - EffectThread]", name));
		effectThread.setDaemon(true);
		effects = new ArrayList<IEffect>();
		queue = new ArrayDeque<Short>(4095);
		lock = new Object();

		leftVolume = 1;
		rightVolume = 1;
		globalVolume = 1;
		factor = 1;
		bufferSize = 0;
		frameDuration = 0;
		lastInputTime = 0;
	}

	/**
	 * @return The name of this audio stream.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the volume in the left channel.
	 * 
	 * @param leftVolume The volume in the left channel.
	 */
	public void setLeftVolume(float leftVolume) {
		this.leftVolume = leftVolume;
	}

	/**
	 * Set the volume in the right channel.
	 * 
	 * @param rightVolume The volume in the right channel.
	 */
	public void setRightVolume(float rightVolume) {
		this.rightVolume = rightVolume;
	}

	/**
	 * Set the volume in both left and right channel.
	 * 
	 * @param globalVolume The volume in both left and right channel.
	 */
	public void setGlobalVolume(float globalVolume) {
		this.globalVolume = globalVolume;
	}

	/**
	 * The average volume of an audio stream my be too low or too high compared to others. This method applies a factor on the global
	 * volume. The method checks if the offset value is in range [0, 2].
	 * 
	 * @param factor The volume factor to apply.
	 */
	public void setVolumeFactor(float factor) {
		if (this.factor == factor)
			return;

		factor = Math.min(2, Math.max(0, factor));
		this.factor = factor;
	}

	/**
	 * Adds an effect at the specified index. If the index is greater than the size of the list of effect then the effect will be
	 * added to the end.
	 * 
	 * @param index  The index at which the effect shall be added.
	 * @param effect The effect to add.
	 */
	public void addEffect(int index, IEffect effect) {
		effect.start();

		synchronized (lock) {
			if (effects.size() <= index)
				index = effects.size();

			effects.add(index, effect);
		}
	}

	/**
	 * Stops the effect associated to the given name.
	 * 
	 * @param name The name of the effect to stop.
	 */
	public void removeEffect(String name) {
		synchronized (lock) {
			for (IEffect effect : effects)
				if (effect.getName().equals(name))
					effect.stop();
		}
	}

	/**
	 * Update the parameters of an effect.
	 * 
	 * @param holder An holder that contains the effect name and gather parameter's name / parameter's value.
	 */
	public void updateEffect(IEffectParametersHolder holder) {
		synchronized (lock) {
			for (IEffect effect : effects)
				if (effect.getName().equals(holder.getEffectName()))
					effect.update(holder);
		}
	}

	/**
	 * Add the given bytes array to the underlying queue of the stream.
	 * 
	 * @param data The bytes array that contains the audio sample to add to a stream.
	 */
	public void write(byte[] data) {
		if (data == null)
			return;

		input.write(data);

		// First call
		if (bufferSize == 0) {
			bufferSize = data.length / 2;
			frameDuration = (int) (bufferSize * 1000.0 / sampleRate);
			effectThread.start();
		}
	}

	/**
	 * Read n bytes from the underlying queue, and update the left / right byte array with the correct samples value.
	 * 
	 * @param left   The samples for the left channel (left and global volume applied)
	 * @param right  The samples for the right channel (right and global volume applied)
	 * @param length The number of short to read.
	 * @return The actual number of shorts read from this stream.
	 */
	public int read(short[] left, short[] right, int length) {
		// First call
		if (tmp == null)
			tmp = new short[length];

		int read = output.read(tmp, length);

		// No output for this stream
		if (read == 0)
			return 0;

		float volume = globalVolume * factor;

		// Applying volumes and clipping
		for (int i = 0; i < read; i++) {
			short value = tmp[i];
			left[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * leftVolume * volume));
			right[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * rightVolume * volume));
		}

		return read;
	}

	/**
	 * Clear the content of this audio stream so that the next call to the read method returns 0.
	 */
	public void flush() {
		synchronized (lock) {
			queue.clear();
		}
	}

	/**
	 * Dispose this audio stream, free all resources.
	 */
	public void dispose() {
		input.dispose();
		output.dispose();
	}

	/**
	 * Functions that apply registered effects until the audio stream is disposed.
	 */
	private void process() {
		while (!Thread.currentThread().isInterrupted()) {

			try {
				// Reading raw audio stream
				short[] raw = new short[bufferSize];
				int read = input.read(raw, raw.length);

				// Input buffer is disposed
				if (read == -1)
					break;

				// No new input check for effect tails
				if (read == 0) {
					boolean shallSleep = true;

					if (((System.currentTimeMillis() - lastInputTime) > 3 * frameDuration) && !effects.isEmpty()) {
						short[] tail = new short[bufferSize];
						int[] length = new int[1];

						synchronized (lock) {
							for (IEffect effect : effects)
								if (effect.processTail(tail, length)) {
									output.write(tail, length[0]);
									shallSleep = false;
								}
						}
					}

					if (shallSleep)
						try {
							Thread.sleep(frameDuration);
						} catch (InterruptedException e) {
							break;
						}

					continue;
				}

				lastInputTime = System.currentTimeMillis();

				// Check if there are effects to apply
				if (!effects.isEmpty()) {
					synchronized (lock) {
						Iterator<IEffect> iterator = effects.iterator();
						while (iterator.hasNext()) {
							IEffect effect = iterator.next();
							if (effect.isStopped()) {
								iterator.remove();
								continue;
							}

							effect.apply(raw, read);
						}
					}
				}

				// Adding to the output buffer
				output.write(raw, read);

			} catch (Exception e) {
				input.reset();
				output.reset();
			}
		}
	}
}
