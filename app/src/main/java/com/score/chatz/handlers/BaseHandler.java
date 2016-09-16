package com.score.chatz.handlers;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import com.score.chatz.services.SenzServiceConnection;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.util.HashMap;

/**
 * Created by Lakmal on 9/4/16.
 */
public class BaseHandler {
    private static final String TAG = BaseHandler.class.getName();

    protected static void broadcastDataSenz(Senz senz, Context context){
        Intent intent = IntentProvider.getDataSenzIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    protected static void broadcastUpdateSenz(Senz senz, Context context){
        Intent intent = IntentProvider.getUpdateSenzIntent();
        intent.putExtra("SENZ", senz);
        context.sendBroadcast(intent);
    }

    protected String[] split(String src, int len) {
        String[] result = new String[(int) Math.ceil((double) src.length() / (double) len)];
        for (int i = 0; i < result.length; i++)
            result[i] = src.substring(i * len, Math.min(src.length(), (i + 1) * len));
        return result;
    }

    public static void sendBusyNotification(final Senz senz, Context context){
        //Get servicve connection
        final SenzServiceConnection serviceConnection = SenzHandler.getInstance(context).getServiceConnection();

        serviceConnection.executeAfterServiceConnected(new Runnable() {
            @Override
            public void run() {
                // service instance
                ISenzService senzService = serviceConnection.getInterface();
                try {
                    // create senz attributes
                    HashMap<String, String> senzAttributes = new HashMap<>();
                    senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

                    //This is unique identifier for each message
                    senzAttributes.put("msg", "userBusy");
                    // new senz
                    String id = "_ID";
                    String signature = "_SIGNATURE";
                    SenzTypeEnum senzType = SenzTypeEnum.DATA;
                    Senz _senz = new Senz(id, signature, senzType, senz.getReceiver(), senz.getSender(), senzAttributes);

                    senzService.send(_senz);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
    }

}
