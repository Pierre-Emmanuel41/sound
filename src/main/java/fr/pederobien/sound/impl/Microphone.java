package fr.pederobien.sound.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.TargetDataLine;

import fr.pederobien.sound.event.MicrophoneClosePostEvent;
import fr.pederobien.sound.event.MicrophoneClosePreEvent;
import fr.pederobien.sound.event.MicrophoneOpenPostEvent;
import fr.pederobien.sound.event.MicrophoneOpenPreEvent;
import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.sound.interfaces.IMixer;
import fr.pederobien.utils.ByteWrapper;
import fr.pederobien.utils.event.EventManager;
import fr.pederobien.utils.event.Logger;

public class Microphone implements IMicrophone {
	private final IMixer mixer;
	private final TargetDataLine microphone;
	private final AtomicBoolean isOpened;

	private Thread fetcher;

	/**
	 * Creates a microphone.
	 * 
	 * @param mixer The mixer used to create the underlying TargetDataLine and post process the microphone output.
	 */
	protected Microphone(IMixer mixer) {
		this.mixer = mixer;
		this.microphone = mixer.getMicrophoneLine();

		isOpened = new AtomicBoolean(false);
	}

	@Override
	public void open() throws Exception {
		// Microphone already opened
		if (!isOpened.compareAndSet(false, true))
			return;

		MicrophoneOpenPreEvent preEvent = new MicrophoneOpenPreEvent(this);
		EventManager.callEvent(preEvent);

		if (preEvent.isCancelled())
			return;

		microphone.open();
		microphone.start();

		fetcher = new Thread(this::process, "Fetcher");
		fetcher.setDaemon(true);
		fetcher.start();

		Logger.info("Microphone enabled");
		EventManager.callEvent(new MicrophoneOpenPostEvent(this));
	}

	@Override
	public void close() throws Exception {
		// Microphone is already closed
		if (!isOpened.compareAndSet(true, false))
			return;

		MicrophoneClosePreEvent preEvent = new MicrophoneClosePreEvent(this);
		EventManager.callEvent(preEvent);

		if (preEvent.isCancelled())
			return;

		microphone.stop();
		microphone.drain();
		microphone.close();

		Logger.info("Microphone disabled");
		EventManager.callEvent(new MicrophoneClosePostEvent(this));
	}

	@Override
	public byte[] fetch() {
		return mixer.fetchProcessedMicrophoneData();
	}

	private void process() {
		while (isOpened.get()) {
			byte[] buffer = new byte[microphone.getBufferSize() / 5];
			int read = microphone.read(buffer, 0, buffer.length);

			// Checking condition to continue
			if (read == 0)
				return;

			if (read != buffer.length)
				buffer = ByteWrapper.wrap(buffer).extract(0, read);

			mixer.registerRawMicrophoneData(buffer);
		}
	}
}
