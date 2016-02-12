package com.videonasocialmedia.avrecorder.overlay;

import android.graphics.drawable.Drawable;
import android.opengl.GLES20;

import com.videonasocialmedia.avrecorder.GlUtil;

/**
 * Full Screen image filter. A
 * <p/>
 * This class should dissapear the moment we figure out how to do it with shaders
 */
public class Filter extends Overlay {

    Drawable filterImage;

    public Filter(Drawable filterImage, int height, int width) {
        super(height, width, 0, 0);
        this.filterImage = filterImage;
    }

    @Override
    protected void initTextures() {
        int textureId = GlUtil.createTextureFromDrawable(filterImage);
        filterImage = null;
        setTextureId(textureId);
    }

    @Override
    protected void setBlendMode() {
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_COLOR);
    }

    @Override
    protected void setGlViewportSize() {
        //TODO set the viewport to full screen
    }

}
