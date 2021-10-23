package fr.pederobien.sound.interfaces;

import javax.sound.sampled.SourceDataLine;

import fr.pederobien.utils.IPausable;

public interface ISpeakers extends IPausable {

	/**
	 * Starts the speakers thread in order to play data with the underlying {@link SourceDataLine}.
	 */
	public void start();

	/**
	 * Stops the speakers thread. It will never be possible to start the speakers again. It release each system resources.
	 */
	public void stop();

	/**
	 * Force the speaker thread to be paused until the method {@link #resume()} is called.
	 */
	public void pause();

	/**
	 * Resumes the speakers thread in order to play again data with the underlying {@link SourceDataLine}.
	 */
	public void resume();
}
