package fr.pederobien.sound.impl;

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

public class Microphone extends Thread implements IMicrophone {
	private static int N_SHORTS = 0xffff;
	private static final short[] VOLUME_NORM_LUT = new short[N_SHORTS];
	private static int MAX_NEGATIVE_AMPLITUDE = 0x8000;
	private TargetDataLine microphone;
	private IEncoder encoder;
	private Object mutex;
	private boolean pauseRequested, isInterrupted;

	static {
		preComputeVolumeNormLUT();
	}

	protected Microphone() {
		super("Microphone");

		mutex = new Object();
		encoder = new Encoder();
		setDaemon(true);
	}

	@Override
	public void start() {
		EventManager.callEvent(new MicrophoneStartPreEvent(this), () -> {
			try {
				microphone = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, SoundConstants.MICROPHONE_AUDIO_FORMAT));
				microphone.open(SoundConstants.MICROPHONE_AUDIO_FORMAT);
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
				byte[] data = new byte[SoundConstants.CHUNK_LENGTH * 2];
				final int numBytesRead = microphone.read(data, 0, data.length);

				if (pauseRequested)
					sleep();

				if (numBytesRead != data.length)
					data = ByteWrapper.wrap(data).extract(0, numBytesRead);

				if (isInterrupted)
					break;

				normalizeVolume(data);
				byte[] encoded = encoder.encode(data);
				if (encoded.length > 0)
					EventManager.callEvent(new MicrophoneDataEncodedEvent(this, data, encoded));
			} catch (Error e) {
				e.printStackTrace();
			}
		}

		microphone.stop();
		microphone.close();
	}

	@Override
	public void pause() {
		EventManager.callEvent(new MicrophonePausePreEvent(this), () -> {
			pauseRequested = true;
			EventManager.callEvent(new MicrophonePausePostEvent(this));
		});
	}

	@Override
	public void relaunch() {
		EventManager.callEvent(new MicrophoneRelaunchPreEvent(this), () -> {
			pauseRequested = false;
			synchronized (mutex) {
				mutex.notify();
			}
			EventManager.callEvent(new MicrophoneRelaunchPostEvent(this));
		});
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

	private void sleep() {
		synchronized (mutex) {
			try {
				mutex.wait();
			} catch (InterruptedException e) {
				// Do nothing
			}
		}
	}
}
