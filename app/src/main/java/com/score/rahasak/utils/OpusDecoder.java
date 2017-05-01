package com.score.rahasak.utils;

public class OpusDecoder {

    private native boolean nativeInitDecoder(int samplingRate, int numberOfChannels, int frameSize);

    private native int nativeDecodeBytes(byte[] in, short[] out);

    private native boolean nativeReleaseDecoder();

    static {
        System.loadLibrary("opus");
    }

    public void init(int sampleRate, int channels, int frameSize) {
        this.nativeInitDecoder(sampleRate, channels, frameSize);
    }

    public int decode(byte[] encodedBuffer, short[] buffer) {
        int decoded = this.nativeDecodeBytes(encodedBuffer, buffer);

        return decoded;
    }

    public void close() {
        this.nativeReleaseDecoder();
    }

}
