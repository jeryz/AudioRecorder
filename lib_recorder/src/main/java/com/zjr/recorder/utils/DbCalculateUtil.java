package com.zjr.recorder.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * created by zjr on 2019/11/13
 */
public class DbCalculateUtil {

    private static final double MAX_LEVEL = 32768;

    public static int getRecordVolume2(byte[] chunk, int size, int bitsPerSample) {
        if(chunk==null)return 0;
        double sumVolume = 0.0;
        double avgVolume;
        if (bitsPerSample == 16) {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(chunk, 0, size);
            final short[] buf = new short[size / 2];
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buf);
            for (short b : buf) {
                sumVolume += Math.abs(b);
            }
            avgVolume = sumVolume * 1000  / (buf.length * MAX_LEVEL);
        } else {
            for (int i = 0; i < size; i++) {
                sumVolume += Math.abs(chunk[i]);
            }
            avgVolume = sumVolume / size;
        }

        if (avgVolume >= 200) {
            avgVolume = 200;
        }

        double v = avgVolume/200.0 * 100;

        return (int) v;
    }

    public static int getRecordVolume(byte[] chunk, int size, int bitsPerSample) {
        if(chunk==null)return 0;
        double sumVolume = 0.0;
        double avgVolume;
        if (bitsPerSample == 16) {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(chunk, 0, size);
            final short[] buf = new short[size / 2];
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buf);
            for (short b : buf) {
                sumVolume += Math.abs(b);
            }
            avgVolume = sumVolume / buf.length;
        } else {
            for (int i = 0; i < size; i++) {
                sumVolume += Math.abs(chunk[i]);
            }
            avgVolume = sumVolume / size;
        }

        int volume = (int) (20 * Math.log10(avgVolume));
        //Log.d(TAG, "volume="+volume+" avgVolume="+avgVolume);

        return volume;
    }

    //60分贝	为正常说话声
    //dbm = 10 * Math.log10(p/base), pBase = 0.01
    //dbm = 20 * Math.log10(v/base), vBase = 100
    public static int rmsDb(byte[] data, int len) {
        if(data==null)return 0;
        if (len <= 0) return 0;
        short[] arrays = new short[len / 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(arrays);
        double sum = 0;
        int sample_count = arrays.length;
        for (short sample : arrays) {
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / sample_count);
        rms = 20 * Math.log10(rms);
        return (int) rms;
    }

    public static int rmsDb1(byte[] data, int len) {
        if(data==null)return 0;
        if (len <= 0) return 0;
        short[] arrays = new short[len / 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(arrays);
        double sum = 0;
        int sample_count = arrays.length;
        int min = (int)MAX_LEVEL;
        int max = 0;
        for (short sample : arrays) {
            sum += sample * sample;
            int abs = Math.abs(sample);
            if(abs>10&&abs<min){
                min = abs;
            }
            if(abs>max){
                max = abs;
            }
        }
        double rms = Math.sqrt(sum / sample_count);
        double v = 20 * Math.log10(rms);
        System.out.println("rmsDb1 min="+min+" max="+max+" rms="+(int)rms+" v="+(int)v);
        return (int) v;
    }

    public static int rmsDb2(byte[] data, int len) {
        if(data==null)return 0;
        if (len <= 0) return 0;
        short[] arrays = new short[len / 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(arrays);

        double sum = 0;
        int sample_count = len;
        for (short sample : arrays) {
            sum += sample * sample;
        }
        double avg = sum / (double) sample_count;
        double volume = 10 * Math.log10(avg);

        return (int) volume;
    }
}
