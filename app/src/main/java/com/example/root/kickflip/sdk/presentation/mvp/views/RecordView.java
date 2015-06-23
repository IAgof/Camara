package com.example.root.kickflip.sdk.presentation.mvp.views;
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

import com.example.root.kickflip.sdk.presentation.views.CustomManualFocusView;
import com.example.root.kickflip.sdk.presentation.views.GLCameraEncoderView;

import java.util.ArrayList;

public interface RecordView {

    void startPreview(GLCameraEncoderView cameraEncoderView, CustomManualFocusView customManualFocusView,
                      boolean supportFocus);

    void stopPreview(GLCameraEncoderView cameraEncoderView, CustomManualFocusView customManualFocusView,
                     boolean supportFocus);

    void showRecordStarted();

    void showRecordFinished();

    void startChronometer();

    void stopChronometer();

    void showEffects(ArrayList<String> effects);

    void showEffectSelected(String colorEffect);

    void showCameraEffects(ArrayList<String> effects);

    void showCameraEffectSelected(String colorEffect);

  /*  void navigateEditActivity();

    void lockScreenRotation();

    void lockNavigator();

    void unLockNavigator();

    void showSettingsCamera(boolean isFlashSupported,boolean isChangeCameraSupported);

    void showFlashModeTorch(boolean mode);

    void showCamera(int cameraMode);
*/

    void showError();
}
