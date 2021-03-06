package fr.pederobien.sound.impl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.sun.jna.ptr.PointerByReference;

import fr.pederobien.javaopuswrapper.Opus;
import fr.pederobien.javaopuswrapper.OpusWrapper;
import fr.pederobien.sound.event.EncoderFailToEncodeEvent;
import fr.pederobien.sound.event.EncoderInitializationFailEvent;
import fr.pederobien.sound.interfaces.IEncoder;
import fr.pederobien.utils.event.EventManager;

public class Encoder implements IEncoder {
	private PointerByReference encoder;
	private boolean isInitialized;

	protected Encoder() {
		IntBuffer error = IntBuffer.allocate(4).put(0);
		encoder = OpusWrapper.getOpus().opus_encoder_create((int) SoundConstants.SAMPLE_RATE, SoundConstants.MICRO_CHANNELS_NUMBER, Opus.OPUS_APPLICATION_VOIP, error);
		if (error.get() < 0)
			EventManager.callEvent(new EncoderInitializationFailEvent(this));
		else
			isInitialized = true;
	}

	@Override
	public byte[] encode(byte[] data) {
		try {
			if (!isInitialized)
				return fail(data);

			ShortBuffer input = ((ByteBuffer) ByteBuffer.allocateDirect(data.length).put(data).rewind()).asShortBuffer();
			ByteBuffer encodedBuffer = ByteBuffer.allocate(2000);
			int encodedLength = OpusWrapper.getOpus().opus_encode(encoder, input, SoundConstants.CHUNK_LENGTH, encodedBuffer, 2000);
			if (encodedLength < 0)
				return fail(data);

			byte[] encoded = new byte[encodedLength];
			encodedBuffer.get(encoded);
			return encoded;
		} catch (Exception e) {
			return fail(data);
		}
	}

	private byte[] fail(byte[] data) {
		EventManager.callEvent(new EncoderFailToEncodeEvent(this, data));
		return new byte[0];
	}
}
