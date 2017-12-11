package com.score.rahasak.utils;

import android.util.Log;

public class AudioHandler {

    public native boolean nativeInit(int samplingRate, int numberOfChannels, String oSenz, String nSenz);

    public native boolean nativeStartCapture();

    public native boolean nativeStopCapture();

    private native boolean nativeStartPlayback();

    private native boolean nativeStopPlayback();

    static {
        System.loadLibrary("senz");
    }

    public void init(int samplingRate, int numberOfChannels, String sender, String receiver) {
        String oSenz = SenzUtils.getOStreamMsg(sender, receiver);
        String nSenz = SenzUtils.getNStreamMsg(sender, receiver);
        nativeInit(samplingRate, numberOfChannels, oSenz, nSenz);
    }

    public void startRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Tag", "--- start recording");
                nativeStartCapture();
            }
        }).start();
    }

    public void startPlayback() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Tag", "--- start playback");
                nativeStartPlayback();
            }
        }).start();
    }

    public void stop() {
        this.nativeStopCapture();
        this.nativeStopPlayback();
    }
}
