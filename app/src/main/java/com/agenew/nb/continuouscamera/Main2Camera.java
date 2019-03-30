package com.agenew.nb.continuouscamera;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.agenew.nb.continuouscamera.base.TakePhotoFunc;
import com.agenew.nb.continuouscamera.commom.CamLog;
import com.agenew.nb.continuouscamera.commom.permission.PermissionHelper;
import com.agenew.nb.continuouscamera.commom.permission.PermissionInterface;
import com.agenew.nb.continuouscamera.view.CamSurfaceView;
import com.agenew.nb.continuouscamera.view.CamTextureView;
import com.agenew.nb.continuouscamera.view.IViewDecorator;

//user camera2 api
public class Main2Camera extends AppCompatActivity implements View.OnClickListener, PermissionInterface, CamSurfaceView.MyCallback, MyCameraManager.ModChange, TakePhotoFunc {

    private final static String TAG = "Main2Camera";

    private final static int REQUEST_CODE = 1;

    private final static String SAVE_PATH = Environment.getExternalStorageDirectory().getPath();

    private CamTextureView surfaceView;
    private TextView countView;
    private FloatingActionButton takePicture;
    private IViewDecorator mViewDecorator;

    private PermissionHelper helper;
    private MyCameraManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);


        surfaceView = (CamTextureView) findViewById(R.id.surfaceView2);
        countView = (TextView) findViewById(R.id.count);
        takePicture = (FloatingActionButton) findViewById(R.id.take_picture);
        takePicture.setOnClickListener(this);

        //mViewDecorator = new IViewDecorator(surfaceView);
        //mViewDecorator.setCallback(this);
        manager = MyCameraManager.me.getInstance();

        initPermissions();
        //mViewDecorator.setCallback(this);
    }

    /*
    @Override
    protected void onDestory() {
        super.onDestroy();
        manager.destroy();
    }
    */

    private void initPermissions() {
        helper = new PermissionHelper(this, this);
        helper.requestPermissions();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.take_picture:
                handler.sendEmptyMessage(MSG_TAKE_PICTURE);
        }
    }

    //permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        helper.requestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public int getPermissionsRequestCode() {
        return REQUEST_CODE;
    }

    @Override
    public String[] getPermissions() {
        return new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
        };
    }

    @Override
    public void permissionsResult(boolean success) {
        if(success) {
            CamLog.e(TAG, "setCallback,permissionsResult=" + success);
            //mViewDecorator.setCallback(this);
        } else {
            CamLog.e(TAG, "requestPermissions,permissionsResult=" + success);
            helper.requestPermissions();
        }
    }

    //camera
    @Override
    public void onChanged(String newMod) {
        CamLog.e(TAG, "newMod=" + newMod);
    }

    @Override
    public void pleaseStart() {
        CamLog.e(TAG, "pleaseStart");

        manager.init(surfaceView.getContext(), surfaceView, mViewDecorator);
        manager.addModChanged(this);
        manager.openCamera();

        countView.setText(count + "");
    }

    @Override
    public void pleaseStop() {
        CamLog.e(TAG, "pleaseStop");

        manager.closeCamera();
        //manager.destroy();
    }

    @Override
    public void onPictureToken(String path) {
        CamLog.e(TAG, "onPictureToken");

        handler.sendEmptyMessageDelayed(MSG_TAKE_PICTURE, 10);
    }

    private void takePicture() {
        if(count < 100) {
            String pictureName = "cc_" + System.currentTimeMillis() + ".png";
            manager.takePicture(SAVE_PATH, pictureName, this);
            count++;

            countView.setText(count + "");
        }
    }

    private int count = 0;

    private final static int MSG_TAKE_PICTURE = 1;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_TAKE_PICTURE:
                    takePicture();
                    break;
            }
            return false;
        }
    });
}
