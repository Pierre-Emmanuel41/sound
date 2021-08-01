package fr.pederobien.sound.impl;

import java.util.function.Consumer;

import fr.pederobien.utils.BlockingQueueTask;

public class AsynchonousEncoder extends Encoder {
	private BlockingQueueTask<Action> encodeQueue, decodeQueue;

	protected AsynchonousEncoder(double lowpassRate, double highpassRate) {
		super(lowpassRate, highpassRate);
		encodeQueue = new BlockingQueueTask<>("AudioEncoder", action -> encode(action));
		decodeQueue = new BlockingQueueTask<>("AudioDecoder", action -> decode(action));
	}

	@Override
	public void start() {
		super.start();
		encodeQueue.start();
		decodeQueue.start();
	}

	@Override
	public void stop() {
		super.stop();
		encodeQueue.dispose();
		decodeQueue.dispose();
	}

	@Override
	public void encode(byte[] data, Consumer<byte[]> action) {
		encodeQueue.add(new Action(data, action));
	}

	@Override
	public void decode(byte[] data, Consumer<byte[]> action) {
		decodeQueue.add(new Action(data, action));
	}
}
