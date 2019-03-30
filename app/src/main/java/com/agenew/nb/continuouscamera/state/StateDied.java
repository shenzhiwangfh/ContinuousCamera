package com.agenew.nb.continuouscamera.state;

import android.hardware.camera2.CameraCaptureSession;

import com.agenew.nb.continuouscamera.MyCameraManager;
import com.agenew.nb.continuouscamera.base.StateBase;

/**
 * 一个特例，我只想用这个类来描述camera 没有open或者died的状态
 */
public class StateDied extends StateBase {
    public StateDied(MyCameraManager mc) {
        super(mc);
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    protected void createSurfaces() {
    }

    @Override
    protected void addTarget() {

    }

    @Override
    protected CameraCaptureSession.StateCallback createStateCallback() {
        return null;
    }
}
