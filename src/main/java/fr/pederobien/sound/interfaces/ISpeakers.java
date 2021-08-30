package fr.pederobien.sound.interfaces;

import javax.sound.sampled.SourceDataLine;

public interface ISpeakers {

	/**
	 * Method to start the speakers in order to play data with the underlying {@link SourceDataLine}.
	 */
	public void start();

	/**
	 * Method to interrupt the speakers. It will never be possible to start again the speakers. It release each system resources.
	 */
	public void interrupt();

	/**
	 * Force the speaker thread to be paused until the method {@link #relaunch()} is called.
	 */
	public void pause();

	/**
	 * Relaunch the speakers thread in order to play again data with the underlying {@link SourceDataLine}.
	 */
	public void relaunch();
}
