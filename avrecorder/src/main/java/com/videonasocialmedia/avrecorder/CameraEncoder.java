package com.videonasocialmedia.avrecorder;

import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import com.videonasocialmedia.avrecorder.event.CameraEncoderResetEvent;
import com.videonasocialmedia.avrecorder.event.CameraOpenedEvent;
import com.videonasocialmedia.avrecorder.overlay.AnimatedOverlay;
import com.videonasocialmedia.avrecorder.overlay.Filter;
import com.videonasocialmedia.avrecorder.overlay.Overlay;
import com.videonasocialmedia.avrecorder.overlay.Sticker;
import com.videonasocialmedia.avrecorder.overlay.Watermark;
import com.videonasocialmedia.avrecorder.overlay.animation.AnimatorController;
import com.videonasocialmedia.avrecorder.overlay.animation.StickerAnimator;
import com.videonasocialmedia.avrecorder.view.GLCameraEncoderView;
import com.videonasocialmedia.avrecorder.view.GLCameraView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;


/**
 * @hide
 */
public class CameraEncoder implements SurfaceTexture.OnFrameAvailableListener, Runnable {

    private static final String TAG = "CameraEncoder";
    private static final boolean TRACE = true;         // Systrace
    private static final boolean VERBOSE = BuildConfig.DEBUG;       // Lots of logging
    // EncoderHandler Message types (Message#what)
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_SURFACE_TEXTURE = 3;
    private static final int MSG_REOPEN_CAMERA = 4;
    private static final int MSG_RELEASE_CAMERA = 5;
    private static final int MSG_RELEASE = 6;
    private static final int MSG_RESET = 7;
    private final Object mStopFence = new Object();
    private final Object mSurfaceTextureFence = new Object();   // guards mSurfaceTexture shared with GLSurfaceView.Renderer
    private final Object mReadyForFrameFence = new Object();    // guards mReadyForFrames/mRecording
    private final Object mReadyFence = new Object();            // guards ready/running
    private final Object mChangeCameraFence = new Object();
    private volatile STATE mState = STATE.UNINITIALIZED;
    private volatile STATE mStateChangeCamera = STATE.UNINITIALIZED;
    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private int mTextureId;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;
    private Camera mCamera;
    private SessionConfig mSessionConfig;
    private float[] mTransform = new float[16];
    private int mCurrentFilter;
    private int mNewFilter;
    private boolean mIncomingSizeUpdated;
    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private EglStateSaver mEglSaver;
    private SurfaceTexture mSurfaceTexture;
    private boolean mReadyForFrames;                            // Is the SurfaceTexture et all created
    private boolean mRecording;                                 // Are frames being recorded
    private boolean mEosRequested;                              // Should an EOS be sent on next frame. Used to stop encoder
    private boolean mReady;                                     // mHandler created on Encoder thread
    private boolean mRunning;                                   // Encoder thread running
    private EventBus mEventBus;
    private boolean mEncodedFirstFrame;
    private GLSurfaceView mDisplayView;
    private CameraSurfaceRenderer mDisplayRenderer;
    private int mCurrentCamera;
    private int mDesiredCamera;
    private String mCurrentFlash;
    private String mDesiredFlash;
    private boolean mThumbnailRequested;
    private int mThumbnailScaleFactor;
    private int mThumbnailRequestedOnFrame;
    private int mCurrentCameraRotation;
    private int cameraInfoOrientation;
    private List<Overlay> overlayList;
    private Watermark watermark;

    AnimatorController animatorController;

