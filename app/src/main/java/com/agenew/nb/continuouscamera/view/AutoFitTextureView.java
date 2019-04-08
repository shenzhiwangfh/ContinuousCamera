package com.agenew.nb.continuouscamera.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        //Log.e(MainActivity.TAG, "setAspectRatio");

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.e(MainActivity.TAG, "onMeasure");

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        //Log.e(MainActivity.TAG, "00width=" + width + ",height=" + height);

        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
            //Log.e(MainActivity.TAG, "11width=" + width + ",height=" + height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                height = width * mRatioHeight / mRatioWidth;
                setMeasuredDimension(width, height);
                //Log.e(MainActivity.TAG, "22width=" + width + ",height=" + height);
            } else {
                width = height * mRatioWidth / mRatioHeight;
                setMeasuredDimension(width, height);
                //Log.e(MainActivity.TAG, "33width=" + width + ",height=" + height);
            }
        }
    }

    public interface Callback {
        void onAvailable(int width, int height);
        void onSizeChanged(int width, int height);
    }
    private Callback callback;

    public void addCallback(Callback callback) {
        this.callback = callback;
        this.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if(callback != null) callback.onAvailable(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if(callback != null) callback.onSizeChanged(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
