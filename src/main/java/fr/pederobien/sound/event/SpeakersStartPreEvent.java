package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;
import fr.pederobien.utils.ICancellable;

public class SpeakersStartPreEvent extends SpeakersEvent implements ICancellable {
	private boolean isCancelled;

	/**
	 * Creates an event thrown when the speakers are about to start.
	 * 
	 * @param speakers The speakers that are about to start.
	 */
	public SpeakersStartPreEvent(ISpeakers speakers) {
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
