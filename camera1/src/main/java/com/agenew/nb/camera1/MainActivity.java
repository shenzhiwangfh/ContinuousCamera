package com.agenew.nb.camera1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, Camera.PictureCallback {
    private SurfaceView mSurfaceView;
    private ImageView mIvStart;
    private TextView mTvCountDown;

    private SurfaceHolder mHolder;

    private Camera mCamera;

    private Handler mHandler = new Handler();

    private int mCurrentTimer = 10;

    private boolean mIsSurfaceCreated = false;
    private boolean mIsTimerRunning = false;

    private static final int CAMERA_ID = 0; //后置摄像头
    // private static final int CAMERA_ID = 1; //前置摄像头
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initEvent();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mIvStart = (ImageView) findViewById(R.id.start);
        mTvCountDown = (TextView) findViewById(R.id.count_down);
    }

    private void initEvent() {
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new MyCallback());

        mIvStart.setOnClickListener(this);
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                Log.e(TAG, "camera,start take picture");
                mCamera.takePicture(null, null, null, MainActivity.this);
                //mHandler2.sendEmptyMessage(101);
                break;
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            Log.e(TAG, "camera,onPictureTaken++");

            FileOutputStream fos = new FileOutputStream(new File
                    (Environment.getExternalStorageDirectory() + File.separator + System.currentTimeMillis() + ".png"));

            //旋转角度，保证保存的图片方向是对的
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.setRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.e(TAG, "camera,onPictureTaken--");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.startPreview();
    }

    /**
     * 播放系统拍照声音
     */
    public void playSound() {
        MediaPlayer mediaPlayer = null;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (mediaPlayer == null)
                mediaPlayer = MediaPlayer.create(this,
                        Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
        }
    }

    /*
    private void getViewImage() {
        //设置监听
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Size size = camera.getParameters().getPreviewSize();
                try {
                    YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                    if (image != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                        Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        //因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上
                        rotateMyBitmap(bmp);
                        stream.close();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error:" + ex.getMessage());
                }
            }
        });
    }

    public void rotateMyBitmap(Bitmap bmp) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1); // 镜像水平翻转(如果左右颠倒)
        matrix.postRotate(180);
        Bitmap nbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        //imageView.setImageBitmap(nbmp);
    }

    private Handler mHandler2 = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    getViewImage();
                    Log.e(TAG, "handleMessage: 拍照");
                    mHandler.sendEmptyMessageDelayed(102, 100);
                    break;
                case 102:
                    mCamera.setPreviewCallback(null);
                    break;
            }
        }
    };
    */

    class MyCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            //初始化相机
            mCamera = Camera.open();
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            if (holder.getSurface() == null) {
                // preview surface does not exist
                return;
            }
            mCamera.stopPreview();


            try {
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //可以将所有参数信息一行行显示，具体方法可以参见我的另外一篇博客：   http://www.cnblogs.com/bokeofzp/p/4743108.html
//      System.out.println(mCamera.getParameters().flatten());
            Camera.Parameters parms = mCamera.getParameters();
            parms.setPictureFormat(ImageFormat.JPEG);//设置图片的格式
            List<Camera.Size> mSupportedPreviewSizes = parms.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parms.getSupportedVideoSizes();
            Camera.Size optimalSize = getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes, height, width);

            parms.setPreviewSize(optimalSize.width, optimalSize.height); // 设置预览图像大小
            parms.setPictureSize(optimalSize.width, optimalSize.height);//设置照片的大小
//       Size size= parms.getPictureSize();
//       Size size2= parms.getPreviewSize();
//       System.out.println(size2.width);
            try {
                //一定要将属性值返回去，否则设置无效
                mCamera.setParameters(parms);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private Camera.Size getOptimalVideoSize(List<Camera.Size> supportedVideoSizes, List<Camera.Size> previewSizes, int w, int h) {
// Use a very small tolerance because we want an exact match.
            final double ASPECT_TOLERANCE = 0.1;
            double targetRatio = (double) w / h;

// Supported video sizes list might be null, it means that we are allowed to use the preview
// sizes
            List<Camera.Size> videoSizes;
            if (supportedVideoSizes != null) {
                videoSizes = supportedVideoSizes;
            } else {
                videoSizes = previewSizes;
            }
            Camera.Size optimalSize = null;

// Start with max value and refine as we iterate over available video sizes. This is the
// minimum difference between view and camera height.
            double minDiff = Double.MAX_VALUE;

// Target view height
            int targetHeight = h;

// Try to find a video size that matches aspect ratio and the target view size.
// Iterate over all available sizes and pick the largest size that can fit in the view and
// still maintain the aspect ratio.
            for (Camera.Size size : videoSizes) {
                double ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                    continue;
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }

// Cannot find video size that matches the aspect ratio, ignore the requirement
            if (optimalSize == null) {
                minDiff = Double.MAX_VALUE;
                for (Camera.Size size : videoSizes) {
                    if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }
            }
            return optimalSize;
        }
    }
}
