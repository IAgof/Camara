package com.videonasocialmedia.avrecorder.overlay.animation;

import com.videonasocialmedia.avrecorder.overlay.Sticker;

import java.util.ArrayList;

/**
 *
 * Control the animation of several stickers
 */
public class AnimatorController {
    ArrayList<StickerAnimator> stickerAnimators;
    long previousTime;

    public AnimatorController() {
        stickerAnimators = new ArrayList<>();
    }

    public void updateStickers() {
        long deltaTime= calculateDelta();
        for (StickerAnimator animator : stickerAnimators) {
            animator.animate(deltaTime);
        }
    }

    private long calculateDelta() {
        if (previousTime == 0)
            initPreviousTime();
        long delta= System.currentTimeMillis() - previousTime;
        previousTime=System.currentTimeMillis();
        return delta;
    }

    private void initPreviousTime() {
        previousTime = System.currentTimeMillis();
    }

    public void register(StickerAnimator animator) {
        stickerAnimators.add(animator);
    }

    public void remove(StickerAnimator animator) {
        stickerAnimators.remove(animator);
    }

}
