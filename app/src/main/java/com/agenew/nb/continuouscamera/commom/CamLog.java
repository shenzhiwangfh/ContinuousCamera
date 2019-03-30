package com.agenew.nb.continuouscamera.commom;

import android.util.Log;

public class CamLog {
    private static final String TAG = "ContinuousCamera";

    /*
    public static void d(String s) {
        Log.i(TAG, s);
    }

    public static void e(String s) {
        Log.e(TAG, s);
    }
    */

    public static void d(String tag, String s) {
        Log.i(TAG, tag + ": " + s);
    }

    public static void e(String tag, String s) {
        Log.e(TAG, tag + ": " + s);
    }
}
