package com.score.rahasak.utils;

public class Recorder {
    public native void nativeStart();

    public native int nativeStartRecord(short[] in);

    public native int nativeStartPlay(short[] out);

    static {
        System.loadLibrary("senz");
    }
}
