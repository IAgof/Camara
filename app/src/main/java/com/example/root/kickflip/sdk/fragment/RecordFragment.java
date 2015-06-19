package com.example.root.kickflip.sdk.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.Kickflip;
import com.example.root.kickflip.sdk.av.gles.FullFrameRect;
import com.example.root.kickflip.sdk.av.Recorder;
import com.example.root.kickflip.sdk.event.BroadcastIsBufferingEvent;
import com.example.root.kickflip.sdk.event.BroadcastIsLiveEvent;
import com.example.root.kickflip.sdk.view.GLCameraEncoderView;
import com.google.common.eventbus.Subscribe;

import java.io.IOException;

/**
 * This is a drop-in broadcasting fragment.
 * Currently, only one BroadcastFragment may be instantiated at a time by
 * design of {@link com.example.root.kickflip.sdk.av.Broadcaster}.
 */
public class RecordFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "RecordFragment";
    private static final boolean VERBOSE = false;
    private static RecordFragment mFragment;
    private static Recorder mRecorder;        // Make static to survive Fragment re-creation
    private GLCameraEncoderView mCameraView;

    View.OnClickListener mRecordButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mRecorder.isRecording()) {
                mRecorder.stopRecording();
            } else {
                mRecorder.startRecording();
                //stopMonitoringOrientation();
                v.setBackgroundResource(R.drawable.red_dot_stop);
            }
        }
    };

    private SensorEventListener mOrientationListener = new SensorEventListener() {
        final int SENSOR_CONFIRMATION_THRESHOLD = 5;
        int[] confirmations = new int[2];
        int orientation = -1;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (getActivity() != null && getActivity().findViewById(R.id.rotateDeviceHint) != null) {
                //Log.i(TAG, "Sensor " + event.values[1]);
                if (event.values[1] > 10 || event.values[1] < -10) {
                    // Sensor noise. Ignore.
                } else if (event.values[1] < 5.5 && event.values[1] > -5.5) {
                    // Landscape
                    if (orientation != 1 && readingConfirmed(1)) {
                        if (mRecorder.getSessionConfig().isConvertingVerticalVideo()) {
                            if (event.values[0] > 0) {
                                mRecorder.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.LANDSCAPE);
                            } else {
                                mRecorder.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.UPSIDEDOWN_LANDSCAPE);
                            }
                        } else {
                            getActivity().findViewById(R.id.rotateDeviceHint).setVisibility(View.GONE);
                        }
                        orientation = 1;
                    }
                } else if (event.values[1] > 7.5 || event.values[1] < -7.5) {
                    // Portrait
                    if (orientation != 0 && readingConfirmed(0)) {
                        if (mRecorder.getSessionConfig().isConvertingVerticalVideo()) {
                            if (event.values[1] > 0) {
                                mRecorder.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.VERTICAL);
                            } else {
                                mRecorder.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.UPSIDEDOWN_VERTICAL);
                            }
                        } else {
                            getActivity().findViewById(R.id.rotateDeviceHint).setVisibility(View.VISIBLE);
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

    public RecordFragment() {
        // Required empty public constructor
        if (VERBOSE) Log.i(TAG, "construct");
    }

    public static RecordFragment getInstance() {
        if (mFragment == null) {
            // We haven't yet created a RecordFragment instance
            mFragment = recreateRecordFragment();
        } else if (mRecorder != null && !mRecorder.isRecording()) {
            // We have a leftover RecordFragment but it is not recording
            // Treat it as finished, and recreate
            mFragment = recreateRecordFragment();
        } else {
            Log.i(TAG, "Recycling RecordFragment");
        }
        return mFragment;
    }

    private static RecordFragment recreateRecordFragment() {
        Log.i(TAG, "Recreating RecordFragment");
        mRecorder = null;
        return new RecordFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (!Kickflip.readyToRecord()) {
            Log.e(TAG, "Kickflip not properly prepared by RecordFragment's onCreate. SessionConfig: " + Kickflip.getSessionConfig()); // + " key " + Kickflip.getApiKey() + " secret " + Kickflip.getApiSecret());
        } else {
            setupRecord();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (VERBOSE) Log.i(TAG, "onAttach");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRecorder != null)
            mRecorder.onHostActivityResumed();
        startMonitoringOrientation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null)
            mRecorder.onHostActivityPaused();
        stopMonitoringOrientation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRecorder != null && !mRecorder.isRecording())
            mRecorder.release();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreateView");

        View root;
        if (mRecorder != null && getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            root = inflater.inflate(R.layout.fragment_record, container, false);
            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mCameraView.setKeepScreenOn(true);
            mRecorder.setPreviewDisplay(mCameraView);
            Button recordButton = (Button) root.findViewById(R.id.recordButton);

            recordButton.setOnClickListener(mRecordButtonClickListener);

            if (mRecorder.isLive()) {
               // setBannerToLiveState();

            }
            if (mRecorder.isRecording()) {
                recordButton.setBackgroundResource(R.drawable.red_dot_stop);
                if (!mRecorder.isLive()) {
                  //  setBannerToBufferingState();
                }
            }
            setupFilterSpinner(root);
            setupCameraFlipper(root);
        } else
            root = new View(container.getContext());
        return root;
    }

    protected void setupRecord() {
        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mRecorder == null) {
                if (VERBOSE)
                    Log.i(TAG, "Setting up Record for output " + Kickflip.getSessionConfig().getOutputPath()); // + " client key: " + Kickflip.getApiKey() + " secret: " + Kickflip.getApiSecret());
                // TODO: Don't start recording until stream start response, so we can determine stream type...
                Context context = getActivity().getApplicationContext();
                try {
                    mRecorder = new Recorder(Kickflip.getSessionConfig()); //, Kickflip.getApiKey(), Kickflip.getApiSecret());
                    mRecorder.getEventBus().register(this);
                    mRecorder.setRecordListener((Kickflip.getRecordListener()));
                    Kickflip.clearSessionConfig();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to create Record. Could be trouble creating MediaCodec encoder.");
                    e.printStackTrace();
                }

            }
        }
    }

    private void setupFilterSpinner(View root) {
        Spinner spinner = (Spinner) root.findViewById(R.id.filterSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
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
                    mRecorder.requestOtherCamera();
                }
            });
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (((String) parent.getTag()).compareTo("filter") == 0) {
            mRecorder.applyFilter(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Subscribe
    public void onBroadcastIsBuffering(BroadcastIsBufferingEvent event) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  //  setBannerToBufferingState();
                  //  animateLiveBanner();
                }
            });
        }
    }

    @Subscribe
    public void onBroadcastIsLive(final BroadcastIsLiveEvent liveEvent) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                       // setBannerToLiveState(liveEvent.getWatchUrl());
                    } catch (Exception e) {
                        Log.i(TAG, "onBroadcastIsLiveException");
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Force this fragment to stop broadcasting.
     * Useful if your application wants to stop broadcasting
     * when a user leaves the Activity hosting this fragment
     */
    public void stopRecording() {
        if (mRecorder.isRecording()) {
            mRecorder.stopRecording();
            mRecorder.release();
        }
    }


    protected void startMonitoringOrientation() {
        if (getActivity() != null) {
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sensorManager.registerListener(mOrientationListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void stopMonitoringOrientation() {
        if (getActivity() != null) {
            View deviceHint = getActivity().findViewById(R.id.rotateDeviceHint);
            if (deviceHint != null) deviceHint.setVisibility(View.GONE);
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(mOrientationListener);
        }
    }
}
