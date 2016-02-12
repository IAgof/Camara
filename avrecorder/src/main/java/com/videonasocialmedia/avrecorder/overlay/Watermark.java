package com.videonasocialmedia.avrecorder.overlay;

import android.graphics.drawable.Drawable;
import android.opengl.GLES20;

import com.videonasocialmedia.avrecorder.GlUtil;

/**
 * Created by jca on 1/12/15.
 */
public class Watermark extends Overlay{

    Drawable watermarkImage;

    public Watermark(Drawable watermarkImage, int height, int width, int positionX, int positionY) {
        super(height, width, positionX, positionY);
        this.watermarkImage = watermarkImage;
    }

    @Override
    protected void initTextures() {
        int textureId=GlUtil.createTextureFromDrawable(watermarkImage);
        watermarkImage=null;
        setTextureId(textureId);
    }

    @Override
    protected void setBlendMode() {
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
