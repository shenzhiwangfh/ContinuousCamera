package com.agenew.nb.continuouscamera.task;

import android.content.Context;
import android.media.Image;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.agenew.nb.continuouscamera.commom.CamLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaveSession {

    private final static String TAG = "SaveSession";

    private Context context;
    private List<Image> list = new ArrayList<>();
    //private Image image;

    private Handler handler;
    private HandlerThread thread;
    private Callback callback;
    private final static int MSG_SAVE = 1;
    private boolean isRunning = false;

    private int captureCount = 0;
    private int savedCount = 0;
    private ImageListener listener;

    private SimpleDateFormat formatter;

    class Callback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAVE:
                    Image image = pickImage();
                    CamLog.i(TAG, "MSG_SAVE");

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

    public SaveSession(Context context, ImageListener listener) {
        this.context = context;
        this.listener = listener;

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

    public synchronized void addImage(Image image) {
        list.add(image);
        Message message = Message.obtain();
        message.what = MSG_SAVE;
        //message.obj = image;
        handler.sendMessage(message);
    }

    private synchronized Image pickImage() {
        if (list.isEmpty()) return null;
        return list.get(0);
        //return null;
    }

    private synchronized void removeImage() {
        list.remove(0);
    }

    public void save(Image image) {
        CamLog.i(TAG, "save image");

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            long currentTime = System.currentTimeMillis();
            Date date = new Date(currentTime);
            String name = "picture_" + formatter.format(date) + "_" + String.valueOf(currentTime % 1000) + ".JPEG";
            File out = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name);
            //CamLog.i(TAG, out.getPath());

            output = new FileOutputStream(out);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        savedCount++;

        if (listener != null) listener.onSaveCompleted(savedCount, captureCount);
    }
}
