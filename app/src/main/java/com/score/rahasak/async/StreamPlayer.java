package com.score.rahasak.async;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.score.rahasak.utils.AudioUtils;
import com.score.rahasak.utils.CMG711;
import com.score.rahasak.utils.SenzParser;
import com.score.senzc.pojos.Senz;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class StreamPlayer {

    private Context context;
    private Player player;

    private DatagramSocket socket;

    public StreamPlayer(Context context, DatagramSocket socket) {
        this.context = context;
        this.socket = socket;

        player = new Player();
    }

    public void play() {
        player.start();
    }

    public void stop() {
        player.shutDown();
    }

    private class Player extends Thread {
        private CMG711 decoder = new CMG711();
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
            try {
                byte[] message = new byte[1024];
                byte[] inBuffer = new byte[1024];

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

                            byte[] stream = Base64.decode(data, Base64.DEFAULT);

                            // decode
                            decoder.decode(stream, 0, stream.length, inBuffer);
                            streamTrack.write(inBuffer, 0, inBuffer.length);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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

    private void enableEarpiece() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
    }

}
