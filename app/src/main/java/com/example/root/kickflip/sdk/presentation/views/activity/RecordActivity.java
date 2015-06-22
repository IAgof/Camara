package com.example.root.kickflip.sdk.presentation.views.activity;

import android.os.Bundle;

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.presentation.views.fragment.RecordFragment;

/**
 * RecordActivity manages a single live record.
 */
public class RecordActivity extends ImmersiveActivity {

    /**
     * LOG_TAG
     */
    private final String LOG_TAG = getClass().getSimpleName();

    private RecordFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);



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


}
