package fr.pederobien.sound.impl;

import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.event.MicrophoneDataReadEvent;
import fr.pederobien.sound.event.MicrophoneInterruptPostEvent;
import fr.pederobien.sound.event.MicrophoneInterruptPreEvent;
import fr.pederobien.sound.event.MicrophonePausePostEvent;
import fr.pederobien.sound.event.MicrophonePausePreEvent;
import fr.pederobien.sound.event.MicrophoneRelaunchPostEvent;
import fr.pederobien.sound.event.MicrophoneRelaunchPreEvent;
import fr.pederobien.sound.event.MicrophoneStartPostEvent;
import fr.pederobien.sound.event.MicrophoneStartPreEvent;
import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.utils.ByteWrapper;
import fr.pederobien.utils.event.EventManager;

public class Microphone extends Thread implements IMicrophone {
	protected static final AudioFormat FORMAT = new AudioFormat(48000, 16, 1, true, false);
	protected static final int CHUNK_SIZE = 2880;
	private boolean pauseRequested;
	private TargetDataLine microphone;
	private Semaphore semaphore;

	protected Microphone() {
		super("Microphone");
		semaphore = new Semaphore(1, true);
		setDaemon(true);
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
			if (microphone != null) {
				microphone.stop();
				microphone.close();
			}
			super.interrupt();
			EventManager.callEvent(new MicrophoneInterruptPostEvent(this));
		});
	}

	@Override
	public void run() {
		byte[] data = new byte[CHUNK_SIZE];
		microphone.start();
		while (!isInterrupted()) {
			try {
				semaphore.acquire();
				final int numBytesRead = microphone.read(data, 0, CHUNK_SIZE);

				if (pauseRequested) {
					semaphore.release();
					Thread.sleep(100);
					continue;
				}

				if (numBytesRead != CHUNK_SIZE)
					data = ByteWrapper.wrap(data).extract(0, numBytesRead);
				EventManager.callEvent(new MicrophoneDataReadEvent(this, data));
				semaphore.release();
			} catch (InterruptedException e) {
				break;
			}
		}
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