    public CameraEncoder(SessionConfig config) {
        mState = STATE.INITIALIZING;
        init(config);
        mEventBus = EventBus.getDefault();
        mEglSaver = new EglStateSaver();
        startEncodingThread();
        animatorController = new AnimatorController();
        mState = STATE.INITIALIZED;
    }

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size.
     * <p/>
     * TODO: should do a best-fit match.
     */
    private static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null && VERBOSE) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            Log.d(TAG, "Camera getSupportedPreviewSizes widthXheigth: " + size.width + " X " + size.height);
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    /**
     * Resets per-recording state. This excludes {@link EglStateSaver},
     * which should be re-used across recordings made by this CameraEncoder instance.
     *
     * @param config the desired parameters for the next recording.
     */
    private void init(SessionConfig config) {
        mEncodedFirstFrame = false;
        mReadyForFrames = false;
        mRecording = false;
        mEosRequested = false;

        mCurrentCamera = -1;
        mDesiredCamera = Camera.CameraInfo.CAMERA_FACING_BACK;

        mCurrentFlash = Parameters.FLASH_MODE_OFF;
        mDesiredFlash = null;

        mCurrentFilter = -1;
        mNewFilter = Filters.FILTER_NONE;

        mThumbnailRequested = false;
        mThumbnailRequestedOnFrame = -1;

        mSessionConfig = checkNotNull(config);
    }

    /**
     * Prepare for a new recording with the given parameters.
     * This must be called after {@link #stopRecording()} and before {@link #release()}
     *
     * @param config the desired parameters for the next recording. Make sure you're
     *               providing a new {@link SessionConfig} to avoid
     *               overwriting a previous recording.
     */
    public void reset(SessionConfig config) {
        if (mState != STATE.UNINITIALIZED) {
            throw new IllegalArgumentException("reset called in invalid state");
        } else {
            mState = STATE.INITIALIZING;
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RESET, config));
        }
    }

    private void handleReset(SessionConfig config) throws IOException {
        if (mState != STATE.INITIALIZING)
            throw new IllegalArgumentException("handleRelease called in invalid state");
        Log.i(TAG, "handleReset");
        init(config);
        // Make display EGLContext current
        mEglSaver.makeSavedStateCurrent();
        prepareEncoder(mEglSaver.getSavedEGLContext(),
                mSessionConfig.getVideoWidth(),
                mSessionConfig.getVideoHeight(),
                mSessionConfig.getVideoBitrate(),
                mSessionConfig.getMuxer());
        mReadyForFrames = true;
        mState = STATE.INITIALIZED;
        if (mEventBus != null)
            mEventBus.post(new CameraEncoderResetEvent());
    }

    public SessionConfig getConfig() {
        return mSessionConfig;
    }

    /**
     * Request the device camera not currently selected
     * be made active. This will take effect immediately
     * or as soon as the camera preview becomes active.
     * <p/>
     * Called from UI thread
     *
     * @return the camera code
     */
    public int requestOtherCamera() {
        int otherCamera = 0;
        if (mCurrentCamera == 0)
            otherCamera = 1;
        requestCamera(otherCamera);

        mStateChangeCamera = STATE.UNINITIALIZED;
        synchronized (mChangeCameraFence) {

            while (mStateChangeCamera != STATE.INITIALIZED) {
                try {
                    mChangeCameraFence.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return otherCamera;
    }

    /**
     * Request a Camera by cameraId. This will take effect immediately
     * or as soon as the camera preview becomes active.
     * <p/>
     * Called from UI thread
     *
     * @param camera
     */
    public void requestCamera(int camera) {
        if (Camera.getNumberOfCameras() == 1) {
            Log.w(TAG, "Ignoring requestCamera: only one device camera available.");
            return;
        }
        mDesiredCamera = camera;
        if (mCamera != null && mDesiredCamera != mCurrentCamera) {
            // Hot swap camera
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_CAMERA));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_REOPEN_CAMERA));
        }
    }

    /**
     * Request a thumbnail be generated from
     * the next available frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnail(int scaleFactor) {
        mThumbnailRequested = true;
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = -1;
    }

    /**
     * Request a thumbnail be generated deltaFrame frames from now.
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnailOnDeltaFrameWithScaling(int deltaFrame, int scaleFactor) {
        requestThumbnailOnFrameWithScaling(mFrameNum + deltaFrame, scaleFactor);
    }

    /**
     * Request a thumbnail be generated from
     * the given frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnailOnFrameWithScaling(int frame, int scaleFactor) {
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = frame;
    }

    public void adjustBitrate(int targetBitrate) {
        mVideoEncoder.adjustBitrate(targetBitrate);
    }

    public void signalVerticalVideo(FullFrameRect.SCREEN_ROTATION orientation) {
        if (mFullScreen != null) mFullScreen.adjustForVerticalVideo(orientation, true);
        mDisplayRenderer.signalVertialVideo(orientation);
    }

    public void logSavedEglState() {
        mEglSaver.logState();
    }

    public void setPreviewDisplay(GLCameraView display) {
        checkNotNull(display);
        mDisplayRenderer = new CameraSurfaceRenderer(this);
        // Prep GLSurfaceView and attach Renderer

        display.setEGLContextClientVersion(2);
        display.setRenderer(mDisplayRenderer);
        //display.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        display.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        display.setPreserveEGLContextOnPause(true);
        mDisplayView = display;
    }

    /**
     * Called from GLSurfaceView.Renderer thread
     *
     * @return The SurfaceTexture containing the camera frame to display. The
     * display EGLContext is current on the calling thread
     * when this call completes
     */
    public SurfaceTexture getSurfaceTextureForDisplay() {
        synchronized (mSurfaceTextureFence) {
            if (mSurfaceTexture == null)
                Log.w(TAG, "getSurfaceTextureForDisplay called before ST created");
            return mSurfaceTexture;
        }
    }

    public boolean isSurfaceTextureReadyForDisplay() {
        synchronized (mSurfaceTextureFence) {
            return !( mSurfaceTexture == null );
        }
    }

    /**
     * Apply a filter to the camera input
     *
     * @param filter
     */
    public void applyFilter(int filter) {
        Filters.checkFilterArgument(filter);
        mDisplayRenderer.changeFilterMode(filter);
        synchronized (mReadyForFrameFence) {
            mNewFilter = filter;
        }
    }

    public Overlay addOverlayFilter(Drawable overlayImage, int width, int height) {
        Overlay overlayToAdd = new Filter(overlayImage, height, width);
        addOverlayFilter(overlayToAdd);
        return overlayToAdd;
    }

    public void addOverlayFilter(Overlay overlayToAdd) {
        initOverlayList();
        overlayList.add(overlayToAdd);
    }

    private void initOverlayList() {
        if (overlayList == null) {
            overlayList = new ArrayList<>();
            if (mDisplayRenderer != null)
                mDisplayRenderer.setOverlayList(overlayList);
        }
    }

    public Overlay addAnimatedOverlayFilter(List<Drawable> images, int videoWidth, int videoHeight) {
        Overlay overlayToAdd = new AnimatedOverlay(images, videoHeight, videoWidth);
        initOverlayList();
        overlayList.add(overlayToAdd);
        return overlayToAdd;
    }


    public void removeOverlayFilter(Overlay overlay) {

        overlayList = null;
        mDisplayRenderer.setOverlayList(null);
        //overlayList.remove(overlay);
    }

    public void addWatermark(Drawable overlayImage,
                             int positionX, int positionY, int width, int height, boolean preview) {
        this.watermark = new Watermark(overlayImage, height, width, positionX, positionY);
        if (preview && mDisplayRenderer != null)
            mDisplayRenderer.setWatermark(watermark);
    }

    public void addWatermark(Drawable overlayImage, boolean preview) {
        int[] size = calculateDefaultWatermarkSize();
        int margin = calculateWatermarkDefaultPosition();
        //addWatermark(overlayImage, 15, 15, 265, 36, preview);
        addWatermark(overlayImage, margin, margin, size[0], size[1], preview);
    }

    private int[] calculateDefaultWatermarkSize() {
        int width = ( mSessionConfig.getVideoWidth() * 265 ) / 1280;
        int height = ( mSessionConfig.getVideoHeight() * 36 ) / 720;
        return new int[]{width, height};
    }

    private int calculateWatermarkDefaultPosition() {
        return ( mSessionConfig.getVideoWidth() * 15 ) / 1280;
    }

    public void removeWaterMark() {
        watermark = null;
        mDisplayRenderer.removeWatermark();
    }

    Sticker addSitcker(Drawable image, int x, int y, int width, int height) {
        initOverlayList();
        Sticker sticker = new Sticker(image, height, width, x, y);
        overlayList.add(sticker);
        return sticker;
    }

    void registerStickerAnimator(StickerAnimator animator) {
        animatorController.register(animator);
    }

    void removeStickerAnimator(StickerAnimator animator){
        animatorController.remove(animator);
    }

    /**
     * Notify the preview and encoder programs
     * of a touch event. Used by
     * GLCameraEncoderView
     *
     * @param ev onTouchEvent event to handle
     */
    public void handleCameraPreviewTouchEvent(MotionEvent ev) {
        if (mFullScreen != null) mFullScreen.handleTouchEvent(ev);
        if (mDisplayRenderer != null) mDisplayRenderer.handleTouchEvent(ev);
    }

    /**
     * Called from UI thread
     *
     * @return is the CameraEncoder in the recording state
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRecording;
        }
    }

    /**
     * Called from UI thread
     */
    public void startRecording() {
        if (mState != STATE.INITIALIZED) {
            Log.e(TAG, "startRecording called in invalid state. Ignoring");
            return;
        }
        synchronized (mReadyForFrameFence) {
            mFrameNum = 0;
            mRecording = true;
            mState = STATE.RECORDING;
        }
    }

    private void startEncodingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "CameraEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    /**
     * Stop recording. After this call you must call either {@link #release()} to release resources if you're not going to
     * make any subsequent recordings, or {@link #reset(SessionConfig)} to prepare
     * the encoder for the next recording
     * <p/>
     * Called from UI thread
     */
    public void stopRecording() {
        if (mState != STATE.RECORDING)
            throw new IllegalArgumentException("StopRecording called in invalid state");
        Log.i(TAG, "stopRecording Video");
        mState = STATE.STOPPING;
        synchronized (mReadyForFrameFence) {
            Log.i(TAG, "stopRecording Video");
            mEosRequested = true;
        }
    }

    /**
     * Release resources, including the Camera.
     * After this call this instance of CameraEncoder is no longer usable.
     * This call blocks until release is complete.
     * <p/>
     * Called from UI thread
     */
    public void release() {
        if (mState == STATE.STOPPING) {
            Log.i(TAG, "Release called while stopping. Trying to sync");
            synchronized (mStopFence) {
                while (mState != STATE.UNINITIALIZED) {
                    Log.i(TAG, "Release called while stopping. Waiting for uninit'd state. Current state: " + mState);
                    try {
                        mStopFence.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.i(TAG, "Stopped. Proceeding to release");
        } else if (mState == STATE.INITIALIZED) {
            releaseEncoder();
            mState = STATE.UNINITIALIZED;
        }
        if (mState != STATE.UNINITIALIZED) {
            Log.i(TAG, "release called in invalid state " + mState);
            throw new IllegalArgumentException("release called in invalid state");
        }
        mState = STATE.RELEASING;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE));
    }

    private void handleRelease() {
        if (mState != STATE.RELEASING)
            throw new IllegalArgumentException("handleRelease called in invalid state");
        Log.i(TAG, "handleRelease");
        shutdown();
        mState = STATE.RELEASED;
    }

    /**
     * Called by release()
     * Safe to release resources
     * <p/>
     * Called on Encoder thread
     */
    private void shutdown() {
        releaseEglResources();
        releaseCamera();
        Looper.myLooper().quit();
    }

    /**
     * Called on an "arbitrary thread"
     *
     * @param surfaceTexture the SurfaceTexture initiating the call
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // Pass SurfaceTexture to Encoding thread via Handler
        // Then Encode and display frame
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, surfaceTexture));
    }

    /**
     * Called on Encoder thread
     *
     * @param surfaceTexture the SurfaceTexure that initiated the call to onFrameAvailable
     */
    private void handleFrameAvailable(SurfaceTexture surfaceTexture) {
        if (TRACE) Trace.beginSection("handleFrameAvail");
        synchronized (mReadyForFrameFence) {
            if (!mReadyForFrames) {
                if (VERBOSE) Log.i(TAG, "Ignoring available frame, not ready");
                return;
            }
            mFrameNum++;
            animatorController.updateStickers();
            // Too much info on screen if (VERBOSE && (mFrameNum % 30 == 0)) Log.i(TAG, "handleFrameAvailable");
            if (!surfaceTexture.equals(mSurfaceTexture))
                Log.w(TAG, "SurfaceTexture from OnFrameAvailable does not match saved SurfaceTexture!");

            if (mRecording) {
                mInputWindowSurface.makeCurrent();
                if (TRACE) Trace.beginSection("drainVEncoder");
                mVideoEncoder.drainEncoder(false);
                if (TRACE) Trace.endSection();
                if (mCurrentFilter != mNewFilter) {
                    Filters.updateFilter(mFullScreen, mNewFilter);
                    mCurrentFilter = mNewFilter;
                    mIncomingSizeUpdated = true;
                }

                if (mIncomingSizeUpdated) {
                    mFullScreen.getProgram().setTexSize(mSessionConfig.getVideoWidth(), mSessionConfig.getVideoHeight());
                    mIncomingSizeUpdated = false;
                }

                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                surfaceTexture.getTransformMatrix(mTransform);
                if (TRACE) Trace.beginSection("drawVEncoderFrame");
                GLES20.glViewport(0, 0, mSessionConfig.getVideoWidth(), mSessionConfig.getVideoHeight());
                mFullScreen.drawFrame(mTextureId, mTransform);
                drawOverlayList();
                if (watermark != null) {
                    if (!watermark.isInitialized())
                        watermark.initProgram();
                    watermark.draw(mFrameNum);
                }
                if (TRACE) Trace.endSection();
                if (!mEncodedFirstFrame) {
                    mEncodedFirstFrame = true;
                }

                if (mThumbnailRequestedOnFrame == mFrameNum) {
                    mThumbnailRequested = true;
                }
                if (mThumbnailRequested) {
                    saveFrameAsImage();
                    mThumbnailRequested = false;
                }

                mInputWindowSurface.setPresentationTime(mSurfaceTexture.getTimestamp());
                mInputWindowSurface.swapBuffers();

                if (mEosRequested) {
                    /*if (VERBOSE) */
                    Log.d(TAG, "Stop: Sending last video frame. Draining encoder");
                    mVideoEncoder.signalEndOfStream();
                    mVideoEncoder.drainEncoder(true);
                    mRecording = false;
                    mEosRequested = false;
                    releaseEncoder();
                    mState = STATE.UNINITIALIZED;
                    synchronized (mStopFence) {
                        mStopFence.notify();
                    }
                }
            }
        }

        // Signal GLSurfaceView to render
        mDisplayView.requestRender();

        if (TRACE) Trace.endSection();
    }

    private void drawOverlayList() {
        if (overlayList != null && overlayList.size() > 0) {
            GLES20.glEnable(GLES20.GL_BLEND);
            for (Overlay overlay : overlayList) {
                if (!overlay.isInitialized())
                    overlay.initProgram();
                overlay.draw(mFrameNum);
            }
        }
    }

    private void saveFrameAsImage() {
        try {
            File recordingDir = new File(mSessionConfig.getMuxer().getOutputPath()).getParentFile();
            File imageFile = new File(recordingDir, String.format("%d.jpg", System.currentTimeMillis()));
            mInputWindowSurface.saveFrame(imageFile, mThumbnailScaleFactor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hook for Host Activity's onPause()
     * Called on UI thread
     */
    public void onHostActivityPaused() {
        Log.i(TAG, "onHostActivityPaused");
        synchronized (mReadyForFrameFence) {
            // Pause the GLSurfaceView's Renderer thread
            if (mDisplayView != null)
                mDisplayView.onPause();
            // Release camera if we're not recording
            //if (!mRecording && mSurfaceTexture != null) {
            if (VERBOSE)
                Log.i("CameraRelease", "Releasing camera");
            if (mDisplayView != null)
                releaseDisplayView();
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_CAMERA));
            // }
        }
    }

//    private void configTexWatermark() {
//        int watermarkSize[] = calculateDefaultWatermarkSize();
//        mFullScreenOverlay.getProgram().setTexSize(watermarkSize[0], watermarkSize[1]);
//    }
//
//    private int[] calculateDefaultWatermarkSize() {
//        int width = (mSessionConfig.getVideoWidth()*265)/1280;
//        int height = (mSessionConfig.getVideoHeight()*36)/720;
//        return new int[] {width, height};
//    }
//
//    private void configViewportWatermark() {
//        int watermarkSize[] = calculateDefaultWatermarkSize();
//        int watermarkPosition = calculateWatermarkDefaultPosition();
//        GLES20.glViewport(watermarkPosition, watermarkPosition, watermarkSize[0], watermarkSize[1]);
//        mFullScreenOverlay.drawFrameWatermark(mOverlayTextureId);
//    }

    /**
     * Hook for Host Activity's onResume()
     * Called on UI thread
     */
    public void onHostActivityResumed() {
        synchronized (mReadyForFrameFence) {
            // Resume the GLSurfaceView's Renderer thread
            if (mDisplayView != null)
                mDisplayView.onResume();
            // Re-open camera if we're not recording and the SurfaceTexture has already been created
            if (!mRecording && mSurfaceTexture != null) {
                if (VERBOSE)
                    Log.i("CameraRelease", "Opening camera and attaching to SurfaceTexture");
                mHandler.sendMessage(mHandler.obtainMessage(MSG_REOPEN_CAMERA));
            } else {
                Log.w("CameraRelease", "Didn't try to open camera onHAResume. rec: " + mRecording + " mSurfaceTexture ready? " + ( mSurfaceTexture == null ? " no" : " yes" ));
            }
        }

    }

    /**
     * The GLSurfaceView.Renderer calls here after creating a
     * new texture in the display rendering context. Use the
     * created textureId to create a SurfaceTexture for
     * connection to the camera
     * <p/>
     * Called on the GlSurfaceView.Renderer thread
     *
     * @param textureId the id of the texture bound to the new display surface
     */
    public void onSurfaceCreated(int textureId) {
        if (VERBOSE) Log.i(TAG, "onSurfaceCreated. Saving EGL State");
        synchronized (mReadyFence) {
            // The Host Activity lifecycle may go through a OnDestroy ... OnCreate ... OnSurfaceCreated ... OnPause ... OnStop...
            // on it's way out, so our real sense of bearing should come from whether the EncoderThread is running
            if (mReady) {
                mEglSaver.saveEGLState();
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_SURFACE_TEXTURE, textureId));
            }
        }
    }

    /**
     * Called on Encoder thread
     */
    private void handleSetSurfaceTexture(int textureId) throws IOException {
        synchronized (mSurfaceTextureFence) {
            if (mSurfaceTexture != null) {
                // We're hot-swapping the display EGLContext after
                // creating the initial SurfaceTexture for camera display
                mInputWindowSurface.makeCurrent();
                mSurfaceTexture.detachFromGLContext();
                // Release the EGLSurface and EGLContext.
                mInputWindowSurface.releaseEglSurface();
                mFullScreen.release();
                mEglCore.release();

                // Create a new EGLContext and recreate the window surface.
                mEglCore = new EglCore(mEglSaver.getSavedEGLContext(), EglCore.FLAG_RECORDABLE);
                mInputWindowSurface.recreate(mEglCore);
                mInputWindowSurface.makeCurrent();

                // Create new programs and such for the new context.
                mTextureId = textureId;
                mFullScreen = new FullFrameRect(
                        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
                mFullScreen.getProgram().setTexSize(mSessionConfig.getVideoWidth(),
                        mSessionConfig.getVideoHeight());
//                mFullScreenOverlay = new FullFrameRect(
//                        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));
//                mFullScreenOverlay.getProgram().setTexSize(mSessionConfig.getVideoWidth(),
//                        mSessionConfig.getVideoHeight());
                mIncomingSizeUpdated = true;
                mSurfaceTexture.attachToGLContext(mTextureId);
                //mEglSaver.makeNothingCurrent();
            } else {
                // We're setting up the initial SurfaceTexture
                prepareEncoder(mEglSaver.getSavedEGLContext(),
                        mSessionConfig.getVideoWidth(),
                        mSessionConfig.getVideoHeight(),
                        mSessionConfig.getVideoBitrate(),
                        mSessionConfig.getMuxer());
                mTextureId = textureId;
                mSurfaceTexture = new SurfaceTexture(mTextureId);

//                mFullScreenOverlay = new FullFrameRect(
//                        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));
//                mFullScreenOverlay.getProgram().setTexSize(mSessionConfig.getVideoWidth(),
//                        mSessionConfig.getVideoHeight());

                if (VERBOSE)
                    Log.i(TAG + "-SurfText", " SurfaceTexture created. " +
                            "pre setOnFrameAvailableListener");
                mSurfaceTexture.setOnFrameAvailableListener(this);
                openAndAttachCameraToSurfaceTexture();
                mReadyForFrames = true;
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        if (VERBOSE) Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
            mReadyFence.notify();
        }
    }

    /**
     * Called with the display EGLContext current, on Encoder thread
     *
     * @param sharedContext The display EGLContext to be shared with the Encoder Surface's context.
     * @param width         the desired width of the encoder's video output
     * @param height        the desired height of the encoder's video output
     * @param bitRate       the desired bitrate of the video encoder
     * @param muxer         the desired output muxer
     */
    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                Muxer muxer) throws IOException {
        mVideoEncoder = new VideoEncoderCore(width, height, bitRate, muxer);
        if (mEglCore == null) {
            // This is the first prepare called for this CameraEncoder instance
            mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        }
        if (mInputWindowSurface != null) mInputWindowSurface.release();
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface());
        mInputWindowSurface.makeCurrent();

        if (mFullScreen != null) mFullScreen.release();
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mFullScreen.getProgram().setTexSize(width, height);
        mIncomingSizeUpdated = true;
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
    }

    /**
     * Release all recording-specific resources.
     * The Encoder, EGLCore and FullFrameRect are tied to capture resolution,
     * and other parameters.
     */
    private void releaseEglResources() {
        mReadyForFrames = false;
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release();
            mFullScreen = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }

        mSurfaceTexture = null;
    }

    private void openAndAttachCameraToSurfaceTexture() {
        openCamera(mSessionConfig.getVideoWidth(), mSessionConfig.getVideoHeight(), mDesiredCamera);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();

            mStateChangeCamera = STATE.INITIALIZED;
            synchronized (mChangeCameraFence) {
                mChangeCameraFence.notify();
            }


            if (VERBOSE)
                Log.i("CameraRelease", "Opened / Started Camera preview. mDisplayView ready? " + ( mDisplayView == null ? " no" : " yes" ));
            if (mDisplayView != null) {
                configureDisplayView();
            } else {

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     */
    private void openCamera(int desiredWidth, int desiredHeight, int requestedCameraType) {
        // There's a confusing conflation of Camera index in Camera.open(i)
        // with Camera.getCameraInfo().facing values. However the API specifies that Camera.open(0)
        // will always be a rear-facing camera, and CAMERA_FACING_BACK = 0.
        if (mCamera != null) {
            throw new RuntimeException("c" +
                    "amera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        int targetCameraType = requestedCameraType;
        boolean triedAllCameras = false;
        cameraLoop:
        while (!triedAllCameras) {
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == targetCameraType) {
                    mCamera = Camera.open(i);
                    mCurrentCamera = targetCameraType;
                    break cameraLoop;
                }
            }
            if (mCamera == null) {
                if (targetCameraType == requestedCameraType)
                    targetCameraType = ( requestedCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK );
                else
                    triedAllCameras = true;
            }

        }

        if (mCamera == null) {
            mCurrentCamera = -1;
            throw new RuntimeException("Unable to open camera");
        }

        initDisplayOrientation();

        Camera.Parameters parms = mCamera.getParameters();

        postCameraOpenedEvent(parms, cameraInfoOrientation);

        List<String> focusModes = parms.getSupportedFocusModes();

        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parms.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else {
            if (VERBOSE) Log.i(TAG, "Camera does not support autofocus");
        }

        List<String> flashModes = parms.getSupportedFlashModes();
        String flashMode = ( mDesiredFlash != null ) ? mDesiredFlash : mCurrentFlash;
        if (isValidFlashMode(flashModes, flashMode)) {
            parms.setFlashMode(flashMode);
        }

        parms.setRecordingHint(true);

        List<int[]> fpsRanges = parms.getSupportedPreviewFpsRange();
        int[] maxFpsRange = null;
        // Get the maximum supported fps not to exceed 30
        for (int x = fpsRanges.size() - 1; x >= 0; x--) {
            maxFpsRange = fpsRanges.get(x);
            if (maxFpsRange[1] / 1000.0 <= 30) break;
        }
        if (maxFpsRange != null) {
            parms.setPreviewFpsRange(maxFpsRange[0], maxFpsRange[1]);
        }

        choosePreviewSize(parms, desiredWidth, desiredHeight);
        // leave the frame rate set to default
        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += " @" + ( fpsRange[0] / 1000.0 ) + "fps";
        } else {
            previewFacts += " @" + ( fpsRange[0] / 1000.0 ) + " - " + ( fpsRange[1] / 1000.0 ) + "fps";
        }
        if (VERBOSE) Log.i(TAG, "Camera preview set: " + previewFacts);

    }

    public Camera getCamera() {
        return mCamera;
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mDisplayView != null)
            releaseDisplayView();
        if (mCamera != null) {
            if (VERBOSE) Log.d(TAG, "releasing camera");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Communicate camera-ready state to our display view.
     * This method allows us to handle custom subclasses
     */
    private void configureDisplayView() {
        if (mDisplayView instanceof GLCameraEncoderView)
            ( (GLCameraEncoderView) mDisplayView ).setCameraEncoder(this);
        else if (mDisplayView instanceof GLCameraView)
            ( (GLCameraView) mDisplayView ).setCamera(mCamera);
    }

    /**
     * Communicate camera-released state to our display view.
     */
    private void releaseDisplayView() {
        if (mDisplayView instanceof GLCameraEncoderView) {
            ( (GLCameraEncoderView) mDisplayView ).releaseCamera();
        } else if (mDisplayView instanceof GLCameraView)
            ( (GLCameraView) mDisplayView ).releaseCamera();
    }

    private void initDisplayOrientation() {
        //Sets the camera right Orientation.
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();

        android.hardware.Camera.getCameraInfo(mCurrentCamera, info);

        int degrees = 90;
        /*
        int rotation = mCurrentCameraRotation;
        switch (rotation) {
            // case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            // case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        */


        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = ( info.orientation + degrees ) % 360;
            result = ( 360 - result ) % 360;  // compensate the mirror
            Log.d(TAG, " front camera " + result);
        } else {  // back-facing
            result = ( info.orientation - degrees + 360 ) % 360;
            Log.d(TAG, " back camera " + result);
        }
        mCamera.setDisplayOrientation(result);
        cameraInfoOrientation = result;
    }

    public void updateRotationDisplay(int rotationView) {

        mCamera.setDisplayOrientation(getDisplayOrientation(rotationView));
        configureDisplayView();
    }

    private int getDisplayOrientation(int rotationView) {

        int displayOrientation = 0;
        if (rotationView == Surface.ROTATION_90) {
            if (cameraInfoOrientation == 0) {
                displayOrientation = 0;
            }
            if (cameraInfoOrientation == 180) {
                displayOrientation = 180;
            }
            Log.d(TAG, "setRotationView rotation 90, cameraOrientation " + cameraInfoOrientation);
        } else if (rotationView == Surface.ROTATION_270) {
            if (cameraInfoOrientation == 0) {
                displayOrientation = 180;
            }
            if (cameraInfoOrientation == 180) {
                displayOrientation = 0;
            }
            Log.d(TAG, "setRotationView rotation 270, cameraOrientation " + cameraInfoOrientation);
        } else if (rotationView == Surface.ROTATION_0) {
            if (cameraInfoOrientation == 90) {
                displayOrientation = 90;
            }
            if (cameraInfoOrientation == 270) {
                displayOrientation = 270;
            }
            Log.d(TAG, "setRotationView rotation 0, cameraOrientation " + cameraInfoOrientation);
        } else if (rotationView == Surface.ROTATION_180) {
            if (cameraInfoOrientation == 90) {
                displayOrientation = 270;
            }
            if (cameraInfoOrientation == 270) {
                displayOrientation = 90;
            }
            Log.d(TAG, "setRotationView rotation 180, cameraOrientation " + cameraInfoOrientation);
        }

        return displayOrientation;
    }

    public int getCurrentCamera() {
        return mCurrentCamera;
    }

    public int getDesiredCamera() {
        return mDesiredCamera;
    }

    /**
     * Toggle the flash mode between TORCH and OFF.
     * This will take effect immediately or as soon
     * as the camera preview becomes active.
     * <p/>
     * Called from UI thread
     *
     * @return whether the flash is on or off
     */
    public boolean toggleFlashMode() {
        String otherFlashMode = "";
        boolean on;
        if (mCurrentFlash.equals(Parameters.FLASH_MODE_TORCH)) {
            otherFlashMode = Parameters.FLASH_MODE_OFF;
            on = false;
        } else {
            otherFlashMode = Parameters.FLASH_MODE_TORCH;
            on = true;
        }
        requestFlash(otherFlashMode);
        return on;
    }

    public boolean setFlashOff() {
        String flashMode = "";
        boolean on = false;
        if (mCurrentFlash.equals(Parameters.FLASH_MODE_TORCH)) {
            flashMode = Parameters.FLASH_MODE_OFF;
            requestFlash(flashMode);
            on = false;
        }

        return on;
    }

    // Check flash support
    public int checkSupportFlash() {
        int supportFlash;

         /* If mCamera for some reason is null now flash mode will be applied
         * next time the camera opens through mDesiredFlash. */
        if (mCamera == null) {
            Log.w(TAG, "Ignoring requestFlash: Camera isn't available now.");
            return 2; // Ignoring case
        }
        Parameters params = mCamera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();

        if (VERBOSE) {
            Log.i(TAG, "checkSupportFlash Trying to set flash into camera: " + mCurrentCamera);
            Log.i(TAG, "checkSupportFlash Trying to set flash to: " + Parameters.FLASH_MODE_TORCH + " modes available: " + flashModes);
        }

        if (isValidFlashMode(flashModes, Parameters.FLASH_MODE_TORCH)) {
            supportFlash = 0; // true
        } else {
            supportFlash = 1; // false
        }

        Log.d(TAG, "checkSupportFlash supportFlash " + supportFlash);

        return supportFlash;
    }

    // Check auto focus
    public int checkSupportAutoFocus() {
        int supportAutoFocus;

         /* If mCamera for some reason is null now flash mode will be applied
         * next time the camera opens through mDesiredFlash. */
        if (mCamera == null) {
            Log.w(TAG, "Ignoring requestFlash: Camera isn't available now.");
            return 2; // Ignoring case
        }
        Parameters params = mCamera.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();

        if (VERBOSE) {
            Log.i(TAG, "checkSupportAutoFocus Trying to set flash into camera: " + mCurrentCamera);
            Log.i(TAG, "checkSupportAutoFocus Trying to set flash to: " + Parameters.FLASH_MODE_TORCH + " modes available: " + focusModes);
        }

        if (isValidFocusMode(focusModes, Parameters.FOCUS_MODE_AUTO)) {
            supportAutoFocus = 0; // true
        } else {
            supportAutoFocus = 1; // false
        }

        Log.d(TAG, "checkSupportAutoFocus supportAutoFocus " + supportAutoFocus);

        return supportAutoFocus;
    }

    /**
     * @param focusModes
     * @param focusMode
     * @return returns true if flashModes aren't null AND they contain the flashMode,
     * else returns false
     */
    private boolean isValidFocusMode(List<String> focusModes, String focusMode) {
        if (focusModes != null && focusModes.contains(focusMode)) {
            return true;
        }
        return false;
    }

    /**
     * Sets the requested flash mode and restarts the
     * camera preview. This will take effect immediately
     * or as soon as the camera preview becomes active.
     * <p/>
     * <p/>
     * Called from UI thread
     */
    public void requestFlash(String desiredFlash) {
        mDesiredFlash = desiredFlash;
        /* If mCamera for some reason is null now flash mode will be applied
         * next time the camera opens through mDesiredFlash. */
        if (mCamera == null) {
            Log.w(TAG, "Ignoring requestFlash: Camera isn't available now.");
            return;
        }
        Parameters params = mCamera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        /* If the device doesn't have a camera flash or
         * doesn't support our desired flash modes return */
        if (VERBOSE) {
            Log.i(TAG, "Trying to set flash into camera: " + mCurrentCamera);
            Log.i(TAG, "Trying to set flash to: " + mDesiredFlash + " modes available: " + flashModes);
        }

        if (isValidFlashMode(flashModes, mDesiredFlash) && mDesiredFlash != mCurrentFlash) {
            mCurrentFlash = mDesiredFlash;
            mDesiredFlash = null;
            try {
                params.setFlashMode(mCurrentFlash);
                mCamera.setParameters(params);
                if (VERBOSE) {
                    Log.i(TAG, "Changed flash successfully!");
                }
            } catch (RuntimeException e) {
                Log.d(TAG, "Unable to set flash" + e);
            }
        }
    }

    /**
     * @param flashModes
     * @param flashMode
     * @return returns true if flashModes aren't null AND they contain the flashMode,
     * else returns false
     */
    private boolean isValidFlashMode(List<String> flashModes, String flashMode) {
        if (flashModes != null && flashModes.contains(flashMode)) {
            return true;
        }
        return false;
    }

    /**
     * @return returns the flash mode set in the camera
     */
    public String getFlashMode() {
        return ( mDesiredFlash != null ) ? mDesiredFlash : mCurrentFlash;
    }

    private void postCameraOpenedEvent(Parameters params, int cameraInfoOrientation) {
        if (mEventBus != null) {
            mEventBus.post(new CameraOpenedEvent(params, cameraInfoOrientation));
        }
    }

    public void setEventBus(EventBus eventBus) {
        mEventBus = eventBus;
    }


    private enum STATE {
        /* Stopped or pre-construction */
        UNINITIALIZED,
        /* Construction-prompted initialization */
        INITIALIZING,
        /* Camera frames are being received */
        INITIALIZED,
        /* Camera frames are being sent to Encoder */
        RECORDING,
        /* Was recording, and is now stopping */
        STOPPING,
        /* Releasing resources. */
        RELEASING,
        /* This instance can no longer be used */
        RELEASED
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<CameraEncoder> mWeakEncoder;

        public EncoderHandler(CameraEncoder encoder) {
            mWeakEncoder = new WeakReference<>(encoder);
        }

        /**
         * Called on Encoder thread
         *
         * @param inputMessage
         */
        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            CameraEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            try {
                switch (what) {
                    case MSG_SET_SURFACE_TEXTURE:
                        encoder.handleSetSurfaceTexture((Integer) obj);
                        break;
                    case MSG_FRAME_AVAILABLE:
                        encoder.handleFrameAvailable((SurfaceTexture) obj);
                        break;
                    case MSG_REOPEN_CAMERA:
                        encoder.openAndAttachCameraToSurfaceTexture();
                        break;
                    case MSG_RELEASE_CAMERA:
                        encoder.releaseCamera();
                        break;
                    case MSG_RELEASE:
                        encoder.handleRelease();
                        break;
                    case MSG_RESET:
                        encoder.handleReset((SessionConfig) obj);
                        break;
                    default:
                        throw new RuntimeException("Unexpected msg what=" + what);
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to reset! Could be trouble creating MediaCodec encoder");
                e.printStackTrace();
            }
        }
    }
}
