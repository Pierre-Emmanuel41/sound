package fr.pederobien.sound.interfaces;

public interface IFilter {
	/**
	 * Value used to normalize the raw stream values.
	 */
	static final double SHORT_MAX_VALUE = (double) Short.MAX_VALUE + 1;

	/**
	 * @return The name of the filter, used only for logging.
	 */
	default String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * Apply a filter on the given array of short. The input buffer will directly be
	 * modified with the filtered values.
	 * 
	 * @param buffer The buffer that contains the raw microphone audio stream.
	 */
	void apply(short[] buffer);
}
