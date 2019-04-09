package com.agenew.nb.camera1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.Image;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaveTask {

    private final static String TAG = "SaveSession";

    private Context context;
    private List<YuvImage> list = new ArrayList<>();
    //private Image image;

    private Handler handler;
    private HandlerThread thread;
    private Callback callback;
    private final static int MSG_SAVE = 1;
    private boolean isRunning = false;

    private int captureCount = 0;
    private int savedCount = 0;

    private SimpleDateFormat formatter;

    private Camera.Size previewSize;

    class Callback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAVE:
                    YuvImage image = pickImage();
                    Log.i(TAG, "MSG_SAVE");

                    if(image != null) {
                        save(image);
                        removeImage();
                    }

                    //Image image = (Image) msg.obj;
                    //save(image);

                    if ((captureCount > savedCount) || isRunning) {
                        //handler.sendEmptyMessageDelayed(MSG_SAVE, 10);
                    }
            }
            return false;
        }
    }

    public SaveTask(Context context) {
        this.context = context;
        formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
    }

    public Handler getHandler() {
        return handler;
    }

    public void start(String TAG) {
        thread = new HandlerThread(TAG);
        thread.start();

        callback = new Callback();
        handler = new Handler(thread.getLooper(), callback);
    }

    public void stop() {
        thread.quitSafely();
        try {
            thread.join();
            thread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    public void resume() {
        if(isRunning) return;

        CamLog.i(TAG, "save session resume");
        isRunning = true;
        handler.sendEmptyMessage(MSG_SAVE);
    }

    public void pause() {
        CamLog.i(TAG, "save session pause");

        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
    */

    public void reset() {
        captureCount = savedCount = 0;
    }

    public void updateCaptureCount() {
        captureCount++;
    }

    public int getCaptureCount() {
        return captureCount;
    }

    public int getSavedCount() {
        return savedCount;
    }

    public synchronized void addImage(YuvImage image) {
        list.add(image);
        Message message = Message.obtain();
        message.what = MSG_SAVE;
        //message.obj = image;
        handler.sendMessage(message);
    }

    private synchronized YuvImage pickImage() {
        if (list.isEmpty()) return null;
        return list.get(0);
        //return null;
    }

    private synchronized void removeImage() {
        list.remove(0);
    }

    public void setSize(Camera.Size previewSize) {
        this.previewSize = previewSize;
    }

    public void save(YuvImage image) {
        Log.i(TAG, "save image");

        try {
            if (image != null) {
                //Log.e(TAG, "image:" + image.getHeight() + "x" + image.getWidth());

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 50, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                //因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上
                //rotateMyBitmap(bmp);
                stream.close();

                FileOutputStream fos = new FileOutputStream(new File
                        (Environment.getExternalStorageDirectory() + File.separator + System.currentTimeMillis() + ".JPEG"));
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error:" + ex.getMessage());
        }
        savedCount++;

        //if (listener != null) listener.onSaveCompleted(savedCount, captureCount);
    }

    public void rotateMyBitmap(Bitmap bmp) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1); // 镜像水平翻转(如果左右颠倒)
        matrix.postRotate(180);
        Bitmap nbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        //imageView.setImageBitmap(nbmp);
    }
}
