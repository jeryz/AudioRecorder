package com.zjr.recorder.processor;

import com.zjr.recorder.AudioRecordEngine;
import com.zjr.recorder.Recorder;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * created by zjr on 2019/11/28
 */
public class DefaultProcessor implements AudioProcessor{

    @Override
    public int onRead(AudioRecordEngine.PCMReader reader, byte[] buffer) throws IOException {
        return reader.read(buffer, 0, buffer.length);
    }

    @Override
    public void onBegin(RandomAccessFile writer, Recorder.Config config) throws IOException {

    }

    @Override
    public void onAudioChunk(RandomAccessFile writer, byte[] chunk, int readLen) throws IOException {
        if(writer!=null){
            writer.write(chunk,0,readLen);
        }
    }

    @Override
    public void onEnd(RandomAccessFile writer) throws IOException {

    }
}
