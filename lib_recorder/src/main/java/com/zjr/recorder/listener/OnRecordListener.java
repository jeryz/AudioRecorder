package com.zjr.recorder.listener;

/**
 * created by zjr on 2019/11/14
 */
public interface OnRecordListener {
    int STATE_ERROR = -1;
    int STATE_RECORDING = 0;
    int STATE_START = 1;
    int STATE_STOP = 2;
    int STATE_PAUSE = 3;

    void onState(int state, String msg);

    void onResult(String path, long duration_s, long size_k);

}
