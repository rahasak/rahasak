package com.score.rahasak.utils;

public class Player {

    public native boolean nativeInit(int samplingRate, int numberOfChannels, String nSenz);

    public native boolean nativeStartPlayback();

    public native boolean nativeStopPlayback();

    static {
        System.loadLibrary("senz");
    }

    public void init(int samplingRate, int numberOfChannels, String sender, String receiver) {
        String nSenz = SenzUtils.getNStreamMsg(sender, receiver);
        this.nativeInit(samplingRate, numberOfChannels, nSenz);
    }

    public void startPlayback() {
        this.nativeStartPlayback();
    }

    public void stop() {
        this.nativeStopPlayback();
    }
}
