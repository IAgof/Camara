package com.videonasocialmedia.avrecorder.overlay;

import android.graphics.drawable.Drawable;

import com.videonasocialmedia.avrecorder.GlUtil;

import java.util.List;
import java.util.Random;

/**
 * Created by jca on 3/12/15.
 */
public class AnimatedOverlay extends Overlay {

    private List<Drawable> overlayImages;
    private int[] textureIds;
    private int previousFrame = 0;
    private int textureCounter;

    Random random = new Random();
    int framesPaintingCurrentLayer = 2;

    public AnimatedOverlay(List<Drawable> overlayImages, int height, int width) {
        super(overlayImages.get(0), height, width, 0, 0);

        this.overlayImages = overlayImages;
        textureIds = new int[overlayImages.size()];
    }

    @Override
    protected void initTextures() {
        for (int textureIndex = 0; textureIndex < textureIds.length; textureIndex++) {
            textureIds[textureIndex] =
                    GlUtil.createTextureFromDrawable(overlayImages.get(textureIndex));
        }
    }


    @Override
    protected void configLayer(int frame) {

        if (frame > previousFrame + framesPaintingCurrentLayer) {
            previousFrame = frame;

            framesPaintingCurrentLayer = randInt(2, 4);
            textureCounter = randInt(0, textureIds.length - 1);
        }
        super.setTextureId(textureIds[textureCounter]);
    }

    private int randInt(int min, int max) {
        return random.nextInt(( max - min ) + 1) + min;
    }

    @Override
    protected void setGlViewportSize() {
        //DO NOTHING
    }
}
