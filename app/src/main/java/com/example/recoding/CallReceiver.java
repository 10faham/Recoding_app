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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallReceiver extends BroadcastReceiver {
    private static final int REQUEST_PERMISSION_CODE = 101;
    private static final String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static MediaRecorder recorder;
    private static boolean isRecording = false;
    private static String filePath;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                // Phone call started, start recording
                if (checkPermissions(context)) {
                    startRecording(context);
                } else {
                    requestPermissions(context);
                }
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                // Phone call ended, stop recording
                stopRecording();
            }
        }
    }

    private void startRecording(Context context) {
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            filePath = getFilePath(context);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    Log.e("CallRecorder", "Failed to create directory");
                }
            }
            recorder.setOutputFile(filePath);
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

    private String getFilePath(Context context) {
        String directoryName = "CallRecorder";
        String fileName = "REC_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".mp4";

        // Check if the external storage is mounted and writable
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.e("CallRecorder", "External storage is not mounted or is read-only");
            return null;
        }

        // Create the directory if it does not exist
        File directory = new File(context.getExternalFilesDir(null), directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e("CallRecorder", "Failed to create directory");
                return null;
            }
        }

        // Create the file path
        File file = new File(directory, fileName);
        String filePath = file.getAbsolutePath();
        Log.d("CallRecorder", "Output file path: " + filePath);

        return filePath;
    }

    private boolean checkPermissions(Context context) {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, REQUEST_PERMISSION_CODE);
        }
    }
}
