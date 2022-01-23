package fr.pederobien.sound.interfaces;

import javax.sound.sampled.TargetDataLine;

import fr.pederobien.utils.IPausable;

public interface IMicrophone extends IPausable {

	/**
	 * Starts the microphone thread in order to receive data from the underlying {@link TargetDataLine}.
	 */
	public void start();

	/**
	 * Stops the microphone thread. It will never be possible to start this microphone again. It release each system resources.
	 */
	public void stop();

	/**
	 * Force the microphone thread to be paused until the method {@link #resume()} is called.
	 */
	public void pause();

	/**
	 * Resume the microphone thread in order to received again data from the underlying {@link TargetDataLine}.
	 */
	public void resume();
}
