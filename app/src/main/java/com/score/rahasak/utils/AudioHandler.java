package com.score.rahasak.utils;

import android.util.Log;

public class AudioHandler {

    public native boolean nativeInit(int samplingRate, int numberOfChannels, String recordFile);

    public native boolean nativeStartCapture();

    public native boolean nativeStopCapture();

    private native boolean nativeStartPlayback();

    private native boolean nativeStopPlayback();

    static {
        System.loadLibrary("senz");
    }

    public void init(int samplingRate, int numberOfChannels, String recordFile) {
        this.nativeInit(samplingRate, numberOfChannels, recordFile);
    }

    public void startRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Tag", "---start recording");
                nativeStartCapture();
            }
        }).start();
    }

    public void startPlayback() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Tag", "---start playback");
                nativeStartPlayback();
            }
        }).start();
    }

    public void stop() {
        this.nativeStopCapture();
        this.nativeStopPlayback();
    }
}
