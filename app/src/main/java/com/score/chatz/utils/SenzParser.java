package com.score.chatz.utils;

import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

/**
 * Created by eranga on 8/27/15.
 */
public class SenzParser {

    public static Senz parse(String senzMessage) {
        // init sez with
        Senz senz = new Senz();
        senz.setAttributes(new HashMap<String, String>());

        // parse senz
        String[] tokens = senzMessage.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (i == 0) {
                // query type at first (PING, SHARE, GET, DATA)
                senz.setSenzType(SenzTypeEnum.valueOf(token.toUpperCase()));

                // if query type is PING we breakup from here :)
                if (senz.getSenzType() == SenzTypeEnum.PING) return senz;
            } else if (i == tokens.length - 1) {
                // signature at the end
                senz.setSignature(token);
            } else if (tokens[i].startsWith("@")) {
                // @0775432012
                senz.setReceiver(new User("", token.substring(1)));
            } else if (token.startsWith("^")) {
                // ^mysensors, ^0775432015
                senz.setSender(new User("", token.substring(1)));
            } else if (token.startsWith("#")) {
                // we remove # from token and store as a key
                String key = token.substring(1);
                String nextToken = tokens[i + 1];

                if (nextToken.startsWith("#") | nextToken.startsWith("@")) {
                    // #lat #lon
                    // #lat @user
                    senz.getAttributes().put(key, "");
                } else {
                    // #lat 3.24 #lon 3.23
                    senz.getAttributes().put(key, tokens[i + 1]);
                    i++;
                }
            }
        }

//        System.out.println(senz.getSender().getUsername());
//        System.out.println(senz.getReceiver().getUsername());
//        System.out.println(senz.getSenzType());
//        System.out.println(senz.getSignature());
//        System.out.println(senz.getAttributes().entrySet());

        return senz;
    }

    public static String getSenzPayload(Senz senz) {
        // add senz type to payload
        String payload = senz.getSenzType().toString();

        // add attributes to payload
        for (String key : senz.getAttributes().keySet()) {
            if (key.equalsIgnoreCase(senz.getAttributes().get(key))) {
                // GET or SHARE query
                // param and value equal since no value to store (SHARE #lat #lon)
                payload = payload.concat(" ").concat("#").concat(key).concat(" ").concat(senz.getAttributes().get(key));
            } else {
                // DATA query
                payload = payload.concat(" ").concat("#").concat(key).concat(" ").concat(senz.getAttributes().get(key));
            }
        }

        // add sender and receiver
        payload = payload.concat(" ").concat("@").concat(senz.getReceiver().getUsername());
        payload = payload.concat(" ").concat("^").concat(senz.getSender().getUsername());

        return payload;
    }

    public static String getSenzMessage(String payload, String signature) {
        String senzMessage = payload + " " + signature;

        System.out.println((senzMessage.replaceAll("\n", "").replaceAll("\r", "")).getBytes().length);

        return senzMessage.replaceAll("\n", "").replaceAll("\r", "");
    }

//    public static void main(String args[]) {
//        String senzMessage3 = "DATA " +
//                "#msg UserCreated " +
//                "#pubkey sd23453451234sfsdfd==  " +
//                "#time 1441806897.71 " +
//                "#msg1 #msg2 rtt " +
//                "@senzswitch " +
//                "^era " +
//                "v50I88VzgvBvubCjGitTMO9";
//
//        parse(senzMessage3);
//    }
}
