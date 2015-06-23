package com.example.root.kickflip.sdk.presentation.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.av.CameraEncoder;

/**
 * Special GLSurfaceView for use with CameraEncoder
 * The tight coupling here allows richer touch interaction
 */
public class GLCameraEncoderView extends GLCameraView {
    private static final String TAG = "GLCameraEncoderView";

    protected CameraEncoder mCameraEncoder;

    //AutoFocus
    private Paint paint;
    private Bitmap bitmap;
    private Canvas canvas;
    public static boolean showDraw = false;

    float x = 0;
    float y = 0;

    public GLCameraEncoderView(Context context) {
        super(context);

        // AutoFocusView
        setFocusable(true);

        Log.d("Focus", "init context");

        paint = new Paint();
        paint.setColor(0xeed7d7d7);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);

        bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.activity_record_icon_focus_focused);

        canvas = new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888, true));


    }

    public GLCameraEncoderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCameraEncoder(CameraEncoder encoder){
        mCameraEncoder = encoder;
        setCamera(mCameraEncoder.getCamera());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d("Focus", "onTouchEvent");

        if(mScaleGestureDetector != null){
            mScaleGestureDetector.onTouchEvent(ev);
            onTouchEvent2(ev);
        }
        if(mCameraEncoder != null && ev.getPointerCount() == 1 && (ev.getAction() == MotionEvent.ACTION_MOVE)){
            mCameraEncoder.handleCameraPreviewTouchEvent(ev);
        }else if(mCameraEncoder != null && ev.getPointerCount() == 1 && (ev.getAction() == MotionEvent.ACTION_DOWN)){
            mCameraEncoder.handleCameraPreviewTouchEvent(ev);
        }
        return true;
      //  return false;
    }


    public boolean onTouchEvent2(MotionEvent event) {

        Log.d("Focus", "onTouchEvent2");

        setFocusable(true);

        paint = new Paint();
        paint.setColor(0xeed7d7d7);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);

        bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.activity_record_icon_focus_focused);

        canvas = new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888, true));


        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            Log.d("Focus", "ACTION_DOWN");

            x = event.getX();
            y = event.getY();
            invalidate();

            showDraw = true;

            canvas.drawBitmap(bitmap, x - 50, y - 50, paint);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // invalidate();
                    showDraw = false;
                    invalidate();
                }
            }, 1000);

        }
        return false;
    }


    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        Log.d("Focus", " onDraw ");

        if (showDraw) {

            canvas.drawBitmap(bitmap, x - 50, y - 50, paint);

        }

    }

    public void onPreviewTouchEvent(Context context) {

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        x = size.x / 2;
        y = size.y / 3;

        Log.d("Focus", " x " + x + " y " + y);

        invalidate();

        showDraw = true;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // invalidate();
                showDraw = false;
                invalidate();

                Log.d("Focus", " postDelayed");

            }
        }, 2500);


    }

}
