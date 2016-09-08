package com.score.chatz.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Lakmal on 8/28/16.
 */
public class AudioRecorder {

    private static final String TAG = AudioRecorder.class.getName();
    private final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private ByteArrayOutputStream recordedData;
    private int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private int BytesPerElement = 2; // 2 bytes in 16bit format

    public void startRecording(final Context context) {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                AudioUtils.RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile(context);
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    //convert short to byte
    private static byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    public void writeAudioDataToFile(Context context) {

        recordedData = new ByteArrayOutputStream();

        short sData[] = new short[BufferElements2Rec];

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                recordedData.write(bData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            recordedData.close();
            AudioUtils.play(recordedData.toByteArray(), context);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void stopRecording() {
        try {
            if (null != recorder) {
                isRecording = false;
                if(recorder.getState() != AudioRecord.STATE_UNINITIALIZED)
                recorder.stop();
                recorder.release();
                recorder = null;
                recordingThread = null;
            }
        }catch(Exception ex){
            Log.e(TAG, "Error in closing recodder " + ex);
        }
    }

    public ByteArrayOutputStream getRecording(){
        return recordedData;
    }


}
