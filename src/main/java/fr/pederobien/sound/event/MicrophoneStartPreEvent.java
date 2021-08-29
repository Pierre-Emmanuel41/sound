package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.utils.ICancellable;

public class MicrophoneStartPreEvent extends MicrophoneEvent implements ICancellable {
	private boolean isCancelled;

	/**
	 * Creates an event thrown when the microphone is about to start.
	 * 
	 * @param microphone The microphone that is about to start.
	 */
	public MicrophoneStartPreEvent(IMicrophone microphone) {
		super(microphone);
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
}
