package com.agenew.nb.continuouscamera.task;

import android.content.Context;
import android.media.Image;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.agenew.nb.continuouscamera.commom.CamLog;

public class ImageSaverTask implements Runnable {

    private final static String TAG = "ImageSaverTask";

    private final Context context;
    private final Image image;
    private ImageSaveListener listener;

    private SimpleDateFormat formatter;

    public ImageSaverTask(Context context, Image image, ImageSaveListener listener) {
        this.context = context;
        this.image = image;
        this.listener = listener;

        formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
    }

    @Override
    public void run() {
        CamLog.i(TAG, "ImageSaverTask++");

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            long currentTime = System.currentTimeMillis();
            Date date = new Date(currentTime);
            File out = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "picture_" + formatter.format(date) + ".JPEG");
            CamLog.i(TAG, out.getPath());

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

        if(listener != null) listener.onSaveCompleted();
        CamLog.i(TAG, "ImageSaverTask--");
    }
}