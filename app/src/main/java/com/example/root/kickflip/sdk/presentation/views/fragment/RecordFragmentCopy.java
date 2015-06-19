
package com.example.root.kickflip.sdk.presentation.views.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.presentation.mvp.presenters.RecordPresenter;
import com.example.root.kickflip.sdk.presentation.views.GLCameraEncoderView;


public class RecordFragmentCopy extends Fragment {

    /**
     * LOG_LOG_TAG
     */

    private final static String LOG_TAG = "RecordFragment"; //getClass().getSimpleName();

    private static final boolean VERBOSE = false;
    private static RecordFragmentCopy mFragment;
    private static RecordPresenter recordPresenter;        // Make static to survive Fragment re-creation
    private GLCameraEncoderView mCameraView;

    View.OnClickListener mRecordButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recordPresenter.isRecording()) {
                recordPresenter.stopRecording();
            } else {
                recordPresenter.startRecording();
                //stopMonitoringOrientation();
                v.setBackgroundResource(R.drawable.red_dot_stop);
            }
        }
    };



    public RecordFragmentCopy() {
        // Required empty public constructor
        if (VERBOSE) Log.i(LOG_TAG, "construct");
    }

    public static RecordFragmentCopy getInstance() {
        if (mFragment == null) {
            // We haven't yet created a RecordFragment instance
            mFragment = recreateRecordFragment();
        } else if (recordPresenter != null && !recordPresenter.isRecording()) {
            // We have a leftover RecordFragment but it is not recording
            // Treat it as finished, and recreate
            mFragment = recreateRecordFragment();
        } else {
            Log.i(LOG_TAG, "Recycling RecordFragment");
        }
        return mFragment;
    }

    @SuppressLint("LongLogTag")
    private static RecordFragmentCopy recreateRecordFragment() {
        Log.i("Recreating RecordFragment", LOG_TAG);
        recordPresenter = null;
        return new RecordFragmentCopy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (VERBOSE) Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

          //  setupRecord();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (VERBOSE) Log.i(LOG_TAG, "onAttach");
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.i(LOG_TAG, "onCreateView");

        View root;
        if (recordPresenter != null && getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            root = inflater.inflate(R.layout.fragment_record, container, false);
            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mCameraView.setKeepScreenOn(true);
            recordPresenter.setPreviewDisplay(mCameraView);
            Button recordButton = (Button) root.findViewById(R.id.recordButton);

            recordButton.setOnClickListener(mRecordButtonClickListener);

            if (recordPresenter.isRecording()) {
                recordButton.setBackgroundResource(R.drawable.red_dot_stop);

            }
         //   setupFilterSpinner(root);
         //   setupCameraFlipper(root);
        } else
            root = new View(container.getContext());
        return root;
    }



}

