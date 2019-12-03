package com.zjr.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.zjr.recorder.listener.OnAudioChunkListener;
import com.zjr.recorder.listener.OnRecordListener;
import com.zjr.recorder.listener.OnVolumeListener;
import com.zjr.recorder.processor.AAC;
import com.zjr.recorder.processor.AMR;
import com.zjr.recorder.processor.AudioProcessor;
import com.zjr.recorder.processor.PCM;
import com.zjr.recorder.processor.WAV;
import com.zjr.recorder.utils.DbCalculateUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * created by zjr on 2019/11/14
 */
public class AudioRecordEngine implements RecordEngine {
    private static final String TAG = "AudioRecordEngine";
    private AudioRecord recorder;
    private Handler handler;
    private Handler threadHandler;
    private int buffSize;
    private OnVolumeListener volumeListener;
    private OnRecordListener recordListener;
    private OnAudioChunkListener onAudioChunkListener;
    private static final int MSG_VOLUME = 4;
    private static final int MSG_RESULT = 5;
    private static final int MSG_STATE = 6;
    private String outputFile;
    private volatile boolean recording;
    private volatile boolean pause;
    private boolean cancel;
    private long pausePosition;
    private Recorder.Config config;
    private AudioProcessor audioProcessor;
    private Thread recordThread;
    private PCMReader reader;

