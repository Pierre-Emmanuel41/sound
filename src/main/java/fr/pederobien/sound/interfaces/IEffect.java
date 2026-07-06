package fr.pederobien.sound.interfaces;

public interface IEffect {

	/**
	 * Value used to normalize the raw stream values.
	 */
	static final double SHORT_MAX_VALUE = (double) Short.MAX_VALUE + 1;

	/**
	 * @return The name of the effect.
	 */
	default String getName() {
		return getClass().getSimpleName();
	}

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
	 * Apply an effect on the given input buffer that represents an audio stream. The method returns a new array of short containing
	 * the modified audio stream. If the effect is stopped, the method shall return the input buffer.
	 * 
	 * @param buffer The buffer that contains frames of an audio stream.
	 * @return The modified audio stream.
	 */
	short[] apply(short[] buffer);

	/**
	 * Update the parameters values of this effect. The parameters defines how this effect modifies input audio stream.
	 * 
	 * @param values An list of values of parameter.
	 */
	void setValues(Object... values);
}
