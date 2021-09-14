package fr.pederobien.sound.impl;

import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

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
import fr.pederobien.utils.event.EventManager;
import fr.pederobien.utils.event.IEventListener;

public class Microphone extends Thread implements IMicrophone, IEventListener {
	protected static final AudioFormat FORMAT = new AudioFormat(48000, 16, 1, true, false);
	protected static final int CHUNK_SIZE = 2880;
	private static int N_SHORTS = 0xffff;
	private static final short[] VOLUME_NORM_LUT = new short[N_SHORTS];
	private static int MAX_NEGATIVE_AMPLITUDE = 0x8000;
	private boolean pauseRequested, isInterrupted;
	private TargetDataLine microphone;
	private Semaphore semaphore;
	private IEncoder encoder;

	static {
		preComputeVolumeNormLUT();
	}

	protected Microphone() {
		super("Microphone");

		EventManager.registerListener(this);
		semaphore = new Semaphore(1, true);
		encoder = new Encoder();
		setDaemon(true);
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

	@Override
	public void start() {
		EventManager.callEvent(new MicrophoneStartPreEvent(this), () -> {
			try {
				microphone = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, FORMAT));
				microphone.open(FORMAT);
				EventManager.callEvent(new MicrophoneStartPostEvent(this));
				super.start();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void interrupt() {
		EventManager.callEvent(new MicrophoneInterruptPreEvent(this), () -> {
			isInterrupted = true;
			super.interrupt();
			EventManager.callEvent(new MicrophoneInterruptPostEvent(this));
		});
	}

	@Override
	public void run() {
		microphone.start();
		while (!isInterrupted()) {
			try {
				semaphore.acquire();
				byte[] data = new byte[CHUNK_SIZE * 2];
				final int numBytesRead = microphone.read(data, 0, data.length);

				if (pauseRequested) {
					semaphore.release();
					Thread.sleep(100);
					continue;
				}

				if (numBytesRead != data.length)
					data = ByteWrapper.wrap(data).extract(0, numBytesRead);

				if (isInterrupted)
					break;

				normalizeVolume(data);
				byte[] encoded = encoder.encode(data);
				if (encoded.length > 0)
					EventManager.callEvent(new MicrophoneDataEncodedEvent(this, data, encoded));
				semaphore.release();
			} catch (InterruptedException e) {
				break;
			} catch (Error e) {
				e.printStackTrace();
			}
		}

		microphone.stop();
		microphone.close();
		EventManager.unregisterListener(this);
	}

	@Override
	public void pause() {
		EventManager.callEvent(new MicrophonePausePreEvent(this), () -> {
			pauseRequested = true;
			try {
				EventManager.callEvent(new MicrophonePausePostEvent(this));
				semaphore.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void relaunch() {
		EventManager.callEvent(new MicrophoneRelaunchPreEvent(this), () -> {
			pauseRequested = false;
			semaphore.release();
			EventManager.callEvent(new MicrophoneRelaunchPostEvent(this));
		});
	}
}
