package fr.pederobien.sound.testing;

import fr.pederobien.utils.IExecutable;
import fr.pederobien.utils.event.Logger;

public class SoundTestApp {

	public static void main(String[] args) {
		Logger.instance().colorized(true).debug(true);

		runTest("Sound API test", () -> soundApiTest());
	}

	private static void soundApiTest() {
		SoundTests tests = new SoundTests();

		tests.soundApiInitializeDisposeTest();
		tests.speakersSimpleOpenCloseTest();
		tests.microphoneSimpleOpenCloseTest();
		tests.microphoneOpenFetchCloseTest();
		tests.playBackTest();
		tests.playBackRightThenLeftThenBothTest();
		tests.microphoneOpenCloseTest();
		tests.speakersOpenCloseTest();
	}

	private static void runTest(String testName, IExecutable test) {
		Logger.warning("Start of %s execution", testName);
		try {
			test.exec();
		} catch (Exception e) {
			Logger.error("Unexpected error: %s", e.getMessage());
			for (StackTraceElement trace : e.getStackTrace()) {
				Logger.error(trace.toString());
			}
		}

		sleep(1000);
		Logger.warning("End of %s execution", testName);
	}

	private static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
