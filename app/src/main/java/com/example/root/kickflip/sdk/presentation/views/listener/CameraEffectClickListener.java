package com.example.root.kickflip.sdk.presentation.views.listener;

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


import com.example.root.kickflip.sdk.presentation.views.adapter.CameraEffectAdapter;

public interface CameraEffectClickListener {

    void onCameraEffectClicked(CameraEffectAdapter adapter, String effectName, int position);

}
