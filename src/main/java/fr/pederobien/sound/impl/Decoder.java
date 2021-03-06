package fr.pederobien.sound.impl;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.sun.jna.ptr.PointerByReference;

import fr.pederobien.javaopuswrapper.OpusWrapper;
import fr.pederobien.sound.event.DecoderFailToDecodeEvent;
import fr.pederobien.sound.event.DecoderInitializationFailEvent;
import fr.pederobien.sound.interfaces.IDecoder;
import fr.pederobien.utils.event.EventManager;

public class Decoder implements IDecoder {
	private PointerByReference decoder;
	private boolean isInitialized;

	protected Decoder() {
		IntBuffer error = IntBuffer.allocate(4).put(0);
		decoder = OpusWrapper.getOpus().opus_decoder_create((int) SoundConstants.SAMPLE_RATE, SoundConstants.MICRO_CHANNELS_NUMBER, error);
		if (error.get() < 0)
			EventManager.callEvent(new DecoderInitializationFailEvent(this));
		else
			isInitialized = true;
	}

	@Override
	public byte[] decode(byte[] data) {
		try {
			if (!isInitialized)
				return fail(data);

			ShortBuffer decodedBuffer = ShortBuffer.allocate(SoundConstants.CHUNK_LENGTH);
			int decodedLength = OpusWrapper.getOpus().opus_decode(decoder, data, data.length, decodedBuffer, SoundConstants.CHUNK_LENGTH, 0);
			if (decodedLength < 0)
				return fail(data);

			short[] decodedShort = decodedBuffer.array();
			byte[] decoded = new byte[SoundConstants.CHUNK_LENGTH * 2];
			int index = 0;
			for (int i = 0; i < decoded.length / 2; i++) {
				decoded[index++] = (byte) (decodedShort[i] & 0xff);
				decoded[index++] = (byte) ((decodedShort[i] >> 8) & 0xff);
			}
			return decoded;
		} catch (Exception e) {
			return fail(data);
		}
	}

	private byte[] fail(byte[] data) {
		EventManager.callEvent(new DecoderFailToDecodeEvent(this, data));
		return new byte[0];
	}
}
