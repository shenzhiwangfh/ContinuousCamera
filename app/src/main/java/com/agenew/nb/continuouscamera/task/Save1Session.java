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

import com.agenew.nb.continuouscamera.R;
import com.agenew.nb.continuouscamera.commom.CamLog;
import com.agenew.nb.continuouscamera.view.AutoFitTextureView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private ImageListener mImageSaveListener;
    private int savedCount = 0;
    private int totalCount = 0;

    private ByteBuffer mByteBuffer;
    private byte[] bytes;
    private int width, height;

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

    public Save1Session(Context context, ImageListener listener) {
        mContext = context;
        mImageSaveListener = listener;

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

        width = mContext.getResources().getDimensionPixelSize(R.dimen.preview_width);
        height = mContext.getResources().getDimensionPixelSize(R.dimen.preview_height);

        mByteBuffer = ByteBuffer.allocate(4 * width * height); //2M
        bytes = new byte[4 * width * height];
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
    private long startCapTime = 0;

    public void startCapture(AutoFitTextureView textureView) {
        saving = true;
        totalCount = 0;
        startCapTime = System.currentTimeMillis();

        mTextureView = textureView;
        Message message = Message.obtain();
        message.what = MSG_CAPTURE;
        //message.obj = image;
        handler.sendMessage(message);
    }

    public void stopCapture() {
        saving = false;

        long consume = System.currentTimeMillis() - startCapTime;
        if (mImageSaveListener != null) mImageSaveListener.onCaptureCompleted(totalCount, consume);
    }

    public void startSave() {
        Message message = Message.obtain();
        message.what = MSG_SAVE;
        //message.obj = image;
        handler.sendMessage(message);
    }

    private void capture() {
        //CamLog.e(TAG, "MSG_SAVE++");
        Canvas canvas = mTextureView.lockCanvas();
        Bitmap bitmap = mTextureView.getBitmap();
        long currentTime = System.currentTimeMillis();

        //int byteSize = bitmap.getByteCount();
        //ByteBuffer buf = ByteBuffer.allocate(byteSize);
        mByteBuffer.rewind();
        bitmap.copyPixelsToBuffer(mByteBuffer);
        //byte[] byteArray = mByteBuffer.array();
        //CamLog.e(TAG, "byteArray.length=" + byteArray.length);

        try {
            //long currentTime = System.currentTimeMillis();
            //Date date = new Date(currentTime);
            //String name = "picture_" + formatter.format(date) + "_" + String.valueOf(currentTime % 1000);

            File out = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "raw_" + currentTime);
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(mByteBuffer.array());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mTextureView.unlockCanvasAndPost(canvas);
        totalCount++;
        //if(mImageSaveListener != null) mImageSaveListener.onCapture(totalCount);
        //CamLog.e(TAG, "MSG_SAVE--,");
    }

    private void save() {
        savedCount = 0;
        long startTime = System.currentTimeMillis();

        File root = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = root.listFiles();
        if (files == null || files.length <= 0) return;

        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!picturesDir.exists()) picturesDir.mkdirs();

        for (File file : files) {
            CamLog.e(TAG, "save+++");
            try {
                FileInputStream fis = new FileInputStream(file);
                //byte[] bytes = readStream(fis);
                //ByteBuffer buf = ByteBuffer.wrap(bytes);
                fis.read(bytes);
                fis.close();

                mByteBuffer.rewind();
                mByteBuffer.put(bytes);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mByteBuffer.rewind();
                bitmap.copyPixelsFromBuffer(mByteBuffer);

                long currentTime = System.currentTimeMillis();
                String name = "snapshot_" + currentTime + ".jpeg";

                File out = new File(picturesDir, name);
                CamLog.e(TAG, "getPath=" + out.getPath());
                saveBitmap(bitmap, out);
            } catch (Exception e) {
                e.printStackTrace();
                CamLog.e(TAG, "Exception," + e.toString());
            }
            CamLog.e(TAG, "save---");
        }

        clear(root);
        scan();

        long consume = System.currentTimeMillis() - startTime;
        if (mImageSaveListener != null) mImageSaveListener.onSaveCompleted(savedCount, consume);
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
        //byte[] buffer = new byte[in.available()];
        //int len = -1;
        //while ((len = in.read(bytes)) != -1) {
        //    outputStream.write(bytes, 0, len);
        //}

        in.read(bytes);
        outputStream.writeTo(outputStream);
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

    private void saveBitmap(Bitmap bm, File saveFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(saveFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();
        fos.close();

        savedCount++;
        if (mImageSaveListener != null) mImageSaveListener.onSave(savedCount, totalCount);
    }

    private void clear(File file) {
        if (!file.exists()) return;

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    clear(child);
                }
            } else {
                file.delete();
            }
        } else {
            file.delete();
        }
    }
}
