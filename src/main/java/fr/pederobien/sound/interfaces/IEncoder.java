package fr.pederobien.sound.interfaces;

import java.util.function.Consumer;

import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public interface IEncoder {

	/**
	 * Allow this encoder to encode and decode.
	 */
	public void start();

	/**
	 * Do not allow this encoder to encode and decode.
	 */
	public void stop();

	/**
	 * Encode the given byte array in order to remove useless frequencies. It applies a {@link FastFourierTransformer} with
	 * {@link TransformType#FORWARD}.
	 * 
	 * @param data   The byte array that represents the data coming from the microphone.
	 * @param action The action to do once the data has been encoded.
	 * 
	 * @throws IllegalStateException If the encoder has not been started.
	 */
	public void encode(byte[] data, Consumer<byte[]> action);

	/**
	 * Decode the given byte array in order to retrieve the original. It applies a {@link FastFourierTransformer} with
	 * {@link TransformType#INVERSE}.
	 * 
	 * @param data   The byte array that represents the data coming from the #{@link IEncoder#encode(byte[], Consumer)} method.
	 * @param action The action to do once the data has been decoded.
	 * 
	 * @throws IllegalStateException If the encoder has not been started.
	 */
	public void decode(byte[] data, Consumer<byte[]> action);
}
