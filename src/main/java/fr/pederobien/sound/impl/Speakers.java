package fr.pederobien.sound.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.SourceDataLine;

import fr.pederobien.sound.event.SpeakersClosePostEvent;
import fr.pederobien.sound.event.SpeakersClosePreEvent;
import fr.pederobien.sound.event.SpeakersOpenPostEvent;
import fr.pederobien.sound.event.SpeakersOpenPreEvent;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.sound.interfaces.ISpeakers;
import fr.pederobien.utils.ByteWrapper;
import fr.pederobien.utils.event.EventManager;
import fr.pederobien.utils.event.Logger;

public class Speakers implements ISpeakers {
	private final IMixer mixer;
	private final SourceDataLine speakers;
	private final AtomicBoolean isOpened;

	private Thread player;

	/**
	 * Creates speakers.
	 * 
	 * @param mixer The mixer used to create the underlying SourceDataLine and player processed samples.
	 */
	protected Speakers(IMixer mixer) {
		this.mixer = mixer;
		this.speakers = mixer.getSpeakersLine();

		isOpened = new AtomicBoolean(false);
	}

	@Override
	public void open() throws Exception {
		// Speakers already opened
		if (!isOpened.compareAndSet(false, true))
			return;

		SpeakersOpenPreEvent preEvent = new SpeakersOpenPreEvent(this);
		EventManager.callEvent(preEvent);

		if (preEvent.isCancelled())
			return;

		mixer.flush();
		speakers.open();
		speakers.start();

		player = new Thread(this::play, "Speakers");
		player.setDaemon(true);
		player.start();

		Logger.info("Speakers enabled");
		EventManager.callEvent(new SpeakersOpenPostEvent(this));
	}

	@Override
	public void close() throws Exception {
		// Speakers already closed
		if (!isOpened.compareAndSet(true, false))
			return;

		SpeakersClosePreEvent preEvent = new SpeakersClosePreEvent(this);
		EventManager.callEvent(preEvent);

		if (preEvent.isCancelled())
			return;

		speakers.drain();
		speakers.stop();
		speakers.close();
		player.interrupt();

		Logger.info("Speakers disabled");
		EventManager.callEvent(new SpeakersClosePostEvent(this));
	}

	private void play() {
		while (isOpened.get()) {
			try {
				byte[] buffer = new byte[speakers.getBufferSize() / 5];
				int read = mixer.read(buffer);

				// Checking conditions to continue
				if (!isOpened.get())
					break;

				// Resizing buffer
				if (read != buffer.length)
					buffer = ByteWrapper.wrap(buffer).extract(0, read);

				// Playing data to the actual speakers
				speakers.write(buffer, 0, buffer.length);
			} catch (Exception e) {
				// In order to avoid to stop the speakers thread when an exception occurs while reading bytes.
				e.printStackTrace();
			}
		}
	}
}
