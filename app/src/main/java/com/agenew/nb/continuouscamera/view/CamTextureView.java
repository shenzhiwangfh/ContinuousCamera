package com.agenew.nb.continuouscamera.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import com.agenew.nb.continuouscamera.commom.CamLog;

public class CamTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private final static String TAG = "CamTextureView";

    private CamSurfaceView.MyCallback mCallback = null;

    public void setCallback(CamSurfaceView.MyCallback mCallback) {
        this.mCallback = mCallback;
    }

    private boolean mIsCreated = false;

    public boolean getIsCreated() {
        return mIsCreated;
    }

    public CamTextureView(Context context) {
        super(context);
        init();
    }

    public CamTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CamTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        CamLog.d(TAG, "SurfaceCreated");
        mIsCreated = true;
        if (mCallback != null) mCallback.pleaseStart();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        CamLog.d(TAG, "SurfaceChanged");
        mIsCreated = true;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        CamLog.d(TAG, "surfaceDestroyed");
        mIsCreated = false;
        if (mCallback != null) mCallback.pleaseStop();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
