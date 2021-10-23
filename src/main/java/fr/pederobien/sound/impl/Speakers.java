package fr.pederobien.sound.impl;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import fr.pederobien.sound.event.SpeakersDataReadEvent;
import fr.pederobien.sound.event.SpeakersInterruptPostEvent;
import fr.pederobien.sound.event.SpeakersInterruptPreEvent;
import fr.pederobien.sound.event.SpeakersPausePostEvent;
import fr.pederobien.sound.event.SpeakersPausePreEvent;
import fr.pederobien.sound.event.SpeakersRelaunchPostEvent;
import fr.pederobien.sound.event.SpeakersRelaunchPreEvent;
import fr.pederobien.sound.event.SpeakersStartPostEvent;
import fr.pederobien.sound.event.SpeakersStartPreEvent;
import fr.pederobien.sound.interfaces.ISpeakers;
import fr.pederobien.utils.ByteWrapper;
import fr.pederobien.utils.event.EventManager;

public class Speakers implements ISpeakers {
	private SourceDataLine speakers;
	private Thread thread;
	private Mixer mixer;
	private Object mutex;
	private boolean pauseRequested;

	protected Speakers(Mixer mixer) {
		this.mixer = mixer;
		thread = new Thread(() -> execute(), "Speakers");
		thread.setDaemon(true);
		mutex = new Object();
	}

	@Override
	public void start() {
		EventManager.callEvent(new SpeakersStartPreEvent(this), () -> {
			try {
				speakers = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, SoundConstants.SPEAKERS_AUDIO_FORMAT));
				speakers.open(SoundConstants.SPEAKERS_AUDIO_FORMAT);
				EventManager.callEvent(new SpeakersStartPostEvent(this));
				thread.start();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void stop() {
		EventManager.callEvent(new SpeakersInterruptPreEvent(this), () -> {
			if (speakers != null) {
				speakers.stop();
				speakers.close();
			}
			thread.interrupt();
			EventManager.callEvent(new SpeakersInterruptPostEvent(this));
		});
	}

	@Override
	public void pause() {
		EventManager.callEvent(new SpeakersPausePreEvent(this), () -> {
			pauseRequested = true;
			speakers.flush();
			EventManager.callEvent(new SpeakersPausePostEvent(this));
		});
	}

	@Override
	public void resume() {
		EventManager.callEvent(new SpeakersRelaunchPreEvent(this), () -> {
			pauseRequested = false;
			synchronized (mutex) {
				mutex.notify();
			}
			EventManager.callEvent(new SpeakersRelaunchPostEvent(this));
		});
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

	private void execute() {
		speakers.start();
		while (!thread.isInterrupted()) {
			try {
				byte[] data = new byte[SoundConstants.CHUNK_LENGTH];
				int read = mixer.read(data, 0, data.length);

				// Pause request while waiting for data, if data has been received, then ignore.
				if (pauseRequested) {
					sleep();
					continue;
				}

				if (read != data.length)
					data = ByteWrapper.wrap(data).extract(0, read);

				EventManager.callEvent(new SpeakersDataReadEvent(this, data));
				speakers.write(data, 0, read);

				Thread.sleep(5);
			} catch (Exception e) {
				// In order to avoid to stop the speakers thread when an exception occurs while reading bytes.
				e.printStackTrace();
			}
		}

	}
}
