package fr.pederobien.sound.impl;

public class AudioPacket {
	private String key;
	private byte[] data;
	private double globalVolume, rightVolume, leftVolume;
	private boolean isMono, isEncoded;

	/**
	 * Creates an audio packet that gather properties for the audio sample.
	 * 
	 * @param key          The key associated to this audio packet.
	 * @param data         The audio sample.
	 * @param isMono       True is the {@link #getData()} returns a mono signal, false means a stereo signal.
	 * @param isEncoded    True if the data has been encoded, false otherwise.
	 * @param globalVolume The global volume of the sample.
	 * @param rightVolume  The volume for the right channel.
	 * @param leftVolume   The volume for the left channel.
	 */
	public AudioPacket(String key, byte[] data, boolean isMono, boolean isEncoded, double globalVolume, double rightVolume, double leftVolume) {
		this.key = key;
		this.data = data;
		this.isMono = isMono;
		this.isEncoded = isEncoded;
		this.globalVolume = globalVolume;
		this.rightVolume = rightVolume;
		this.leftVolume = leftVolume;
	}

	/**
	 * When this packet has been added to the mixer, it is used to get the sound associated to the key.
	 * 
	 * @return The key associated to this audio packet.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return The audio sample.
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * @return True is the {@link #getData()} returns a mono signal, false means a stereo signal.
	 */
	public boolean isMono() {
		return isMono;
	}

	/**
	 * @return True if the data has been encoded, false otherwise.
	 */
	public boolean isEncoded() {
		return isEncoded;
	}

	/**
	 * @return The global volume of the sample.
	 */
	public double getGlobalVolume() {
		return globalVolume;
	}

	/**
	 * @return The volume for the right channel.
	 */
	public double getRightVolume() {
		return rightVolume;
	}

	/**
	 * @return The volume for the left channel.
	 */
	public double getLeftVolume() {
		return leftVolume;
	}
}
