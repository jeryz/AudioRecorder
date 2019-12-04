package com.zjr.recorder.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * created by zjr on 2019/11/28
 * pcm转wav,amr,aac
 */
public class PCMConvertUtil {

    /**
     * 将pcm文件转wav文件
     */
    public static void pcm2wav(int sampleRate, int channels, String pcmPath, String wavPath) {
        FileInputStream is = null;
        FileOutputStream out = null;
        try {
            File file = new File(pcmPath);
            long totalAudioLength = file.length();
            long totalAudioLen = totalAudioLength - 44;
            long totalDataLen = totalAudioLength - 8;
            int bitsPerSample = 16;
            long byteRate = sampleRate * channels * bitsPerSample / 8;
            byte[] header = new byte[44];
            //WAV文件采用的是RIFF格式结构。至少是由3个块构成,分别是RIFF、fmt 和Data。所有基于压缩编码的WAV文件必须含有fact块
            //多声道样本数据采用交替方式存储。
            // 例如:立体声(双声道)采样值的存储顺序为:通道1第1采样值,通道2第1采样值;通道1第2采样值,通道2第2采样值;以此类推
            //RIFF/WAVE header
            //RIFF(Resources lnterchange File Format) 文件结构标识，4 bytes
            // 'RIFF' chunk1
            header[0] = 'R';
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);//4 bytes:不包括RIFF的文件长度=文件总长度-8bytes
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';//4 bytes: WAVE文件格式类型
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            // 'fmt' chunk2 描述音频数据的信息格式
            header[12] = 'f';
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16; // 4 bytes: fmt chunk 的长度，不确定，可以是16，18，20，40等
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1; // format = 1  编码格式=pcm
            header[21] = 0;
            header[22] = (byte) channels;//通道数
            header[23] = 0;
            header[24] = (byte) (sampleRate & 0xff);//4 bytes: 采样率，每个声道单位时间采样次数
            header[25] = (byte) ((sampleRate >> 8) & 0xff);
            header[26] = (byte) ((sampleRate >> 16) & 0xff);
            header[27] = (byte) ((sampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);//4 bytes: 数据传输速率=采样率*通道数*采样位数/8
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (channels * (bitsPerSample / 8)); //2 bytes: 采样帧大小=通道数*采样位数/8
            header[33] = 0;
            header[34] = (byte) bitsPerSample; // 2 bytes:每个采样的采样位数
            header[35] = 0;
            //'data' chunk3 描述音频数据大小
            header[36] = 'd';//4 bytes: 音频数据标志符data
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);//4 bytes: 音频数据的长度：文件总长 - 44bytes
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
            //then raw audio data
            is = new FileInputStream(pcmPath);
            out = new FileOutputStream(wavPath);
            out.write(header);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(out, is);
        }
    }

    /**
     * 将pcm文件转aac文件
     */
    public static void pcm2aac(int sampleRate, int channelCount, int bitRate, String pcmPath, String wavPath) {
        AACEncode aacEncode = new AACEncode(sampleRate, channelCount, bitRate);
        FileInputStream is = null;
        FileOutputStream out = null;
        try {
            is = new FileInputStream(pcmPath);
            out = new FileOutputStream(wavPath);
            byte[] buffer = new byte[4096];
            int len;
            aacEncode.start();
            while ((len = is.read(buffer)) > 0) {
                aacEncode.encodeAAC(out,buffer,len);
            }
            aacEncode.release();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close(out, is);
        }

    }

    /**
     * 将pcm文件转amr文件
     */
    public static void pcm2Amr(String pcmPath, String amrPath) {
        try {
            pcm2Amr(new FileInputStream(pcmPath), amrPath);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 将pcm数据流转amr文件
     */
    public static void pcm2Amr(InputStream pcmStream, String amrPath) {
        AmrInputStream is = null;
        OutputStream out = null;
        try {
            is = new AmrInputStream(pcmStream);
            out = new FileOutputStream(amrPath);
            byte[] buffer = new byte[4096];
            int len;
            out.write(0x23);
            out.write(0x21);
            out.write(0x41);
            out.write(0x4D);
            out.write(0x52);
            out.write(0x0A);
            while ((len = is.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(out, is, pcmStream);
        }
    }

    private static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final class AACEncode {
        private final String MINE_TYPE_AAC = MediaFormat.MIMETYPE_AUDIO_AAC;//"audio/mp4a-latm";
        private MediaCodec mediaCodec;
        private int bufferSize;
        private int bitRate;
        private int sampleRate;
        private int channelCount;
        private long presentationTimeUs;
        private MediaCodec.BufferInfo bufferInfo;
        private boolean endOfStream;

        public AACEncode(int sampleRate, int channelCount, int bitRate) {
            this.sampleRate = sampleRate;
            this.channelCount = channelCount;
            this.bitRate = bitRate;
            bufferSize = channelCount * 16 / 8 * sampleRate * 100 / 1000;
        }

        public void start() throws IOException {
            if (mediaCodec == null) {
                mediaCodec = MediaCodec.createEncoderByType(MINE_TYPE_AAC);
                MediaFormat format = MediaFormat.createAudioFormat(MINE_TYPE_AAC, sampleRate, channelCount);
                format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                bufferInfo = new MediaCodec.BufferInfo();
            }
            endOfStream = false;
            mediaCodec.start();
        }

        public void release(){
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
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

        private void addADTS2Packet(byte[] packet, int channelCount, int sampleRate, int packetLen) {
            int freqIdx = getFreqIndex(sampleRate);
            int profile = 2;  //AACProcessor LC
            //int chanCfg = channelCount;  //CPE
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (channelCount >> 2));
            packet[3] = (byte) (((channelCount & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
        }


        public void encodeAAC(FileOutputStream writer, byte[] chunk, int readLen) throws IOException {
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = getInputBuffer(inputBufferIndex);

                inputBuffer.clear();
                inputBuffer.put(chunk);
                inputBuffer.limit(readLen);

                if (presentationTimeUs == 0) {
                    presentationTimeUs = System.nanoTime() / 1000;
                }
                presentationTimeUs = System.nanoTime() / 1000 - presentationTimeUs;

                mediaCodec.queueInputBuffer(inputBufferIndex, 0, readLen, presentationTimeUs, endOfStream ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                //mediaCodec.queueInputBuffer(inputBufferIndex, 0, readLen, 0, 0);
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
    }

    public static final class AmrInputStream extends InputStream {
        private final static String TAG = "AmrInputStream";

        // frame is 20 msec at 8.000 khz
        private final static int SAMPLES_PER_FRAME = 8000 * 20 / 1000;

        MediaCodec mCodec;
        MediaCodec.BufferInfo mInfo;
        boolean mSawOutputEOS;
        boolean mSawInputEOS;

        // pcm input stream
        private InputStream mInputStream;

        // result amr stream
        private final byte[] mBuf = new byte[SAMPLES_PER_FRAME * 2];
        private int mBufIn = 0;
        private int mBufOut = 0;

        // helper for bytewise read()
        private byte[] mOneByte = new byte[1];

        /**
         * DO NOT USE - use MediaCodec instead
         */
        @SuppressLint("NewApi")
        public AmrInputStream(InputStream inputStream) {
            Log.w(TAG, "@@@@ AmrInputStream is not a public API @@@@");
            mInputStream = inputStream;

            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AMR_NB);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 8000);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 12200);

            MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            String name = mcl.findEncoderForFormat(format);
            if (name != null) {
                try {
                    mCodec = MediaCodec.createByCodecName(name);
                    mCodec.configure(format,
                            null /* surface */,
                            null /* crypto */,
                            MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mCodec.start();
                } catch (IOException e) {
                    if (mCodec != null) {
                        mCodec.release();
                    }
                    mCodec = null;
                }
            }
            mInfo = new MediaCodec.BufferInfo();
        }

        /**
         * DO NOT USE
         */
        @Override
        public int read() throws IOException {
            int rtn = read(mOneByte, 0, 1);
            return rtn == 1 ? (0xff & mOneByte[0]) : -1;
        }

        /**
         * DO NOT USE
         */
        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        /**
         * DO NOT USE
         */
        @SuppressLint("NewApi")
        @Override
        public int read(byte[] b, int offset, int length) throws IOException {
            if (mCodec == null) {
                throw new IllegalStateException("not open");
            }

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
                        for (numRead = 0; numRead < SAMPLES_PER_FRAME * 2; ) {
                            int n = mInputStream.read(mBuf, numRead, SAMPLES_PER_FRAME * 2 - numRead);
                            if (n == -1) {
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
                System.arraycopy(mBuf, mBufOut, b, offset, length);
                mBufOut += length;
                return length;
            }

            if (mSawInputEOS && mSawOutputEOS) {
                // no more data available in buffer, codec or input stream
                return -1;
            }

            // caller should try again
            return 0;
        }

        @SuppressLint("NewApi")
        @Override
        public void close() throws IOException {
            try {
                if (mInputStream != null) {
                    mInputStream.close();
                }
            } finally {
                mInputStream = null;
                try {
                    if (mCodec != null) {
                        mCodec.release();
                    }
                } finally {
                    mCodec = null;
                }
            }
        }

        @SuppressLint("NewApi")
        @Override
        protected void finalize() throws Throwable {
            if (mCodec != null) {
                Log.w(TAG, "AmrInputStream wasn't closed");
                mCodec.release();
            }
        }
    }
}
