package fr.pederobien.sound.testing;

import fr.pederobien.utils.IExecutable;
import fr.pederobien.utils.event.Logger;

public class SoundTestApp {

	public static void main(String[] args) {
		Logger.setPrintInColor(true);
		Logger.setPrintEvent(true);

		runTest("Sound API test", () -> soundApiTest());
	}

	private static void soundApiTest() {
		SoundTests tests = new SoundTests();

		tests.soundApiInitializeDisposeTest();
		tests.speakersSimpleOpenCloseTest();
		tests.microphoneSimpleOpenCloseTest();
		tests.microphoneOpenFetchCloseTest();
		tests.playBackTest();
		tests.volumeFactorTest();
		tests.playbackLowPassFilterTest(3400);
		tests.openCloseLowPassFilterTest(3400);
		tests.playbackHighPassFilterTest(500);
		tests.openCloseHighPassFilterTest(500);
		tests.playbackBandPassFilterTest(100, 3000);
		tests.openCloseBandPassFilterTest(50, 3400);
		tests.playbackBiquadBandPassFilterTest(1000.0, 0.707);
		tests.openCloseBiquadBandPassFilterTest(1000.0, 0.707);
		tests.echoEffectTest();
		tests.underWaterEffectTest();
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
