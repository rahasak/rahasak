package com.score.rahasak.utils;

public class Recorder {

    public native boolean nativeInit(int samplingRate, int numberOfChannels, String oSenz);

    public native boolean nativeStartCapture();

    public native boolean nativeStopCapture();

    public native void nativeStart();

    public native void nativeStartPlay();

    static {
        System.loadLibrary("senz");
    }

    public void init(int samplingRate, int numberOfChannels, String sender, String receiver) {
        String oSenz = SenzUtils.getOStreamMsg(sender, receiver);
        this.nativeInit(samplingRate, numberOfChannels, oSenz);
    }

    public void startRecord() {
        this.nativeStartCapture();
    }

    public void stop() {
        this.nativeStopCapture();
    }

    public void start() {
        nativeStart();
    }

    public void startPlay() {
        new Thread(new Runnable() {
            public void run() {
                nativeStartPlay();
            }
        }).start();
    }
}
