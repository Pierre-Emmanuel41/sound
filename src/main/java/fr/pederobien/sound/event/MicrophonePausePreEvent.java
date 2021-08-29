package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.IMicrophone;
import fr.pederobien.utils.ICancellable;

public class MicrophonePausePreEvent extends MicrophoneEvent implements ICancellable {
	private boolean isCancelled;

	/**
	 * Creates an event thrown when a microphone is about to be paused.
	 * 
	 * @param microphone The microphone that is about to be paused.
	 */
	public MicrophonePausePreEvent(IMicrophone microphone) {
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
