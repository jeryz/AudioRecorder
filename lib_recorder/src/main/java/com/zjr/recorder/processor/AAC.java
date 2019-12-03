package com.zjr.recorder.processor;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.zjr.recorder.AudioRecordEngine;
import com.zjr.recorder.Recorder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * created by zjr on 2019/11/14
 * AAC LC, ADTS
 */
public class AAC extends PCM implements Runnable {

    private MediaCodec mediaCodec;
    private int bufferSize;
    private final String MINE_TYPE_AAC = MediaFormat.MIMETYPE_AUDIO_AAC;//"audio/mp4a-latm";
    private int sampleRate;
    private int channelCount;
    private long presentationTimeUs;
    private MediaCodec.BufferInfo bufferInfo;
    LinkedBlockingQueue<AudioChunk> blockingQueue = new LinkedBlockingQueue();
    private RandomAccessFile writer;
    private Thread thread;
    private boolean finish;
    private boolean endOfStream;

    public AAC() {
    }

    private static boolean AACSupported() {
        try {
            MediaRecorder.OutputFormat.class.getField("AAC_ADTS");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void addADTS2Packet(byte[] packet, int channelCount, int sampleRate, int packetLen) {
        int freqIdx = getFreqIndex(sampleRate);
        int profile = 2;  //AAC LC
        //int chanCfg = channelCount;  //CPE
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (channelCount >> 2));
        packet[3] = (byte) (((channelCount & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private int getFreqIndex(int sampleRate) {
        int[] sampleRateArray = new int[]{96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000};
        for (int i = 0; i < sampleRateArray.length; i++) {
            if (sampleRateArray[i] == sampleRate) {
                return i;
            }
        }
        return 4;
    }

    @Override
    public int onRead(AudioRecordEngine.PCMReader recorder, byte[] buffer) throws IOException {
        int len = super.onRead(recorder, buffer);
        if (len < 0) {
            endOfStream = true;
        }
        return len;
    }

    @Override
    public void onBegin(RandomAccessFile writer, Recorder.Config config) throws IOException {
        this.writer = writer;
        sampleRate = config.sampleRate;
        channelCount = config.channel;
        bufferSize = config.getBufferSize();
        if (mediaCodec == null) {
            mediaCodec = MediaCodec.createEncoderByType(MINE_TYPE_AAC);
            MediaFormat format = MediaFormat.createAudioFormat(MINE_TYPE_AAC, config.sampleRate, config.channel);
            //format.setInteger(MediaFormat.KEY_CHANNEL_MASK, config.getChannelConfig());
            format.setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            bufferInfo = new MediaCodec.BufferInfo();
        }

        mediaCodec.start();
//        thread = new Thread(this);
//        thread.start();
        endOfStream = false;
    }

    @Override
    public void onAudioChunk(RandomAccessFile writer, byte[] chunk, int readLen) throws IOException {
//        try {
//            blockingQueue.put(new AudioChunk(chunk, readLen));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        encodeAAC(writer, chunk, readLen);
    }

    @Override
    public void onEnd(RandomAccessFile writer) throws IOException {
        finish = true;
        if (thread != null) thread.interrupt();
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }


    private void encodeAAC(RandomAccessFile writer, byte[] chunk, int readLen) throws IOException {
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = getInputBuffer(inputBufferIndex);

            inputBuffer.clear();
            inputBuffer.put(chunk);
            inputBuffer.limit(readLen);
//            long pts = new Date().getTime() * 1000 - presentationTimeUs;
//            presentationTimeUs = pts;
            //presentationTimeUs += (long) (1.0 * bufferSize / (sampleRate * channelCount * (bitsPerSample / 8)) * 1000000.0);

//            if (presentationTimeUs == 0) {
//                presentationTimeUs = bufferInfo.presentationTimeUs;
//            }
//            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - presentationTimeUs;

            if (presentationTimeUs == 0) {
                presentationTimeUs = System.nanoTime() / 1000;
            }
            presentationTimeUs = System.nanoTime() / 1000 - presentationTimeUs;

            mediaCodec.queueInputBuffer(inputBufferIndex, 0, readLen, presentationTimeUs, endOfStream ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            //mediaCodec.queueInputBuffer(inputBufferIndex, 0, readLen, 0, 0);
        }
        if (finish) {
            return;
        }
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

        while (outputBufferIndex >= 0) {
            if (bufferInfo.size == 0) {
                break;
            }

            int outChunkSize = bufferInfo.size + 7;
            byte[] outChunk = new byte[outChunkSize];

            ByteBuffer outputBuffer = getOutputBuffer(outputBufferIndex);
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

            addADTS2Packet(outChunk, channelCount, sampleRate, outChunkSize);

            outputBuffer.get(outChunk, 7, bufferInfo.size);
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.clear();

            writer.write(outChunk, 0, outChunkSize);

            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                endOfStream = true;
                break;
            }
        }
    }

    private ByteBuffer getInputBuffer(int inputIndex) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return mediaCodec.getInputBuffer(inputIndex);
        } else {
            return mediaCodec.getInputBuffers()[inputIndex];
        }
    }

    private ByteBuffer getOutputBuffer(int outputIndex) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return mediaCodec.getOutputBuffer(outputIndex);
        } else {
            return mediaCodec.getOutputBuffers()[outputIndex];
        }
    }


    @Override
    public void run() {
        while (!thread.isInterrupted()) {
            AudioChunk audioChunk = blockingQueue.poll();
            if (finish) {
                break;
            }
            if (audioChunk != null) {
                try {
                    encodeAAC(writer, audioChunk.chunk, audioChunk.readLen);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    class AudioChunk {
        byte[] chunk;
        int readLen;

        public AudioChunk(byte[] chunk, int readLen) {
            this.chunk = chunk;
            this.readLen = readLen;
        }
    }
}
