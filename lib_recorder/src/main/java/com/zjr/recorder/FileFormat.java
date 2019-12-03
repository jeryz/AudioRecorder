package com.zjr.recorder;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * created by zjr on 2019/11/25
 */
public class FileFormat {
    @IntDef({Format.PCM, Format.WAV, Format.AAC, Format.AMR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Format {
        int PCM = 0;
        int WAV = 1;
        int AAC = 2;
        int AMR = 3;
    }
    public static String value(int format) {
        String suffix = "pcm";
        switch (format) {
            case Format.WAV:
                suffix = "wav";
                break;
            case Format.AAC:
                suffix = "aac";
                break;
            case Format.AMR:
                suffix = "amr";
                break;
        }
        return suffix;
    }
}
