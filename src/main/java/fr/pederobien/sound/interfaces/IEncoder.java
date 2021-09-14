package fr.pederobien.sound.interfaces;

public interface IEncoder {

	/**
	 * Encode the given byte array in order to remove useless frequencies.
	 * 
	 * @param data The raw byte array coming from the microphone.
	 * 
	 * @return The encoded bytes array.
	 */
	public byte[] encode(byte[] data);
}
