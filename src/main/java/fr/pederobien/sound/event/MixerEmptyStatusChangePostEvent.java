package fr.pederobien.sound.event;

import java.util.StringJoiner;

import fr.pederobien.sound.interfaces.IMixer;

public class MixerEmptyStatusChangePostEvent extends MixerEvent {
	private boolean oldEmpty;

	/**
	 * Creates an event thrown when there is no bytes to read in the given mixer.
	 * 
	 * @param mixer    The mixer whose the empty status has changed.
	 * @param oldEmpty True if the mixer was previously empty, false otherwise.
	 */
	public MixerEmptyStatusChangePostEvent(IMixer mixer, boolean oldEmpty) {
		super(mixer);
		this.oldEmpty = oldEmpty;
	}

	/**
	 * @return True if the mixer is now empty, false otherwise.
	 */
	public boolean isEmpty() {
		return !getOldEmpty();
	}

	/**
	 * @return The old mixer's empty status.
	 */
	public boolean getOldEmpty() {
		return oldEmpty;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ", "{", "}");
		joiner.add("mixer=#" + getMixer().hashCode());
		joiner.add("currentEmpty=" + isEmpty());
		joiner.add("oldEmpty=" + getOldEmpty());
		return String.format("%s_%s", getName(), joiner);
	}
}
