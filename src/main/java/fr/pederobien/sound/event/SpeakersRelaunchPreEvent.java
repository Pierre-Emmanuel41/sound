package fr.pederobien.sound.event;

import fr.pederobien.sound.interfaces.ISpeakers;
import fr.pederobien.utils.ICancellable;

public class SpeakersRelaunchPreEvent extends SpeakersEvent implements ICancellable {
	private boolean isCancelled;

	/**
	 * Creates an event thrown when the speakers are about to be relaunched.
	 * 
	 * @param speakers The speakers that is about to be relaunched.
	 */
	public SpeakersRelaunchPreEvent(ISpeakers speakers) {
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
