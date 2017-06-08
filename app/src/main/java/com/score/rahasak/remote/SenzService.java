package com.score.rahasak.remote;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
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
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.NotificationUtils;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.rahasak.utils.SenzParser;
import com.score.rahasak.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
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

    private static final String SENZ_HOST = "senz.rahasak.com";
    public static final int SENZ_PORT = 7070;

    public static final String STREAM_HOST = "senz.rahasak.com";
    public static final int STREAM_PORT = 9090;

    // senz socket
    private Socket socket;
    private DataInputStream inStream;
    private DataOutputStream outStream;

    // status of the online/offline
    private boolean senzCommRunning;
    private boolean connectedSwitch;

    // keep retry count
    private static int MAX_RETRY_COUNT = 3;
    private static int RETRY_COUNT = 0;

    // wake lock to keep
    private PowerManager powerManager;
    private PowerManager.WakeLock senzWakeLock;

    // stubs that service expose(defines in ISenzService.aidl)
    private final ISenzService.Stub stubs = new ISenzService.Stub() {
        @Override
        public void send(Senz senz) throws RemoteException {
            Log.d(TAG, "Senz service call with senz " + senz.getId());
            writeSenz(senz);
        }

        @Override
        public void sendStream(List<Senz> senzList) throws RemoteException {
            writeSenzes(senzList);
        }
    };

    private BroadcastReceiver smsRequestAcceptReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationUtils.cancelNotification(NotificationUtils.SMS_NOTIFICATION_ID, SenzService.this);

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
            NotificationUtils.cancelNotification(NotificationUtils.SMS_NOTIFICATION_ID, SenzService.this);
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

        Log.d(TAG, "onCreate ---");

        registerReceivers();
        initWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d(TAG, "onStartCommand --- ");

        initSenzComm();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy --- ");

        unRegisterReceivers();

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

    private void unRegisterReceivers() {
        // un register receivers
        unregisterReceiver(smsRequestAcceptReceiver);
        unregisterReceiver(smsRequestRejectReceiver);
        unregisterReceiver(smsRequestConfirmReceiver);
    }

    private void initWakeLock() {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        senzWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SenzWakeLock");
    }

    private void initSenzComm() {
        if (NetworkUtil.isAvailableNetwork(this)) {
            if (!connectedSwitch) {
                Log.d(TAG, "Not connectedSwitch, check to start senzcomm");
                if (!senzCommRunning) {
                    senzCommRunning = true;
                    new SenzComm().execute();
                } else {
                    Log.d(TAG, "Already running senzcomm exists..");
                }
            } else {
                Log.d(TAG, "Already connectedSwitch");
                ping();
            }
        } else {
            Log.d(TAG, "No network to init senzcomm");
        }
    }

    private void initSoc() throws IOException {
        Log.d(TAG, "Init socket");
        socket = new Socket(InetAddress.getByName(SENZ_HOST), SENZ_PORT);

        inStream = new DataInputStream(socket.getInputStream());
        outStream = new DataOutputStream(socket.getOutputStream());

        connectedSwitch = true;
        RETRY_COUNT = 0;
    }

    private void clrSoc() throws IOException {
        Log.d(TAG, "Reset socket");
        connectedSwitch = false;

        if (socket != null) {
            socket.close();
            inStream.close();
            outStream.close();
        }
    }

    private void initReader() throws IOException {
        Log.d(TAG, "Init reader");

        StringBuilder builder = new StringBuilder();
        int z;
        char c;
        while ((z = inStream.read()) != -1) {
            // obtain wake lock
            if (senzWakeLock != null) {
                if (!senzWakeLock.isHeld())
                    senzWakeLock.acquire();
            }

            c = (char) z;
            if (c == ';') {
                String senz = builder.toString();
                builder = new StringBuilder();

                if (senz.equalsIgnoreCase("TIK")) {
                    // send tuk
                    write("TUK");
                } else {
                    Log.d(TAG, "Senz received " + senz);
                    SenzHandler.getInstance().handle(senz, SenzService.this);
                }

                // release wake lock
                if (senzWakeLock != null) {
                    if (senzWakeLock.isHeld())
                        senzWakeLock.release();
                }
            } else {
                builder.append(c);
            }
        }
    }

    private void reconnect() {
        final int delay;

        // retry
        RETRY_COUNT++;
        if (RETRY_COUNT <= MAX_RETRY_COUNT) {
            switch (RETRY_COUNT) {
                case 1:
                    delay = 1000;
                    break;
                case 2:
                    delay = 3000;
                    break;
                case 3:
                    delay = 5000;
                    break;
                default:
                    delay = 1000;
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Retry after " + delay + " seconds");
                    initSenzComm();
                }
            }, delay);
        }
    }

    private void ping() {
        Senz senz = SenzUtils.getPingSenz(SenzService.this);
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

    void writeSenz(final Senz senz) {
        new Thread(new Runnable() {
            public void run() {
                // sign and write senz
                try {
                    PrivateKey privateKey = CryptoUtils.getPrivateKey(SenzService.this);

                    // set sender
                    senz.setSender(PreferenceUtils.getUser(getBaseContext()));

                    // get digital signature of the senz
                    String senzPayload = SenzParser.getSenzPayload(senz);
                    String signature;
                    if (senz.getSenzType() == SenzTypeEnum.STREAM) {
                        signature = "SIG";
                    } else {
                        signature = CryptoUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);
                    }
                    String message = SenzParser.getSenzMessage(senzPayload, signature);
                    Log.d(TAG, "Senz to be send: " + message);

                    //  sends the message to the server
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
                        String senzSignature;
                        if (senz.getAttributes().containsKey("stream")) {
                            senzSignature = CryptoUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);
                        } else {
                            senzSignature = "SIGNATURE";
                        }
                        String message = SenzParser.getSenzMessage(senzPayload, senzSignature);

                        Log.d(TAG, "Senz to be send: " + message);

                        //  sends the message to the server
                        write(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void write(String msg) throws IOException {
        //  sends the message to the server
        if (connectedSwitch) {
            outStream.writeBytes(msg + ";");
            outStream.flush();
        } else {
            Log.e(TAG, "Socket disconnected");
        }
    }

    class SenzComm extends AsyncTask<String, String, Integer> {
        @Override
        protected Integer doInBackground(String[] params) {
            if (!connectedSwitch) {
                Log.d(TAG, "Not online, so init comm");
                try {
                    initSoc();
                    ping();
                    initReader();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Connected, so send ping");
                ping();
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer status) {
            Log.e(TAG, "Stop SenzComm");
            senzCommRunning = false;

            try {
                clrSoc();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // reconnect
            reconnect();
        }
    }

}


