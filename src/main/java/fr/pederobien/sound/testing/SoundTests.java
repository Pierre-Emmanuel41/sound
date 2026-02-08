package fr.pederobien.sound.testing;

import java.util.concurrent.atomic.AtomicBoolean;

import fr.pederobien.sound.impl.SoundApi;
import fr.pederobien.sound.interfaces.IAudioStream;
import fr.pederobien.sound.interfaces.ISoundApi;
import fr.pederobien.utils.IExecutable;
import fr.pederobien.utils.event.Logger;

public class SoundTests {

	public void soundApiInitializeDisposeTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.dispose();
		};

		runTest("soundApiInitializeDisposeTest", test);
	}

	public void speakersSimpleOpenCloseTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.getSpeakers().open();

			sleep(1000);

			api.getSpeakers().close();
			api.dispose();
		};

		runTest("speakersSimpleOpenCloseTest", test);
	}

	public void microphoneSimpleOpenCloseTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.getMicrophone().open();

			sleep(1000);

			api.getMicrophone().close();
			api.dispose();
		};

		runTest("microphoneSimpleOpenCloseTest", test);
	}

	public void microphoneOpenFetchCloseTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.getMicrophone().open();

			for (int i = 0; i < 5; i++) {
				api.getMicrophone().fetch();
			}

			api.getMicrophone().close();
			api.dispose();
		};

		runTest("microphoneOpenFetchCloseTest", test);
	}

	public void playBackTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.getMicrophone().open();
			api.getSpeakers().open();

			AtomicBoolean stop = new AtomicBoolean(false);
			Thread stopThread = new Thread(() -> {
				sleep(5000);
				stop.set(true);
			});
			stopThread.start();

			while (!stop.get())
				api.getSpeakers().getMixer().getOrCreateStream("Player 1").put(api.getMicrophone().fetch());

			api.getMicrophone().close();
			api.getSpeakers().close();
			api.dispose();
		};

		runTest("playBackTest", test);
	}

	public void playBackRightThenLeftThenBothTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.getMicrophone().open();
			api.getSpeakers().open();

			String name = "Player 1";
			AtomicBoolean stop = new AtomicBoolean(false);
			Thread stopThread = new Thread(() -> {
				sleep(5000);
				Logger.debug("Moving sound to right only");
				IAudioStream stream = api.getSpeakers().getMixer().getOrCreateStream(name);
				stream.setRightVolume(2);
				stream.setLeftVolume(0);
				sleep(3000);
				Logger.debug("Moving sound to left only");
				stream.setLeftVolume(2);
				stream.setRightVolume(0);
				sleep(3000);
				Logger.debug("Moving sound back to both channels");
				stream.setLeftVolume(1);
				stream.setRightVolume(1);
				sleep(5000);
				stop.set(true);
			});
			stopThread.start();

			while (!stop.get())
				api.getSpeakers().getMixer().getOrCreateStream(name).put(api.getMicrophone().fetch());

			api.getMicrophone().close();
			api.getSpeakers().close();
			api.dispose();
		};

		runTest("playBackRightThenLeftThenBothTest", test);
	}

	public void microphoneOpenCloseTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.getMicrophone().open();
			api.getSpeakers().open();

			AtomicBoolean stop = new AtomicBoolean(false);
			Thread stopThread = new Thread(() -> {
				try {
					sleep(3000);
					Logger.debug("Disabling microphone");
					api.getMicrophone().close();
					sleep(1000);
					Logger.debug("Enabling microphone");
					api.getMicrophone().open();
					sleep(5000);
					stop.set(true);
				} catch (Exception e) {
					// Do nothing
				}
			});
			stopThread.start();

			while (!stop.get())
				api.getSpeakers().getMixer().getOrCreateStream("Player 1").put(api.getMicrophone().fetch());

			api.getMicrophone().close();
			api.getSpeakers().close();
			api.dispose();
		};

		runTest("microphoneOpenCloseTest", test);
	}

	public void speakersOpenCloseTest() {
		IExecutable test = () -> {
			ISoundApi api = createSoundApi();
			api.initialize();
			api.getMicrophone().open();
			api.getSpeakers().open();

			AtomicBoolean stop = new AtomicBoolean(false);
			Thread stopThread = new Thread(() -> {
				try {
					sleep(3000);
					Logger.debug("Disabling speakers");
					api.getSpeakers().close();
					sleep(1000);
					Logger.debug("Enabling speakers");
					api.getSpeakers().open();
					sleep(5000);
					stop.set(true);
				} catch (Exception e) {
					// Do nothing
				}
			});
			stopThread.start();

			while (!stop.get())
				api.getSpeakers().getMixer().getOrCreateStream("Player 1").put(api.getMicrophone().fetch());

			api.getMicrophone().close();
			api.getSpeakers().close();
			api.dispose();
		};

		runTest("speakersOpenCloseTest", test);
	}

	private ISoundApi createSoundApi() {
		return new SoundApi();
	}

	private void runTest(String testName, IExecutable test) {
		Logger.warning("Begin %s", testName);
		try {
			test.exec();
		} catch (Exception e) {
			Logger.error("Unexpected error: %s", e.getMessage());
			for (StackTraceElement trace : e.getStackTrace()) {
				Logger.error(trace.toString());
			}
		}

		Logger.warning("End %s", testName);
		sleep(1000);
	}

	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
