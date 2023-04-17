package com.example.recoding;
import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class CallReceiver extends BroadcastReceiver {
    private static MediaRecorder recorder;
    private static boolean isRecording = false;
    private static final int REQUEST_PERMISSION_CODE = 101;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                // Phone call started, start recording
                if (checkPermission(context)) {
                    startRecording();
                } else {
                    requestPermission(context);
                }
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                // Phone call ended, stop recording
                stopRecording();
            }
        }
    }

    private void startRecording() {
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(getFilePath());
            recorder.prepare();
            recorder.start();
            isRecording = true;
            Log.d("CallRecorder", "Recording started");

        } catch (IllegalStateException | IOException e) {
            Log.e("CallRecorder", "Failed to start recording: " + e.getMessage());
        }
    }

    private void stopRecording() {
        if (isRecording) {
            try {
                recorder.stop();
                recorder.release();
                isRecording = false;
                Log.d("CallRecorder", "Recording stopped");

            } catch (IllegalStateException e) {
                Log.e("CallRecorder", "Failed to stop recording: " + e.getMessage());
            }
        }
    }

    private String getFilePath() {
        File folder = new File(Environment.getExternalStorageDirectory() + "/CallRecorder");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.e("CallRecorder", "Failed to create directory");
            }
        }
        String filePath = folder.getAbsolutePath() + "/" + new Date().getTime() + ".mp4";
        Log.d("CallRecorder", "Output file path: " + filePath);
        return filePath;
    }

    private boolean checkPermission(Context context) {
        int recordAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return recordAudioPermission == PackageManager.PERMISSION_GRANTED && writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }
}
