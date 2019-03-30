package com.agenew.nb.continuouscamera;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.agenew.nb.continuouscamera.base.TakePhotoFunc;
import com.agenew.nb.continuouscamera.commom.permission.PermissionHelper;
import com.agenew.nb.continuouscamera.commom.permission.PermissionInterface;
import com.agenew.nb.continuouscamera.view.CamSurfaceView;
import com.agenew.nb.continuouscamera.view.CamTextureView;
import com.agenew.nb.continuouscamera.view.IViewDecorator;

//user camera2 api
public class Main2Camera extends AppCompatActivity implements View.OnClickListener, PermissionInterface, CamSurfaceView.MyCallback, MyCameraManager.ModChange {

    private final static int REQUEST_CODE = 1;

    private final static String SAVE_PATH = Environment.getExternalStorageDirectory().getPath();

    private FloatingActionButton takePicture;
    private CamTextureView mSurfaceView;

    private IViewDecorator mViewDecorator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        mSurfaceView = (CamTextureView) findViewById(R.id.surfaceView2);
        takePicture = (FloatingActionButton) findViewById(R.id.take_picture);
        takePicture.setOnClickListener(this);

        initPermissions();

        mViewDecorator = new IViewDecorator(mSurfaceView);
        mViewDecorator.setCallback(this);
    }

    private void initPermissions() {
        PermissionHelper helper = new PermissionHelper(this, this);
        helper.requestPermissions();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.take_picture:
                String pictureName = "cc_" + System.currentTimeMillis() + ".png";

                MyCameraManager.me.getInstance().takePicture(SAVE_PATH, pictureName, new TakePhotoFunc() {
                    @Override
                    public void onPictureToken(String path) {
                        //MyToast.toastNew(getApplicationContext(), mView, "Saved: " + path);
                    }
                });
        }
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
    public void requestPermissionsSuccess() {

    }

    @Override
    public void requestPermissionsFail() {

    }

    @Override
    public void onChanged(String newMod) {

    }

    @Override
    public void pleaseStart() {
        MyCameraManager.me.getInstance().init(mSurfaceView.getContext(), mSurfaceView, mViewDecorator);
        MyCameraManager.me.getInstance().addModChanged(this);
        MyCameraManager.me.getInstance().openCamera();
    }

    @Override
    public void pleaseStop() {
        MyCameraManager.me.getInstance().closeCamera();
    }
}
