package com.zjr.app;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.zjr.recorder.FileFormat;
import com.zjr.recorder.Recorder;
import com.zjr.recorder.audiotrack.AudioTrackManager;
import com.zjr.recorder.listener.OnRecordListener;
import com.zjr.recorder.listener.OnVolumeListener;
import com.zjr.recorder.view.VoiceView;

import java.util.List;

public class RecordActivity extends AppCompatActivity {

    private String filepath;
    private int oldSampleRate;
    private int oldChannel;
    private Recorder recorder;
    private VoiceView voiceView;
    private TextView tvText;
    private TextView tvState;
    private TextView tvVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorder_activity);
        voiceView = findViewById(R.id.voiceView);
        tvText = findViewById(R.id.tvText);
        tvState = findViewById(R.id.tvState);
        tvVolume = findViewById(R.id.tvVolume);
        checkperm();
        final Spinner spinner1 = findViewById(R.id.spinner1);
        final Spinner spinner2 = findViewById(R.id.spinner2);
        final Spinner spinner3 = findViewById(R.id.spinner3);
        final Spinner spinner4 = findViewById(R.id.spinner4);

        final String[] array1 = new String[]{"8000","16000","22050","24000", "32000","44100","48000","64000"};
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, array1);
        spinner1.setAdapter(adapter1);
        spinner1.setSelection(1);

        final String[] array2 = new String[]{"1", "2"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, array2);
        spinner2.setAdapter(adapter2);
        spinner2.setSelection(1);

        final String[] array3 = new String[]{"pcm", "wav", "aac", "amr"};
        final int[] fileFormat = new int[]{FileFormat.Format.PCM, FileFormat.Format.WAV, FileFormat.Format.AAC, FileFormat.Format.AMR};
        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, array3);
        spinner3.setAdapter(adapter3);
        spinner3.setSelection(1);

        final String[] array4 = new String[]{"0", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"};
        ArrayAdapter<String> adapter4 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, array4);
        spinner4.setAdapter(adapter4);
        spinner4.setSelection(1);

        findViewById(R.id.btn_record_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int sample = Integer.parseInt((String) spinner1.getSelectedItem());
                int channel = Integer.parseInt((String) spinner2.getSelectedItem());
                int duration = Integer.parseInt((String) spinner4.getSelectedItem());
                int position = spinner3.getSelectedItemPosition();

                setupRecorder(channel,sample,fileFormat[position],duration);
                recorder.start();
                tvText.setText("");
            }
        });

        findViewById(R.id.btn_record_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stop();
            }
        });

        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int sample = Integer.parseInt((String) spinner1.getSelectedItem());
                int channel = Integer.parseInt((String) spinner2.getSelectedItem());
                if(sample != oldSampleRate||channel != oldChannel){
                    AudioTrackManager.getInstance().release();
                    AudioTrackManager.getInstance().setAudioTrack(channel,sample,16);
                }
                oldSampleRate = sample;
                oldChannel = channel;

                if(filepath!=null){
                    AudioTrackManager.getInstance().startPlay(filepath);
                }

            }
        });
        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               AudioTrackManager.getInstance().stopPlay();
            }
        });

    }

    private void setupRecorder(int channel,int sampleRate, @FileFormat.Format int format, int duration) {
        recorder = new Recorder.Builder(this)
                .setBitsPerSample(16)
                .setChannel(channel)
                .setSampleRate(sampleRate)
                .setFileFormat(format)
                .setOutputPath("sdcard/RecorderFile")
                .setRecordTimeout(1000*duration)
                .build();

        recorder.setVolumeListener(new OnVolumeListener() {
            @Override
            public void onVolume(double volume) {
                //double v1 = (volume - 30) / 60 * 100;
                tvVolume.setText("db="+volume);
                voiceView.updateVolume((int) volume);
            }
        });
        recorder.setRecordListener(new OnRecordListener() {
            @Override
            public void onState(int state, String msg) {
                tvState.setText(msg);
            }

            @Override
            public void onResult(String path, long duration_s, long size_k) {
                System.out.println("Record path="+path);
                tvText.setText(path+","+duration_s+"s, "+size_k+"k");
                filepath = path;
                //recorder.release();
            }
        });
    }

    private void checkperm() {
        PermissionUtil.checkPermissions(this, new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.SET_ALARM,
                Manifest.permission.SYSTEM_ALERT_WINDOW
        }, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.handlePermissionsResult(requestCode, permissions, grantResults, new PermissionUtil.ResultCallback() {
            @Override
            public void onGranted(int requestCode, List<String> list) {
                //audioRecorder2.setOutputPath("sdcard/RecorderFile");
            }

            @Override
            public void onDenied(int requestCode, List<String> list) {

            }
        });
    }
}
