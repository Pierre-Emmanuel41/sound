package fr.pederobien.sound.interfaces;

public interface IEffect extends IFilter {

	/**
	 * Apply effect on an audio stream. The method shall transition smoothly from no modification to normal modification associated to
	 * this effect to avoid potentials clicks in the output audio stream.
	 */
	void start();

	/**
	 * Stop applying effect on an audio stream. The method shall transition smoothly from normal modification associated to this
	 * effect to no modification to avoid potentials clicks in the output audio stream.
	 */
	void stop();

	/**
	 * @return True if the transition to no modification is over, false otherwise.
	 */
	boolean isStopped();

	/**
	 * Update the parameters values of this effect. The parameters defines how this effect modifies input audio stream.
	 * 
	 * @param values An list of values of parameter.
	 */
	void setValues(Object... values);

	/**
	 * This method is called when there is no new samples for an audio stream, but the effect is not finished.
	 * 
	 * @param buffer An buffer that contains only 0 and to be filled with effect tail.
	 * @param length An array of one integer that contains the number of bytes written in the input buffer.
	 * @return True if the effect still has remaining audio to output, false if fully dried up.
	 */
	boolean processTail(short[] buffer, int[] length);
}
