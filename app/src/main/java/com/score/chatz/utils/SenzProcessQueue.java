package com.score.chatz.utils;

import com.score.senzc.pojos.Senz;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by lakmal.caldera on 9/10/2016.
 */
public class SenzProcessQueue {
    private static final String TAG = SenzProcessQueue.class.getName();
    private static HashMap<String, Senz> waitingSenz = new HashMap<String, Senz>();
    public static void getNext(){
        //return waitingSenz.pop();
    }

    public static void addToQueue(String key, Senz senz){
        waitingSenz.put(key, senz);
    }
}
