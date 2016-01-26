package com.videonasocialmedia.avrecorder.overlay;

import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.videonasocialmedia.avrecorder.FullFrameRect;
import com.videonasocialmedia.avrecorder.GlUtil;
import com.videonasocialmedia.avrecorder.Texture2dProgram;

/**
 * Created by jca on 25/11/15.
 */
public abstract class Overlay {

    Drawable overlayImage;
    private float[] IDENTITY_MATRIX = new float[16];
    protected FullFrameRect overlayLayer;
    private int height;
    private int width;
    private int positionX;
    private int positionY;
    private int textureId;

    public Overlay(Drawable overlayImage, int height, int width, int positionX, int positionY) {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        Matrix.scaleM(IDENTITY_MATRIX, 0, 1, -1, 1);
        this.overlayImage = overlayImage;
        this.height = height;
        this.width = width;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /**
     * Creates a texture and a shader program. It MUST be called on the GL thread
     */
    public final void initProgram() {
        initTextures();
        Texture2dProgram program =
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
        program.setTexSize(width, height);
        overlayLayer = new FullFrameRect(program);
    }

    protected void initTextures() {
        textureId = GlUtil.createTextureFromDrawable(overlayImage);
    }


    public final void draw(int frame) {
        setGlViewportSize();
        setBlendMode();
        configLayer(frame);
        overlayLayer.drawFrame(textureId, IDENTITY_MATRIX,false);
    }

    protected void configLayer(int frame) {

    }

    protected void setBlendMode(){
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    protected void setGlViewportSize() {
        GLES20.glViewport(positionX, positionY, width, height);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean isInitialized() {
        return overlayLayer != null;
    }

    protected void setTextureId(int textureId) {
        this.textureId = textureId;
    }
}
