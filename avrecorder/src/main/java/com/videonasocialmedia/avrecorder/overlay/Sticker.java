package com.videonasocialmedia.avrecorder.overlay;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.videonasocialmedia.avrecorder.GlUtil;

/**
 *
 */
public class Sticker extends Overlay{

    Drawable overlayImage;
    String text;
    Typeface typeface;

    public Sticker(Drawable overlayImage, int height, int width, int positionX, int positionY) {
        super(height, width, positionX, positionY);
        this.overlayImage=overlayImage;
    }

    public Sticker(int height, int width, int positionX, int positionY, String text, Typeface typeface) {
        super(height, width, positionX, positionY);
        this.text = text;
        this.typeface = typeface;
    }

    @Override
    protected void initTextures() {
        int textureId;
        if (overlayImage!=null) {
            textureId = GlUtil.createTextureFromDrawable(overlayImage);
            overlayImage = null;
        }else{
            textureId=GlUtil.createTextureWithTextContent(text,typeface);
        }
        setTextureId(textureId);
    }

    public void setPositionX(int x){
        this.positionX=x;
    }

    public void setPositionY(int y){
        this.positionY=y;
    }

    public void scale(float scaleFactor) {
        width*=scaleFactor;
        height*=scaleFactor;
    }
}
