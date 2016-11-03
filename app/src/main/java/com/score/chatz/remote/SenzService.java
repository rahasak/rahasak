package com.score.chatz.remote;

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

import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.ui.AddUserActivity;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.RSAUtils;
import com.score.chatz.utils.SenzParser;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

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
import java.util.HashMap;
import java.util.List;

public class SenzService extends Service {

    private static final String TAG = SenzService.class.getName();

    // socket host, port
    //public static final String SENZ_HOST = "10.2.2.49";
    //public static final String SENZ_HOST = "udp.mysensors.info";

    private static final String SENZ_HOST = "52.77.228.195";

    //private static final String SENZ_HOST = "connect.rahasak.com";

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

            //should check null because in air plan mode it will be null
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                Log.d(TAG, "Network status changed[online]");

                // init comm
                new SenzComm().execute();
            } else {
                // means disconnected
                resetSoc();
            }
        }
    };

    // broadcst receiver to automatically add user when valid sms is received
    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Add user sms received. init add user.");
            String usernameToAdd = intent.getStringExtra("USERNAME").trim();
            if(!new SenzorsDbSource(SenzService.this).isAddedUser(usernameToAdd) && !isCurrentUser(usernameToAdd)) {
                share(usernameToAdd);
            }
        }
    };

    /**
     * Share current sensor
     * Need to send share query to server via web socket
     */
    private void share(String username) {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("msg", "");
        senzAttributes.put("status", "");

        Long timestamp = (System.currentTimeMillis() / 1000);
        senzAttributes.put("time", timestamp.toString());
        senzAttributes.put("uid", SenzUtils.getUid(this, timestamp.toString()));

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.SHARE;
        User receiver = new User("", username.trim());
        Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

        // send to service
        try {
            apiEndPoints.send(senz);
        }catch (RemoteException ex){
            ex.printStackTrace();
        }
    }

    // API end point of this service, we expose the endpoints define in ISenzService.aidl
    private final ISenzService.Stub apiEndPoints = new ISenzService.Stub() {
        @Override
        public String getUser() throws RemoteException {
            return null;
        }

        @Override
        public void send(Senz senz) throws RemoteException {
            Log.d(TAG, "Senz service call with senz " + senz.getId());
            //SenzTracker.getInstance(SenzService.this).startSenzTrack(senz);
            writeSenz(senz);
        }

        @Override
        public void sendInOrder(List<Senz> senzList) throws RemoteException {
            writeSenzList(senzList);
        }

        @Override
        public void sendFromUri(String uri, Senz senz, String uid) throws RemoteException {
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return apiEndPoints;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate called");

        registerReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d(TAG, "onStartCommand executed");

        // init comm
        if (NetworkUtil.isAvailableNetwork(this)) {
            new SenzComm().execute();
        } else {
            Log.e(TAG, "no network to start senzcomm");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");

        unRegisterReceivers();

        // restart service again
        // its done via broadcast receiver
        Intent intent = new Intent("senz.action.SENZ_RESTART");
        sendBroadcast(intent);
    }

    private void registerReceivers() {
        // Register network status receiver
        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStatusReceiver, networkFilter);
        registerReceiver(smsReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SMS_RECEIVED));
    }

    private void unRegisterReceivers() {
        // un register receivers
        unregisterReceiver(networkStatusReceiver);
        unregisterReceiver(smsReceiver);
    }

    private void initSoc() {
        Log.d(TAG, "Reset socket");

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
        Log.d(TAG, "Reset socket");

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
        Log.d(TAG, "Init reader");

        try {
            String line;
            while (isOnline && (line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String senz = line.replaceAll("\n", "").replaceAll("\r", "").trim();

                    // handle senz
                    if (!senz.equalsIgnoreCase("TAK")) {
                        Log.d(TAG, "Senz received " + senz);
                        SenHandler.getInstance().handle(senz, SenzService.this);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPing() {
        Senz senz = SenzUtils.getPingSenz(SenzService.this);
        if (senz != null) writeSenz(senz);
    }

    public void writeSenz(final Senz senz) {
        Log.d(TAG, "Send PING");

        new Thread(new Runnable() {
            public void run() {
                // sign and write senz
                try {
                    PrivateKey privateKey = RSAUtils.getPrivateKey(SenzService.this);

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

    public void writeSenzList(final List<Senz> senzList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PrivateKey privateKey = RSAUtils.getPrivateKey(SenzService.this);

                    //for (Senz senz : senzList) {
                    for (int i = 0; i < senzList.size(); i++) {
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

                        Log.d(TAG, "Senz to be send: " + message);

                        //  sends the message to the server
                        if (isOnline) {
                            writer.println(message);
                            writer.flush();
                        } else {
                            Log.e(TAG, "Socket disconnected");
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
                Log.d(TAG, "Not online, so init comm");
                initSoc();
                sendPing();
                initReader();
            } else {
                Log.d(TAG, "Online, so send ping");
                sendPing();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            Log.e(TAG, "Stop SenzComm");
            resetSoc();
        }
    }

    private boolean isCurrentUser(String username){
        String currentUser = "";
        try{
            currentUser = PreferenceUtils.getUser(SenzService.this).getUsername();
        }catch (NoUserException e){
            e.printStackTrace();
        }
        if(currentUser.toLowerCase().equalsIgnoreCase(username)){
            return true;
        }else{
            return false;
        }
    }

}


