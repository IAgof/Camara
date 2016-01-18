package com.videonasocialmedia.videonarecorder.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.videonasocialmedia.avrecorder.AVRecorder;
import com.videonasocialmedia.avrecorder.SessionConfig;
import com.videonasocialmedia.avrecorder.event.CameraEncoderResetEvent;
import com.videonasocialmedia.avrecorder.view.GLCameraEncoderView;
import com.videonasocialmedia.videonarecorder.R;

import java.io.File;
import java.io.IOException;

import de.greenrobot.event.EventBus;

/**
 * Created by jca on 18/1/16.
 */
public class RecorderSample extends Activity {

    private final static String FOLDER= Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES) + File.separator + "VideonaRecoder Sample";

    private AVRecorder recorder;
    private boolean firstTimeRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorder_activity);
        initRecordButton();
        SessionConfig config = new SessionConfig(FOLDER+System.currentTimeMillis()+".pm4");
        try {
            recorder = new AVRecorder(config);
            GLCameraEncoderView cameraPreview =
                    (GLCameraEncoderView) findViewById(R.id.cameraPreview);
            recorder.setPreviewDisplay(cameraPreview);
            firstTimeRecording = true;
        } catch (IOException e) {
            Log.e("RecorderSample", e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onResume();
        EventBus.getDefault().unregister(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initRecordButton() {
        ToggleButton recordButton = (ToggleButton) findViewById(R.id.recordButton);
        recordButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    if (recorder.isRecording())
                        recorder.stopRecording();
                }
            }
        });
    }

    private void startRecord(){
        if (!recorder.isRecording()) {
            if (!firstTimeRecording) {
                try {
                    resetRecorder();
                } catch (IOException ioe) {
                    //recordView.showError();
                }
            } else {
                recorder.startRecording();
            }
        }
    }

    private void resetRecorder() throws IOException {
        SessionConfig config = new SessionConfig(FOLDER+System.currentTimeMillis()+".pm4");
        recorder.reset(config);
    }

    public void onEventMainThread(CameraEncoderResetEvent e) {
        startRecord();
    }


}
