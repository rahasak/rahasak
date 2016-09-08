package com.score.chatz.viewholders;

import android.os.Parcel;

import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

/**
 * Created by Lakmal on 7/3/16.
 * ViewHolder for The recycler view of the chatActivity
 *
 */
public class Message {

    private String text;
    private User sender;
    private boolean mine;
    final String myMessageBg = "drawable/my_message_box";
    final String notMyMessageBg = "drawable/not_my_message_box";

    public Message(String text, User sender, boolean mine) {
        this.text = text;
        this.sender = sender;
        this.mine = mine;
    }

    public String getText() {
        return text;
    }

    public User getSender() {
        return sender;
    }

    public boolean getMine() {
        return mine;
    }


    /*
     * Following getting feed the alignment to the view
     */
    public boolean getAlignLeft(){
        if(mine == true){
            return true;
        }else{
            return false;
        }
    }

    public boolean getAligRight(){
        if(mine == true){
            return false;
        }else{
            return true;
        }
    }

    public String getBackgroundImage(){
        if(mine == true) {
            return myMessageBg;
        }else{
            return notMyMessageBg;
        }
    }

}
