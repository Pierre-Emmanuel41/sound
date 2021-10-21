package fr.pederobien.sound.impl;

public class AudioSample {
	private static final int LOGISTICS_RANGE = 80;
	private byte[] data;
	private double global, left, right;
	private int position;
	private boolean logisticsApplied;

	/**
	 * Creates an audio sample based on the given bytes array.
	 * 
	 * @param data   The bytes array that contains the audio sample.
	 * @param packet The packet that contain sound properties of the sample.
	 */
	public AudioSample(byte[] data, AudioPacket packet) {
		this.data = data;
		this.global = packet.getGlobalVolume();
		this.left = packet.getLeftVolume();
		this.right = packet.getRightVolume();
	}

	/**
	 * Reads audio data from the input buffer. The requested number of bytes is read into the specified array, starting at the
	 * specified offset into the array in bytes.
	 * 
	 * @param buffer A byte array that will contain the requested input data when this method returns.
	 * @param offset The offset from the beginning of the array, in bytes.
	 * @param length The requested number of bytes to read.
	 * @param next   The next audio sample in order to perform the sound volume passage.
	 * @return The number of bytes actually read.
	 */
	public int read(int[] buffer, int offset, int length, AudioSample next) {
		if (isRead())
			return 0;

		int read = length;

		// The number of bytes to read exceeds the size of the bytes array from the position value.
		if (position + length >= data.length)
			read = data.length - position;

		Logistic globalLogistic = null, leftLogistic = null, rightLogistic = null;
		if (next != null) {
			globalLogistic = new Logistic(global, next.getGlobal(), 1.0);
			leftLogistic = new Logistic(left, next.getLeft(), 1.0);
			rightLogistic = new Logistic(right, next.getRight(), 1.0);
		}

		for (int i = offset; i < read; i += 2) {
			// Transforming bytes to short value
			short leftValue = toShort(data[position + 1], data[position]);
			short rightValue = toShort(data[position + 3], data[position + 2]);

			// Applying sound volume
			if (!isSoundVolumeAlreadyPerformed()) {
				// Computing once for optimization.
				double logisticIndex = (double) (position - data.length) / 20.0;

				// Evolution range, needs to apply logistics functions if and only if the next audio sample is not null
				if (next != null && -LOGISTICS_RANGE < logisticIndex) {
					double sampleGlobal = globalLogistic.value(logisticIndex);
					leftValue = (short) (leftValue * sampleGlobal * leftLogistic.value(logisticIndex));
					rightValue = (short) (rightValue * sampleGlobal * rightLogistic.value(logisticIndex));
				} else {
					leftValue = (short) (leftValue * global * left);
					rightValue = (short) (rightValue * global * right);
				}
			}

			// Updating the output buffer
			buffer[i] = leftValue;
			buffer[i + 1] = rightValue;

			// Updating the reading position value.
			position += 4;
		}

		// Applying sound volume on the beginning of the next audio sample.
		if (next != null)
			next.applyLogistics(globalLogistic, leftLogistic, rightLogistic);

		return read;
	}

	private void applyLogistics(Logistic globalLogistic, Logistic leftLogistic, Logistic rightLogistic) {
		if (logisticsApplied)
			return;

		for (int i = 0; i < data.length / 2; i += 4) {
			// Transforming bytes to short value
			short leftValue = toShort(data[i + 1], data[i]);
			short rightValue = toShort(data[i + 3], data[i + 2]);

			// Evolution range, needs to apply logistics functions
			if (i < LOGISTICS_RANGE) {
				// Computing once for optimization.
				double logisticIndex = i / 20.0;
				double sampleGlobal = globalLogistic.value(logisticIndex);

				leftValue = (short) (leftValue * sampleGlobal * leftLogistic.value(logisticIndex));
				rightValue = (short) (rightValue * sampleGlobal * rightLogistic.value(logisticIndex));
			} else {
				leftValue = (short) (leftValue * global * left);
				rightValue = (short) (rightValue * global * right);
			}

			// left channel bytes
			data[i + 1] = (byte) ((leftValue >> 8) & 0xFF); // MSB
			data[i] = (byte) (leftValue & 0xFF); // LSB
			// then right channel bytes
			data[i + 3] = (byte) ((rightValue >> 8) & 0xFF); // MSB
			data[i + 2] = (byte) (rightValue & 0xFF); // LSB
		}

		logisticsApplied = true;
	}

	/**
	 * @return The bytes array that represents the audio sample.
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * @return The global volume for this sample.
	 */
	public double getGlobal() {
		return global;
	}

	/**
	 * @return The volume of the left audio channel.
	 */
	public double getLeft() {
		return left;
	}

	/**
	 * @return The volume of the right audio channel.
	 */
	public double getRight() {
		return right;
	}

	/**
	 * @return The current position in order to read bytes for this sample.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @return True if this sample has been read, false otherwise.
	 */
	public boolean isRead() {
		return position >= data.length;
	}

	/**
	 * @return True if at the current reading position the sound volume has already been performed.
	 */
	private boolean isSoundVolumeAlreadyPerformed() {
		return logisticsApplied && position < data.length / 2;
	}

	/**
	 * Creates a short from the two given bytes.
	 * 
	 * @param msb The most significant byte.
	 * @param lsb The less significant byte.
	 * 
	 * @return The short value associated to the two bytes.
	 */
	private short toShort(byte msb, byte lsb) {
		return (short) ((msb << 8) | (lsb & 0xFF));
	}

	private class Logistic {
		private double before, after, growthRate;

		/**
		 * Creates a logistic function in order to go continuously from the <code>before</code> value to the <code>after</code> value.
		 * 
		 * @param before     The original value.
		 * @param after      The target value.
		 * @param growthRate The rate at which the the function growth from <i>before</i> to <i>after</i>.
		 */
		public Logistic(double before, double after, double growthRate) {
			this.before = before;
			this.after = after;
			this.growthRate = growthRate;
		}

		public double value(double x) {
			return before + (after - before) / (1 + Math.exp(-growthRate * x));
		}
	}
}
