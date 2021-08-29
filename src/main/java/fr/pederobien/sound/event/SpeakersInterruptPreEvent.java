package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;
import fr.pederobien.utils.ICancellable;

public class SpeakersInterruptPreEvent extends SpeakersEvent implements ICancellable {
	private boolean isCancelled;

	/**
	 * Creates an event thrown when the speakers are about to be interrupted.
	 * 
	 * @param speakers The speakers that are about to be interrupted.
	 */
	public SpeakersInterruptPreEvent(ISpeakers speakers) {
		super(speakers);
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
