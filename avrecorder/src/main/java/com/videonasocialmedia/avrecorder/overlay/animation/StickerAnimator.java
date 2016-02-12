package com.videonasocialmedia.avrecorder.overlay.animation;

import android.util.Log;

import com.videonasocialmedia.avrecorder.overlay.Sticker;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *
 */
public class StickerAnimator {
    private Sticker sticker;
    //TODO how to represent path

    final TreeMap<Long, Coord> positionRoute;
    final TreeMap<Long, Float> sizeVariation;
    boolean repeat;

    long elapsedTime;


    public StickerAnimator(Sticker sticker, TreeMap<Long, Coord> positionRoute,
                           TreeMap<Long, Float> sizeVariation, boolean repeat) {
        this.sticker = sticker;
        this.positionRoute = positionRoute;
        this.sizeVariation = sizeVariation;
        this.repeat = repeat;
    }

    public void animate(long deltaTime) {
        Log.d("Animator", "delta " + deltaTime);
        elapsedTime += deltaTime;
        updatePosition();
        updateSize();
    }

    private void updatePosition() {
        if (positionRoute != null) {
            long normalizedTime = normalizeElapsedTime(positionRoute.lastKey());
            Map.Entry<Long, Coord> entry = positionRoute.floorEntry(normalizedTime);

            Coord coord;
            if (entry == null) {
                coord = positionRoute.firstEntry().getValue();
            } else
                coord = entry.getValue();

            Log.d("Animator", "normalized " + normalizedTime +"," +"total elapsed"+ elapsedTime );
            Log.d("Animator", "coords: ("+coord.x + "," + coord.y + ")");
            sticker.setPositionX(coord.x);
            sticker.setPositionY(coord.y);
        }
    }

    private long normalizeElapsedTime(Long baseTime) {
        long normalizedTime = elapsedTime;
        if (repeat)
            normalizedTime = elapsedTime % baseTime;
        return normalizedTime;
    }

    private void updateSize() {
        if (sizeVariation != null) {
            long normalizedTime = normalizeElapsedTime(sizeVariation.lastKey());
            Map.Entry<Long, Float> entry = sizeVariation.floorEntry(normalizedTime);
            float scaleFactor;
            if (entry == null) {
                scaleFactor = 1f;
            } else
                scaleFactor = entry.getValue();

            sticker.scale(scaleFactor);
        }
    }

    public static class Coord {
        private final int x;
        private final int y;

        public Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }

    }
}



