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

import com.example.root.kickflip.sdk.exception.KickflipException;


/**
 * Provides callbacks for the major lifecycle benchmarks of a Broadcast.
 */
public interface RecordListener {

        /**
         * The broadcast has started, and is currently buffering.
         */
        public void onRecordStart();


        /**
         * The broadcast has ended.
         */
        public void onRecordStop();

        /**
         * An error occurred.
         */
        public void onRecordError(KickflipException error);
    }