package com.zjr.recorder.processor;

import com.zjr.recorder.Recorder;
import com.zjr.recorder.utils.WavHeaderUtil;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * created by zjr on 2019/11/18
 */
public class WAVProcessor extends DefaultProcessor {

    @Override
    public void onBegin(RandomAccessFile writer, Recorder.Config config) throws IOException  {
        if (writer == null)return;
        writer.seek(0);
        writer.write(WavHeaderUtil.wavFileHeader(writer.length(), config.sampleRate, config.channel, config.bitsPerSample));
    }

    @Override
    public void onEnd(RandomAccessFile writer) throws IOException  {
        if (writer == null)return;
        WavHeaderUtil.updateWavFileHeader(writer, writer.length());
    }
}
