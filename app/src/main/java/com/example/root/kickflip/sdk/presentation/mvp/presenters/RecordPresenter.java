package com.example.root.kickflip.sdk.presentation.mvp.presenters;
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

import android.util.Log;

import com.example.root.kickflip.sdk.Util;
import com.example.root.kickflip.sdk.av.CameraEncoder;
import com.example.root.kickflip.sdk.av.MicrophoneEncoder;
import com.example.root.kickflip.sdk.presentation.views.listener.OnRecordListener;
import com.example.root.kickflip.sdk.av.SessionConfig;
import com.example.root.kickflip.sdk.av.gles.FullFrameRect;
import com.example.root.kickflip.sdk.presentation.exception.RecordException;
import com.example.root.kickflip.sdk.presentation.mvp.views.RecordView;
import com.example.root.kickflip.sdk.presentation.views.GLCameraView;

import java.io.File;
import java.io.IOException;

public class RecordPresenter implements OnRecordListener {

    /**
     * LOG_TAG
     */
    private final String LOG_TAG = getClass().getSimpleName();

    private final RecordView recordView;

    private SessionConfig mConfig;

    protected CameraEncoder mCamEncoder;
    protected MicrophoneEncoder mMicEncoder;

    private boolean mIsRecording;

    public RecordPresenter(RecordView recordView, SessionConfig config) throws IOException {

        this.recordView = recordView;
        mConfig = config;
        start();
    }

    private void init(SessionConfig config) throws IOException {
        mCamEncoder = new CameraEncoder(config);
        mMicEncoder = new MicrophoneEncoder(config);
        mConfig = config;
        mIsRecording = false;
    }

    private void setupDefaultSessionConfig() {
        Log.i(LOG_TAG, "Setting default SessonConfig");

        String outputLocation = new File(Util.PATH_APP, Util.testRecorded).getAbsolutePath();
        mConfig = new SessionConfig.Builder(outputLocation)
                .withVideoBitrate(2 * 1000 * 1000)
                .withAudioBitrate(192 * 1000)
                .build();
    }


    public SessionConfig getSessionConfig() {
        return mConfig;
    }

    public void setPreviewDisplay(GLCameraView display){
        mCamEncoder.setPreviewDisplay(display);
    }

    public void applyFilter(int filter){
        mCamEncoder.applyFilter(filter);
    }

    public void requestOtherCamera(){
        mCamEncoder.requestOtherCamera();
    }

    public void requestCamera(int camera){
        mCamEncoder.requestCamera(camera);
    }

    public void toggleFlash(){
        mCamEncoder.toggleFlashMode();
    }

    public void adjustVideoBitrate(int targetBitRate){
        mCamEncoder.adjustBitrate(targetBitRate);
    }

    /**
     * Signal that the recorder should treat
     * incoming video frames as Vertical Video, rotating
     * and cropping them for proper display.
     *
     * This method only has effect if {SessionConfig#setConvertVerticalVideo(boolean)}
     * has been set true for the current recording session.
     *
     */
    public void signalVerticalVideo(FullFrameRect.SCREEN_ROTATION orientation) {
        mCamEncoder.signalVerticalVideo(orientation);
    }

    public void startRecording(){
        mIsRecording = true;
        mMicEncoder.startRecording();
        mCamEncoder.startRecording();
    }

    public boolean isRecording(){
        return mIsRecording;
    }

    public void stopRecording(){
        mIsRecording = false;
        mMicEncoder.stopRecording();
        mCamEncoder.stopRecording();
    }

    /**
     *
     */
    public void start(){
        if (mConfig == null) {
            setupDefaultSessionConfig();
        }

        try {
            init(getSessionConfig());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepare for a subsequent recording. Must be called after {@link #stopRecording()}
     * and before {@link #release()}
     * @param config
     */
    public void reset(SessionConfig config) throws IOException {
        mCamEncoder.reset(config);
        mMicEncoder.reset(config);
        mConfig = config;
        mIsRecording = false;
    }

    /**
     * Release resources. Must be called after {@link #stopRecording()} After this call
     * this instance may no longer be used.
     */
    public void release() {
        mCamEncoder.release();
        // MicrophoneEncoder releases all it's resources when stopRecording is called
        // because it doesn't have any meaningful state
        // between recordings. It might someday if we decide to present
        // persistent audio volume meters etc.
        // Until then, we don't need to write MicrophoneEncoder.release()
    }

    public void onHostActivityPaused(){
        mCamEncoder.onHostActivityPaused();
    }

    public void onHostActivityResumed(){
        mCamEncoder.onHostActivityResumed();
    }

    @Override
    public void onRecordStart() {

        startRecording();
    }

    @Override
    public void onRecordStop() {

        stopRecording();

    }

    @Override
    public void onRecordError(RecordException error) {

    }
}
