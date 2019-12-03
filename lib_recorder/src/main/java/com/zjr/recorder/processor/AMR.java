package com.zjr.recorder.processor;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaFormat;

import com.zjr.recorder.AudioRecordEngine;
import com.zjr.recorder.Recorder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * created by zjr on 2019/11/26
 */
public class AMR extends PCM {

    private MediaCodec mCodec;
    private MediaCodec.BufferInfo mInfo;
    private boolean mSawOutputEOS;
    private boolean mSawInputEOS;

    // frame is 20 msec at 8.000/16.000 khz
    private int samples_per_frame;
    // result amr stream
    private byte[] mBuf;
    private long totalLen = 0;
    private int mBufIn = 0;
    private int mBufOut = 0;
    private Recorder.Config config;

    public AMR() {
        // initCodec();
    }

    @SuppressLint("NewApi")
    private void initCodec() {
        MediaFormat format = new MediaFormat();
        if (config.sampleRate == 16000) {//amr_wb
            //bitrate: 6.6kb/s  8.85kb/s  12.65kb/s  14.25kb/s  15.85kb/s  18.25kb/s 19.85kb/s  23.05kb/s  23.85kb/s,
            format.setInteger(MediaFormat.KEY_BIT_RATE, 18250);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AMR_WB);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 16000);
            //frame is 20 msec at 16.000 khz
            samples_per_frame = 16000 * 20 / 1000;
        } else {
            config.sampleRate = 8000;
            format.setInteger(MediaFormat.KEY_BIT_RATE, 12200);// 32byte
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AMR_NB);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 8000);
            // frame is 20 msec at 8.000 khz
            samples_per_frame = 8000 * 20 / 1000;
        }
        mBuf = new byte[samples_per_frame * 2];

        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        String name = mcl.findEncoderForFormat(format);
        if (name != null) {
            try {
                mCodec = MediaCodec.createByCodecName(name);
                mCodec.configure(format,
                        null /* surface */,
                        null /* crypto */,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (IOException e) {
                if (mCodec != null) {
                    mCodec.release();
                }
                mCodec = null;
            }
        }
        mInfo = new MediaCodec.BufferInfo();
    }


    @Override
    public void onBegin(RandomAccessFile writer, Recorder.Config config) throws IOException {
        this.config = config;
        //AMR的文件头
        //"#!AMR\n"    (or 0x2321414d520a in hexadecimal)
        //"#!AMR-WB\n" (or 0x2321414d522d57420a in hexadecimal)
        if (config.sampleRate == 16000) {
//            writer.write(0x23);
//            writer.write(0x21);
//            writer.write(0x41);
//            writer.write(0x4D);
//            writer.write(0x52);
//            writer.write(0x2D);
//            writer.write(0x57);
//            writer.write(0x42);
//            writer.write(0x0A);
            writer.write("#!AMR-WB\n".getBytes());

        } else {
//            writer.write(0x23);
//            writer.write(0x21);
//            writer.write(0x41);
//            writer.write(0x4D);
//            writer.write(0x52);
//            writer.write(0x0A);
            writer.write("#!AMR\n".getBytes());
        }

        if (mCodec == null) {
            initCodec();
        }
        mCodec.start();
        totalLen = 0;
//        The magic number for multi-channel AMR files MUST consist of the
//        ASCII character string:
//
//        "#!AMR_MC1.0\n"
//        (or 0x2321414d525F4D43312E300a in hexadecimal).
//
//        The magic number for multi-channel AMR-WB files MUST consist of the
//        ASCII character string:
//
//        "#!AMR-WB_MC1.0\n"
//        (or 0x2321414d522d57425F4D43312E300a in hexadecimal).
    }

    @Override
    public int onRead(AudioRecordEngine.PCMReader reader, byte[] buffer) throws IOException {
        if (mCodec == null) {
            throw new IllegalStateException("not open");
        }
        int length = buffer.length;
        if (mBufOut >= mBufIn && !mSawOutputEOS) {
            // no data left in buffer, refill it
            mBufOut = 0;
            mBufIn = 0;

            // first push as much data into the encoder as possible
            while (!mSawInputEOS) {
                int index = mCodec.dequeueInputBuffer(0);
                if (index < 0) {
                    // no input buffer currently available
                    break;
                } else {
                    int numRead;
                    for (numRead = 0; numRead < samples_per_frame * 2; ) {
                        int n = reader.read(mBuf, numRead, samples_per_frame * 2 - numRead);
                        if (n < 0) {
                            mSawInputEOS = true;
                            break;
                        }
                        numRead += n;
                    }
                    ByteBuffer buf = mCodec.getInputBuffer(index);
                    buf.put(mBuf, 0, numRead);
                    mCodec.queueInputBuffer(index,
                            0 /* offset */,
                            numRead,
                            0 /* presentationTimeUs */,
                            mSawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0 /* flags */);
                }
            }

            // now read encoded data from the encoder
            int index = mCodec.dequeueOutputBuffer(mInfo, 0);
            if (index >= 0) {
                mBufIn = mInfo.size;
                ByteBuffer out = mCodec.getOutputBuffer(index);
                out.get(mBuf, 0 /* offset */, mBufIn /* length */);
                mCodec.releaseOutputBuffer(index, false /* render */);
                if ((mInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    mSawOutputEOS = true;
                }
            }
        }

        if (mBufOut < mBufIn) {
            // there is data in the buffer
            if (length > mBufIn - mBufOut) {
                length = mBufIn - mBufOut;
            }
            System.arraycopy(mBuf, mBufOut, buffer, 0, length);
            mBufOut += length;

            totalLen+=length;
            return length;
        }

        if (mSawInputEOS && mSawOutputEOS) {
            // no more data available in buffer, codec or input stream
            return -1;
        }

        // caller should try again
        return 0;
    }

    private long getDurationInMills(long length) {
        return length * 8 * 1000 / config.bitsPerSample / samples_per_frame / config.channel;
    }

    @Override
    public void onEnd(RandomAccessFile writer) throws IOException {
        System.out.println("amrDuration="+getDurationInMills(totalLen));
        try {
            if (mCodec != null) {
                mCodec.release();
            }
        } finally {
            mCodec = null;
        }
    }


}
