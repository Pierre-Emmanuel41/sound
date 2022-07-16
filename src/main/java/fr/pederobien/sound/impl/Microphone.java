package fr.pederobien.sound.impl;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.event.EncoderFailToEncodeEvent;
import fr.pederobien.sound.event.MicrophoneDataEncodedEvent;
import fr.pederobien.sound.event.MicrophoneInterruptPostEvent;
import fr.pederobien.sound.event.MicrophoneInterruptPreEvent;
import fr.pederobien.sound.event.MicrophonePausePostEvent;
import fr.pederobien.sound.event.MicrophonePausePreEvent;
import fr.pederobien.sound.event.MicrophoneRelaunchPostEvent;
import fr.pederobien.sound.event.MicrophoneRelaunchPreEvent;
import fr.pederobien.sound.event.MicrophoneStartPostEvent;
import fr.pederobien.sound.event.MicrophoneStartPreEvent;
import fr.pederobien.sound.interfaces.IEncoder;
import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.utils.ByteWrapper;
import fr.pederobien.utils.event.EventHandler;
import fr.pederobien.utils.event.EventManager;
import fr.pederobien.utils.event.IEventListener;

public class Microphone implements IMicrophone, IEventListener {
	private static int N_SHORTS = 0xffff;
	private static final short[] VOLUME_NORM_LUT = new short[N_SHORTS];
	private static int MAX_NEGATIVE_AMPLITUDE = 0x8000;
	private TargetDataLine microphone;
	private IEncoder encoder;
	private Thread thread;
	private Lock lock;
	private Condition sleep;
	private boolean pauseRequested, interrupt;
	private PausableState state;

	static {
		preComputeVolumeNormLUT();
	}

	protected Microphone() {
		lock = new ReentrantLock(true);
		sleep = lock.newCondition();
		encoder = new Encoder();
		state = PausableState.NOT_STARTED;
		EventManager.registerListener(this);
	}

	@Override
	public void start() {
		if (state == PausableState.STARTED || state == PausableState.PAUSED)
			return;

		Supplier<Boolean> start = () -> {
			try {
				microphone = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, SoundConstants.MICROPHONE_AUDIO_FORMAT));
				microphone.open(SoundConstants.MICROPHONE_AUDIO_FORMAT);

				interrupt = false;
				thread = new Thread(() -> execute(), "Microphone");
				thread.setDaemon(true);
				thread.start();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				return false;
			}
			state = PausableState.STARTED;
			return true;
		};
		EventManager.callEvent(new MicrophoneStartPreEvent(this), start, new MicrophoneStartPostEvent(this));
	}

	@Override
	public void stop() {
		if (state == PausableState.NOT_STARTED)
			return;

		Runnable stop = () -> {
			interrupt = true;
			state = PausableState.NOT_STARTED;
		};
		EventManager.callEvent(new MicrophoneInterruptPreEvent(this), stop, new MicrophoneInterruptPostEvent(this));
	}

	@Override
	public void pause() {
		if (state == PausableState.NOT_STARTED || state == PausableState.PAUSED)
			return;

		Runnable pause = () -> {
			pauseRequested = true;
			microphone.flush();
			state = PausableState.PAUSED;
		};
		EventManager.callEvent(new MicrophonePausePreEvent(this), pause, new MicrophonePausePostEvent(this));
	}

	@Override
	public void resume() {
		if (state == PausableState.NOT_STARTED || state == PausableState.STARTED)
			return;

		Runnable resume = () -> {
			pauseRequested = false;
			state = PausableState.STARTED;
			signal();
		};
		EventManager.callEvent(new MicrophoneRelaunchPreEvent(this), resume, new MicrophoneRelaunchPostEvent(this));
	}

	@Override
	public PausableState getState() {
		return state;
	}

	@EventHandler
	private void onEncodeFail(EncoderFailToEncodeEvent event) {
		System.err.println("[Microphone] Fail to encode bytes array");
	}

	private void execute() {
		microphone.start();
		while (!interrupt) {
			try {
				byte[] data = new byte[SoundConstants.CHUNK_LENGTH * 2];
				final int numBytesRead = microphone.read(data, 0, data.length);

				if (pauseRequested)
					sleep();

				if (numBytesRead != data.length)
					data = ByteWrapper.wrap(data).extract(0, numBytesRead);

				normalizeVolume(data);

				byte[] encoded = encoder.encode(data);
				if (encoded.length > 0)
					EventManager.callEvent(new MicrophoneDataEncodedEvent(this, data, encoded));

				Thread.sleep(5);
			} catch (InterruptedException e) {
				// Do nothing
			} catch (Exception e) {
				// In order to avoid to stop the speakers thread when an exception occurs while reading bytes.
				e.printStackTrace();
			}
		}

		microphone.stop();
		microphone.close();
	}

	private void normalizeVolume(byte[] audioSamples) {
		for (int i = 0; i < audioSamples.length; i += 2) {
			short res = (short) ((audioSamples[i + 1] & 0xff) << 8 | audioSamples[i] & 0xff);

			res = VOLUME_NORM_LUT[Math.min(res + MAX_NEGATIVE_AMPLITUDE, N_SHORTS - 1)];
			audioSamples[i] = (byte) res;
			audioSamples[i + 1] = (byte) (res >> 8);
		}
	}

	private static void preComputeVolumeNormLUT() {
		for (int s = 0; s < N_SHORTS; s++) {
			double v = s - MAX_NEGATIVE_AMPLITUDE;
			double sign = Math.signum(v);
			// Non-linear volume boost function
			// fitted exponential through (0,0), (10000, 25000), (32767, 32767)
			VOLUME_NORM_LUT[s] = (short) (sign * (1.240769e-22 - (-4.66022 / 0.0001408133) * (1 - Math.exp(-0.0001408133 * v * sign))));
		}
	}

	/**
	 * Forces the microphone thread to sleep until the {@link #sleep} condition is signaled.
	 */
	private void sleep() {
		lock.lock();
		try {
			sleep.await();
		} catch (InterruptedException e) {
			// do nothing
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Signal the {@link #sleep} condition in order to awake the microphone thread.
	 */
	private void signal() {
		lock.lock();
		try {
			sleep.signal();
		} finally {
			lock.unlock();
		}
	}
}
