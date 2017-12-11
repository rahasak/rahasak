package com.score.rahasak.utils;

import android.util.Log;

public class AudioHandler {

    public native boolean nativeInit(int samplingRate, int numberOfChannels);

    public native boolean nativeStartCapture(String oSenz);

    public native boolean nativeStopCapture();

    private native boolean nativeStartPlayback(String nSenz);

    private native boolean nativeStopPlayback();

    static {
        System.loadLibrary("senz");
    }

    public void init(int samplingRate, int numberOfChannels) {
        this.nativeInit(samplingRate, numberOfChannels);
    }

    public void startRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Tag", "---start recording");
                String oSenz = SenzUtils.getOStreamMsg("eranga", "lakmal");
                nativeStartCapture(oSenz);
            }
        }).start();
    }

    public void startPlayback() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Tag", "---start playback");
                String nSenz = SenzUtils.getNStreamMsg("eranga", "lakmal");
                nativeStartPlayback(nSenz);
            }
        }).start();
    }

    public void stop() {
        this.nativeStopCapture();
        this.nativeStopPlayback();
    }
}
