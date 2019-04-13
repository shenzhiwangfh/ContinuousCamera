package com.agenew.nb.continuouscamera.task;

import android.content.Context;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.agenew.nb.continuouscamera.Main2Camera;

public class BackgroundHandler extends Handler {

    private String TAG;
    private Context context;
    private HandlerThread mBackgroundThread;

    public BackgroundHandler(Context context, String TAG) {
        this.context = context;
        this.TAG = TAG;
    }

    public void start() {
        mBackgroundThread = new HandlerThread(TAG);
        mBackgroundThread.start();
    }

    public void stop() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