    public AudioRecordEngine(Recorder.Config config) {
        this.config = config;
        handler = new Handler(Looper.getMainLooper(), msgCallback);
        audioProcessor = getAudioProcessor(config);

        HandlerThread thread = new HandlerThread("record");
        thread.start();
        threadHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj != null) {
                    byte[] rawData = (byte[]) msg.obj;
                    int readLen = msg.arg1;
                    if (volumeListener != null) {
                        //int amplitude = DbCalculateUtil.rmsDb1(buffer, readLen);
                        // int amplitude2 = DbCalculateUtil.rmsDb2(buffer, readLen);
                        int amplitude4 = DbCalculateUtil.getRecordVolume2(rawData, readLen, config.bitsPerSample);
                        //Log.d(TAG, "audioAmplitude rmsDb=" + amplitude + " " + amplitude2 + " " + amplitude4);

                        sentMsg(MSG_VOLUME, amplitude4, null);
                    }
                }
            }
        };
    }

    private AudioProcessor getAudioProcessor(Recorder.Config config) {
        AudioProcessor audioProcessor = null;
        if (config.extraAudioProcessor == null) {
            if (config.fileFormat == FileFormat.Format.WAV) {
                audioProcessor = new WAV();
            } else if (config.fileFormat == FileFormat.Format.AAC) {
                audioProcessor = new AAC();
            } else if (config.fileFormat == FileFormat.Format.AMR) {
                audioProcessor = new AMR();
            } else {
                audioProcessor = new PCM();
            }
        } else {
            audioProcessor = config.extraAudioProcessor;
        }
        return audioProcessor;
    }

    Handler.Callback msgCallback = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            Bundle data = msg.getData();
            switch (msg.what) {
                case MSG_STATE:
                    int state = data.getInt("state");
                    String stateMsg = data.getString("msg");
                    if (recordListener != null) {
                        recordListener.onState(state, stateMsg);
                    }
                    return true;
                case MSG_RESULT:
                    String filePath = data.getString("filePath");
                    long length = data.getLong("fileLength");
                    long duration = data.getLong("duration");

                    if (recordListener != null) {
                        recordListener.onResult(filePath, duration / 1000, length / 1024L);
                    }
                    Log.d(TAG, "output=" + filePath + "," + length / 1024L + "k," + duration + "ms");
                    return true;
                case MSG_VOLUME:
                    int volume = (int) msg.obj;
                    if (volumeListener != null) volumeListener.onVolume(volume);
                    return true;
                default:
                    return false;
            }
        }
    };


    @Override
    public void start() {
        if (recording) return;

        if (config.fileFormat == FileFormat.Format.AMR && (config.sampleRate != 8000 && config.sampleRate != 16000)) {
            throw new IllegalArgumentException("amr only support samplerate 8000 or 16000");
        }
        int audioFormat = config.getAudioFormat();
        int channelConfig = config.getChannelConfig();
        buffSize = config.getBufferSize();
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, config.sampleRate, channelConfig, audioFormat, buffSize);
        reader = new PCMReader(recorder);
        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            startRecordThread();
        } else {
            Log.e(TAG, "state uninitialized");
            setState(OnRecordListener.STATE_ERROR, "uninitialized");
        }

        String c = channelConfig == AudioFormat.CHANNEL_IN_STEREO ? "stereo" : "mono";
        Log.d(TAG, c + ", channel=" + config.channel + ", sampleRate=" + config.sampleRate + ", bitsPerSample=" + config.bitsPerSample + " buffSize=" + buffSize);

    }

    private void startRecordThread() {
        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                record();
            }
        });
        recordThread.start();
    }

    private void setState(int state, String msg) {
        Bundle bundle = new Bundle();
        bundle.putInt("state", state);
        bundle.putString("msg", msg);
        switch (state) {
            case OnRecordListener.STATE_START:
                Log.d(TAG, "start");
                break;
            case OnRecordListener.STATE_STOP:
                recording = false;
                Log.d(TAG, "stop");
                break;
            case OnRecordListener.STATE_RECORDING:
                Log.d(TAG, "recording");
                break;
            case OnRecordListener.STATE_ERROR:
                recording = false;
                Log.e(TAG, "error:" + msg);
                break;
        }
        sentMsg(MSG_STATE, state, bundle);
    }


    private void sentResult(long recordLength, long duration, long fileLength) {
        Bundle bundle = new Bundle();
        bundle.putString("filePath", outputFile);
        bundle.putLong("fileLength", fileLength);
        bundle.putLong("duration", duration);
        bundle.putLong("recordLength", recordLength);
        sentMsg(MSG_RESULT, null, bundle);
    }

    private void sentMsg(int what, Object obj, Bundle bundle) {
        Message msg = Message.obtain();
        msg.obj = obj;
        msg.what = what;
        if (bundle != null) {
            msg.setData(bundle);
        }
        handler.sendMessage(msg);
    }

    private void record() {
        recorder.startRecording();

        setState(OnRecordListener.STATE_START, "started");
        if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            setState(OnRecordListener.STATE_ERROR, "mic occupied");
            release();
            return;
        }
        recording = true;
        setState(OnRecordListener.STATE_RECORDING, "recording");

        RandomAccessFile writer = null;

        try {
            if (config.saveFile) {
                writer = getRecordFile();
            }
            byte[] buffer = new byte[buffSize];
            audioProcessor.onBegin(writer, config);
            reader.readBegin();
            while (recording) {
                long durationInMills = getDurationInMills(reader.rawLength);
                if (durationInMills >= config.timeout) {
                    Log.d(TAG, "timeout=" + (durationInMills) + ">=" + config.timeout);
                    recording = false;
                    break;
                }

                int readLen = audioProcessor.onRead(reader, buffer);
                if (readLen > 0) {
                    audioProcessor.onAudioChunk(writer, buffer, readLen);
                    if (onAudioChunkListener != null) {
                        onAudioChunkListener.onAudioChunk(buffer);
                    }
                } else if (readLen < 0) {
                    setState(OnRecordListener.STATE_ERROR, "read data error");
                    break;
                }
            }
            reader.readEnd();

            //recording end
            long fileLength = writer != null ? writer.length() : 0;

            //audioProcessor.onEnd(writer);

            if (pause) {
                pausePosition = fileLength;
                setState(OnRecordListener.STATE_PAUSE, "pause");
                recording = false;
            }

            long duration = getDurationInMills(reader.rawLength);
            sentResult(0, duration, fileLength);

        } catch (Exception e) {
            e.printStackTrace();
            setState(OnRecordListener.STATE_ERROR, e.getMessage());
        } finally {
            setState(OnRecordListener.STATE_STOP, "stop");
            try {
                audioProcessor.onEnd(writer);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            try {
                release();
                if (writer != null) {
                    writer.close();
                    if (cancel) {
                        //delete when recording cancel
                        File file = new File(outputFile);
                        file.deleteOnExit();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            cancel = false;
            recording = false;
            pause = false;
        }
    }

    private RandomAccessFile getRecordFile() throws IOException {
        try {
            File file = new File(config.outputPath);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        RandomAccessFile writer;
        if (pausePosition > 0) {
            writer = new RandomAccessFile(outputFile, "rw");
            writer.seek(pausePosition);
        } else {
            outputFile = config.getOutputFile();
            writer = new RandomAccessFile(outputFile, "rw");
        }
        return writer;
    }

    private long getDurationInMills(long length) {
        return length * 8 * 1000 / config.bitsPerSample / config.sampleRate / config.channel;
    }

    @Override
    public void pause() {
        pause = true;
    }

    @Override
    public void cancel() {
        cancel = true;
        recording = false;
    }

    @Override
    public void stop() {
        recording = false;
        pausePosition = 0;
    }

    @Override
    public void release() {
        if (recorder != null) {
            recording = false;
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }
            recorder.release();
        }
    }

    @Override
    public void setConfig(Recorder.Config config) {
        this.config = config;
    }

    @Override
    public Recorder.Config getConfig() {
        return config;
    }

    @Override
    public boolean isRecording() {
        return recording;
    }

    @Override
    public void setRecordListener(OnRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    @Override
    public void setOnAudioChunkListener(OnAudioChunkListener listener) {
        onAudioChunkListener = listener;
    }

    @Override
    public void setVolumeListener(OnVolumeListener volumeListener) {
        this.volumeListener = volumeListener;
    }

    public class PCMReader {

        AudioRecord recorder;
        private byte[] rawData;
        private long rawLength;
        private long time;
        private long diffTotal;

        public PCMReader(AudioRecord recorder) {
            this.recorder = recorder;
        }

        public int read(byte[] audioData, int offset, int sizes) {
            int len = recorder.read(audioData, offset, sizes);
            rawData = audioData;
            if (len > 0) {
                rawLength += len;
            }
            sentRawData(audioData, len);
            return len;
        }

        private void sentRawData(byte[] audioData, int len) {
            long diff = System.currentTimeMillis() - time;
            diffTotal += diff;
            if (diffTotal >= 50) {//50 ms
                diffTotal = 0;
                Message.obtain(threadHandler, 0, len, 0, audioData).sendToTarget();
            }
            time = System.currentTimeMillis();
        }

        public void readBegin() {
            rawLength = 0;
        }

        public void readEnd() {

        }
    }

}
