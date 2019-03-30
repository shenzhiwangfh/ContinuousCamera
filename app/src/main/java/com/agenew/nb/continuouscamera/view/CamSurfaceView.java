package com.agenew.nb.continuouscamera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.agenew.nb.continuouscamera.commom.CamLog;


public class CamSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final static String TAG = "CamSurfaceView";

    public interface MyCallback {
        void pleaseStart();

        void pleaseStop();
    }

    private MyCallback mCallback;

    public void setCallback(MyCallback mCallback) {
        this.mCallback = mCallback;
    }

    public CamSurfaceView(Context context) {
        super(context);
        init();
    }

    public CamSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CamSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CamLog.d(TAG, "SurfaceCreated");
        if (mCallback != null) mCallback.pleaseStart();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        CamLog.d(TAG, "SurfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CamLog.d(TAG, "surfaceDestroyed");
        if (mCallback != null) mCallback.pleaseStop();
    }

}
