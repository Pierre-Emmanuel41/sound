package fr.pederobien.sound.impl;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

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
	private Lock lock;
	private Condition sleep;
	private boolean pauseRequested, interrupt;
	private PausableState state;

	protected Speakers(Mixer mixer) {
		this.mixer = mixer;
		try {
			speakers = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, SoundConstants.SPEAKERS_AUDIO_FORMAT));
			lock = new ReentrantLock(true);
			sleep = lock.newCondition();
			state = PausableState.NOT_STARTED;
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
		if (state == PausableState.STARTED || state == PausableState.PAUSED)
			return;

		Supplier<Boolean> start = () -> {
			try {
				if (speakers == null)
					return false;

				speakers.open(SoundConstants.SPEAKERS_AUDIO_FORMAT);

				interrupt = false;
				thread = new Thread(() -> execute(), "Speakers");
				thread.setDaemon(true);
				thread.start();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				return false;
			}
			state = PausableState.STARTED;
			return true;
		};
		EventManager.callEvent(new SpeakersStartPreEvent(this), start, new SpeakersStartPostEvent(this));
	}

	@Override
	public void stop() {
		if (state == PausableState.NOT_STARTED)
			return;

		Runnable stop = () -> {
			interrupt = true;
			state = PausableState.NOT_STARTED;
		};
		EventManager.callEvent(new SpeakersInterruptPreEvent(this), stop, new SpeakersInterruptPostEvent(this));
	}

	@Override
	public void pause() {
		if (state == PausableState.NOT_STARTED || state == PausableState.PAUSED)
			return;

		Runnable pause = () -> {
			pauseRequested = true;
			state = PausableState.PAUSED;
		};
		EventManager.callEvent(new SpeakersPausePreEvent(this), pause, new SpeakersPausePostEvent(this));
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
		EventManager.callEvent(new SpeakersRelaunchPreEvent(this), resume, new SpeakersRelaunchPostEvent(this));
	}

	@Override
	public PausableState getState() {
		return state;
	}

	/**
	 * Forces the speaker thread to sleep until the {@link #sleep} condition is signaled.
	 */
	private void sleep() {
		lock.lock();
		try {
			speakers.flush();
			speakers.stop();
			speakers.close();
			sleep.await();
			speakers.open();
			speakers.start();
		} catch (InterruptedException | LineUnavailableException e) {
			// do nothing
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Signal the {@link #sleep} condition in order to awake the speakers thread.
	 */
	private void signal() {
		lock.lock();
		try {
			sleep.signal();
		} finally {
			lock.unlock();
		}
	}

	private void execute() {
		speakers.start();
		while (!interrupt) {
			try {
				byte[] data = new byte[SoundConstants.CHUNK_LENGTH];
				int read = mixer.read(data, 0, data.length);

				if (read == 0)
					continue;

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
			} catch (InterruptedException e) {
				// Do nothing
			} catch (Exception e) {
				// In order to avoid to stop the speakers thread when an exception occurs while reading bytes.
				e.printStackTrace();
			}
		}

		speakers.stop();
		speakers.flush();
		speakers.close();
	}
}
