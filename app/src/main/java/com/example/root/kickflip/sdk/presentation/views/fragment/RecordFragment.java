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

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.presentation.views.GLCameraEncoderView;

/**
 *
 */
public class RecordFragment extends Fragment {
    /**
     * LOG_LOG_TAG
     */
    private final static String LOG_TAG = "RecordFragment"; //getClass().getSimpleName();

    private static final boolean VERBOSE = true;
    private static RecordFragment mFragment;
    private GLCameraEncoderView mCameraView;

    public RecordFragment() {
        // Required empty public constructor
        if (VERBOSE) Log.i(LOG_TAG, "construct");
    }


    public static RecordFragment getInstance() {
        if (mFragment == null) {
            // We haven't yet created a RecordFragment instance
            mFragment = recreateRecordFragment();
        }
        return mFragment;
    }

    @SuppressLint("LongLogTag")
    private static RecordFragment recreateRecordFragment() {
        Log.i("Recreating RecordFragment", LOG_TAG);

        return new RecordFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (VERBOSE) Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

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
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            root = inflater.inflate(R.layout.fragment_record, container, false);

            if (VERBOSE) Log.i(LOG_TAG, "onCreateView mCameraView");

            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mCameraView.setKeepScreenOn(true);

        } else

            if (VERBOSE) Log.i(LOG_TAG, "onCreateView not mCameraView");
            root = new View(container.getContext());
        return root;
    }









}
