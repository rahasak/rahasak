package com.score.rahasak.remote;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.exceptions.NoUserException;
import com.score.rahasak.utils.CryptoUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.rahasak.utils.SenzParser;
import com.score.rahasak.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.List;

public class SenzService extends Service {

    private static final String TAG = SenzService.class.getName();

    //public static final String SENZ_HOST = "senz.rahasak.com";
    public static final String SENZ_HOST = "52.77.242.96";
    //public static final String SENZ_HOST = "10.2.2.1";
    public static final int SENZ_PORT = 3000;
    public static final int STREAM_PORT = 9090;

    // wake lock to keep
    private PowerManager powerManager;
    private PowerManager.WakeLock senzWakeLock;

    // senz socket
    private Socket socket;
    private DataInputStream inStream;
    private DataOutputStream outStream;

    // comm running
    private boolean running;

    // stubs that service expose(defines in ISenzService.aidl)
    private final ISenzService.Stub stubs = new ISenzService.Stub() {
        @Override
        public void send(Senz senz) throws RemoteException {
            writeSenz(senz);
        }

        @Override
        public void sendInOrder(List<Senz> senzList) throws RemoteException {
            writeSenzes(senzList);
        }

        @Override
        public void sendStream(Senz senz) throws RemoteException {
            writeStream(senz);
        }
    };

    private BroadcastReceiver smsRequestAcceptReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SenzNotificationManager.getInstance(SenzService.this).cancelNotification(SenzNotificationManager.SMS_NOTIFICATION_ID, SenzService.this);

            if (NetworkUtil.isAvailableNetwork(context)) {
                String phone = intent.getStringExtra("PHONE").trim();
                String username = intent.getStringExtra("USERNAME").trim();
                try {
                    sendSMS(phone, "#Rahasak #confirm\nI have confirmed your request. #username " + PreferenceUtils.getUser(SenzService.this).getUsername() + " #code 31e3e");

                    // get pubkey
                    getPubKey(username);
                } catch (NoUserException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(context, "No network connection", Toast.LENGTH_LONG).show();
            }
        }
    };

    private BroadcastReceiver smsRequestRejectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SenzNotificationManager.getInstance(SenzService.this).cancelNotification(SenzNotificationManager.SMS_NOTIFICATION_ID, SenzService.this);
        }
    };

    private BroadcastReceiver smsRequestConfirmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String username = intent.getStringExtra("USERNAME").trim();

            // get pubkey
            getPubKey(username);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return stubs;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registerReceivers();
        initWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (running) {
            // tuk
            tuk();
        } else {
            // reg
            new SenzCom().start();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceivers();

        // restart service again
        // its done via broadcast receiver
        Intent intent = new Intent(IntentProvider.ACTION_RESTART);
        sendBroadcast(intent);
    }

    private void registerReceivers() {
        registerReceiver(smsRequestAcceptReceiver, IntentProvider.getIntentFilter(IntentType.SMS_REQUEST_ACCEPT));
        registerReceiver(smsRequestRejectReceiver, IntentProvider.getIntentFilter(IntentType.SMS_REQUEST_REJECT));
        registerReceiver(smsRequestConfirmReceiver, IntentProvider.getIntentFilter(IntentType.SMS_REQUEST_CONFIRM));
    }

    private void unregisterReceivers() {
        // un register receivers
        unregisterReceiver(smsRequestAcceptReceiver);
        unregisterReceiver(smsRequestRejectReceiver);
        unregisterReceiver(smsRequestConfirmReceiver);
    }

    private void initWakeLock() {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        senzWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SenzWakeLock");
    }

    private void ping() {
        Senz senz = SenzUtils.getRegSenz(SenzService.this);
        if (senz != null) writeSenz(senz);
    }

    private void getPubKey(String username) {
        Senz senz = SenzUtils.getPubkeySenz(this, username);
        writeSenz(senz);
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    void tuk() {
        new Thread(new Runnable() {
            public void run() {
                write("TUK");
            }
        }).start();
    }

    void writeSenz(final Senz senz) {
        new Thread(new Runnable() {
            public void run() {
                // sign and write senz
                try {
                    PrivateKey privateKey = CryptoUtils.getPrivateKey(SenzService.this);

                    // if sender not already set find user(sender) and set it to senz first
                    if (senz.getSender() == null || senz.getSender().toString().isEmpty())
                        senz.setSender(PreferenceUtils.getUser(getBaseContext()));

                    // get digital signature of the senz
                    String senzPayload = SenzParser.getSenzPayload(senz);
                    String signature = senz.getAttributes().containsKey("cam") ? "_SIG" : CryptoUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);

                    //  sends the message to the server
                    String message = SenzParser.getSenzMessage(senzPayload, signature);
                    write(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void writeSenzes(final List<Senz> senzList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PrivateKey privateKey = CryptoUtils.getPrivateKey(SenzService.this);
                    User sender = PreferenceUtils.getUser(SenzService.this);

                    for (Senz senz : senzList) {
                        senz.setSender(sender);

                        // get digital signature of the senz
                        String senzPayload = SenzParser.getSenzPayload(senz);
                        String signature = senz.getAttributes().containsKey("cam") ? "_SIG" : CryptoUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);

                        // sends the message to the server
                        String message = SenzParser.getSenzMessage(senzPayload, signature);
                        write(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void writeStream(final Senz senz) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // set sender
                    if (senz.getSender() == null || senz.getSender().toString().isEmpty())
                        senz.setSender(PreferenceUtils.getUser(getBaseContext()));

                    // send img
                    if (senz.getAttributes().containsKey("cam")) {
                        for (String packet : ImageUtils.splitImg(senz.getAttributes().get("img"), 1024))
                            write(packet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void write(String msg) {
        try {
            //  sends the message to the server
            if (socket != null) {
                outStream.writeBytes(msg + ";");
                outStream.flush();
            } else {
                Log.e(TAG, "Socket disconnected");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class SenzCom extends Thread {

        @Override
        public void run() {
            running = true;

            try {
                initCom();
                ping();
                readCom();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeCom();
            }
        }

        private void initCom() throws IOException {
            Log.d(TAG, "init socket");

            socket = new Socket(InetAddress.getByName(SENZ_HOST), SENZ_PORT);
            inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
        }

        private void readCom() throws IOException {
            Log.d(TAG, "read socket");

            StringBuilder builder = new StringBuilder();
            int z;
            char c;
            while ((z = inStream.read()) != -1) {
                // obtain wake lock
                if (senzWakeLock != null && !senzWakeLock.isHeld()) senzWakeLock.acquire();

                c = (char) z;
                if (c == ';') {
                    String senz = builder.toString();
                    if (!senz.isEmpty()) {
                        Log.d(TAG, "Senz received " + senz);

                        builder = new StringBuilder();
                        SenzHandler.getInstance().handle(senz, SenzService.this);

                        // release wake lock
                        if (senzWakeLock != null && senzWakeLock.isHeld()) senzWakeLock.release();
                    }
                } else {
                    builder.append(c);
                }
            }
        }

        private void closeCom() {
            running = false;

            try {
                if (socket != null) {
                    socket.close();
                    inStream.close();
                    outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}


