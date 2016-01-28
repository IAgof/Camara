package com.videonasocialmedia.videonarecorder.sample;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.videonasocialmedia.avrecorder.AVRecorder;
import com.videonasocialmedia.avrecorder.Filters;
import com.videonasocialmedia.avrecorder.FullFrameRect;
import com.videonasocialmedia.avrecorder.SessionConfig;
import com.videonasocialmedia.avrecorder.event.CameraEncoderResetEvent;
import com.videonasocialmedia.avrecorder.event.CameraOpenedEvent;
import com.videonasocialmedia.avrecorder.view.AspectFrameLayout;
import com.videonasocialmedia.avrecorder.view.GLCameraEncoderView;
import com.videonasocialmedia.videonarecorder.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by jca on 18/1/16.
 */
public class RecorderSample extends Activity {

    private final static String FOLDER = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES) + File.separator + "VideonaRecoder Sample";

    private AVRecorder recorder;
    private boolean firstTimeRecording;
    GLCameraEncoderView cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorder_activity);
        initRecordButton();
        initRotateButton();
        cameraPreview = (GLCameraEncoderView) findViewById(R.id.cameraPreview);
        //SessionConfig config = new SessionConfig(FOLDER+System.currentTimeMillis()+".pm4");
        SessionConfig config = new SessionConfig(FOLDER,
                640, 480, 5 * 1000 * 1000, 1, 48000, 192000);
        try {
            recorder = new AVRecorder(config);

            GLCameraEncoderView cameraPreview =
                    (GLCameraEncoderView) findViewById(R.id.cameraPreview);
            recorder.setPreviewDisplay(cameraPreview);
            List<Drawable> animatedOverlayFrames = getAnimatedOverlay();
            recorder.addAnimatedOverlayFilter(animatedOverlayFrames);
            recorder.applyFilter(Filters.FILTER_SEPIA);
            firstTimeRecording = true;
        } catch (IOException e) {
            Log.e("RecorderSample", e.getMessage());
        }
    }

    @NonNull
    private List<Drawable> getAnimatedOverlay() {
        List<Drawable> animatedOverlayFrames = new ArrayList<>();
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_j));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_a));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_b));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_h));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_g));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_c));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_d));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_e));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_f));
        animatedOverlayFrames.add(this.getResources().getDrawable(R.mipmap.silent_film_overlay_i));
        return animatedOverlayFrames;
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
        super.onPause();
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

    private void initRotateButton() {
        ToggleButton button = (ToggleButton) findViewById(R.id.rotateCameraButton);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    recorder.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.VERTICAL);
                else
                    recorder.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.LANDSCAPE);
            }
        });
    }


    private void startRecord() {
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
        SessionConfig config = new SessionConfig(FOLDER + System.currentTimeMillis() + ".mp4",
                640, 480, 5 * 1000 * 1000, 1, 48000, 192000);
        recorder.reset(config);
    }

    public void onEventMainThread(CameraEncoderResetEvent e) {
        startRecord();
    }

}
