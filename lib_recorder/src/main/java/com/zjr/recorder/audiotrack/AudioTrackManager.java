package com.zjr.recorder.audiotrack;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static android.media.AudioTrack.MODE_STREAM;
import static android.media.AudioTrack.PLAYSTATE_PLAYING;
import static android.media.AudioTrack.PLAYSTATE_STOPPED;

/**
 * created by zjr on 2019/10/28
 */
public class AudioTrackManager {
    private AudioTrack mAudioTrack;
    private Thread mRecordThread;
    private volatile static AudioTrackManager mInstance;
    private int bufferSize;
    private int channel = 1;
    private int sampleRate = 44100;
    private int bitsPerSample = 16;


    public AudioTrackManager() {
        setAudioTrack(channel,sampleRate,bitsPerSample);
    }

    public void setAudioTrack(int channel, int sampleRate, int bitsPerSample){
        this.channel = channel;
        this.sampleRate = sampleRate;
        this.bitsPerSample = bitsPerSample;
        mAudioTrack = getAudioTrack(channel,sampleRate,bitsPerSample);
    }

    private AudioTrack getAudioTrack(int channel, int sampleRate, int bitsPerSample) {
        int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;
        if (bitsPerSample == 8) {
            audioFormat =  AudioFormat.ENCODING_PCM_8BIT;
        }else if (bitsPerSample == 16) {
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        }

        int channelConfig = channel == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2;

        AudioTrack player;
        if (Build.VERSION.SDK_INT >= 23) {
            player = new AudioTrack.Builder()
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .build();
        } else {
            player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    channelConfig, audioFormat,
                    bufferSize, MODE_STREAM);
        }

        return player;
    }

    public static AudioTrackManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioTrackManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioTrackManager();
                }
            }
        }
        return mInstance;
    }

    private void destroyThread() {
        try {
            if (null != mRecordThread && Thread.State.RUNNABLE == mRecordThread.getState()) {
                try {
                    Thread.sleep(100);
                    mRecordThread.interrupt();
                } catch (Exception e) {
                    mRecordThread = null;
                }
            }
            mRecordThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mRecordThread = null;
        }
    }

    public void startPlay(String path) {
        destroyThread();
        if (mRecordThread == null) {
            mRecordThread = new Thread(new PlayTask(path));
            mRecordThread.start();
        }
    }

    public void release(){
        if (mAudioTrack != null) {
            mAudioTrack.release();//释放audioTrack资源
        }
    }

    public boolean isPlaying(){
        return mAudioTrack != null && mAudioTrack.getPlayState() == PLAYSTATE_PLAYING;
    }

    public boolean isStopped(){
        return mAudioTrack != null && mAudioTrack.getPlayState() == PLAYSTATE_STOPPED;
    }

    public void stopPlay() {
        try {
            destroyThread();
            if (mAudioTrack != null) {
                if (mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
                    mAudioTrack.stop();
                }
                if (mAudioTrack != null) {
                    mAudioTrack.release();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class PlayTask implements Runnable{

        private String filePath;
        private DataInputStream data;

        public PlayTask(String filePath) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try {
                if(mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED){
                    setAudioTrack(channel,sampleRate,bitsPerSample);
                }

                File file = new File(filePath);
                data = new DataInputStream(new FileInputStream(file));

                byte[] tempBuffer = new byte[bufferSize];
                int readCount = 0;
                while (data.available() > 0) {
                    readCount= data.read(tempBuffer);
                    if (readCount > 0) {
                        mAudioTrack.play();
                        mAudioTrack.write(tempBuffer, 0, readCount);
                    }
                }
                stopPlay();
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    if(data!=null)data.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
