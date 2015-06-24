/*
 * Copyright (c) 2015. Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.example.videonacamera.kickflip.sdk.presentation.mvp.presenters;

import java.util.ArrayList;

public interface OnCameraEffectListener {

    void onCameraEffectAdded(String cameraEffect, long time);

    void onCameraEffectRemoved(String cameraEffect, long time);

    void onCameraEffectListRetrieved(ArrayList<String> effect);

}
