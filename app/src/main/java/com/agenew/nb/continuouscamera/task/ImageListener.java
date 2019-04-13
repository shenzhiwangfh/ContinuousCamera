package com.agenew.nb.continuouscamera.task;

public interface ImageListener {
    void onCapture(int count);
    void onCaptureCompleted(int total, long time);

    void onSave(int saved, int total);
    void onSaveCompleted(int total, long time);
}
