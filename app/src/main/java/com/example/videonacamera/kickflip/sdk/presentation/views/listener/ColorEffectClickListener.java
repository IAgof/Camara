package com.example.videonacamera.kickflip.sdk.presentation.views.listener;

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


import com.example.videonacamera.kickflip.sdk.presentation.views.adapter.ColorEffectAdapter;

public interface ColorEffectClickListener {

    void onColorEffectClicked(ColorEffectAdapter adapter, String effectName, int position);

}
