package com.example.videonacamera.kickflip.sdk.presentation.views.adapter;

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

import java.util.ArrayList;

public class CameraEffectList {


    public static ArrayList<String> getCameraEffectList() {

        ArrayList<String> cameraEffects = new ArrayList<String>();

        cameraEffects.add("Normal");
        cameraEffects.add("Black&White");
        cameraEffects.add("Chroma");
        cameraEffects.add("Blur");
        cameraEffects.add("Sharpen");
        cameraEffects.add("Edge detect");
        cameraEffects.add("Emboss");
        cameraEffects.add("Squeeze");
        cameraEffects.add("Twirl");
        cameraEffects.add("Tunnel");
        cameraEffects.add("Bulge");
        cameraEffects.add("Dent");
        cameraEffects.add("Fisheye");
        cameraEffects.add("Stretch");
        cameraEffects.add("Mirror");


        return cameraEffects;
    }



}
