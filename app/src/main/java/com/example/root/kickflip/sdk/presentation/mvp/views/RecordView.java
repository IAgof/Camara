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

import com.example.root.kickflip.sdk.presentation.views.GLCameraEncoderView;

public interface RecordView {

 /*   void startPreview(CameraPreview cameraPreview, CustomManualFocusView customManualFocusView,
                      boolean supportAutoFocus);

    void stopPreview(CameraPreview cameraPreview, CustomManualFocusView customManualFocusView,
                     boolean supportAutoFocus);
 */

    void startPreview(GLCameraEncoderView cameraEncoderView);

    void showRecordStarted();

    void showRecordFinished();

  /*  void startChronometer();

    void stopChronometer();

    void showEffects(ArrayList<String> effects);

    void showEffectSelected(String colorEffect);

    void navigateEditActivity();

    void lockScreenRotation();

    void lockNavigator();

    void unLockNavigator();

    void showSettingsCamera(boolean isFlashSupported,boolean isChangeCameraSupported);

    void showFlashModeTorch(boolean mode);

    void showCamera(int cameraMode);
*/

    void showError();
}
