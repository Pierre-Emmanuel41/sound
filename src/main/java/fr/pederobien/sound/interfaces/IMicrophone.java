package fr.pederobien.sound.interfaces;

import javax.sound.sampled.TargetDataLine;

public interface IMicrophone {

	/**
	 * Method to start the microphone in order to receive data from the underlying {@link TargetDataLine}.
	 */
	public void start();

	/**
	 * Method to interrupt the microphone. It will never be possible to start again this microphone. It release each system resources.
	 */
	public void interrupt();

	/**
	 * Force the microphone thread to be paused until the method {@link #relaunch()} is called.
	 */
	public void pause();

	/**
	 * Relaunch the microphone thread in order to received again data from the underlying {@link TargetDataLine}.
	 */
	public void relaunch();
}
