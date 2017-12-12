package com.score.rahasak.utils;

import com.score.rahasak.remote.SenzService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Recorder {

    public native void nativeStart();

    public native int nativeStartRecord(short[] in);

    public native int nativeStartPlay(short[] out);

    // we are listing for UDP socket
    private InetAddress address;
    private DatagramSocket recvSoc;
    private DatagramSocket sendSoc;

    static {
        System.loadLibrary("senz");
    }

    public void start() {
        nativeStart();
    }

    private void initUdpSoc() {
        try {
            if (sendSoc == null) {
                sendSoc = new DatagramSocket();
            }

            if (recvSoc == null) {
                recvSoc = new DatagramSocket();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void initUdpConn() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // connect
                    if (address == null)
                        address = InetAddress.getByName(SenzService.SENZ_HOST);

                    // send O message
                    String oMsg = SenzUtils.getOStreamMsg("eranga", "lkmal");
                    DatagramPacket oPacket = new DatagramPacket(oMsg.getBytes(), oMsg.length(), address, SenzService.STREAM_PORT);
                    sendSoc.send(oPacket);

                    // send N message
                    String nMsg = SenzUtils.getNStreamMsg("eranga", "lakmal");
                    DatagramPacket nPacket = new DatagramPacket(nMsg.getBytes(), nMsg.length(), address, SenzService.STREAM_PORT);
                    recvSoc.send(nPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void startRecord() {
        new Thread(new Runnable() {
            public void run() {
                short[] in = new short[160];
                while (true) {
                    nativeStartRecord(in);

                    // send in
                }
            }
        }).start();
    }

    private void startPlay() {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    //nativeStartPlay();
                }
            }
        }).start();
    }

    private void sendStream(String senz) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(senz.getBytes(), senz.length(), address, SenzService.STREAM_PORT);
        sendSoc.send(sendPacket);
    }

}
