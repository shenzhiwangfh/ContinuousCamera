package com.agenew.nb.continuouscamera.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.agenew.nb.continuouscamera.commom.CamLog;
import com.agenew.nb.continuouscamera.view.AutoFitTextureView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Save1Session {
    private final static String TAG = "Save1Session";

    private Context mContext;

    private AutoFitTextureView mTextureView;

    private Handler handler;
    private HandlerThread thread;
    private final static int MSG_CAPTURE = 1;
    private final static int MSG_SAVE = 2;
    private Callback callback;

    private SimpleDateFormat formatter;

    class Callback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CAPTURE:
                    if (!saving) break;
                    capture();
                    handler.sendEmptyMessage(MSG_CAPTURE);
                    break;
                case MSG_SAVE:
                    save();
                    break;
            }
            return false;
        }
    }

    public Save1Session(Context context) {
        mContext = context;
        //this.listener = listener;

        formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
    }

    public Handler getHandler() {
        return handler;
    }

    public void start(String TAG) {
        thread = new HandlerThread(TAG);
        thread.start();

        callback = new Save1Session.Callback();
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

    private boolean saving = false;

    public void startCapture(AutoFitTextureView textureView) {
        saving = true;

        mTextureView = textureView;
        Message message = Message.obtain();
        message.what = MSG_CAPTURE;
        //message.obj = image;
        handler.sendMessage(message);
    }

    public void stopCapture() {
        saving = false;
    }

    public void startSave() {
        Message message = Message.obtain();
        message.what = MSG_SAVE;
        //message.obj = image;
        handler.sendMessage(message);
    }

    private void capture() {
        CamLog.e(TAG, "MSG_SAVE++");
        Canvas canvas = mTextureView.lockCanvas();

        Bitmap bitmap = mTextureView.getBitmap();
        int byteSize = bitmap.getByteCount();
        CamLog.e(TAG, "byteSize=" + byteSize);

        ByteBuffer buf = ByteBuffer.allocate(byteSize);
        bitmap.copyPixelsToBuffer(buf);

        byte[] byteArray = buf.array();
        CamLog.e(TAG, "byteArray.length=" + byteArray.length);

        try {
            long currentTime = System.currentTimeMillis();
            //Date date = new Date(currentTime);
            //String name = "picture_" + formatter.format(date) + "_" + String.valueOf(currentTime % 1000);

            File out = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), String.valueOf(currentTime));
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(byteArray);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mTextureView.unlockCanvasAndPost(canvas);
        CamLog.e(TAG, "MSG_SAVE--,");
    }

    private void save() {
        File root = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = root.listFiles();
        if (files == null || files.length <= 0) return;

        for (File file : files) {
            CamLog.e(TAG, "save+++");
            CamLog.e(TAG, "" + file.getPath());

            try {
                FileInputStream fis = new FileInputStream(file);
                //byte[] buf = readStream(fis);
                //Bitmap bitmap = bytes2bimap(buffer);

                byte[] buf = new byte[fis.available()];
                fis.read(buf);

                YuvImage yuvimage = new YuvImage(buf, ImageFormat.NV21, 720, 1280, null); //20、20分别是图的宽度与高度
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(0, 0, 720, 1280), 80, outputStream);//80--JPG图片的质量[0-100],100最高
                byte[] byteArray = outputStream.toByteArray();
                Bitmap bitmap = bytes2bimap(byteArray);

                File out = new File(Environment.getExternalStorageDirectory(), file.getName() + ".jpeg");
                FileOutputStream fos = new FileOutputStream(out);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                //if(bitmap != null) bitmap.isRecycled();
            } catch (Exception e) {
                e.printStackTrace();
                CamLog.e(TAG, "Exception," + e.toString());
            }
            CamLog.e(TAG, "save---");

            scan();
        }
    }

    private Bitmap bytes2bimap(byte[] byteArray) {
        if (byteArray.length != 0) {
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        } else {
            return null;
        }
    }

    private byte[] readStream(FileInputStream in) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[in.available()];
        int len = -1;
        while ((len = in.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        outputStream.close();
        in.close();
        return outputStream.toByteArray();
    }

    private void scan() {
        MediaScannerConnection.scanFile(mContext,
                new String[]{Environment.getExternalStorageDirectory().getPath()},
                new String[]{"image/jpeg"}, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

}
