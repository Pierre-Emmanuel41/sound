package fr.pederobien.sound.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.commons.math3.analysis.function.Logistic;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import fr.pederobien.sound.interfaces.IEncoder;
import fr.pederobien.utils.ByteWrapper;

public class Encoder implements IEncoder {
	private FastFourierTransformer fastFourierTransformer;
	private AtomicBoolean isStarted;
	private double lowpassRate, highpassRate;
	private byte[] lastSample;
	private Logistic logistic;

	public Encoder(double lowpassRate, double highpassRate) {
		this.lowpassRate = lowpassRate;
		this.highpassRate = highpassRate;

		fastFourierTransformer = new FastFourierTransformer(DftNormalization.STANDARD);
		isStarted = new AtomicBoolean(false);

		double k = 1, m = 1, b = 20, q = 0.000001, a = 0, n = 1;
		logistic = new Logistic(k, m, b, q, a, n);
	}

	@Override
	public void start() {
		isStarted.set(true);
	}

	@Override
	public void stop() {
		isStarted.set(false);
	}

	@Override
	public void encode(byte[] data, Consumer<byte[]> action) {
		encode(new Action(data, action));
	}

	@Override
	public void decode(byte[] data, Consumer<byte[]> action) {
		decode(new Action(data, action));
	}

	protected void encode(Action action) {
		// Need minimum 2 samples to force the output signal to be continuous
		if (lastSample == null) {
			lastSample = action.getData();
			return;
		}

		// Encoding the concatenation of the last sample and the current sample.
		byte[] encoded = encode(ByteWrapper.wrap(lastSample).put(action.getData()).get());

		// The last sample becomes the current one
		lastSample = action.getData();

		// No need to run the callback if no data encoded.
		if (encoded.length == 0)
			return;

		action.getAction().accept(encoded);
	}

	protected void decode(Action action) {
		action.getAction().accept(decode(action.getData()));
	}

	private byte[] encode(byte[] bytes) {
		// from byte to short, from short to double.
		double[] buffer = fromBytesToDoubles(bytes);

		// Spectral analysis.
		Complex[] result = fastFourierTransformer.transform(buffer, TransformType.FORWARD);

		// Frequencies selection in order to send a pure signal.
		Complex[] selection = select(result);

		// Sample that contains only noise, no need to send.
		if (!isPlayerSpeaking(selection))
			return new byte[0];

		// Transforming complexes array as bytes array.
		return export(selection);
	}

	private byte[] decode(byte[] bytes) {
		Complex[] buffer = extract(bytes);

		// Creating the original signal from the spectral analysis.
		Complex[] result = fastFourierTransformer.transform(buffer, TransformType.INVERSE);

		// Creating the signal bytes array.
		return getRealSignal(result);
	}

	private Complex[] select(Complex[] complexes) {
		double min = Double.MAX_VALUE, max = Double.MIN_VALUE;

		// Step 1: Applying low pass and high pass filter
		for (int i = 0; i < complexes.length / 2; i++) {
			double frequency = i * Microphone.FORMAT.getSampleRate() / (double) complexes.length;
			if (frequency > lowpassRate || frequency < highpassRate)
				complexes[i] = null;
			else {
				double abs = complexes[i].abs();
				if (abs < min)
					min = abs;
				if (max < abs)
					max = abs;
			}
		}

		// Step 2: Apply logistic function
		for (int i = 0; i < complexes.length / 2; i++) {
			if (complexes[i] == null)
				continue;
			double normalized = (complexes[i].abs() - min) / (max - min);
			double reduced = logistic.value(normalized);
			if (reduced < 0.0045)
				complexes[i] = null;
		}

		return complexes;
	}

	private boolean isPlayerSpeaking(Complex[] complexes) {
		return true;
	}

	private double[] fromBytesToDoubles(byte[] bytes) {
		double[] buffer = new double[bytes.length / 2];
		int index = 0;
		for (int i = 0; i < bytes.length; i += 2) {
			// From two bytes creating a short value and then casting it as double.
			buffer[index] = (double) (short) ((bytes[i + 1] & 0xff) << 8 | bytes[i] & 0xff);
			index++;
		}
		return buffer;
	}

	private byte[] export(Complex[] complexes) {
		ByteWrapper wrapper = ByteWrapper.create();

		for (int i = 0; i < complexes.length / 2; i++) {
			if (complexes[i] == null)
				continue;

			wrapper.putShort((short) i);
			wrapper.putDouble(complexes[i].getReal());
			wrapper.putDouble(complexes[i].getImaginary());
		}

		return wrapper.get();
	}

	private byte[] getRealSignal(Complex[] complexes) {
		byte[] signal = new byte[complexes.length * 2];
		int index = 0;
		for (int i = 0; i < complexes.length; i++) {
			short shortValue = (short) complexes[i].getReal();
			signal[index++] = (byte) shortValue;
			signal[index++] = (byte) (shortValue >> 8);
		}
		ByteWrapper wrapper = ByteWrapper.wrap(signal);
		return wrapper.extract(wrapper.get().length / 2, wrapper.get().length / 2);
	}

	private Complex[] extract(byte[] bytes) {
		Complex[] complexes = new Complex[Microphone.CHUNK_SIZE];
		ByteWrapper wrapper = ByteWrapper.wrap(bytes);

		int first = 0;
		for (int i = 0; i < bytes.length; i += 18) {
			int index = wrapper.getShort(first);
			first += 2;
			double real = wrapper.getDouble(first);
			first += 8;
			double imaginary = wrapper.getDouble(first);
			first += 8;

			complexes[index] = new Complex(real, imaginary);
			if (index > 0)
				complexes[complexes.length - index] = new Complex(real, -imaginary);
		}

		for (int i = 0; i < complexes.length; i++)
			if (complexes[i] == null)
				complexes[i] = new Complex(0, 0);

		return complexes;
	}

	protected void checkIsStarted() {
		if (!isStarted.get())
			throw new UnsupportedOperationException("This encoder is not started");
	}

	protected class Action {
		private byte[] data;
		private Consumer<byte[]> action;

		public Action(byte[] data, Consumer<byte[]> action) {
			this.data = data;
			this.action = action;
		}

		public byte[] getData() {
			return data;
		}

		public Consumer<byte[]> getAction() {
			return action;
		}
	}
}
