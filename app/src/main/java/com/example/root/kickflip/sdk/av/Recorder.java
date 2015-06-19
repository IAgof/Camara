package com.example.root.kickflip.sdk.av;
/*
 * Copyright (C) 2015 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 *
 * Authors:
 * Álvaro Martínez Marco
 *
 */

import android.content.Context;
import android.util.Log;

import com.google.common.eventbus.EventBus;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;


public class Recorder extends AVRecorder {


    private static final String TAG = "Recorder";
    private static final boolean VERBOSE = false;

    private SessionConfig mConfig;
    private RecordListener mRecordListener;
    private EventBus mEventBus;
    private boolean mSentRecordLiveEvent;
    private int mVideoBitrate;

    public Recorder(SessionConfig config) throws IOException {
        super(config);

        init();

        mConfig = config;
        mConfig.getMuxer().setEventBus(mEventBus);
        mVideoBitrate = mConfig.getVideoBitrate();

        if (VERBOSE) Log.i(TAG, "Initial video bitrate : " + mVideoBitrate);

    }

    private void init() {

        mSentRecordLiveEvent = false;
        mEventBus = new EventBus("Record");
        mEventBus.register(this);
    }


    /**
     * Set a Listener to be notified of basic Broadcast events relevant to
     * updating a broadcasting UI.
     * e.g: Broadcast begun, went live, stopped, or encountered an error.
     * <p/>
     * See {@link com.example.root.kickflip.sdk.av.RecorderListener}
     *
     * @param listener
     */
    public void setRecordListener(RecordListener listener) {
        mRecordListener = listener;
    }

    /**
     * Set an {@link com.google.common.eventbus.EventBus} to be notified
     * of events between {@link com.example.root.kickflip.sdk.av.Broadcaster},
     * {@link com.example.root.kickflip.sdk.av.HlsFileObserver}, {@link com.example.root.kickflip.sdk.api.s3.S3BroadcastManager}
     * e.g: A HLS MPEG-TS segment or .m3u8 Manifest was written to disk, or uploaded.
     * See a list of events in {@link com.example.root.kickflip.sdk.event}
     *
     * @return
     */
    public EventBus getEventBus() {
        return mEventBus;
    }

    /**
     * Start broadcasting.
     * <p/>
     * Must be called after {@link Broadcaster#setPreviewDisplay(com.example.root.kickflip.sdk.view.GLCameraView)}
     */
    @Override
    public void startRecording() {
        super.startRecording();

        mCamEncoder.requestThumbnailOnDeltaFrameWithScaling(10, 1);
        Log.i(TAG, "got StartStreamResponse");
    }


    /**
     * Stop broadcasting and release resources.
     * After this call this Broadcaster can no longer be used.
     */
    @Override
    public void stopRecording() {
        super.stopRecording();
        mSentRecordLiveEvent = false;

    }


    public SessionConfig getSessionConfig() {
        return mConfig;
    }

    /**
     * Check if the broadcast has gone live
     *
     * @return
     */
    public boolean isLive() {
        return mSentRecordLiveEvent;
    }

}
