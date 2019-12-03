package com.zjr.recorder.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;

/**
 * created by zjr on 2019/7/1
 */
public class VoiceView extends View {

    private final Paint mPaint;
    private float mVolume;
    private int mStrokeWidth = 6;
    private int mCount = 100;

    LinkedList<Float> volumeList = new LinkedList<>();

    public VoiceView(Context context) {
        this(context, null);
    }

    public VoiceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0x33000000);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mStrokeWidth);
    }

    public void setLineCount(int count){
        mCount = count;
    }

    public void updateVolume(float volume) {
        mVolume = volume / 100.0f;
        volumeList.add(mVolume);
        invalidate();
        int index = volumeList.size() - mCount;
        if (index > 0) {
            volumeList.removeFirst();
        }
    }

    public void clear() {
        volumeList.clear();
        invalidate();
    }

    public void reset() {
        volumeList.clear();
        for (int i = 0; i < mCount; i++) {
            volumeList.add(0.0F);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int cY = height / 2 + mStrokeWidth;
        int lineX = width / mCount;
        int drawX = width - mStrokeWidth;


        int index = volumeList.size() - mCount;
        index = index > 0 ? index : 0;

        for (int begin = volumeList.size() - 1; begin >= index; begin--) {
            float y = cY * volumeList.get(begin) / 2;
            y = y == 0 ? 1 : y;
            canvas.drawLine(drawX, cY - y, drawX, cY + y, mPaint);
            drawX -= lineX;
        }

    }
//
//    float quadraticBezier3(float p0, float p1, float p2, float p3, float t) {
//        float k = 1 - t;
//        return p0 * k * k * k + 3 * p1 * t * k * k + 3 * p2 * t * t * k + p3 * t * t * t;    //二次贝赛尔曲线
//    }

//    public static float quadraticBezier(float p0, float p1, float p2, @FloatRange(from = 0, to = 1) float t) {
//        float k = 1 - t;
//        return k * k * p0 + 2 * t * k * p1 + t * t * p2;
//    }
}
