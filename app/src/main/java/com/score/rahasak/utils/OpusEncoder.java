package com.score.rahasak.utils;

public class OpusEncoder {

    private native boolean nativeInitEncoder(int samplingRate, int numberOfChannels, int frameSize);

    private native int nativeEncodeBytes(short[] in, byte[] out);

    private native boolean nativeReleaseEncoder();

    static {
        System.loadLibrary("opus");
    }

    public void init(int sampleRate, int channels, int frameSize) {
        this.nativeInitEncoder(sampleRate, channels, frameSize);
    }

    public int encode(short[] buffer, byte[] out) {
        int encoded = this.nativeEncodeBytes(buffer, out);

        return encoded;
    }

    public void close() {
        this.nativeReleaseEncoder();
    }

}
