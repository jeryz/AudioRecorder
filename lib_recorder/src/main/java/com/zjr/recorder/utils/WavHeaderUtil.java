package com.zjr.recorder.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * created by zjr on 2019/11/4
 * wav音频文件头
 */
public class WavHeaderUtil{


    public static byte[] wavFileHeader(long totalAudioLength, long sampleRate, int channels, int bitsPerSample) {

        long totalAudioLen = totalAudioLength - 44;
        long totalDataLen = totalAudioLength - 8;
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
        return header;
    }


    public static void updateWavFileHeader(RandomAccessFile file, long totalFileLen) {
        try {
            //"RIFF"的数据长度：文件总长 - 8
            file.seek(4);
            file.write(long2bytes(totalFileLen - 8));

            //更新实际音频数据长度：文件总长 - 44（wavHeader）
            file.seek(40);
            file.write(long2bytes(totalFileLen - 44));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] long2bytes(long l) {
        byte[] data = new byte[4];
        data[0] = (byte) (l & 0xff);
        data[1] = (byte) ((l >> 8) & 0xff);
        data[2] = (byte) ((l >> 16) & 0xff);
        data[3] = (byte) ((l >> 24) & 0xff);
        return data;
    }

}
