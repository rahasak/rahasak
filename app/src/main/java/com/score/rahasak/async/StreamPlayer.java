package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.SenzBuffer;
import com.score.rahasak.utils.SenzParser;
import com.score.senzc.pojos.Senz;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class StreamPlayer {

    private Context context;
    private SenzBuffer senzBuffer;
    private Reader reader;
    private Player player;

    private DatagramSocket socket;

    public StreamPlayer(Context context, DatagramSocket socket) {
        this.context = context;
        this.socket = socket;

        senzBuffer = new SenzBuffer();
        reader = new Reader();
        player = new Player();
    }

    public void play() {
        reader.start();
        player.start();
    }

    public void stop() {
        reader.shutDown();
        player.shutDown();
    }

    private class Player extends Thread {
        private AudioTrack streamTrack;
        private int minBufSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        private boolean playing = true;

        @Override
        public void run() {
            if (playing) {
                startPlay();
                play();
            }
        }

        private void startPlay() {
            streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    AudioUtils.RECORDER_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize,
                    AudioTrack.MODE_STREAM);

            enableEarpiece();
            streamTrack.play();
        }

        private void play() {
            while (playing) {
                if (senzBuffer.size() > minBufSize) {
                    byte[] data = senzBuffer.get(0, minBufSize);

                    streamTrack.write(data, 0, data.length);
                }
            }
        }

        void shutDown() {
            playing = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                streamTrack.pause();
                streamTrack.flush();
            } else {
                streamTrack.stop();
            }
        }
    }

    private class Reader extends Thread {
        private AudioTrack streamTrack;
        private int minBufSize = AudioTrack.getMinBufferSize(AudioUtils.RECORDER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        private boolean reading = true;

        @Override
        public void run() {
            if (reading) {
                startPlay();
                read();
            }
        }

        private void startPlay() {
            streamTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    AudioUtils.RECORDER_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize,
                    AudioTrack.MODE_STREAM);

            enableEarpiece();
            streamTrack.play();
        }

        void read() {
            try {
                byte[] message = new byte[1024];

                while (true) {
                    // listen for senz
                    DatagramPacket receivePacket = new DatagramPacket(message, message.length);
                    socket.receive(receivePacket);
                    String msg = new String(message, 0, receivePacket.getLength());

                    Log.d("TAG", "Stream received: " + msg);

                    // parser and obtain audio data
                    // play it
                    if (!msg.isEmpty()) {
                        Senz senz = SenzParser.parse(msg);
                        if (senz.getAttributes().containsKey("mic")) {
                            String data = senz.getAttributes().get("mic");
                            senzBuffer.put(Base64.decode(data, Base64.DEFAULT));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void shutDown() {
            reading = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                streamTrack.pause();
                streamTrack.flush();
            } else {
                streamTrack.stop();
            }

            // close cons
            if (socket != null) {
                socket.close();
            }
        }
    }

    private void enableEarpiece() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.STREAM_VOICE_CALL);
        audioManager.setSpeakerphoneOn(false);
    }

}
