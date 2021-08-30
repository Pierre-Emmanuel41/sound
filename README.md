# 1) Presentation

The project sound provides an overlay of the java.sound package in order to simplify how to get data from a [TargetDataLine](https://docs.oracle.com/javase/7/docs/api/javax/sound/sampled/TargetDataLine.html) and how to play data with a [SourceDataLine](https://docs.oracle.com/javase/7/docs/api/javax/sound/sampled/SourceDataLine.html).

# 2) Download

First you need to download this project on your computer. To do so, you can use the following command line :

```git
git clone https://github.com/Pierre-Emmanuel41/sound.git --recursive
```

and then double click on the deploy.bat file. This will deploy this project and all its dependencies on your computer. Which means it generates the folder associated to this project and its dependencies in your .m2 folder. Once this has been done, you can add the project as maven dependency on your maven project :

```xml
<dependency>
	<groupId>fr.pederobien</groupId>
	<artifactId>sound</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
```

# 3) Tutorial

It is very simple to use the features implemented in this API. The developer needs to instantiate a <code>ISoundResourcesProvider</code>. From this provider, the developer has access to the microphone, the speakers and a mixer.

```java
ISoundResourcesProvider provider = new SoundResourcesProvider();
IMicrophone micro = provider.getMicrophone();
ISpeakers speakers = provider.getSpeakers();
IMixer mixer = provider.getMixer();
```

Then, the microphone needs to be observed in order to get the bytes array that corresponds to a sample. And it is also possible to observe the speakers in order to get the bytes array that is about to be played :

```java
IObsMicrophone obsMicro = (data, length) -> System.out.println(String.format("Reading %s bytes from the microphone", length));
provider.getMicrophone().addObserver(obsMicro);

IObsSpeakers obsSpeakers = (data, length) -> System.out.println(String.format("Playing %s bytes with the speakers", length));
provider.getSpeakers().addObserver(obsSpeakers);
```

To play data, the bytes array is not directly given to the speakers. There is an intermediate object : <code>IMixer</code>. This mixer is particularly powerful when several sounds are played simultaneously. It is his responsibility to merge the signals coming from each registered sound in order to create one unique signal used by the speakers.

```java
// bytes array coming from the microphone
byte[] dataMicro = new byte[1024];

// Sound name
String soundName = "Sound 1";

// Global volume of the sound
double globalVolume = 0.5;

// Sound type signal : True because the AudioFormat used by the Microphone is mono.
boolean isMono = true;

mixer.put(soundName, dataMicro, globalVolume, isMono);
```

Once this is done, the developer needs to open system resources in order to collect data :

```java
provider.getMicrophone().start();
provider.getSpeakers().start();
```

In order to close system resources :

```java
provider.getMicrophone().interrupt();
provider.getSpeakers().interrupt();
```

# 4) Example

```java
public static void main(String[] args) {
	ISoundResourcesProvider provider = new SoundResourcesProvider(isAsynchronous, lowpassRate, highpassRate);
	IMicrophone micro = provider.getMicrophone();
	ISpeakers speakers = provider.getSpeakers();
	IMixer mixer = provider.getMixer();

	// Sound name
	String soundName = "Sound 1";

	// Global volume of the sound
	double globalVolume = 0.5;

	// Sound type signal : True because the AudioFormat used by the Microphone is mono.
	boolean isMono = true;

	IObsMicrophone obsMicro = (data, length) -> mixer.put(soundName, data, globalVolume, isMono);
	micro.addObserver(obsMicro);

	IObsSpeakers obsSpeakers = (data, length) -> System.out.println(String.format("Playing %s bytes with the speakers", length));
	speakers.addObserver(obsSpeakers);

	micro.start();
	speakers.start();

	try {
		Thread.sleep(2000);
	} catch (InterruptedException e) {
		e.printStackTrace();
	}

	// Data are no more collected from the underlying TargetDataLine
	micro.pause();

	try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		e.printStackTrace();
	}

	// Data are again collected from the underlying TargetDataLine
	micro.relaunch();

	try {
		Thread.sleep(3000);
	} catch (InterruptedException e) {
		e.printStackTrace();
	}

	micro.removeObserver(obsMicro);
	speakers.removeObserver(obsSpeakers);
	micro.interrupt();
	speakers.interrupt();
}
```