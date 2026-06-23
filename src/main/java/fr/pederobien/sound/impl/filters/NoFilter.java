package fr.pederobien.sound.impl.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fr.pederobien.sound.interfaces.IFilter;
import fr.pederobien.utils.event.Logger;

public class NoFilter implements IFilter {
	private final Lock lock;
	private final Condition notEmpty;
	private final List<Byte> stream;
	private boolean isEnabled;

	/**
	 * Creates a cleaner that does not do anything on the microphone stream.
	 */
	public NoFilter() {
		lock = new ReentrantLock(true);
		notEmpty = lock.newCondition();
		stream = new ArrayList<Byte>();
		isEnabled = false;
	}

	@Override
	public void write(byte[] buffer) {
		lock.lock();
		try {
			for (byte b : buffer)
				stream.add(b);

			notEmpty.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int read(byte[] data) {
		if (stream.isEmpty()) {
			lock.lock();
			try {
				notEmpty.await();
			} catch (Exception e) {
				return -1;
			} finally {
				lock.unlock();
			}
		}

		lock.lock();

		try {
			int size = Math.min(data.length, stream.size());
			for (int i = 0; i < size; i++)
				data[i] = stream.remove(0);

			return size;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		if (this.isEnabled == isEnabled)
			return;

		this.isEnabled = isEnabled;
		debug("Filter %s", isEnabled ? "Enabled" : "Disabled");
	}

	@Override
	public void dispose() {
		// Do nothing
	}

	private void debug(String format, Object... args) {
		Logger.debug(1, "[NoFilter] - %s", String.format(format, args));
	}
}
