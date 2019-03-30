package com.agenew.nb.continuouscamera;

import android.Manifest;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.agenew.nb.continuouscamera.commom.permission.PermissionHelper;
import com.agenew.nb.continuouscamera.commom.permission.PermissionInterface;

//user camera2 api
public class Main2Camera extends AppCompatActivity implements View.OnClickListener, PermissionInterface {

    private final static int REQUEST_CODE = 1;

    private FloatingActionButton takePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        takePicture = (FloatingActionButton) findViewById(R.id.take_picture);
        takePicture.setOnClickListener(this);

        initPermissions();
    }

    private void initPermissions() {
        PermissionHelper helper = new PermissionHelper(this, this);
        helper.requestPermissions();
    }

    @Override
    public void onClick(View v) {

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
}
