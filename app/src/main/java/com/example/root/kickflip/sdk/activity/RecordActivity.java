package com.example.root.kickflip.sdk.activity;

import android.os.Bundle;
import android.util.Log;

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.Kickflip;
import com.example.root.kickflip.sdk.av.RecordListener;
import com.example.root.kickflip.sdk.exception.KickflipException;
import com.example.root.kickflip.sdk.fragment.RecordFragment;

/**
 * BroadcastActivity manages a single live broadcast. It's a thin wrapper around {@link io.kickflip.sdk.fragment.BroadcastFragment}
 */
public class RecordActivity extends ImmersiveActivity implements RecordListener {
    private static final String TAG = "BroadcastActivity";

    private RecordFragment mFragment;
    private RecordListener mMainRecordListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        mMainRecordListener = (RecordListener) Kickflip.getRecordListener();
        Kickflip.setRecordListener(this);

        if (savedInstanceState == null) {
            mFragment = RecordFragment.getInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, mFragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (mFragment != null) {
            mFragment.stopRecording();
        }
        super.onBackPressed();
    }

    @Override
    public void onRecordStart() {
        mMainRecordListener.onRecordStart();
        Log.d("RecordActivity", "onRecordStart");
    }


    @Override
    public void onRecordStop() {
        finish();
        mMainRecordListener.onRecordStop();
        Log.d("RecordActivity", "onRecordStop");
    }

    @Override
    public void onRecordError(KickflipException error) {
        mMainRecordListener.onRecordError(error);
    }

}
