package com.videonasocialmedia.avrecorder.overlay;

import android.graphics.drawable.Drawable;
import android.opengl.GLES20;

import com.videonasocialmedia.avrecorder.FullFrameRect;
import com.videonasocialmedia.avrecorder.GlUtil;

import java.util.List;

/**
 * Created by jca on 3/12/15.
 */
public class AnimatedOverlay extends Overlay {

    private List<Drawable> overlayImages;
    private int[] textureIds;
    private int previousFrame = 0;
    private int textureCounter;

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
        if (frame > previousFrame + 2) {
            previousFrame = frame;
            if (textureCounter >= textureIds.length-1) {
                textureCounter = 0;
            } else {
                textureCounter++;
            }
        }
        super.setTextureId(textureIds[textureCounter]);
    }

    @Override
         protected void setGlViewportSize() {
        //DO NOTHING
    }
}
