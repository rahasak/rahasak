package com.score.chatz.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.handlers.SenzHandler;
import com.score.chatz.handlers.SenzStatusTracker;
import com.score.chatz.receivers.AlarmReceiver;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.RSAUtils;
import com.score.chatz.utils.SenzParser;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;


public class RemoteSenzService extends Service {

    private static final String TAG = RemoteSenzService.class.getName();

    // socket host, port
    //public static final String SENZ_HOST = "10.2.2.49";
    //public static final String SENZ_HOST = "udp.mysensors.info";
    private static final String SENZ_HOST = "52.77.228.195";

    public static final int SENZ_PORT = 7070;

    // senz socket
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // status of the online/offline
    private boolean isOnline;

    // broadcast receiver to check network status changes
    private final BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();

            Log.d(TAG, "Network status changed");

            //should check null because in air plan mode it will be null
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                // init comm
                new SenzComm().execute();
            } else {
                // means disconnected
                resetSoc();
            }
        }
    };

    // broadcast receiver to send ping message
    private final BroadcastReceiver pingAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Ping alarm received in senz service");

            // ping senz
            sendPing();
        }
    };

    // API end point of this service, we expose the endpoints define in ISenzService.aidl
    private final ISenzService.Stub apiEndPoints = new ISenzService.Stub() {
        @Override
        public String getUser() throws RemoteException {
            return null;
        }

        @Override
        public void send(Senz senz) throws RemoteException {
            Log.d(TAG, "Senz service call with senz " + senz.getId());
            SenzStatusTracker.addSenz(SenzStatusTracker.addUidToSenz(senz), getApplicationContext());
            writeSenz(senz);

        }

        @Override
        public void sendInOrder(List<Senz> senzList) throws RemoteException {
            writeSenzList(senzList);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return apiEndPoints;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");

        registerReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand executed");

        // init comm
        new SenzComm().execute();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        unRegisterReceivers();

        // restart service again
        // its done via broadcast receiver
        Intent intent = new Intent("senz.action.RESTART");
        sendBroadcast(intent);
    }

    private void registerReceivers() {
        // Register network status receiver
        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStatusReceiver, networkFilter);

        // register local ping alarm receiver
        IntentFilter alarmFilter = new IntentFilter();
        alarmFilter.addAction("PING_ALARM");
        registerReceiver(pingAlarmReceiver, alarmFilter);
    }

    private void unRegisterReceivers() {
        // un register receivers
        unregisterReceiver(networkStatusReceiver);
        unregisterReceiver(pingAlarmReceiver);
    }

    private void initPing() {
        // register ping alarm receiver
        Intent intentAlarm = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * 10, 1000 * 60 * 10, pendingIntent);
    }

    private void initSoc() {
        try {
            socket = new Socket(InetAddress.getByName(SENZ_HOST), SENZ_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            isOnline = true;
        } catch (IOException e) {
            e.printStackTrace();

            isOnline = true;
        }
    }

    private void resetSoc() {
        if (isOnline) {
            isOnline = false;

            try {
                if (socket != null) {
                    socket.close();
                    reader.close();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initReader() {
        try {
            String line;
            while (isOnline && (line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String senz = line.replaceAll("\n", "").replaceAll("\r", "");

                    Log.d(TAG, "Senz received " + senz);

                    // handle senz
                    if (senz.trim().equalsIgnoreCase("PING")) {
                        writer.println("PONG");
                        writer.flush();
                    } else {
                        // handle senz
                        SenzHandler.getInstance(RemoteSenzService.this).handleSenz(senz);
                    }
                } else {
                    Log.e(TAG, "empty senz received");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPing() {
        Senz senz = SenzUtils.getPingSenz(RemoteSenzService.this);
        if (senz != null) writeSenz(senz);
    }

    private void writeSenz(final Senz senz) {
        new Thread(new Runnable() {
            public void run() {
                // sign and write senz
                try {
                    PrivateKey privateKey = RSAUtils.getPrivateKey(RemoteSenzService.this);

                    // if sender not already set find user(sender) and set it to senz first
                    if (senz.getSender() == null || senz.getSender().toString().isEmpty())
                        senz.setSender(PreferenceUtils.getUser(getBaseContext()));

                    // get digital signature of the senz
                    String senzPayload = SenzParser.getSenzPayload(senz);
                    String senzSignature = RSAUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);
                    String message = SenzParser.getSenzMessage(senzPayload, senzSignature);
                    Log.d(TAG, "Senz to be send: " + message);

                    //  sends the message to the server
                    if (isOnline) {
                        writer.println(message);
                        writer.flush();
                    } else {
                        Log.e(TAG, "Socket disconnected");
                    }
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | NoUserException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void writeSenzList(final List<Senz> senzList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PrivateKey privateKey = RSAUtils.getPrivateKey(RemoteSenzService.this);

                    //for (Senz senz : senzList) {
                        for(int i = 0 ; i < senzList.size(); i++){
                            Senz senz = senzList.get(i);
                        // if sender not already set find user(sender) and set it to senz first
                        if (senz.getSender() == null || senz.getSender().toString().isEmpty())
                            senz.setSender(PreferenceUtils.getUser(getBaseContext()));

                        // get digital signature of the senz
                        String senzPayload = SenzParser.getSenzPayload(senz);
                        String senzSignature;
                        if (senz.getAttributes().containsKey("stream")) {
                            senzSignature = RSAUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);
                        } else {
                            senzSignature = "SIGNATURE";
                        }
                        String message = SenzParser.getSenzMessage(senzPayload, senzSignature);


                            SenzStatusTracker.addSenz(SenzStatusTracker.addUidToSenz(senz), getApplicationContext());

                        Log.d(TAG, "Senz to be send: " + message);

                        //  sends the message to the server
                        if (isOnline) {
                            if(i > (senzList.size() - 2)){
                                try {
                                    Thread.sleep(2000);
                                }catch (InterruptedException ex){
                                    Log.e(TAG, "Exception - " + ex);
                                }
                            }
                            writer.println(message);
                            writer.flush();
                        } else {
                            Log.e(TAG, "Socket disconnected");
                            resetSoc();
                            initSoc();
                        }
                    }
                } catch (NoSuchAlgorithmException | NoUserException | InvalidKeySpecException | SignatureException | InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    class SenzComm extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            if (!isOnline) {
                initSoc();
                initPing();
                sendPing();
                initReader();
            } else {
                sendPing();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            Log.e(TAG, "Stop SenzComm");
            resetSoc();
            new SenzComm().execute();
        }
    }

}


