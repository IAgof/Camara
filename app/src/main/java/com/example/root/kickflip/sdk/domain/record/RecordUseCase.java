package com.example.root.kickflip.sdk.domain.record;
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

import android.hardware.Camera;
import android.os.SystemClock;

import com.example.root.kickflip.sdk.presentation.mvp.presenters.OnCameraEffectListener;
import com.example.root.kickflip.sdk.presentation.mvp.presenters.OnColorEffectListener;
import com.example.root.kickflip.sdk.presentation.views.adapter.CameraEffectList;
import com.example.root.kickflip.sdk.presentation.views.adapter.ColorEffectList;

import java.util.ArrayList;

public class RecordUseCase {

    /**
     * Time to select color effect
     */
    private long timeColorEffect = 0;

    public RecordUseCase(){

    }


    /**
     * Get available camera effects
     *
     * @param listener
     */
    public void getAvailableCameraEffects(OnCameraEffectListener listener) {
        /// TODO getAvailableColorEffects from model
        ArrayList<String> cameraEffectList = CameraEffectList.getCameraEffectList();
        //ArrayList<String> effectList = ColorEffectList.getColorEffectList(getCameraInstance());
        listener.onCameraEffectListRetrieved(cameraEffectList);
    }

    public void addCameraEffect(String cameraEffect, OnCameraEffectListener listener) {

        listener.onCameraEffectAdded(cameraEffect, getTimeColorEffect());

    }


    /**
     * Get available color effects
     *
     * @param listener
     */
    public void getAvailableColorEffects(OnColorEffectListener listener, Camera camera) {
        /// TODO getAvailableColorEffects from model
        ArrayList<String> effectList = ColorEffectList.getColorEffectList(camera);
        //ArrayList<String> effectList = ColorEffectList.getColorEffectList(getCameraInstance());
        listener.onColorEffectListRetrieved(effectList);
    }

    /**
     * Add effect
     *
     * @param colorEffect
     * @param listener
     */
        //TODO add CameraEffect, add Effect, add time and add effect to Project
    public void addAndroidCameraEffect(String colorEffect, Camera camera, OnColorEffectListener listener) {
      /*  Camera.Parameters parameters = camera.getParameters();
        parameters.setColorEffect(colorEffect);
        //camera.setDisplayOrientation(180);
        camera.setParameters(parameters);
        listener.onColorEffectAdded(colorEffect, getTimeColorEffect());
        Log.d(LOG_TAG, " addAndroidCameraEffect " + colorEffect + " time " + getTimeColorEffect());
       */

        Camera.Parameters parameters = camera.getParameters();
        parameters.setColorEffect(colorEffect);
        camera.setParameters(parameters);
        listener.onColorEffectAdded(colorEffect, getTimeColorEffect());

    }


    /**
     * Remove effect
     */
    //TODO removeEffect, add time and remove effect from Project
  /*  public void removeEffect(String colorEffect, OnColorEffectListener listener) {
        // removeEffect, addEffect none. Implement effect.getDefaultName()
        Camera.Parameters parameters = camera.getParameters();
        // parameters.setColorEffect(effect.getDefaultName());
        parameters.setColorEffect(Constants.COLOR_EFFECT_NONE);
        camera.setParameters(parameters);
        listener.onColorEffectRemoved(colorEffect, timer.getBase());
    }
   */

    private void setTimer() {
        timeColorEffect = SystemClock.uptimeMillis();
    }

    private long getTimeColorEffect() {
        if (timeColorEffect == 0) {
            return 0;
        } else {
            return SystemClock.uptimeMillis() - timeColorEffect;
        }
    }

}
