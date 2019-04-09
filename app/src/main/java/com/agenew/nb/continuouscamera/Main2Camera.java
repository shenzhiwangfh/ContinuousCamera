package com.agenew.nb.continuouscamera;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.agenew.nb.continuouscamera.commom.CamLog;
import com.agenew.nb.continuouscamera.task.ImageSaveListener;
import com.agenew.nb.continuouscamera.task.SaveSession;
import com.agenew.nb.continuouscamera.view.AutoFitTextureView;

//user camera2 api
public class Main2Camera extends AppCompatActivity implements View.OnClickListener, AutoFitTextureView.Callback, ImageSaveListener {

    /**
     * Tag for the {@link CamLog}.
     */
    public static final String TAG = "Main2Camera";

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }
    };

    /**
     * A {@link SaveSession} for running tasks in the background.
     */
    //private BackgroundHandler mBackgroundHandler;
    private SaveSession mSaveSession;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //mBackgroundHandler.addTask(reader.acquireNextImage(), Main2Camera.this);

            //handler.sendEmptyMessage(MSG_TAKE_PICTURE);
            mSaveSession.addImage(reader.acquireLatestImage());
        }
    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
        }
    };


    private AutoFitTextureView mTextureView;
    private TextView mTime;
    private TextView mCount;
    private TextView mSaveSpeed;
    private TextView mCaptureSpeed;
    private TextView mInitMem;
    private TextView mMem;
    private TextView mLowMem;
    private FloatingActionButton mCapture1;
    private FloatingActionButton mCapture2;


    private ActivityManager activityManager;

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            CamLog.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main2);

        mTextureView = findViewById(R.id.surfaceView2);
        mTime = findViewById(R.id.time);
        mCount = findViewById(R.id.count);
        mSaveSpeed = findViewById(R.id.save_speed);
        mCaptureSpeed = findViewById(R.id.capture_speed);
        mInitMem = findViewById(R.id.initmem);
        mMem = findViewById(R.id.mem);
        mLowMem = findViewById(R.id.lowmem);

        mCapture1 = findViewById(R.id.capture1);
        //mCapture1.setOnClickListener(this);
        mCapture2 = findViewById(R.id.capture2);
        mCapture2.setOnClickListener(this);

        //mBackgroundHandler = new BackgroundHandler(this, TAG);
        mSaveSession = new SaveSession(this, this);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        //mBackgroundHandler.start();
        mSaveSession.start(TAG);


        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.addCallback(this);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        mSaveSession.stop();
        //mBackgroundHandler.stop();
        super.onPause();
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        CamLog.e(TAG, "width=" + width + ",height=" + height);

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                //List<Size> sizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
                //for (Size size : sizes) {
                //    Log.e(TAG, size.toString());
                //}

                /*
                01-05 21:14:47.071  8035  8035 E Camera2API: 2560x1920
                01-05 21:14:47.071  8035  8035 E Camera2API: 1920x1088
                01-05 21:14:47.071  8035  8035 E Camera2API: 1280x720
                01-05 21:14:47.071  8035  8035 E Camera2API: 640x480
                01-05 21:14:47.071  8035  8035 E Camera2API: 320x240
                 */

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                Size pictureSize = new Size(720, 1280);

                mImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/10);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mSaveSession.getHandler());

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        CamLog.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, pictureSize);
                //CamLog.e(TAG, "rotatedPreviewWidth=" + rotatedPreviewWidth + ",rotatedPreviewHeight=" + rotatedPreviewHeight);
                //CamLog.e(TAG, "maxPreviewWidth=" + maxPreviewWidth + ",maxPreviewHeight=" + maxPreviewHeight);
                //CamLog.e(TAG, "pictureSize=" + pictureSize.toString());
                //CamLog.e(TAG, "mPreviewSize=" + mPreviewSize.toString());
                mPreviewSize = new Size(pictureSize.getHeight(), pictureSize.getWidth());
                //CamLog.e(TAG, "mPreviewSize=" + mPreviewSize.toString());

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //
        }
    }

    /**
     * Opens the camera specified by {@link #mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mSaveSession.getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            startPreview();
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link # mCaptureCallback} from both {@link # lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    CamLog.e(TAG, "onCaptureCompleted");
                    //startPreview();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();

            //CamLog.e(TAG, "capture");
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void startPreview() {
        try {
            // Finally, we start displaying the camera preview.
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mPreviewCallback, mSaveSession.getHandler());
        } catch (CameraAccessException e) {

        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.capture1: {
                CamLog.e(TAG, "capture1");
                //captureStillPicture();

                if (isRunning) {
                    isRunning = false;
                } else {
                    isRunning = true;
                    mSaveSession.reset();
                    mCapture2.setEnabled(false);

                    startTime = System.currentTimeMillis();

                    ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
                    activityManager.getMemoryInfo(outInfo);
                    String availMem = fileSizeConver(outInfo.availMem);
                    String totalMem = fileSizeConver(outInfo.totalMem);
                    mInitMem.setText(getString(R.string.origin_memory, availMem, totalMem));

                    handler.sendEmptyMessage(MSG_TAKE_PICTURE1);
                    handler.sendEmptyMessage(MSG_SHOW_MESSAGE);
                }
                break;
            }
            case R.id.capture2: {
                CamLog.e(TAG, "capture2");
                //captureStillPicture();

                if (isRunning) {
                    isRunning = false;
                } else {
                    isRunning = true;
                    mSaveSession.reset();
                    mCapture2.setEnabled(false);

                    startTime = System.currentTimeMillis();

                    ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
                    activityManager.getMemoryInfo(outInfo);
                    String availMem = fileSizeConver(outInfo.availMem);
                    String totalMem = fileSizeConver(outInfo.totalMem);
                    mInitMem.setText(getString(R.string.origin_memory, availMem, totalMem));

                    handler.sendEmptyMessage(MSG_TAKE_PICTURE2);
                    handler.sendEmptyMessage(MSG_SHOW_MESSAGE);
                }
                break;
            }
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    public void onSaveCompleted(int saved, int total) {
        CamLog.i(TAG, "onSaveCompleted," + saved + "/" + total);

        /*
        Message message = Message.obtain();
        message.what = MSG_SHOW_MESSAGE;
        message.arg1 = saved;
        message.arg2 = total;
        handler.sendMessage(message);
        */
        //savedCount = saved;
        //totalCount = total;
    }

    @Override
    public void onAvailable(int width, int height) {
        openCamera(width, height);
    }

    @Override
    public void onSizeChanged(int width, int height) {
        configureTransform(width, height);
    }

    private boolean isRunning = false;
    private final static int MSG_TAKE_PICTURE1 = 1;
    private final static int MSG_TAKE_PICTURE2 = 2;
    private final static int MSG_SHOW_MESSAGE = 0;
    private int CAPTURE_INTERVAL_TIME = 30; //
    private int SHOW_INTERVAL_TIME = 1000;
    private int savedCount, captureCount;

    private long startTime;
    private final static long MAX_TIME = 1 * 10 * 1000;

    //private void sendNext(int msg, intCAPTURE interval) {
    //
    //}

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TAKE_PICTURE1: {
                    if (!isRunning) break;
                    CamLog.i(TAG, "MSG_TAKE_PICTURE");

                    long start = System.currentTimeMillis();
                    //CamLog.i(TAG, "start");

                    mSaveSession.updateCaptureCount();
                    //captureStillPicture();

                    long end = System.currentTimeMillis();
                    long next = 0;
                    if ((end - start) < CAPTURE_INTERVAL_TIME)
                        next = CAPTURE_INTERVAL_TIME - (end - start);
                    handler.sendEmptyMessageDelayed(MSG_TAKE_PICTURE1, next);

                    //CamLog.i(TAG, "next=" + next);
                }
                case MSG_TAKE_PICTURE2: {
                    if (!isRunning) break;
                    CamLog.i(TAG, "MSG_TAKE_PICTURE");

                    long start = System.currentTimeMillis();
                    //CamLog.i(TAG, "start");

                    mSaveSession.updateCaptureCount();
                    captureStillPicture();

                    long end = System.currentTimeMillis();
                    long next = 0;
                    if ((end - start) < CAPTURE_INTERVAL_TIME)
                        next = CAPTURE_INTERVAL_TIME - (end - start);
                    handler.sendEmptyMessageDelayed(MSG_TAKE_PICTURE2, next);

                    //CamLog.i(TAG, "next=" + next);
                }
                break;
                case MSG_SHOW_MESSAGE: {
                    //if (!isRunning) break;
                    CamLog.i(TAG, "MSG_SHOW_MESSAGE");

                    long start = System.currentTimeMillis();

                    long time = (start - startTime) / 1000;
                    int hh = (int) time / 3600;
                    int min = (int) time / 60;
                    int sec = (int) time % 60;
                    mTime.setText(getString(R.string.total_time, hh, min, sec));

                    int saved = mSaveSession.getSavedCount();
                    int captured = mSaveSession.getCaptureCount();
                    mCount.setText(getString(R.string.capture_number, saved, captured));

                    int savedSpeed = saved - savedCount;
                    int captureSpeed = captured - captureCount;
                    mSaveSpeed.setText(getString(R.string.save_speed, savedSpeed));
                    mCaptureSpeed.setText(getString(R.string.capture_speed, captureSpeed));

                    ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
                    activityManager.getMemoryInfo(outInfo);
                    String availMem = fileSizeConver(outInfo.availMem);
                    String totalMem = fileSizeConver(outInfo.totalMem);
                    mMem.setText(getString(R.string.now_memory, availMem, totalMem));
                    mLowMem.setText(getString(R.string.low_memory, String.valueOf(outInfo.lowMemory)));

                    long end = System.currentTimeMillis();
                    long next = 0;
                    if ((end - start) < SHOW_INTERVAL_TIME)
                        next = SHOW_INTERVAL_TIME - (end - start);

                    savedCount = saved;
                    captureCount = captured;

                    if ((end - startTime) > MAX_TIME) { //
                        isRunning = false;
                    }
                    handler.sendEmptyMessageDelayed(MSG_SHOW_MESSAGE, next);
                }
                break;
            }
            return true;
        }
    });

    private String fileSizeConver(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    private void pickSurfaceImage() {

    }
}
