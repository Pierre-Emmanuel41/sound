package fr.pederobien.sound.interfaces;

public interface IFilter {
	/**
	 * Value used to normalize the raw stream values.
	 */
	static final double SHORT_MAX_VALUE = (double) Short.MAX_VALUE + 1;

	/**
	 * Computes the square root of the given number.
	 * 
	 * @param number The input number.
	 * @return The square root of the input number.
	 */
	default double fastSqrt(double number) {
		// Bitwise approximation for speed
		double sqrt = Double.longBitsToDouble(((Double.doubleToLongBits(number) - (1l << 52)) >> 1) + (1l << 61));

		// One Newton-Raphson iteration to improve accuracy
		return (sqrt + number / sqrt) / 2.0;
	}

	/**
	 * @return The name of the filter, used only for logging.
	 */
	default String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * Apply a filter on the given array of short. The input buffer will directly be modified with the filtered values.
	 * 
	 * @param buffer The buffer that contains the raw microphone audio stream.
	 */
	void apply(short[] buffer);
}
