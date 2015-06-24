package com.example.videonacamera.kickflip.sdk.presentation.views.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.example.videonacamera.kickflip.R;
import com.example.videonacamera.kickflip.sdk.Util;
import com.example.videonacamera.kickflip.sdk.av.SessionConfig;
import com.example.videonacamera.kickflip.sdk.av.gles.FullFrameRect;
import com.example.videonacamera.kickflip.sdk.presentation.mvp.presenters.RecordPresenter;
import com.example.videonacamera.kickflip.sdk.presentation.mvp.views.RecordView;
import com.example.videonacamera.kickflip.sdk.presentation.views.CustomManualFocusView;
import com.example.videonacamera.kickflip.sdk.presentation.views.GLCameraEncoderView;
import com.example.videonacamera.kickflip.sdk.presentation.views.adapter.CameraEffectAdapter;
import com.example.videonacamera.kickflip.sdk.presentation.views.adapter.ColorEffectAdapter;
import com.example.videonacamera.kickflip.sdk.presentation.views.listener.CameraEffectClickListener;
import com.example.videonacamera.kickflip.sdk.presentation.views.listener.ColorEffectClickListener;

import org.lucasr.twowayview.TwoWayView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class RecordFragment extends Fragment implements RecordView, ColorEffectClickListener,
        CameraEffectClickListener {

    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "RecordFragment"; //getClass().getSimpleName();
    /**
     * Activate some log_tag
     */
    private static final boolean VERBOSE = false;
    /**
     * Record Fragment
     */
    private static RecordFragment mFragment;
    /**
     * Record Presenter
     */
    private static RecordPresenter recordPresenter;        // Make static to survive Fragment re-creation
    /**
     * GLCameraEncoderView, openGL surfaceView
     */
    private GLCameraEncoderView mCameraView;
    /**
     * SessionConfig, configure audio and video encoder settings
     */
    private SessionConfig mConfig;
    /**
     * Button to record video
     */
    @InjectView(R.id.button_record)
    ImageButton buttonRecord;
    /**
     * Chronometer, indicate time recording video
     */
    @InjectView(R.id.chronometer_record)
    Chronometer chronometerRecord;
    /**
     * Rec point, animation
     */
    @InjectView(R.id.imageRecPoint)
    ImageView imageRecPoint;
    /**
     * Button to apply color effects
     */
    @InjectView(R.id.button_color_effect)
    ImageButton buttonColorEffect;
    /**
     * Button to apply camera effects
     */
    @InjectView(R.id.button_camera_effect)
    ImageButton buttonCameraEffect;
    /**
     * Button change camera
     */
    @InjectView((R.id.button_change_camera))
    ImageButton buttonChangeCamera;
    /**
     * Adapter to add images color effect
     */
    private ColorEffectAdapter colorEffectAdapter;
    /**
     * Position color effect pressed
     */
    public static int positionColorEffectPressed = 0;
    /**
     * Adapter to add images color effect
     */
    private CameraEffectAdapter cameraEffectAdapter;
    /**
     * Position camera effect pressed
     */
    public static int positionCameraEffectPressed = 0;
    /**
     * RelativeLayout to show and hide color effects
     */
    @InjectView(R.id.relativelayout_color_effect)
    RelativeLayout relativeLayoutColorEffect;
    /**
     * ListView to use horizontal adapter
     */
    @InjectView(R.id.listview_items_color_effect)
    TwoWayView listViewItemsColorEffect;
    /**
     * RelativeLayout to show and hide camera effects
     */
    @InjectView(R.id.relativelayout_camera_effect)
    RelativeLayout relativeLayoutCameraEffect;
    /**
     * ListView to use horizontal adapter
     */
    @InjectView(R.id.listview_items_camera_effect)
    TwoWayView listViewItemsCameraEffect;
    /**
     * CustomManualFocusView
     */
    @InjectView(R.id.customManualFocusView)
    CustomManualFocusView customManualFocusView;

    @InjectView(R.id.button_settings_camera)
    ImageButton buttonSettingsCamera;
    /**
     * Boolean, control show settings camera options
     */
    private boolean isSettingsCameraPressed = false;
    /**
     * Relative layout to show, hide camera options
     */
    @InjectView(R.id.linearLayoutRecordCameraOptions)
    LinearLayout linearLayoutRecordCameraOptions;

    private SensorEventListener mOrientationListener = new SensorEventListener() {
        final int SENSOR_CONFIRMATION_THRESHOLD = 5;
        int[] confirmations = new int[2];
        int orientation = -1;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (getActivity() != null && getActivity().findViewById(R.id.rotateDeviceHint) != null) {
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
                            getActivity().findViewById(R.id.rotateDeviceHint).setVisibility(View.GONE);
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
        if (VERBOSE) Log.i(LOG_TAG, "construct");

        if (mConfig == null) {
            setupDefaultSessionConfig();
        }
    }

    public static RecordFragment getInstance() {
        if (mFragment == null) {
            // We haven't yet created a RecordFragment instance
            mFragment = recreateRecordFragment();
        } else if (recordPresenter != null && !recordPresenter.isRecording()) {
            // We have a leftover RecordFragment but it is not recording
            // Treat it as finished, and recreate
            mFragment = recreateRecordFragment();
        } else {
            Log.i(LOG_TAG, "Recycling recreateRecordFragment");
        }
        return mFragment;
    }

    private static RecordFragment recreateRecordFragment() {
        Log.i(LOG_TAG, "Recreating recreateRecordFragment");
        recordPresenter = null;
        return new RecordFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (VERBOSE) Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setupRecordPresenter();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (VERBOSE) Log.i(LOG_TAG, "onAttach");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recordPresenter != null)
            recordPresenter.onHostActivityResumed();
        startMonitoringOrientation();

        if (colorEffectAdapter != null) {
            colorEffectAdapter = null;
            recordPresenter.colorEffectClickListener();
        }

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.i(LOG_TAG, "onCreateView");


        final View root;
        if (recordPresenter != null && getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            root = inflater.inflate(R.layout.fragment_record, container, false);

            ButterKnife.inject(this, root);

            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mCameraView.setKeepScreenOn(true);

            recordPresenter.setPreviewDisplay(mCameraView);

        } else
            root = new View(container.getContext());


        // AutoFocusView
        customManualFocusView.onPreviewTouchEvent(getActivity().getApplicationContext());

        // Hide menu camera options
        linearLayoutRecordCameraOptions.setVisibility(View.GONE);

        return root;
    }


    protected void setupRecordPresenter() {
        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (recordPresenter == null) {

                // TODO: Don't start recording until stream start response, so we can determine stream type...
                Context context = getActivity().getApplicationContext();
                try {

                    recordPresenter = new RecordPresenter(this, mConfig);

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Unable to create RecordPresenter. Could be trouble creating MediaCodec encoder.");
                    e.printStackTrace();
                }

            }
        }
    }

    private void setupDefaultSessionConfig() {

        //TODO Set this settings from Profile, user preferences

        String outputLocation = new File(Util.PATH_APP, Util.testRecorded).getAbsolutePath();
        mConfig = new SessionConfig.Builder(outputLocation)
                .withVideoBitrate(5 * 1000 * 1000)
                .withVideoResolution(1280,720)
                .withAudioChannels(2)
                .withAudioSamplerate(48000)
                .withAudioBitrate(192 * 1000)
                .build();
    }

    /**
     *  Change camera listener
     */
    @OnClick (R.id.button_change_camera)
    public void buttonChangeCameraListener(){

        recordPresenter.requestOtherCamera();
    }

    /**
     * Camera settings listener
     */
    @OnClick(R.id.button_settings_camera)
    public void buttonSettinsCameraListener(){

        if(isSettingsCameraPressed){
            // Hide menu
            linearLayoutRecordCameraOptions.setVisibility(View.GONE);
            buttonSettingsCamera.setImageResource(R.drawable.activity_record_settings_camera_normal);
            buttonSettingsCamera.setBackground(null);
            isSettingsCameraPressed = false;
        } else {
            // Show menu
            linearLayoutRecordCameraOptions.setVisibility(View.VISIBLE);
            buttonSettingsCamera.setImageResource(R.drawable.activity_record_settings_camera_pressed);
            buttonSettingsCamera.setBackgroundResource(R.color.transparent_palette_grey);
            isSettingsCameraPressed = true;
        }
    }

    /**
     * Record button on click listener
     *
     * @return view
     */
    //TODO buttonRecordListener on Presenter
    @OnClick(R.id.button_record)
    public void buttonRecordListener() {
        Log.d(LOG_TAG, "buttonRecordListener");

        if (recordPresenter.isRecording()) {
            recordPresenter.stopRecording();

        } else {
            recordPresenter.startRecording();
            //stopMonitoringOrientation();
            buttonRecord.setBackgroundResource(R.drawable.activity_record_icon_stop_normal);
        }
    }

    /**
     * Color effect on click listener
     */
    @OnClick(R.id.button_color_effect)
    public void colorEffectButtonListener() {
        recordPresenter.colorEffectClickListener();
    }

    /**
     * Camera effect on click listener
     */
    @OnClick(R.id.button_camera_effect)
    public void cameraEffectButtonListener() {
        recordPresenter.cameraEffectClickListener();
    }

    /**
     * User select effect
     *
     * @param adapter
     * @param colorEffect
     */
    @Override
    public void onColorEffectClicked(ColorEffectAdapter adapter, String colorEffect, int position) {
        Log.d(LOG_TAG, "onColorEffectClicked() RecordActivity");
        positionColorEffectPressed = position;
        adapter.notifyDataSetChanged();
        recordPresenter.setColorEffect(colorEffect);
    }

    /**
     * User select camera effect
     *
     * @param adapter
     * @param cameraEffect
     */
    @Override
    public void onCameraEffectClicked(CameraEffectAdapter adapter, String cameraEffect, int position) {
        Log.d(LOG_TAG, "onColorEffectClicked() RecordActivity");
        positionCameraEffectPressed = position;
        adapter.notifyDataSetChanged();
        recordPresenter.setCameraEffect(position);
    }

    /**
     * Show list of effects
     *
     * @param effects
     */
    @Override
    public void showEffects(ArrayList<String> effects) {
        Log.d(LOG_TAG, "showEffects() RecordActivity");

        colorEffectAdapter = new ColorEffectAdapter(this, effects);

        if (relativeLayoutColorEffect.isShown()) {

            relativeLayoutColorEffect.setVisibility(View.INVISIBLE);

            buttonColorEffect.setImageResource(R.drawable.common_icon_filters_normal);

            return;

        }
        relativeLayoutColorEffect.setVisibility(View.VISIBLE);
        buttonColorEffect.setImageResource(R.drawable.common_icon_filters_pressed);
        colorEffectAdapter.setViewClickColorEffectListener(RecordFragment.this);
        listViewItemsColorEffect.setAdapter(colorEffectAdapter);
    }

    /**
     * Update view with effect selected
     *
     * @param colorEffect
     */
    @Override
    public void showEffectSelected(String colorEffect) {
        Log.d(LOG_TAG, "showEffectSelected() RecordActivity");
        /// TODO apply animation effect
        colorEffectAdapter.notifyDataSetChanged();
    }


    /**
     * Show list of effects
     *
     * @param effects
     */
    @Override
    public void showCameraEffects(ArrayList<String> effects) {
        Log.d(LOG_TAG, "showEffects() RecordActivity");

        cameraEffectAdapter = new CameraEffectAdapter(this, effects);

        if (relativeLayoutCameraEffect.isShown()) {

            relativeLayoutCameraEffect.setVisibility(View.INVISIBLE);

            buttonCameraEffect.setImageResource(R.drawable.effects_bg);

            return;

        }
        relativeLayoutCameraEffect.setVisibility(View.VISIBLE);
        //buttonCameraEffect.setImageResource(R.drawable.common_icon_filters_pressed);
        cameraEffectAdapter.setViewClickCameraEffectListener(RecordFragment.this);
        listViewItemsCameraEffect.setAdapter(cameraEffectAdapter);
    }

    /**
     * Update view with effect selected
     *
     * @param cameraEffect
     */
    @Override
    public void showCameraEffectSelected(String cameraEffect) {
        Log.d(LOG_TAG, "showEffectSelected() RecordActivity");
        /// TODO apply animation effect
        cameraEffectAdapter.notifyDataSetChanged();
    }



    /**
     * Force this fragment to stop recording.
     * Useful if your application wants to stop recording.
     * when a user leaves the Activity hosting this fragment
     */
    public void stopRecording() {
        if (recordPresenter.isRecording()) {
            recordPresenter.stopRecording();
            recordPresenter.release();
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


    @Override
    public void showRecordStarted() {
        buttonRecord.setImageResource(R.drawable.activity_record_icon_stop_normal);
        buttonRecord.setImageAlpha(125); // (50%)
    }

    @Override
    public void showRecordFinished() {
        buttonRecord.setImageResource(R.drawable.activity_record_icon_rec_normal);
        buttonRecord.setEnabled(false);
    }

    @Override
    public void startChronometer() {

        setChronometer();
        chronometerRecord.start();
        // Activate animation rec
        imageRecPoint.setVisibility(View.VISIBLE);
        AnimationDrawable frameAnimation = (AnimationDrawable)imageRecPoint.getDrawable();
        frameAnimation.setCallback(imageRecPoint);
        frameAnimation.setVisible(true, true);

    }

    @Override
    public void stopChronometer() {

        chronometerRecord.stop();
        imageRecPoint.setVisibility(View.INVISIBLE);

    }

    /**
     * Set chronometer with format 00:00
     */
    public void setChronometer() {
        Log.d(LOG_TAG, "setChronometer() RecordActivity");
        chronometerRecord.setBase(SystemClock.elapsedRealtime());
        chronometerRecord.setOnChronometerTickListener(new android.widget.Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(android.widget.Chronometer chronometer) {

                long time = SystemClock.elapsedRealtime() - chronometer.getBase();

                int h = (int) (time / 3600000);
                int m = (int) (time - h * 3600000) / 60000;
                int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                // String hh = h < 10 ? "0"+h: h+"";
                String mm = m < 10 ? "0" + m : m + "";
                String ss = s < 10 ? "0" + s : s + "";
                chronometerRecord.setText(mm+":"+ss);
                RecordFragment.this.chronometerRecord.setText(mm + ":" + ss);

            }
        });
    }

    @Override
    public void showError() {

    }


    /**
     * OnClick buttons, tracking Google Analytics
     */
    @OnClick({R.id.button_record, R.id.button_color_effect, R.id.button_flash_mode,
            R.id.button_settings_camera, R.id.button_change_camera})
    public void clickListener(View view) {
        sendButtonTracked(view.getId());
    }

    /**
     * Sends button clicks to Google Analytics
     *
     * @param id identifier of the clicked view
     */
    private void sendButtonTracked(int id) {
        String label;
        switch (id) {
            case R.id.button_record:
                label = "Capture ";
                break;
            case R.id.button_color_effect:
                label = "Show available effects";
                break;
            case R.id.button_change_camera:
                label = "Change camera";
                break;
            case R.id.button_flash_mode:
                label = "Flash camera";
                break;
            case R.id.button_settings_camera:
                label = "Settings camera";
                break;
            default:
                label = "Other";
        }

        //TODO add tracker to Fragment
     /*   tracker.send(new HitBuilders.EventBuilder()
                .setCategory("RecordActivity")
                .setAction("button clicked")
                .setLabel(label)
                .build());
        GoogleAnalytics.getInstance(this.getApplication().getBaseContext()).dispatchLocalHits();
        */
    }
}
