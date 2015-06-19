package com.example.root.kickflip.sdk.presentation.views.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.av.gles.FullFrameRect;
import com.example.root.kickflip.sdk.presentation.mvp.presenters.RecordPresenter;
import com.example.root.kickflip.sdk.presentation.mvp.views.RecordView;
import com.example.root.kickflip.sdk.presentation.views.fragment.RecordFragment;

import java.io.IOException;

/**
 * RecordActivity manages a single live record.
 */
public class RecordActivity extends ImmersiveActivity implements RecordView, AdapterView.OnItemSelectedListener{

    /**
     * LOG_TAG
     */
    private final String LOG_TAG = getClass().getSimpleName();

    private RecordFragment recordFragment;

    // Presenter
    RecordPresenter recordPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        try {
            recordPresenter = new RecordPresenter(this);
        } catch (IOException e) {
            e.printStackTrace();
        }


       if (savedInstanceState == null) {
            recordFragment = RecordFragment.getInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, recordFragment)
                    .commit();
        }
        //recordFragment = (RecordFragment) getFragmentManager().findFragmentById(R.id.fragment)

    }

    @Override
    public void onResume() {
        super.onResume();
        if (recordPresenter != null)
            recordPresenter.onHostActivityResumed();
        startMonitoringOrientation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (recordPresenter != null)
            recordPresenter.onHostActivityPaused();
        stopMonitoringOrientation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recordPresenter != null && !recordPresenter.isRecording())
            recordPresenter.release();
    }

    @Override
    public void onBackPressed() {
        if (recordFragment != null) {
            stopRecording();
        }
        super.onBackPressed();
    }


    /**
     * Force this fragment to stop broadcasting.
     * Useful if your application wants to stop broadcasting
     * when a user leaves the Activity hosting this fragment
     */
    public void stopRecording() {
        if (recordPresenter.isRecording()) {
            recordPresenter.stopRecording();
            recordPresenter.release();
        }
    }

    @Override
    public void showRecordStarted() {

    }

    @Override
    public void showRecordFinished() {

    }

    @Override
    public void showError() {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (((String) parent.getTag()).compareTo("filter") == 0) {
            recordPresenter.applyFilter(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }



    protected void startMonitoringOrientation() {
        if (this != null) {
            SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
            sensorManager.registerListener(mOrientationListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void setupRecord() {
        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (recordPresenter == null) {
                // if (VERBOSE)
                //  Log.i(LOG_TAG, "Setting up Record for output " + Kickflip.getSessionConfig().getOutputPath()); // + " client key: " + Kickflip.getApiKey() + " secret: " + Kickflip.getApiSecret());
                // TODO: Don't start recording until stream start response, so we can determine stream type...
                Context context = this.getApplicationContext();
                try {
                 /*   recordPresenter = new Recorder(Kickflip.getSessionConfig()); //, Kickflip.getApiKey(), Kickflip.getApiSecret());
                    recordPresenter.getEventBus().register(this);
                    recordPresenter.setRecordListener((Kickflip.getRecordListener()));
                    Kickflip.clearSessionConfig();
                    */
                    recordPresenter = new RecordPresenter(this);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Unable to create Record. Could be trouble creating MediaCodec encoder.");
                    e.printStackTrace();
                }

            }
        }
    }

    private void setupFilterSpinner(View root) {
        Spinner spinner = (Spinner) root.findViewById(R.id.filterSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.camera_filter_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    private void setupCameraFlipper(View root) {
        View flipper = root.findViewById(R.id.cameraFlipper);
        if (Camera.getNumberOfCameras() == 1) {
            flipper.setVisibility(View.GONE);
        } else {
            flipper.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recordPresenter.requestOtherCamera();
                }
            });
        }
    }

    protected void stopMonitoringOrientation() {
        if (this != null) {
            View deviceHint = this.findViewById(R.id.rotateDeviceHint);
            if (deviceHint != null) deviceHint.setVisibility(View.GONE);
            SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(mOrientationListener);
        }
    }

    private SensorEventListener mOrientationListener = new SensorEventListener() {
        final int SENSOR_CONFIRMATION_THRESHOLD = 5;
        int[] confirmations = new int[2];
        int orientation = -1;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (this != null && findViewById(R.id.rotateDeviceHint) != null) {
                //Log.i(LOG_TAG, "Sensor " + event.values[1]);
                if (event.values[1] > 10 || event.values[1] < -10) {
                    // Sensor noise. Ignore.
                } else if (event.values[1] < 5.5 && event.values[1] > -5.5) {
                    // Landscape
                    if (orientation != 1 && readingConfirmed(1)) {
                        if (recordPresenter.getSessionConfig().isConvertingVerticalVideo()) {
                            if (event.values[0] > 0) {
                                recordPresenter.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.LANDSCAPE);
                            } else {
                                recordPresenter.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.UPSIDEDOWN_LANDSCAPE);
                            }
                        } else {
                            findViewById(R.id.rotateDeviceHint).setVisibility(View.GONE);
                        }
                        orientation = 1;
                    }
                } else if (event.values[1] > 7.5 || event.values[1] < -7.5) {
                    // Portrait
                    if (orientation != 0 && readingConfirmed(0)) {
                        if (recordPresenter.getSessionConfig().isConvertingVerticalVideo()) {
                            if (event.values[1] > 0) {
                                recordPresenter.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.VERTICAL);
                            } else {
                                recordPresenter.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.UPSIDEDOWN_VERTICAL);
                            }
                        } else {
                            findViewById(R.id.rotateDeviceHint).setVisibility(View.VISIBLE);
                        }
                        orientation = 0;
                    }
                }
            }
        }

        /**
         * Determine if a sensor reading is trustworthy
         * based on a series of consistent readings
         */
        private boolean readingConfirmed(int orientation) {
            confirmations[orientation]++;
            confirmations[orientation == 0 ? 1 : 0] = 0;
            return confirmations[orientation] > SENSOR_CONFIRMATION_THRESHOLD;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
}
