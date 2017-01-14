package com.score.rahasak.utils;

import android.util.Log;

public class OpusDecoder {

    private native boolean nativeInitDecoder(int samplingRate, int numberOfChannels, int frameSize);

    private native int nativeDecodeBytes(byte[] in, short[] out);

    //private native boolean nativeReleaseDecoder();

    static {
        System.loadLibrary("app");
    }


    public void init(int sampleRate, int channels, int frameSize) {
        this.nativeInitDecoder(sampleRate, channels, frameSize);
    }

    public int decode(byte[] encodedBuffer, short[] buffer) {
        int decoded = this.nativeDecodeBytes(encodedBuffer, buffer);
        Log.d("DECODER", decoded + " +++++++++++++++");

        return decoded;
    }

//    public void close() {
//        this.nativeReleaseDecoder();
//    }

}
