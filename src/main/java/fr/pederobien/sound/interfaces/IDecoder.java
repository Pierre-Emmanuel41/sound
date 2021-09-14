package fr.pederobien.sound.interfaces;

public interface IDecoder {

	/**
	 * Decode the given byte array in order to retrieve the original signal.
	 * 
	 * @param data The byte array that represents the encoded data.
	 * 
	 * @return The decoded bytes array.
	 */
	public byte[] decode(byte[] data);
}
