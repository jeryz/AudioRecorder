package com.zjr.recorder;


import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;

import com.zjr.recorder.listener.OnAudioChunkListener;
import com.zjr.recorder.listener.OnRecordListener;
import com.zjr.recorder.listener.OnVolumeListener;
import com.zjr.recorder.processor.AudioProcessor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * created by zjr on 2019/11/14
 */
public class Recorder implements RecordEngine {

    private RecordEngine engine;
    private Config config;

    private Recorder() {
    }

    private Recorder(Builder builder) {
        this.config = builder.config;
        engine = new AudioRecordEngine(config);
    }

    @Override
    public void start() {
        engine.start();
    }

    @Override
    public void pause() {
        engine.pause();
    }

    @Override
    public void cancel() {
        engine.cancel();
    }

    @Override
    public void stop() {
        engine.stop();
    }

    @Override
    public void release() {
        engine.release();
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
        engine.setConfig(config);
    }

    @Override
    public Config getConfig() {
        return engine.getConfig();
    }

    @Override
    public boolean isRecording() {
        return engine.isRecording();
    }

    @Override
    public void setRecordListener(OnRecordListener listener) {
        engine.setRecordListener(listener);
    }

    @Override
    public void setOnAudioChunkListener(OnAudioChunkListener listener) {
        engine.setOnAudioChunkListener(listener);
    }

    @Override
    public void setVolumeListener(OnVolumeListener volumeListener) {
        engine.setVolumeListener(volumeListener);
    }

    public static class Builder {
        private final Config config;

        public Builder(Context context) {
            config = new Config();
            config.cachePath = context.getCacheDir().getAbsolutePath();
        }

        public Builder setChannel(int channel) {
            config.channel = channel;
            return this;
        }

        public Builder setSampleRate(int sampleRate) {
            config.sampleRate = sampleRate;
            return this;
        }

        public Builder setBitsPerSample(int bitsPerSample) {
            config.bitsPerSample = bitsPerSample;
            return this;
        }

        public Builder setOutputPath(String outputPath) {
            config.outputPath = outputPath;
            return this;
        }

        public Builder setFileFormat(@FileFormat.Format int fileFormat) {
            config.fileFormat = fileFormat;
            return this;
        }

        public Builder setRecordTimeout(long timeout) {
            config.timeout = timeout;
            return this;
        }

        public Builder setBitRate(int bitRate) {
            config.bitRate = bitRate;
            return this;
        }

        public Builder setBuffSize(int buffSize) {
            config.buffSize = buffSize;
            return this;
        }

        public Builder setSaveOutputFile(boolean saveFile) {
            config.saveFile = saveFile;
            return this;
        }

        public Builder setAudioProcessor(String fileFormat, AudioProcessor audioProcessor) {
            config.extraAudioProcessor = audioProcessor;
            config.extraFileFormat = fileFormat;
            return this;
        }

        public Recorder build() {

            return new Recorder(this);
        }

    }

    public static class Config {
        private String cachePath;
        public int sampleRate = 16000;
        public int bitsPerSample = 16;
        public int channel = 1;
        public int bitRate = 96000;//kbps = sampleRate * bitsPerSample/8 * channelCount;
        public long timeout;//ms
        public int buffSize;
        public String outputPath;
        public AudioProcessor extraAudioProcessor;
        public String extraFileFormat;
        public boolean saveFile = true;
        public @FileFormat.Format
        int fileFormat = FileFormat.Format.PCM;

        public String getOutputFile() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm_ss", Locale.getDefault());
            String fileName = dateFormat.format(new Date(System.currentTimeMillis()));
            String fileDir = outputPath != null && outputPath.length() > 0 ? outputPath : cachePath;
            String suffix = extraFileFormat!=null?extraFileFormat:FileFormat.value(fileFormat);
            return String.format("%s/%s.%s", fileDir, fileName, suffix);
        }

        public int getBufferSize() {
            if (buffSize > 0) return buffSize;
            buffSize = AudioRecord.getMinBufferSize(sampleRate, getChannelConfig(), getAudioFormat()) * 2;
            //buffSize = 2 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            return buffSize;
            //return channel * bitsPerSample / 8 * sampleRate * 100 / 1000;
        }

        public int getChannelConfig() {
            return channel == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        }

        public int getAudioFormat() {
            if (bitsPerSample == 16) {
                return AudioFormat.ENCODING_PCM_16BIT;
            } else if (bitsPerSample == 8) {
                return AudioFormat.ENCODING_PCM_8BIT;
            } else {
                return AudioFormat.ENCODING_PCM_FLOAT;
            }
        }
    }
}
