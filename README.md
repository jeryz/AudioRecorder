# AudioRecorder
record pcm,wav,aac,amr format

### 使用

```java
Recorder recorder = new Recorder.Builder(this)
        .setChannel(1)//通道数，默认1
        .setSampleRate(16000)//采样率，默认16000
        .setBitsPerSample(16)//采样位,默认16
        .setFileFormat(FileFormat.Format.PCM)//设置录音格式(PCM,WAV,AAC,AMR)，默认pcm
        .setOutputPath("sdcard/RecordFile")//设置输出文件路径，默认是app的cachePath
        .setOutputFileName("record.wav")//设置输出文件名，默认文件名格式是yyyyMMdd_HHmm_ss.suffix
        .setRecordTimeout(1000*10)//设置录音时长，默认0
        .setBitRate(96000)//aac编码可设置比特率，默认96000
        .setBuffSize(1024)//录音缓冲区大小，默认是2倍AudioRecord.getMinBufferSize计算的大小
        .setSaveOutputFile(false)//是否保存录音数据到文件,默认true
        //设置自定义编码器AudioProcessor
        .setAudioProcessor("wav", new WAVProcessor() {
            @Override
            public void onBegin(RandomAccessFile writer, Recorder.Config config) throws IOException {
                super.onBegin(writer, config);
            }

            @Override
            public void onEnd(RandomAccessFile writer) throws IOException {
                super.onEnd(writer);
            }

            @Override
            public int onRead(AudioRecordEngine.PCMReader reader, byte[] buffer) throws IOException {
                return super.onRead(reader, buffer);
            }

            @Override
            public void onAudioChunk(RandomAccessFile writer, byte[] chunk, int readLen) throws IOException {
                super.onAudioChunk(writer, chunk, readLen);
            }
        })
        .build();

recorder.start();
recorder.stop();
recorder.cancel();
recorder.pause();
recorder.release();
recorder.isRecording();
recorder.setVolumeListener(new OnVolumeListener() {
    @Override
    public void onVolume(double volume) {
        //输出录音音量
    }
});

recorder.setRecordListener(new OnRecordListener() {
    @Override
    public void onState(int state, String msg) {
        //输出状态OnRecordListener.STATE_START,OnRecordListener.STATE_ERROR ...
    }

    @Override
    public void onResult(String path, long duration_s, long size_k) {
        //输出结果
    }
});

recorder.setOnAudioChunkListener(new OnAudioChunkListener() {
    @Override
    public void onAudioChunk(byte[] chunk) {
        //输出编码后的数据
    }
});
```

### 引用AudioRecorder

```groovy
Step 1. Add it in your root build.gradle at the end of repositories:
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}
Step 2. Add the dependency
dependencies {
	implementation 'com.github.jeryz:AudioRecorder:v1.0'
}
```


![](D:\androidProject\AudioRecorder\screencap.png)
